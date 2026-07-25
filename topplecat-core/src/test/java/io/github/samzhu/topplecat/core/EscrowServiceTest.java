package io.github.samzhu.topplecat.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscrowServiceTest {
    @TempDir
    Path project;

    @Test
    void hidesRestoresAndRehidesTheCompleteReviewerSourceSet() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path test = hidden.resolve("java/example/HiddenOrderTest.java");
        Path cases = hidden.resolve("resources/topplecat/cases/SPEC-42/coupon-hidden.yaml");
        Files.createDirectories(test.getParent());
        Files.createDirectories(cases.getParent());
        Files.writeString(test, "class HiddenOrderTest {}\n");
        Files.writeString(cases, "- caseId: hidden\n  acId: AC-ORDER\n  inputs: {}\n  expected: {total: 700}\n");
        EscrowService escrow = new EscrowService();

        EscrowManifest hiddenManifest = escrow.hide(project, hidden);

        assertEquals(EscrowState.HIDDEN, hiddenManifest.state());
        assertEquals(2, hiddenManifest.entries().size());
        assertFalse(Files.exists(hidden));
        assertTrue(Files.isRegularFile(project.resolve(".topplecat/escrow/manifest.json")));
        assertEquals(EscrowSourceKind.HIDDEN_CASES, hiddenManifest.entries().stream()
                .filter(entry -> entry.path().endsWith("coupon-hidden.yaml")).findFirst().orElseThrow().sourceKind());

        EscrowManifest restored = escrow.restore(project);
        assertEquals(EscrowState.RESTORED, restored.state());
        assertEquals("class HiddenOrderTest {}\n", Files.readString(test));

        EscrowManifest rehidden = escrow.rehide(project);
        assertEquals(EscrowState.HIDDEN, rehidden.state());
        assertFalse(Files.exists(hidden));
        assertEquals(hiddenManifest.entries(), rehidden.entries());
    }

    @Test
    void refusesToOverwriteReviewerSourceWithDifferentBytes() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path test = hidden.resolve("java/example/HiddenOrderTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, "class HiddenOrderTest {}\n");
        EscrowService escrow = new EscrowService();
        escrow.hide(project, hidden);
        Files.createDirectories(test.getParent());
        Files.writeString(test, "class ModifiedHiddenOrderTest {}\n");

        ToppleCatException error = assertThrows(ToppleCatException.class, () -> escrow.restore(project));

        assertTrue(error.getMessage().contains("Refusing to overwrite"));
    }

    @Test
    void hidesAndRestoresIdempotently() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path test = hidden.resolve("java/example/HiddenOrderTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, "class HiddenOrderTest {}\n");
        EscrowService escrow = new EscrowService();

        EscrowManifest firstHide = escrow.hide(project, hidden);
        EscrowManifest secondHide = escrow.hide(project, hidden);
        EscrowManifest firstRestore = escrow.restore(project);
        EscrowManifest secondRestore = escrow.restore(project);

        assertEquals(firstHide, secondHide);
        assertEquals(EscrowState.RESTORED, firstRestore.state());
        assertEquals(firstRestore, secondRestore);
        assertEquals("class HiddenOrderTest {}\n", Files.readString(test));
    }

    @Test
    void hidesAllReviewerAssetsAcrossSpecDirectoriesAsOneSourceSet() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path first = hidden.resolve("resources/topplecat/cases/SPEC-42/first.yaml");
        Path second = hidden.resolve("resources/topplecat/cases/SPEC-43/second.yaml");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(first, "- caseId: first\n  acId: SPEC-42-AC-01\n  inputs: {}\n  expected: {accepted: true}\n");
        Files.writeString(second, "- caseId: second\n  acId: SPEC-43-AC-01\n  inputs: {}\n  expected: {accepted: true}\n");
        EscrowService escrow = new EscrowService();

        EscrowManifest hiddenManifest = escrow.hide(project, hidden);

        assertEquals(2, hiddenManifest.entries().size());
        assertFalse(Files.exists(hidden));

        escrow.restore(project);

        assertTrue(Files.isRegularFile(first));
        assertTrue(Files.isRegularFile(second));
    }

    @Test
    void recoversAKnownPartialSourceSetWithoutAcceptingNewReviewerFiles() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path first = hidden.resolve("java/example/FirstReviewerTest.java");
        Path second = hidden.resolve("java/example/SecondReviewerTest.java");
        Files.createDirectories(first.getParent());
        Files.writeString(first, "class FirstReviewerTest {}\n");
        Files.writeString(second, "class SecondReviewerTest {}\n");
        EscrowService escrow = new EscrowService();
        escrow.hide(project, hidden);

        Files.createDirectories(first.getParent());
        Files.writeString(first, "class FirstReviewerTest {}\n");

        EscrowManifest restored = escrow.restore(project);

        assertEquals(EscrowState.RESTORED, restored.state());
        assertEquals("class FirstReviewerTest {}\n", Files.readString(first));
        assertEquals("class SecondReviewerTest {}\n", Files.readString(second));
    }

    @Test
    void refusesToRestoreIntoAReviewerTreeThatContainsUnexpectedFiles() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path escrowed = hidden.resolve("java/example/HiddenOrderTest.java");
        Path unexpected = hidden.resolve("java/example/NewReviewerTest.java");
        Files.createDirectories(escrowed.getParent());
        Files.writeString(escrowed, "class HiddenOrderTest {}\n");
        EscrowService escrow = new EscrowService();
        escrow.hide(project, hidden);
        Files.createDirectories(unexpected.getParent());
        Files.writeString(unexpected, "class NewReviewerTest {}\n");

        ToppleCatException error = assertThrows(ToppleCatException.class, () -> escrow.restore(project));

        assertTrue(error.getMessage().contains("not exactly the escrowed source"), error::getMessage);
        assertEquals("class NewReviewerTest {}\n", Files.readString(unexpected));
        assertEquals(EscrowState.HIDDEN, escrow.manifest(project).state());
        assertFalse(Files.exists(escrowed));
    }

    @Test
    void refusesToRehideWhenRestoredReviewerSourceHasChangedOrGainedFiles() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path escrowed = hidden.resolve("java/example/HiddenOrderTest.java");
        Path unexpected = hidden.resolve("java/example/NewReviewerTest.java");
        Files.createDirectories(escrowed.getParent());
        Files.writeString(escrowed, "class HiddenOrderTest {}\n");
        EscrowService escrow = new EscrowService();
        escrow.hide(project, hidden);
        escrow.restore(project);
        Files.writeString(unexpected, "class NewReviewerTest {}\n");

        ToppleCatException error = assertThrows(ToppleCatException.class, () -> escrow.rehide(project));

        assertTrue(error.getMessage().contains("does not exactly match"), error::getMessage);
        assertTrue(error.getMessage().contains("toppleCatUpdateEscrow"), error::getMessage);
        assertEquals("class HiddenOrderTest {}\n", Files.readString(escrowed));
        assertEquals("class NewReviewerTest {}\n", Files.readString(unexpected));
        assertEquals(EscrowState.RESTORED, escrow.manifest(project).state());
    }

    @Test
    void rejectsASecondCustodyOperationWhileTheProjectEscrowIsLocked() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path test = hidden.resolve("java/example/HiddenOrderTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, "class HiddenOrderTest {}\n");
        Path lockPath = project.resolve(".topplecat/escrow/.lock");
        Files.createDirectories(lockPath.getParent());
        EscrowService escrow = new EscrowService();

        try (FileChannel channel = FileChannel.open(lockPath, CREATE, WRITE); FileLock ignored = channel.lock()) {
            ToppleCatException error = assertThrows(ToppleCatException.class, () -> escrow.hide(project, hidden));
            assertTrue(error.getMessage().contains("Another ToppleCat custody operation"), error::getMessage);
        }

        assertTrue(Files.exists(test));
        assertFalse(Files.exists(project.resolve(".topplecat/escrow/manifest.json")));
    }

    @Test
    void updatesRestoredEscrowAfterAddingReviewerFile() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path original = hidden.resolve("java/example/HiddenOrderTest.java");
        Path added = hidden.resolve("resources/topplecat/cases/order-reviewer.yaml");
        Files.createDirectories(original.getParent());
        Files.writeString(original, "class HiddenOrderTest {}\n");
        EscrowService escrow = new EscrowService();
        escrow.hide(project, hidden);
        escrow.restore(project);
        Files.createDirectories(added.getParent());
        Files.writeString(added, "- caseId: reviewer-added\n  acId: AC-ORDER\n  inputs: {}\n  expected: {total: 700}\n");

        EscrowManifest updated = escrow.update(project, hidden);

        assertEquals(EscrowState.HIDDEN, updated.state());
        assertFalse(Files.exists(hidden));
        EscrowManifest restored = escrow.restore(project);
        assertEquals(2, restored.entries().size());
        assertEquals("class HiddenOrderTest {}\n", Files.readString(original));
        assertTrue(Files.readString(added).contains("reviewer-added"));
    }

    @Test
    void updatesRestoredEscrowAfterModifyingAndRemovingReviewerFiles() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path modified = hidden.resolve("java/example/HiddenOrderTest.java");
        Path removed = hidden.resolve("java/example/RemovedReviewerTest.java");
        Files.createDirectories(modified.getParent());
        Files.writeString(modified, "class HiddenOrderTest {}\n");
        Files.writeString(removed, "class RemovedReviewerTest {}\n");
        EscrowService escrow = new EscrowService();
        escrow.hide(project, hidden);
        escrow.restore(project);
        Files.writeString(modified, "class HiddenOrderTest { int revision = 2; }\n");
        Files.delete(removed);

        EscrowManifest updated = escrow.update(project, hidden);

        assertEquals(EscrowState.HIDDEN, updated.state());
        escrow.restore(project);
        assertEquals("class HiddenOrderTest { int revision = 2; }\n", Files.readString(modified));
        assertFalse(Files.exists(removed));
        assertEquals(1, escrow.manifest(project).entries().size());
        EscrowUpdateAudit audit = updateAudit(project);
        assertEquals(0, audit.added());
        assertEquals(1, audit.changed());
        assertEquals(1, audit.removed());
    }

    @Test
    void updateRejectsReviewerSourceWhileEscrowIsHidden() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path source = hidden.resolve("java/example/HiddenOrderTest.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class HiddenOrderTest {}\n");
        EscrowService escrow = new EscrowService();
        escrow.hide(project, hidden);

        ToppleCatException error = assertThrows(ToppleCatException.class, () -> escrow.update(project, hidden));

        assertTrue(error.getMessage().contains("toppleCatRestore"), error::getMessage);
        assertEquals(EscrowState.HIDDEN, escrow.manifest(project).state());
        assertFalse(Files.exists(hidden));
    }

    @Test
    void updateRejectsCorruptPreviousEscrowBlobBeforeActivation() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path source = hidden.resolve("java/example/HiddenOrderTest.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class HiddenOrderTest {}\n");
        EscrowService escrow = new EscrowService();
        EscrowManifest original = escrow.hide(project, hidden);
        escrow.restore(project);
        EscrowEntry entry = original.entries().getFirst();
        Path blob = project.resolve(".topplecat/escrow/files").resolve(entry.sha256().substring(0, 2)).resolve(entry.sha256());
        Files.writeString(blob, "corrupt\n");

        assertThrows(ToppleCatException.class, () -> escrow.update(project, hidden));

        assertEquals(EscrowState.RESTORED, escrow.manifest(project).state());
        assertEquals("class HiddenOrderTest {}\n", Files.readString(source));
    }

    @Test
    void updateRestoresPreviousSourceAndRetainsModifiedRevisionWhenActivationFails() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path source = hidden.resolve("java/example/HiddenOrderTest.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class HiddenOrderTest {}\n");
        EscrowService original = new EscrowService();
        original.hide(project, hidden);
        original.restore(project);
        Files.writeString(source, "class HiddenOrderTest { int revision = 2; }\n");
        EscrowService escrow = new EscrowService(() -> {
            throw new IllegalStateException("injected activation failure");
        });

        ToppleCatException error = assertThrows(ToppleCatException.class, () -> escrow.update(project, hidden));

        assertTrue(error.getMessage().contains("no update was activated"), error::getMessage);
        assertEquals(EscrowState.RESTORED, original.manifest(project).state());
        assertEquals("class HiddenOrderTest {}\n", Files.readString(source));
        Path recovery = project.resolve(".topplecat/escrow/recovery");
        assertTrue(Files.isDirectory(recovery));
        try (var paths = Files.walk(recovery)) {
            Path revisedSource = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("HiddenOrderTest.java"))
                    .findFirst().orElseThrow();
            assertEquals("class HiddenOrderTest { int revision = 2; }\n", Files.readString(revisedSource));
        }
    }

    @Test
    void updateWritesAuditWithoutReviewerPathsOrContents() throws Exception {
        Path hidden = project.resolve("src/hiddenTest");
        Path original = hidden.resolve("java/example/HiddenOrderTest.java");
        Path added = hidden.resolve("resources/topplecat/cases/private-reviewer.yaml");
        Files.createDirectories(original.getParent());
        Files.writeString(original, "class HiddenOrderTest {}\n");
        EscrowService escrow = new EscrowService();
        escrow.hide(project, hidden);
        escrow.restore(project);
        Files.createDirectories(added.getParent());
        Files.writeString(added, "- caseId: reviewer-case-id\n  acId: AC-ORDER\n  inputs: {secret: 9876}\n  expected: {failure: raw failure}\n");

        escrow.update(project, hidden);

        Path auditPath = updateAuditPath(project);
        String auditJson = Files.readString(auditPath);
        EscrowUpdateAudit audit = EscrowUpdateAuditJson.read(auditJson);
        assertEquals(EscrowUpdateAudit.SCHEMA_VERSION, audit.schemaVersion());
        assertEquals(1, audit.added());
        assertEquals(0, audit.changed());
        assertEquals(0, audit.removed());
        assertFalse(auditJson.contains("private-reviewer.yaml"), auditJson);
        assertFalse(auditJson.contains("reviewer-case-id"), auditJson);
        assertFalse(auditJson.contains("9876"), auditJson);
        assertFalse(auditJson.contains("raw failure"), auditJson);
    }

    private static EscrowUpdateAudit updateAudit(Path project) throws Exception {
        return EscrowUpdateAuditJson.read(Files.readString(updateAuditPath(project)));
    }

    private static Path updateAuditPath(Path project) throws Exception {
        Path revisions = project.resolve(".topplecat/escrow/revisions");
        try (var paths = Files.walk(revisions)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("audit.json"))
                    .findFirst().orElseThrow();
        }
    }
}
