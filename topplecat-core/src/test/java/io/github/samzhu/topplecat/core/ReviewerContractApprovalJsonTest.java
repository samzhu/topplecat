package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewerContractApprovalJsonTest {
  @Test
  void createsADeterministicCurrentApprovalAndRoundTrips() {
    ReviewerContractApproval approval = approval("a".repeat(64), 100);

    assertEquals(
        approval, ReviewerContractApprovalJson.read(ReviewerContractApprovalJson.write(approval)));
    assertEquals("topplecat.contract-approval.v5", approval.schemaVersion());
  }

  @Test
  void changesTheApprovalDigestWhenAContractFileOrPolicyChanges() {
    assertNotEquals(
        approval("a".repeat(64), 100).approvalDigest(),
        approval("b".repeat(64), 100).approvalDigest());
    assertNotEquals(
        approval("a".repeat(64), 100).approvalDigest(),
        approval("a".repeat(64), 99).approvalDigest());
  }

  @Test
  void rejectsAnyPreviousApprovalSchema() {
    String old =
        ReviewerContractApprovalJson.write(approval("a".repeat(64), 100))
            .replace("topplecat.contract-approval.v5", "topplecat.contract-approval.v4");

    assertThrows(ToppleCatException.class, () -> ReviewerContractApprovalJson.read(old));
  }

  private static ReviewerContractApproval approval(String entryDigest, int threshold) {
    return ReviewerContractApproval.create(
        List.of(
            new PublicContractEntry(
                "src/test/java/example/AmountAcceptanceTest.java", entryDigest)),
        "c".repeat(64),
        new VerificationPolicy("0.0.14", true, true, true, true, threshold));
  }
}
