package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.CaseVisibility;
import tools.jackson.databind.JsonNode;

import java.util.List;

/** Reviewer-only authoring row with no execution result. */
public record ReviewCase(CaseVisibility visibility, String caseId, JsonNode inputs, JsonNode expected,
                         List<ReviewScenarioStep> scenario) {
    public ReviewCase {
        inputs = inputs.deepCopy();
        expected = expected.deepCopy();
        scenario = List.copyOf(scenario == null ? List.of() : scenario);
    }

    /** Compatibility constructor for callers that only have AC-level static sentences. */
    public ReviewCase(CaseVisibility visibility, String caseId, JsonNode inputs, JsonNode expected) {
        this(visibility, caseId, inputs, expected, List.of());
    }
}
