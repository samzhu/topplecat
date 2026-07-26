package io.github.samzhu.topplecat.core;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.PosixFileAttributeView;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Custody service for the complete {@code src/hiddenTest} source set. */
public final class EscrowService {
    private static final Path LEGACY_ESCROW_ROOT = Path.of(".topplecat", "escrow");
    private static final String STATE_ROOT_PROPERTY = "topplecat.stateRoot";
    private static final String REVIEWER_STATE_BASE = ".topplecat";
    private static final String PROJECTS_DIRECTORY = "projects";
    private static final String ESCROW_DIRECTORY = "escrow";
    private static final PosixFilePermission[] OWNER_ONLY = new PosixFilePermission[]{
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
    };
    private static final Runnable NO_OP = () -> {
    };

    private final Runnable beforeActivation;
    private final Runnable beforeLegacyMigrationLock;
    private final Path reviewerStateRoot;

    public EscrowService() {
        this(defaultReviewerStateRoot(), NO_OP, NO_OP);
    }

    public EscrowService(Path reviewerStateRoot) {
        this(reviewerStateRoot, NO_OP, NO_OP);
    }

    EscrowService(Path reviewerStateRoot, Runnable beforeActivation) {
        this(reviewerStateRoot, beforeActivation, NO_OP);
    }

    EscrowService(Path reviewerStateRoot, Runnable beforeActivation, Runnable beforeLegacyMigrationLock) {
        this.beforeActivation = Objects.requireNonNull(beforeActivation);
        this.beforeLegacyMigrationLock = Objects.requireNonNull(beforeLegacyMigrationLock);
        this.reviewerStateRoot = Objects.requireNonNull(reviewerStateRoot);
    }

    EscrowService(Runnable beforeActivation) {
        this(defaultReviewerStateRoot(), beforeActivation, NO_OP);
    }

    public static Path defaultReviewerStateRoot() {
        String configured = System.getProperty(STATE_ROOT_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            throw new ToppleCatException("Cannot determine reviewer escrow state root because user.home is not configured.");
        }
        return Paths.get(home).resolve(REVIEWER_STATE_BASE);
    }

