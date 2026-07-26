package io.github.samzhu.topplecat.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewerContractApprovalJsonTest {
    @Test
    void createsADeterministicApprovalDigestAndRoundTripsCanonicalJson() {
        VerificationPolicy policy = new VerificationPolicy("0.0.4", true, true, true, 100,
                MutationProducerKind.DEFAULT, null);
        ReviewerContractApproval approval = ReviewerContractApproval.create(List.of(
                new PublicContractEntry("src/test/java/example/AmountContractTest.java", "a".repeat(64)),
                new PublicContractEntry("src/test/resources/topplecat/cases/amount.json", "b".repeat(64))
        ), "c".repeat(64), policy);

        String json = ReviewerContractApprovalJson.write(approval);

        assertEquals(approval, ReviewerContractApprovalJson.read(json));
        assertEquals(approval.approvalDigest(), ReviewerContractApprovalJson.read(json).approvalDigest());
        assertEquals("topplecat.contract-approval.v1", approval.schemaVersion());
    }

    @Test
    void changesTheApprovalDigestWhenAContractFileOrPolicyChanges() {
        ReviewerContractApproval initial = approval("a".repeat(64), 100);
        ReviewerContractApproval changedFile = approval("b".repeat(64), 100);
        ReviewerContractApproval changedPolicy = approval("a".repeat(64), 99);

        assertNotEquals(initial.approvalDigest(), changedFile.approvalDigest());
        assertNotEquals(initial.approvalDigest(), changedPolicy.approvalDigest());
    }

    @Test
    void rejectsAnUnsortedOrTamperedApproval() {
        VerificationPolicy policy = new VerificationPolicy("0.0.4", true, true, true, 100,
                MutationProducerKind.DEFAULT, null);

        assertThrows(ToppleCatException.class, () -> new ReviewerContractApproval(
                ReviewerContractApproval.SCHEMA_VERSION,
                List.of(
                        new PublicContractEntry("src/test/resources/topplecat/cases/amount.json", "a".repeat(64)),
                        new PublicContractEntry("src/test/java/example/AmountContractTest.java", "b".repeat(64))
                ),
                "c".repeat(64), policy, "d".repeat(64)));
    }

    private static ReviewerContractApproval approval(String entryDigest, int threshold) {
        return ReviewerContractApproval.create(List.of(
                new PublicContractEntry("src/test/java/example/AmountContractTest.java", entryDigest)
        ), "c".repeat(64), new VerificationPolicy("0.0.4", true, true, true, threshold,
                MutationProducerKind.DEFAULT, null));
    }
}
