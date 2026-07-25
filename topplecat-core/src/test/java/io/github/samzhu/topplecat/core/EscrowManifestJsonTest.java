package io.github.samzhu.topplecat.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscrowManifestJsonTest {
    @Test
    void readsAndWritesTheExistingVersionOneManifestWithoutApproval() {
        EscrowManifest legacy = new EscrowManifest(EscrowManifest.SCHEMA_VERSION_V1, EscrowState.HIDDEN,
                List.of(new EscrowEntry("src/hiddenTest/java/example/ReviewerTest.java", "a".repeat(64),
                        EscrowSourceKind.HIDDEN_TEST)), null);

        String json = EscrowManifestJson.write(legacy);

        assertEquals(legacy, EscrowManifestJson.read(json));
        assertFalse(json.contains("approval"), json);
    }

    @Test
    void readsAndWritesVersionTwoWithTheExactApproval() {
        ReviewerContractApproval approval = ReviewerContractApproval.create(List.of(
                new PublicContractEntry("src/test/java/example/ContractTest.java", "b".repeat(64))
        ), "c".repeat(64), new VerificationPolicy("0.0.3", true, true, true, 100,
                MutationProducerKind.DEFAULT, null));
        EscrowManifest manifest = new EscrowManifest(EscrowManifest.SCHEMA_VERSION_V2, EscrowState.HIDDEN,
                List.of(new EscrowEntry("src/hiddenTest/java/example/ReviewerTest.java", "a".repeat(64),
                        EscrowSourceKind.HIDDEN_TEST)), approval);

        String json = EscrowManifestJson.write(manifest);

        assertEquals(manifest, EscrowManifestJson.read(json));
        assertTrue(json.contains("topplecat.contract-approval.v1"), json);
    }
}
