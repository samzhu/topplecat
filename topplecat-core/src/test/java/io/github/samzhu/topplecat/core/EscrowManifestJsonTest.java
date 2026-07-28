package io.github.samzhu.topplecat.core;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscrowManifestJsonTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

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
        ), "c".repeat(64), new VerificationPolicy("0.0.4", true, true, true, 100,
                MutationProducerKind.DEFAULT, null));
        EscrowManifest manifest = new EscrowManifest(EscrowManifest.SCHEMA_VERSION_V2, EscrowState.HIDDEN,
                List.of(new EscrowEntry("src/hiddenTest/java/example/ReviewerTest.java", "a".repeat(64),
                        EscrowSourceKind.HIDDEN_TEST)), approval);

        String json = EscrowManifestJson.write(manifest);

        assertEquals(manifest, EscrowManifestJson.read(json));
        assertTrue(json.contains("topplecat.contract-approval.v2"), json);
    }

    @Test
    void readsVersionTwoManifestContainingVersionOneApproval() {
        VerificationPolicy policy = new VerificationPolicy("0.0.5", true, true, true, 100,
                MutationProducerKind.DEFAULT, null);
        List<PublicContractEntry> publicFiles = List.of(new PublicContractEntry(
                "src/test/java/example/AmountContractTest.java", "a".repeat(64)));
        VersionOneApprovalPayload approvalPayload = new VersionOneApprovalPayload(
                "topplecat.contract-approval.v1", publicFiles, "b".repeat(64), policy);
        String approvalDigest = Hashing.sha256(JSON.writeValueAsBytes(approvalPayload));
        VersionOneApproval approval = new VersionOneApproval(
                approvalPayload.schemaVersion(), approvalPayload.publicFiles(),
                approvalPayload.publicDefinitionDigest(), approvalPayload.verificationPolicy(), approvalDigest);
        String source = JSON.writeValueAsString(new LegacyApprovalManifest(
                EscrowManifest.SCHEMA_VERSION_V2, EscrowState.HIDDEN,
                List.of(new EscrowEntry("src/hiddenTest/java/example/ReviewerTest.java", "c".repeat(64),
                        EscrowSourceKind.HIDDEN_TEST)),
                approval));

        EscrowManifest migrated = EscrowManifestJson.read(source);

        assertEquals(ReviewerContractApproval.SCHEMA_VERSION, migrated.approval().schemaVersion());
        assertEquals(SelectedSpecScope.empty(), migrated.approval().selectedSpecScope());
    }

    private record VersionOneApprovalPayload(
            String schemaVersion,
            List<PublicContractEntry> publicFiles,
            String publicDefinitionDigest,
            VerificationPolicy verificationPolicy
    ) {
    }

    private record VersionOneApproval(
            String schemaVersion,
            List<PublicContractEntry> publicFiles,
            String publicDefinitionDigest,
            VerificationPolicy verificationPolicy,
            String approvalDigest
    ) {
    }

    private record LegacyApprovalManifest(
            String schemaVersion,
            EscrowState state,
            List<EscrowEntry> entries,
            VersionOneApproval approval
    ) {
    }
}
