package io.github.samzhu.topplecat.report;

import tools.jackson.databind.JsonNode;

/** Public contract row safe for agents and non-reviewer readers. */
public record SpecCase(String caseId, JsonNode inputs, JsonNode expected) {
    public SpecCase {
        inputs = inputs.deepCopy();
        expected = expected.deepCopy();
    }
}
