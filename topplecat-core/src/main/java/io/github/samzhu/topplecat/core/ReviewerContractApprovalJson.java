package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** JSON codec for the reviewer-local public-contract approval embedded in escrow v2. */
public final class ReviewerContractApprovalJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String SCHEMA_VERSION_V1 = "topplecat.contract-approval.v1";

    private ReviewerContractApprovalJson() {
    }

    public static String write(ReviewerContractApproval approval) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(approval) + "\n";
    }

    public static ReviewerContractApproval read(String source) {
        RawApproval approval = JSON.readValue(source, RawApproval.class);
        if (ReviewerContractApproval.SCHEMA_VERSION.equals(approval.schemaVersion())) {
            return new ReviewerContractApproval(approval.schemaVersion(), approval.publicFiles(),
                    approval.publicDefinitionDigest(), approval.verificationPolicy(), approval.selectedSpecScope(),
                    approval.approvalDigest());
        }
        if (SCHEMA_VERSION_V1.equals(approval.schemaVersion())) {
            validateVersionOne(approval);
            return ReviewerContractApproval.create(approval.publicFiles(), approval.publicDefinitionDigest(),
                    approval.verificationPolicy(), SelectedSpecScope.empty());
        }
        throw new ToppleCatException("Unsupported reviewer contract approval schema: " + approval.schemaVersion());
    }

    private static void validateVersionOne(RawApproval approval) {
        if (approval.selectedSpecScope() != null || approval.publicFiles() == null
                || !digest(approval.publicDefinitionDigest()) || approval.verificationPolicy() == null
                || !digest(approval.approvalDigest()) || !sortedDistinct(approval.publicFiles())) {
            throw new ToppleCatException("Reviewer contract approval is invalid.");
        }
        String expected = Hashing.sha256(JSON.writeValueAsBytes(new VersionOnePayload(
                approval.schemaVersion(), approval.publicFiles(), approval.publicDefinitionDigest(),
                approval.verificationPolicy())));
        if (!expected.equals(approval.approvalDigest())) {
            throw new ToppleCatException("Reviewer contract approval digest validation failed.");
        }
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

    private record RawApproval(
            String schemaVersion,
            List<PublicContractEntry> publicFiles,
            String publicDefinitionDigest,
            VerificationPolicy verificationPolicy,
            SelectedSpecScope selectedSpecScope,
            String approvalDigest
    ) {
    }

    private record VersionOnePayload(
            String schemaVersion,
            List<PublicContractEntry> publicFiles,
            String publicDefinitionDigest,
            VerificationPolicy verificationPolicy
    ) {
    }
}