    public static String projectKey(Path projectRoot) {
        return Hashing.sha256(canonicalProjectRoot(projectRoot).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
    }

    public static Path reviewerStatePath(Path projectRoot, Path reviewerStateRoot) {
        return reviewerStateRoot.resolve(PROJECTS_DIRECTORY)
                .resolve(projectKey(projectRoot))
                .resolve(ESCROW_DIRECTORY);
    }

    public static Path legacyEscrowPath(Path projectRoot) {
        return normalizedRoot(projectRoot).resolve(LEGACY_ESCROW_ROOT);
    }

    public static boolean hasLegacyEscrow(Path projectRoot) {
        return Files.exists(legacyEscrowPath(projectRoot));
    }

    public static Path canonicalProjectRoot(Path projectRoot) {
        try {
            return projectRoot.toAbsolutePath().normalize().toRealPath();
        } catch (IOException exception) {
            return projectRoot.toAbsolutePath().normalize();
        }
    }

    public EscrowManifest hide(Path projectRoot, Path hiddenSourceRoot) {
        return hide(projectRoot, hiddenSourceRoot, null);
    }

    /** Creates a reviewer-approved v2 epoch only when this is the initial hide. */
    public EscrowManifest hide(
            Path projectRoot,
            Path hiddenSourceRoot,
            ReviewerContractApproval initialApproval
    ) {
        Path root = normalizedRoot(projectRoot);
        Path hidden = hiddenSourceRoot.toAbsolutePath().normalize();
        requireInside(root, hidden);
        ensureStateDirectories(root, reviewerStateRoot);
        ensureNoLegacyBlockingOperation(root);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root, reviewerStateRoot)) {
            Path manifestPath = manifestPath(root, reviewerStateRoot);
            if (!Files.exists(manifestPath)) {
                return hideInitialSource(root, hidden, manifestPath, reviewerStateRoot, initialApproval);
            }

            EscrowManifest manifest = readManifest(manifestPath);
            validateStoredFiles(root, manifest, reviewerStateRoot);
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
        ensureStateDirectories(root, reviewerStateRoot);
        ensureNoLegacyBlockingOperation(root);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root, reviewerStateRoot)) {
            Path manifestPath = manifestPath(root, reviewerStateRoot);
            if (!Files.exists(manifestPath)) {
                throw new ToppleCatException("No ToppleCat escrow manifest exists. Run toppleCatHide before restoring reviewer source.");
            }
            EscrowManifest manifest = readManifest(manifestPath);
            validateStoredFiles(root, manifest, reviewerStateRoot);
            Path hidden = root.resolve("src/hiddenTest");

            if (manifest.state() == EscrowState.RESTORED) {
                requireExactRestoredSource(root, hidden, manifest);
                return manifest;
            }
            if (!sourceIsManifestSubset(root, hidden, manifest)) {
                throw new ToppleCatException("Refusing to overwrite reviewer source because src/hiddenTest is not exactly "
                        + "the escrowed source.");
            }
            restoreEntries(root, manifest, reviewerStateRoot);
            return writeState(manifestPath, manifest, EscrowState.RESTORED);
        }
    }

    public EscrowManifest rehide(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        ensureStateDirectories(root, reviewerStateRoot);
        ensureNoLegacyBlockingOperation(root);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root, reviewerStateRoot)) {
            Path manifestPath = manifestPath(root, reviewerStateRoot);
            if (!Files.exists(manifestPath)) {
                throw new ToppleCatException("No ToppleCat escrow manifest exists. Run toppleCatHide before re-hiding reviewer source.");
            }
            EscrowManifest manifest = readManifest(manifestPath);
            validateStoredFiles(root, manifest, reviewerStateRoot);
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
        return update(projectRoot, hiddenSourceRoot, null);
    }

    /** Explicitly activates a reviewed reviewer-source and approval epoch together. */
    public EscrowManifest update(
            Path projectRoot,
            Path hiddenSourceRoot,
            ReviewerContractApproval approval
    ) {
        Path root = normalizedRoot(projectRoot);
        Path hidden = hiddenSourceRoot.toAbsolutePath().normalize();
        requireInside(root, hidden);
        ensureStateDirectories(root, reviewerStateRoot);
        ensureNoLegacyBlockingOperation(root);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root, reviewerStateRoot)) {
            return updateRestoredSource(root, hidden, approval);
        }
    }

    public EscrowManifest manifest(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        ensureStateDirectories(root, reviewerStateRoot);
        ensureNoLegacyBlockingOperation(root);
        try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root, reviewerStateRoot)) {
            return readManifest(manifestPath(root, reviewerStateRoot));
        }
    }

    private EscrowManifest updateRestoredSource(Path root, Path hidden, ReviewerContractApproval approval) {
        Path manifestPath = manifestPath(root, reviewerStateRoot);
        if (!Files.isRegularFile(manifestPath)) {
            throw new ToppleCatException("No ToppleCat escrow manifest exists. Run toppleCatHide before updating reviewer custody.");
        }
        EscrowManifest previous = readManifest(manifestPath);
        if (previous.state() != EscrowState.RESTORED) {
            throw new ToppleCatException("Escrow update requires restored reviewer source. Run toppleCatRestore, edit "
                    + "src/hiddenTest, review it, then run toppleCatUpdateEscrow.");
        }
        validateStoredFiles(root, previous, reviewerStateRoot);

        if (!previous.isLegacyVersionOne() && approval == null) {
            throw new ToppleCatException("Escrow update requires the reviewer-approved public contract and verification policy.");
        }
        List<EscrowEntry> entries = inventory(root, hidden);
        EscrowManifest updated = updatedManifest(previous, entries, approval);
        storeEntries(root, entries, reviewerStateRoot);
        validateStoredFiles(root, updated, reviewerStateRoot);

        String previousManifest = EscrowManifestJson.write(previous);
        String updatedManifest = EscrowManifestJson.write(updated);
        String previousDigest = Hashing.sha256(previousManifest.getBytes(StandardCharsets.UTF_8));
        String updatedDigest = Hashing.sha256(updatedManifest.getBytes(StandardCharsets.UTF_8));
        EscrowUpdateAudit audit = updateAudit(previous, updated, previousDigest, updatedDigest);

        Path escrow = escrowRoot(root);
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

    private ToppleCatException recoverAfterFailedUpdate(
            Path root,
            Path hidden,
            Path manifestPath,
            EscrowManifest previous,
            Path stagedRevision,
            String revisionId,
            RuntimeException cause
    ) {
        Path recovery = escrowRoot(root).resolve("recovery").resolve(revisionId);
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
            restoreEntries(root, previous, reviewerStateRoot);
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

    private static EscrowManifest hideInitialSource(
            Path root,
            Path hidden,
            Path manifestPath,
            Path reviewerStateRoot,
            ReviewerContractApproval initialApproval
    ) {
        if (!hasFiles(hidden) && initialApproval != null && initialApproval.verificationPolicy().hiddenRetestEnabled()) {
            throw new ToppleCatException("Cannot create reviewer approval without reviewer source. Restore or provide the complete "
                    + "src/hiddenTest source set before running toppleCatHide.");
        }
        List<EscrowEntry> entries = inventory(root, hidden);
        storeEntries(root, entries, reviewerStateRoot);

        EscrowManifest hiddenManifest = initialApproval == null
                ? new EscrowManifest(EscrowManifest.SCHEMA_VERSION_V1, EscrowState.HIDDEN, entries)
                : new EscrowManifest(EscrowManifest.SCHEMA_VERSION_V2, EscrowState.HIDDEN, entries, initialApproval);
        writeManifest(manifestPath, hiddenManifest);
        deleteTree(hidden);
        return hiddenManifest;
    }

    private static void restoreEntries(Path root, EscrowManifest manifest) {
        restoreEntries(root, manifest, defaultReviewerStateRoot());
    }

    private static void restoreEntries(Path root, EscrowManifest manifest, Path reviewerStateRoot) {
        for (EscrowEntry entry : manifest.entries()) {
            Path target = root.resolve(entry.path()).normalize();
            requireInside(root, target);
            Path stored = storedFile(root, entry.sha256(), reviewerStateRoot);
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
        EscrowManifest updated = new EscrowManifest(manifest.schemaVersion(), state, manifest.entries(), manifest.approval());
        writeManifest(manifestPath, updated);
        return updated;
    }

    private static EscrowManifest updatedManifest(
            EscrowManifest previous,
            List<EscrowEntry> entries,
            ReviewerContractApproval approval
    ) {
        if (approval == null) {
            return new EscrowManifest(EscrowManifest.SCHEMA_VERSION_V1, EscrowState.HIDDEN, entries);
        }
        return new EscrowManifest(EscrowManifest.SCHEMA_VERSION_V2, EscrowState.HIDDEN, entries, approval);
    }

    private static Path normalizedRoot(Path projectRoot) {
        return projectRoot.toAbsolutePath().normalize();
    }

    private static void ensureOwnerOnly(Path path) {
        if (path == null) {
            return;
        }
        try {
            PosixFileAttributeView permissions = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            if (permissions != null) {
                permissions.setPermissions(Set.copyOf(Set.of(OWNER_ONLY)));
            }
        } catch (IOException | UnsupportedOperationException ignored) {
            // best-effort hardening for reviewer custody storage.
        }
    }

    private static void ensureStateDirectories(Path projectRoot, Path reviewerStateRoot) {
        Path escrow = reviewerStatePath(projectRoot, reviewerStateRoot);
        try {
            Files.createDirectories(escrow);
            ensureOwnerOnly(reviewerStateRoot);
            ensureOwnerOnly(escrow);
        } catch (IOException ignored) {
            // security hardening is non-authoritative and best-effort only.
        }
    }

    private static String legacyEscrowPrompt(Path projectRoot) {
        return "No reviewer-local escrow exists. A legacy ToppleCat escrow exists at " + legacyEscrowPath(projectRoot)
                + ". Run toppleCatMigrateEscrow first.";
    }

    private void ensureNoLegacyBlockingOperation(Path root) {
        ensureNoLegacyBlockingOperation(root, reviewerStateRoot);
    }

    private void ensureNoLegacyBlockingOperation(Path root, Path stateRoot) {
        if (!Files.exists(legacyEscrowPath(root))) {
            return;
        }
        if (!Files.exists(manifestPath(root, stateRoot))) {
            throw new ToppleCatException(legacyEscrowPrompt(root));
        }
    }

    private Path escrowRoot(Path projectRoot) {
        return reviewerStatePath(projectRoot, reviewerStateRoot);
    }

    public EscrowManifest migrateLegacyEscrow(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        Path legacy = legacyEscrowPath(root);
        if (!Files.exists(legacy)) {
            throw new ToppleCatException("No legacy ToppleCat escrow exists at " + legacy + " to migrate.");
        }

        EscrowManifest manifest = readManifest(legacy.resolve("manifest.json"));
        validateStoredFilesInEscrow(root, manifest, legacy);
        Path destination = escrowRoot(root);
        Path staging;
        try {
            staging = Files.createTempDirectory("topplecat-legacy-escrow-");
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot prepare legacy escrow migration staging directory for " + legacy + ": "
                    + exception.getMessage(), exception);
        }
        Path backup = legacy.resolveSibling(".escrow.backup-" + UUID.randomUUID());
        List<Path> movedPayload = new ArrayList<>();
        boolean legacyMoved = false;
        try {
            copyDirectory(legacy, staging);
            Path stagedManifest = staging.resolve("manifest.json");
            EscrowManifest staged = readManifest(stagedManifest);
            if (!staged.equals(manifest)) {
                throw new ToppleCatException("Migrated legacy escrow manifest does not match source.");
            }
            validateStoredFilesInEscrow(root, staged, staging);
            beforeLegacyMigrationLock.run();
            try (EscrowProjectLock ignored = EscrowProjectLock.acquireOperation(root, reviewerStateRoot)) {
                // Acquiring the project-scoped lock creates destination/.lock.  Keep that
                // live lock in place and move the legacy payload around it; attempting to
                // atomically move the whole staged directory would therefore always fail
                // with "Directory not empty".
                try {
                    if (hasEscrowPayload(destination)) {
                        throw new ToppleCatException("Reviewer escrow already exists at " + destination
                                + ", refuse migration to avoid merge.");
                    }
                    Files.createDirectories(destination.getParent());
                    moveAtomically(legacy, backup);
                    legacyMoved = true;
                    moveStagedEscrowPayload(staging, destination, movedPayload);
                    EscrowManifest activated = readManifest(destination.resolve("manifest.json"));
                    if (!activated.equals(staged)) {
                        throw new ToppleCatException("Migrated reviewer escrow manifest does not match staged custody.");
                    }
                    validateStoredFilesInEscrow(root, activated, destination);
                } catch (IOException | RuntimeException exception) {
                    try {
                        deleteMigratedPayload(movedPayload);
                    } catch (RuntimeException cleanupFailure) {
                        exception.addSuppressed(cleanupFailure);
                    }
                    if (legacyMoved) {
                        try {
                            moveAtomically(backup, legacy);
                        } catch (IOException restoreFailure) {
                            exception.addSuppressed(restoreFailure);
                        }
                    }
                    throw exception instanceof ToppleCatException ? (ToppleCatException) exception
                            : new ToppleCatException("Cannot migrate legacy escrow from " + legacy + " to " + destination + ": "
                            + exception.getMessage(), exception);
                }
                try {
                    deleteTree(backup);
                } catch (RuntimeException cleanupFailure) {
                    // leave backup cleanup as best-effort; migration is complete once destination is active.
                }
            }
            return readManifest(destination.resolve("manifest.json"));
        } catch (RuntimeException exception) {
            throw exception instanceof ToppleCatException ? (ToppleCatException) exception
                    : new ToppleCatException("Cannot migrate legacy escrow at " + legacy + ": " + exception.getMessage(), exception);
        } finally {
            deleteTree(staging);
        }
    }

    private static void moveStagedEscrowPayload(Path staging, Path destination, List<Path> movedPayload) throws IOException {
        try (Stream<Path> paths = Files.list(staging)) {
            for (Path child : paths.toList()) {
                if (child.getFileName().toString().equals(".lock")) {
                    continue;
                }
                Path target = destination.resolve(child.getFileName().toString());
                moveAtomically(child, target);
                movedPayload.add(target);
            }
        }
    }

    private static boolean hasEscrowPayload(Path destination) throws IOException {
        try (Stream<Path> paths = Files.list(destination)) {
            return paths.anyMatch(path -> !path.getFileName().toString().equals(".lock"));
        }
    }

    private static void deleteMigratedPayload(List<Path> movedPayload) {
        for (Path path : movedPayload.reversed()) {
            deleteTree(path);
        }
    }

    private static Path manifestPath(Path projectRoot) {
        return manifestPath(projectRoot, defaultReviewerStateRoot());
    }

    private static Path manifestPath(Path projectRoot, Path reviewerStateRoot) {
        return reviewerStatePath(projectRoot, reviewerStateRoot).resolve("manifest.json");
    }

    private static Path storedFile(Path projectRoot, String hash) {
        return storedFile(projectRoot, hash, defaultReviewerStateRoot());
    }

    private static Path storedFile(Path projectRoot, String hash, Path reviewerStateRoot) {
        return storedFileInEscrow(reviewerStatePath(projectRoot, reviewerStateRoot), hash);
    }

    private static Path storedFileInEscrow(Path escrowRoot, String hash) {
        return escrowRoot.resolve("files").resolve(hash.substring(0, 2))
                .resolve(hash);
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
                approvalDigest(previous), approvalDigest(updated), added, changed, removed);
    }

    private static String approvalDigest(EscrowManifest manifest) {
        return manifest.approval() == null ? null : manifest.approval().approvalDigest();
    }

    private static Map<String, String> entriesByPath(List<EscrowEntry> entries) {
        Map<String, String> result = new HashMap<>();
        for (EscrowEntry entry : entries) {
            result.put(entry.path(), entry.sha256());
        }
        return result;
    }

    private static void storeEntries(Path root, List<EscrowEntry> entries) {
        storeEntries(root, entries, defaultReviewerStateRoot());
    }

    private static void storeEntries(Path root, List<EscrowEntry> entries, Path reviewerStateRoot) {
        for (EscrowEntry entry : entries) {
            Path source = root.resolve(entry.path()).normalize();
            byte[] bytes = readBytes(source);
            Path stored = storedFile(root, entry.sha256(), reviewerStateRoot);
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
        validateStoredFiles(projectRoot, manifest, defaultReviewerStateRoot());
    }

    private static void validateStoredFiles(Path projectRoot, EscrowManifest manifest, Path reviewerStateRoot) {
        validateStoredFilesInEscrow(projectRoot, manifest, reviewerStatePath(projectRoot, reviewerStateRoot));
    }

    private static void validateStoredFilesInEscrow(Path projectRoot, EscrowManifest manifest, Path escrowRoot) {
        Set<String> paths = new HashSet<>();
        for (EscrowEntry entry : manifest.entries()) {
            if (entry.path() == null || entry.path().isBlank() || entry.sha256() == null || entry.sha256().length() != 64
                    || entry.sourceKind() == null || !paths.add(entry.path())) {
                throw new ToppleCatException("Escrow manifest contains an invalid entry.");
            }
            Path source = projectRoot.resolve(entry.path()).normalize();
            requireInside(projectRoot, source);
            Path stored = storedFileInEscrow(escrowRoot, entry.sha256());
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

    private static void copyDirectory(Path source, Path target) {
        try {
            if (!Files.exists(source)) {
                throw new ToppleCatException("Cannot copy missing source directory " + source);
            }
            try (Stream<Path> paths = Files.walk(source)) {
                paths.sorted().forEach(path -> {
                    Path destination = target.resolve(source.relativize(path));
                    try {
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(destination);
                        } else {
                            Files.createDirectories(Objects.requireNonNull(destination.getParent()));
                            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException exception) {
                        throw new ToppleCatException("Cannot copy escrow legacy state from " + path + " to " + destination + ": "
                                + exception.getMessage(), exception);
                    }
                });
            }
        } catch (RuntimeException exception) {
            throw exception instanceof ToppleCatException ? (ToppleCatException) exception
                    : new ToppleCatException("Cannot copy escrow legacy state from " + source + " to " + target + ": "
                    + exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot iterate legacy escrow source at " + source + ": " + exception.getMessage(),
                    exception);
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
