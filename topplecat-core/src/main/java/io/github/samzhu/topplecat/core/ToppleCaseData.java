package io.github.samzhu.topplecat.core;

import tools.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable, typed case data loaded from a public or reviewer-only JSON/YAML source. */
public record ToppleCaseData(
        String caseId,
        String acId,
        CaseVisibility visibility,
        JsonNode inputs,
        JsonNode expected,
        Path source
) {
    public ToppleCaseData {
        requireText(caseId, "caseId");
        requireText(acId, "acId");
        visibility = Objects.requireNonNull(visibility, "visibility");
        inputs = objectCopy(inputs, "inputs", caseId);
        expected = objectCopy(expected, "expected", caseId);
        if (expected.isEmpty()) {
            throw new ToppleCatException("Topple case " + caseId + " requires at least one expected value.");
        }
        source = Objects.requireNonNull(source, "source").normalize();
    }

    private static JsonNode objectCopy(JsonNode value, String field, String caseId) {
        if (value == null || !value.isObject()) {
            throw new ToppleCatException("Topple case " + caseId + " requires an object " + field + ".");
        }
        return value.deepCopy();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ToppleCatException("Topple case " + field + " is required.");
        }
    }
}
