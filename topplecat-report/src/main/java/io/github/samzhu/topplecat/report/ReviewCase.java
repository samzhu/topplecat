package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.CaseVisibility;
import tools.jackson.databind.JsonNode;

/** Reviewer-only authoring row with no execution result. */
public record ReviewCase(CaseVisibility visibility, String caseId, JsonNode inputs, JsonNode expected) {
    public ReviewCase {
        inputs = inputs.deepCopy();
        expected = expected.deepCopy();
    }
}
