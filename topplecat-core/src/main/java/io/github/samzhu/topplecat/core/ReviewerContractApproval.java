package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/** Deterministic reviewer approval of the public executable contract and effective verification policy. */
public record ReviewerContractApproval(
        String schemaVersion,
        List<PublicContractEntry> publicFiles,
        String publicDefinitionDigest,
        VerificationPolicy verificationPolicy,
        String approvalDigest
) {
    public static final String SCHEMA_VERSION = "topplecat.contract-approval.v1";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public ReviewerContractApproval {
        publicFiles = List.copyOf(publicFiles == null ? List.of() : publicFiles);
        if (!SCHEMA_VERSION.equals(schemaVersion) || !digest(publicDefinitionDigest) || verificationPolicy == null
                || !sortedDistinct(publicFiles) || !digest(approvalDigest)) {
            throw new ToppleCatException("Reviewer contract approval is invalid.");
        }
        String expected = digest(publicFiles, publicDefinitionDigest, verificationPolicy);
        if (!expected.equals(approvalDigest)) {
            throw new ToppleCatException("Reviewer contract approval digest validation failed.");
        }
    }

    public static ReviewerContractApproval create(
            List<PublicContractEntry> publicFiles,
            String publicDefinitionDigest,
            VerificationPolicy verificationPolicy
    ) {
        List<PublicContractEntry> canonicalFiles = new ArrayList<>(publicFiles == null ? List.of() : publicFiles);
        canonicalFiles.sort(PublicContractEntry::compareTo);
        String digest = digest(canonicalFiles, publicDefinitionDigest, verificationPolicy);
        return new ReviewerContractApproval(SCHEMA_VERSION, canonicalFiles, publicDefinitionDigest, verificationPolicy, digest);
    }

    static String digest(
            List<PublicContractEntry> publicFiles,
            String publicDefinitionDigest,
            VerificationPolicy verificationPolicy
    ) {
        ApprovalPayload payload = new ApprovalPayload(SCHEMA_VERSION, List.copyOf(publicFiles == null ? List.of() : publicFiles),
                publicDefinitionDigest, verificationPolicy);
        return Hashing.sha256(JSON.writeValueAsBytes(payload));
    }

    private static boolean sortedDistinct(List<PublicContractEntry> entries) {
        String previous = null;
        for (PublicContractEntry entry : entries) {
            if (entry == null || previous != null && previous.compareTo(entry.path()) >= 0) {
                return false;
            }
            previous = entry.path();
        }
        return true;
    }

    private static boolean digest(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private record ApprovalPayload(
            String schemaVersion,
            List<PublicContractEntry> publicFiles,
            String publicDefinitionDigest,
            VerificationPolicy verificationPolicy
    ) {
    }
}
