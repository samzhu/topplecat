package io.github.samzhu.topplecat.core;

import java.util.List;
import java.util.Objects;

/** Compiler-resolved shape of one canonical {@code @ToppleTest} method. */
public record ScenarioTemplate(String scenarioId, String canonicalMethodIdentity, SourceRef sourceRef,
                               List<StepTemplate> steps) {
    public ScenarioTemplate {
        requireText(scenarioId, "scenarioId");
        requireText(canonicalMethodIdentity, "canonicalMethodIdentity");
        sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (steps.isEmpty()) {
            throw new ToppleCatException("Scenario " + scenarioId + " requires at least one Stage step.");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ToppleCatException("Scenario " + field + " is required.");
        }
    }
}
