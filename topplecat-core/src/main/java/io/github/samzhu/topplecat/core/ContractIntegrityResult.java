package io.github.samzhu.topplecat.core;

import java.util.List;

/** Reviewer-only diagnostics from comparing the active approval with the current public contract. */
public record ContractIntegrityResult(
        String schemaVersion,
        EvidenceVerdict verdict,
        String approvedApprovalDigest,
        String currentApprovalDigest,
        List<String> addedPaths,
        List<String> changedPaths,
        List<String> removedPaths,
        boolean publicDefinitionMatches,
        List<String> changedPolicyFields
) {
    public static final String SCHEMA_VERSION = "topplecat.contract-integrity.v1";

    public ContractIntegrityResult {
        if (!SCHEMA_VERSION.equals(schemaVersion) || verdict == null || verdict == EvidenceVerdict.DISABLED
                || !sortedPaths(addedPaths) || !sortedPaths(changedPaths) || !sortedPaths(removedPaths)
                || !sortedFields(changedPolicyFields) || !optionalDigest(approvedApprovalDigest)
                || !optionalDigest(currentApprovalDigest)) {
            throw new ToppleCatException("Contract integrity result is invalid.");
        }
        addedPaths = List.copyOf(addedPaths == null ? List.of() : addedPaths);
        changedPaths = List.copyOf(changedPaths == null ? List.of() : changedPaths);
        removedPaths = List.copyOf(removedPaths == null ? List.of() : removedPaths);
        changedPolicyFields = List.copyOf(changedPolicyFields == null ? List.of() : changedPolicyFields);
    }

    private static boolean optionalDigest(String value) {
        return value == null || value.matches("[0-9a-f]{64}");
    }

    private static boolean sortedPaths(List<String> values) {
        return sorted(values, true);
    }

    private static boolean sortedFields(List<String> values) {
        return sorted(values, false);
    }

    private static boolean sorted(List<String> values, boolean paths) {
        String previous = null;
        for (String value : values == null ? List.<String>of() : values) {
            if (value == null || value.isBlank() || previous != null && previous.compareTo(value) >= 0
                    || paths && (value.startsWith("/") || value.contains("\\") || value.contains(".."))) {
                return false;
            }
            previous = value;
        }
        return true;
    }
}
