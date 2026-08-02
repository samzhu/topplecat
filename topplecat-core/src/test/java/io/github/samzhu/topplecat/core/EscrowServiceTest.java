package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EscrowServiceTest {
  @TempDir Path project;

  @Test
  void sealsRestoresAndRehidesTheCompleteReviewerSourceSet() throws Exception {
    Path hidden = writeReviewerSource();
    EscrowService service = service();

    EscrowManifest sealed = service.hide(project, hidden, approval());

    assertEquals(EscrowState.HIDDEN, sealed.state());
    assertFalse(Files.exists(hidden));
    EscrowManifest restored = service.restore(project);
    assertEquals(EscrowState.RESTORED, restored.state());
    assertTrue(Files.isRegularFile(hidden.resolve("java/example/HiddenAcceptanceSupport.java")));
    assertEquals(EscrowState.HIDDEN, service.rehide(project).state());
    assertFalse(Files.exists(hidden));
  }

  @Test
  void resealingRequiresRestoredCustodyAndCurrentApproval() throws Exception {
    Path hidden = writeReviewerSource();
    EscrowService service = service();
    service.hide(project, hidden, approval());

    assertThrows(ToppleCatException.class, () -> service.update(project, hidden, approval()));
    service.restore(project);
    Files.writeString(hidden.resolve("java/example/HiddenAcceptanceSupport.java"), "changed\n");
    assertEquals(EscrowState.HIDDEN, service.update(project, hidden, approval()).state());
  }

  @Test
  void rejectsAnApprovalFromAnyPreviousSchema() throws Exception {
    Path hidden = writeReviewerSource();
    ReviewerContractApproval current = approval();
    String old =
        ReviewerContractApprovalJson.write(current)
            .replace("topplecat.contract-approval.v5", "topplecat.contract-approval.v4");

    assertThrows(ToppleCatException.class, () -> ReviewerContractApprovalJson.read(old));
    assertEquals(EscrowState.HIDDEN, service().hide(project, hidden, current).state());
  }

  private Path writeReviewerSource() throws Exception {
    Path hidden = project.resolve("src/hiddenTest");
    Path source = hidden.resolve("java/example/HiddenAcceptanceSupport.java");
    Files.createDirectories(source.getParent());
    Files.writeString(source, "package example; class HiddenAcceptanceSupport {}\n");
    return hidden;
  }

  private EscrowService service() {
    return new EscrowService(project.resolve("reviewer-state"));
  }

  private static ReviewerContractApproval approval() {
    return ReviewerContractApproval.create(
        List.of(
            new PublicContractEntry(
                "src/test/java/example/CouponAcceptanceTest.java", "a".repeat(64))),
        "b".repeat(64),
        new VerificationPolicy("0.0.14", true, true, true, true, 100));
  }
}
