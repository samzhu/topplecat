package io.github.samzhu.topplecat.core;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/** Custody service for the complete {@code src/hiddenTest} source set. */
public final class EscrowService {
    private static final Path ESCROW_ROOT = Path.of(".topplecat", "escrow");

    private final Runnable beforeActivation;

    public EscrowService() {
        this(() -> {
        });
    }

    EscrowService(Runnable beforeActivation) {
        this.beforeActivation = Objects.requireNonNull(beforeActivation);
    }

    public EscrowManifest hide(Path projectRoot, Path hiddenSourceRoot) {
        Path root = normalizedRoot(projectRoot);
        Path hidden = hiddenSourceRoot.toAbsolutePath().normalize();
        requireInside(root, hidden);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root)) {
            Path manifestPath = manifestPath(root);
            if (!Files.exists(manifestPath)) {
                return hideInitialSource(root, hidden, manifestPath);
            }

            EscrowManifest manifest = readManifest(manifestPath);
            validateStoredFiles(root, manifest);
            if (manifest.state() == EscrowState.HIDDEN) {
                if (!hasFiles(hidden)) {
                    return manifest;
                }
                if (sourceIsManifestSubset(root, hidden, manifest)) {
                    deleteTree(hidden);
                    return manifest;
                }
                throw new ToppleCatException("Escrow already hides reviewer source, but src/hiddenTest contains "
                        + "different files. Restore the escrowed source or move the new reviewer files before hiding.");
            }

            requireExactRestoredSource(root, hidden, manifest);
            deleteTree(hidden);
            return writeState(manifestPath, manifest, EscrowState.HIDDEN);
        }
    }

    public EscrowManifest restore(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root)) {
            Path manifestPath = manifestPath(root);
            if (!Files.exists(manifestPath)) {
                throw new ToppleCatException("No ToppleCat escrow manifest exists. Run toppleCatHide before restoring reviewer source.");
            }
            EscrowManifest manifest = readManifest(manifestPath);
            validateStoredFiles(root, manifest);
            Path hidden = root.resolve("src/hiddenTest");

            if (manifest.state() == EscrowState.RESTORED) {
                requireExactRestoredSource(root, hidden, manifest);
                return manifest;
            }
            if (!sourceIsManifestSubset(root, hidden, manifest)) {
                throw new ToppleCatException("Refusing to overwrite reviewer source because src/hiddenTest is not exactly "
                        + "the escrowed source.");
            }
            restoreEntries(root, manifest);
            return writeState(manifestPath, manifest, EscrowState.RESTORED);
        }
    }

    public EscrowManifest rehide(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root)) {
            Path manifestPath = manifestPath(root);
            if (!Files.exists(manifestPath)) {
                throw new ToppleCatException("No ToppleCat escrow manifest exists. Run toppleCatHide before re-hiding reviewer source.");
            }
            EscrowManifest manifest = readManifest(manifestPath);
            validateStoredFiles(root, manifest);
            Path hidden = root.resolve("src/hiddenTest");
            if (manifest.state() == EscrowState.HIDDEN) {
                if (sourceIsManifestSubset(root, hidden, manifest)) {
                    deleteTree(hidden);
                    return manifest;
                }
                throw new ToppleCatException("Reviewer source does not exactly match the escrow manifest. Restore the original "
                        + "source or move new reviewer files outside src/hiddenTest before hiding.");
            }
            requireExactRestoredSource(root, hidden, manifest);
            deleteTree(hidden);
            return writeState(manifestPath, manifest, EscrowState.HIDDEN);
        }
    }

    /** Explicitly replaces restored reviewer custody after the reviewer has checked and reviewed it. */
    public EscrowManifest update(Path projectRoot, Path hiddenSourceRoot) {
        Path root = normalizedRoot(projectRoot);
        Path hidden = hiddenSourceRoot.toAbsolutePath().normalize();
        requireInside(root, hidden);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root)) {
            return updateRestoredSource(root, hidden);
        }
    }

    public EscrowManifest manifest(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root)) {
            return readManifest(manifestPath(root));
        }
    }

    private EscrowManifest updateRestoredSource(Path root, Path hidden) {
        Path manifestPath = manifestPath(root);
        if (!Files.isRegularFile(manifestPath)) {
            throw new ToppleCatException("No ToppleCat escrow manifest exists. Run toppleCatHide before updating reviewer custody.");
        }
        EscrowManifest previous = readManifest(manifestPath);
        if (previous.state() != EscrowState.RESTORED) {
            throw new ToppleCatException("Escrow update requires restored reviewer source. Run toppleCatRestore, edit "
                    + "src/hiddenTest, review it, then run toppleCatUpdateEscrow.");
        }
        validateStoredFiles(root, previous);

        List<EscrowEntry> entries = inventory(root, hidden);
        EscrowManifest updated = new EscrowManifest(EscrowManifest.SCHEMA_VERSION, EscrowState.HIDDEN, entries);
        storeEntries(root, entries);
        validateStoredFiles(root, updated);

        String previousManifest = EscrowManifestJson.write(previous);
        String updatedManifest = EscrowManifestJson.write(updated);
        String previousDigest = Hashing.sha256(previousManifest.getBytes(StandardCharsets.UTF_8));
        String updatedDigest = Hashing.sha256(updatedManifest.getBytes(StandardCharsets.UTF_8));
        EscrowUpdateAudit audit = updateAudit(previous, updated, previousDigest, updatedDigest);

        Path escrow = root.resolve(ESCROW_ROOT);
        String revisionId = UUID.randomUUID().toString();
        Path pending = escrow.resolve("pending").resolve(revisionId);
        Path pendingManifest = pending.resolve("manifest.json");
        Path pendingAudit = pending.resolve("audit.json");
        writeManifest(pendingManifest, updated);
        writeAudit(pendingAudit, audit);
        if (!readManifest(pendingManifest).equals(updated) || !readAudit(pendingAudit).equals(audit)) {
            throw new ToppleCatException("Escrow update staging metadata did not validate.");
        }

        boolean sourceMoveStarted = false;
        Path stagedRevision = pending;
        try {
            Path stagedSource = pending.resolve("source");
            sourceMoveStarted = true;
            moveReviewerSource(hidden, stagedSource);
            if (!inventoryAsHiddenRoot(root, stagedSource, hidden).equals(updated.entries())) {
                throw new ToppleCatException("Escrow update staged reviewer source does not match the planned manifest.");
            }

            Path revision = escrow.resolve("revisions").resolve(updatedDigest + "-" + revisionId);
            try {
                Files.createDirectories(Objects.requireNonNull(revision.getParent()));
                moveAtomically(pending, revision);
            } catch (IOException exception) {
                throw new ToppleCatException("Cannot finalize staged escrow revision " + revision + ": "
                        + exception.getMessage(), exception);
            }
            stagedRevision = revision;

            Path history = escrow.resolve("history").resolve(previousDigest + ".json");
            try {
                Files.createDirectories(Objects.requireNonNull(history.getParent()));
                writeStringAtomically(history, previousManifest);
            } catch (IOException exception) {
                throw new ToppleCatException("Cannot preserve the previous escrow manifest " + history + ": "
                        + exception.getMessage(), exception);
            }

            beforeActivation.run();
            writeManifest(manifestPath, updated);
            return updated;
        } catch (RuntimeException exception) {
            if (!sourceMoveStarted) {
                throw exception;
            }
            throw recoverAfterFailedUpdate(root, hidden, manifestPath, previous, stagedRevision, revisionId, exception);
        }
    }

    private static ToppleCatException recoverAfterFailedUpdate(
            Path root,
            Path hidden,
            Path manifestPath,
            EscrowManifest previous,
            Path stagedRevision,
            String revisionId,
            RuntimeException cause
    ) {
        Path recovery = root.resolve(ESCROW_ROOT).resolve("recovery").resolve(revisionId);
        Path retained = stagedRevision;
        try {
            if (Files.exists(stagedRevision)) {
                Files.createDirectories(Objects.requireNonNull(recovery.getParent()));
                moveAtomically(stagedRevision, recovery);
                retained = recovery;
            }
        } catch (IOException | RuntimeException retentionFailure) {
            cause.addSuppressed(retentionFailure);
        }
        try {
            deleteTree(hidden);
            restoreEntries(root, previous);
            if (!sourceMatchesManifest(root, hidden, previous)) {
                throw new ToppleCatException("Recovered reviewer source does not match the previous escrow manifest.");
            }
            writeManifest(manifestPath, previous);
        } catch (RuntimeException recoveryFailure) {
            cause.addSuppressed(recoveryFailure);
            return new ToppleCatException("Escrow update did not activate. Modified reviewer source remains at " + retained
                    + "; recover it locally before retrying.", cause);
        }
        return new ToppleCatException("Escrow update did not activate. Previous reviewer source was restored and modified "
                + "reviewer source remains at " + retained + "; no update was activated.", cause);
    }

    private static EscrowManifest hideInitialSource(Path root, Path hidden, Path manifestPath) {
        List<EscrowEntry> entries = inventory(root, hidden);
        storeEntries(root, entries);

        EscrowManifest hiddenManifest = new EscrowManifest(EscrowManifest.SCHEMA_VERSION, EscrowState.HIDDEN, entries);
        writeManifest(manifestPath, hiddenManifest);
        deleteTree(hidden);
        return hiddenManifest;
    }

    private static void restoreEntries(Path root, EscrowManifest manifest) {
        for (EscrowEntry entry : manifest.entries()) {
            Path target = root.resolve(entry.path()).normalize();
            requireInside(root, target);
            Path stored = storedFile(root, entry.sha256());
            byte[] bytes = readBytes(stored);
            try {
                Files.createDirectories(Objects.requireNonNull(target.getParent()));
                if (Files.exists(target) && !Hashing.sha256(readBytes(target)).equals(entry.sha256())) {
                    throw new ToppleCatException("Refusing to overwrite reviewer source with different bytes: " + target);
                }
                if (!Files.exists(target)) {
                    writeBytesAtomically(target, bytes);
                }
            } catch (IOException exception) {
                throw new ToppleCatException("Cannot restore reviewer source " + target + ": " + exception.getMessage(), exception);
            }
        }
    }

    private static EscrowManifest writeState(Path manifestPath, EscrowManifest manifest, EscrowState state) {
        EscrowManifest updated = new EscrowManifest(EscrowManifest.SCHEMA_VERSION, state, manifest.entries());
        writeManifest(manifestPath, updated);
        return updated;
    }

    private static Path normalizedRoot(Path projectRoot) {
        return projectRoot.toAbsolutePath().normalize();
    }

    private static Path manifestPath(Path projectRoot) {
        return projectRoot.resolve(ESCROW_ROOT).resolve("manifest.json");
    }

    private static Path storedFile(Path projectRoot, String hash) {
        return projectRoot.resolve(ESCROW_ROOT).resolve("files").resolve(hash.substring(0, 2)).resolve(hash);
    }

    private static EscrowManifest readManifest(Path path) {
        try {
            return EscrowManifestJson.read(Files.readString(path));
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot read escrow manifest " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static void writeManifest(Path path, EscrowManifest manifest) {
        try {
            Files.createDirectories(Objects.requireNonNull(path.getParent()));
            writeStringAtomically(path, EscrowManifestJson.write(manifest));
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot write escrow manifest " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static void writeAudit(Path path, EscrowUpdateAudit audit) {
        try {
            Files.createDirectories(Objects.requireNonNull(path.getParent()));
            writeStringAtomically(path, EscrowUpdateAuditJson.write(audit));
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot write escrow update audit " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static EscrowUpdateAudit readAudit(Path path) {
        try {
            return EscrowUpdateAuditJson.read(Files.readString(path));
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot read escrow update audit " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static EscrowUpdateAudit updateAudit(
            EscrowManifest previous,
            EscrowManifest updated,
            String previousDigest,
            String updatedDigest
    ) {
        Map<String, String> previousEntries = entriesByPath(previous.entries());
        Map<String, String> updatedEntries = entriesByPath(updated.entries());
        int added = (int) updatedEntries.keySet().stream().filter(path -> !previousEntries.containsKey(path)).count();
        int removed = (int) previousEntries.keySet().stream().filter(path -> !updatedEntries.containsKey(path)).count();
        int changed = (int) updatedEntries.entrySet().stream()
                .filter(entry -> previousEntries.containsKey(entry.getKey()))
                .filter(entry -> !entry.getValue().equals(previousEntries.get(entry.getKey())))
                .count();
        return new EscrowUpdateAudit(EscrowUpdateAudit.SCHEMA_VERSION, Instant.now(), previousDigest, updatedDigest,
                added, changed, removed);
    }

    private static Map<String, String> entriesByPath(List<EscrowEntry> entries) {
        Map<String, String> result = new HashMap<>();
        for (EscrowEntry entry : entries) {
            result.put(entry.path(), entry.sha256());
        }
        return result;
    }

    private static void storeEntries(Path root, List<EscrowEntry> entries) {
        for (EscrowEntry entry : entries) {
            Path source = root.resolve(entry.path()).normalize();
            byte[] bytes = readBytes(source);
            Path stored = storedFile(root, entry.sha256());
            try {
                Files.createDirectories(Objects.requireNonNull(stored.getParent()));
                if (Files.exists(stored) && !Hashing.sha256(readBytes(stored)).equals(entry.sha256())) {
                    throw new ToppleCatException("Escrow content hash collision at " + stored);
                }
                if (!Files.exists(stored)) {
                    writeBytesAtomically(stored, bytes);
                }
            } catch (IOException exception) {
                throw new ToppleCatException("Cannot write escrow file " + stored + ": " + exception.getMessage(), exception);
            }
        }
    }

    private static void validateStoredFiles(Path projectRoot, EscrowManifest manifest) {
        Set<String> paths = new HashSet<>();
        for (EscrowEntry entry : manifest.entries()) {
            if (entry.path() == null || entry.path().isBlank() || entry.sha256() == null || entry.sha256().length() != 64
                    || entry.sourceKind() == null || !paths.add(entry.path())) {
                throw new ToppleCatException("Escrow manifest contains an invalid entry.");
            }
            Path source = projectRoot.resolve(entry.path()).normalize();
            requireInside(projectRoot, source);
            Path stored = storedFile(projectRoot, entry.sha256());
            if (!Files.isRegularFile(stored) || !Hashing.sha256(readBytes(stored)).equals(entry.sha256())) {
                throw new ToppleCatException("Escrow integrity failed for " + entry.path());
            }
        }
    }

    private static void requireExactRestoredSource(Path root, Path hidden, EscrowManifest manifest) {
        if (!sourceMatchesManifest(root, hidden, manifest)) {
            throw new ToppleCatException("Reviewer source does not exactly match the escrow manifest. Restore the original "
                    + "source or use toppleCatUpdateEscrow after review for intentional changes.");
        }
    }

    private static boolean sourceMatchesManifest(Path root, Path hidden, EscrowManifest manifest) {
        return inventory(root, hidden).equals(manifest.entries());
    }

    private static boolean sourceIsManifestSubset(Path root, Path hidden, EscrowManifest manifest) {
        Set<EscrowEntry> expected = Set.copyOf(manifest.entries());
        return inventory(root, hidden).stream().allMatch(expected::contains);
    }

    private static List<EscrowEntry> inventory(Path projectRoot, Path sourceRoot) {
        List<EscrowEntry> entries = new ArrayList<>();
        for (Path file : files(sourceRoot)) {
            byte[] bytes = readBytes(file);
            entries.add(new EscrowEntry(relative(projectRoot, file), Hashing.sha256(bytes), kind(sourceRoot, file)));
        }
        return List.copyOf(entries);
    }

    private static List<EscrowEntry> inventoryAsHiddenRoot(Path projectRoot, Path sourceRoot, Path hiddenRoot) {
        List<EscrowEntry> entries = new ArrayList<>();
        for (Path file : files(sourceRoot)) {
            Path logicalFile = hiddenRoot.resolve(sourceRoot.relativize(file)).normalize();
            requireInside(projectRoot, logicalFile);
            byte[] bytes = readBytes(file);
            entries.add(new EscrowEntry(relative(projectRoot, logicalFile), Hashing.sha256(bytes), kind(hiddenRoot, logicalFile)));
        }
        return List.copyOf(entries);
    }

    private static void moveReviewerSource(Path source, Path target) {
        try {
            Files.createDirectories(Objects.requireNonNull(target.getParent()));
            if (Files.exists(source)) {
                moveAtomically(source, target);
            } else {
                Files.createDirectories(target);
            }
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot stage reviewer source for escrow update: " + exception.getMessage(), exception);
        }
    }

    private static List<Path> files(Path root) {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot read reviewer source " + root + ": " + exception.getMessage(), exception);
        }
    }

    private static boolean hasFiles(Path root) {
        return !files(root).isEmpty();
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new ToppleCatException("Cannot remove reviewer source " + path + ": " + exception.getMessage(), exception);
                }
            });
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot remove reviewer source " + root + ": " + exception.getMessage(), exception);
        }
    }

    private static void writeBytesAtomically(Path target, byte[] bytes) throws IOException {
        Path temporary = Files.createTempFile(Objects.requireNonNull(target.getParent()), target.getFileName() + ".", ".tmp");
        try {
            Files.write(temporary, bytes);
            moveAtomically(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeStringAtomically(Path target, String content) throws IOException {
        Path temporary = Files.createTempFile(Objects.requireNonNull(target.getParent()), target.getFileName() + ".", ".tmp");
        try {
            Files.writeString(temporary, content);
            moveAtomically(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot read " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static EscrowSourceKind kind(Path hiddenRoot, Path file) {
        Path relative = hiddenRoot.relativize(file);
        return relative.startsWith(Path.of("resources", "topplecat", "cases"))
                ? EscrowSourceKind.HIDDEN_CASES : EscrowSourceKind.HIDDEN_TEST;
    }

    private static void requireInside(Path root, Path path) {
        if (!path.startsWith(root)) {
            throw new ToppleCatException("Escrow path escapes the project root: " + path);
        }
    }

}
