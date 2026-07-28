package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/** Immutable, tamper-evident selection of external Spec documents and their executable AC IDs. */
public record SelectedSpecScope(
        String schemaVersion,
        List<SelectedSpecDocument> specDocuments,
        List<String> acceptanceConditionIds,
        String acceptanceConditionSetDigest
) {
    public static final String SCHEMA_VERSION = "topplecat.selected-spec-scope.v1";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public SelectedSpecScope {
        specDocuments = List.copyOf(specDocuments == null ? List.of() : specDocuments);
        acceptanceConditionIds = List.copyOf(acceptanceConditionIds == null ? List.of() : acceptanceConditionIds);
        if (!SCHEMA_VERSION.equals(schemaVersion) || !sortedDocuments(specDocuments)
                || !sortedAcceptanceConditions(acceptanceConditionIds)
                || acceptanceConditionSetDigest == null || !acceptanceConditionSetDigest.matches("[0-9a-f]{64}")
                || !acceptanceConditionSetDigest.equals(digest(specDocuments, acceptanceConditionIds))) {
            throw new ToppleCatException("Selected Spec scope is invalid.");
        }
    }

    public static SelectedSpecScope create(List<SelectedSpecDocument> specDocuments, List<String> acceptanceConditionIds) {
        List<SelectedSpecDocument> documents = new ArrayList<>(specDocuments == null ? List.of() : specDocuments);
        documents.sort(SelectedSpecDocument::compareTo);
        List<String> acIds = new ArrayList<>(acceptanceConditionIds == null ? List.of() : acceptanceConditionIds);
        acIds.sort(String::compareTo);
        return new SelectedSpecScope(SCHEMA_VERSION, documents, acIds, digest(documents, acIds));
    }

    public static SelectedSpecScope empty() {
        return create(List.of(), List.of());
    }

    /** True when a delivery selected one or more external Spec documents. */
    public boolean selected() {
        return !specDocuments.isEmpty();
    }

    private static String digest(List<SelectedSpecDocument> documents, List<String> acIds) {
        return Hashing.sha256(JSON.writeValueAsBytes(new DigestPayload(SCHEMA_VERSION, documents, acIds)));
    }

    private static boolean sortedDocuments(List<SelectedSpecDocument> documents) {
        SelectedSpecDocument previous = null;
        for (SelectedSpecDocument document : documents) {
            if (document == null || previous != null && previous.compareTo(document) >= 0) {
                return false;
            }
            previous = document;
        }
        return true;
    }

    private static boolean sortedAcceptanceConditions(List<String> acIds) {
        String previous = null;
        for (String acId : acIds) {
            if (acId == null || !acId.matches("AC-[A-Za-z0-9][A-Za-z0-9-]*")
                    || previous != null && previous.compareTo(acId) >= 0) {
                return false;
            }
            previous = acId;
        }
        return true;
    }

    private record DigestPayload(String schemaVersion, List<SelectedSpecDocument> specDocuments,
                                 List<String> acceptanceConditionIds) {
    }
}
