package io.github.samzhu.topplecat.core;

import java.util.List;
import java.util.Objects;

/**
 * One javac-emitted descriptor. It contains no case values and can therefore be safely
 * produced during compilation before the Gradle plugin joins it with public/reviewer data.
 */
public record CompilerScenarioDescriptor(
        String schemaVersion,
        String acId,
        String title,
        String scenarioId,
        String declaringBinaryName,
        String methodName,
        String methodDescriptor,
        SourceRef sourceRef,
        List<StepTemplate> steps
) {
    public static final String SCHEMA_VERSION = "topplecat.compiler-scenario.v1";

    public CompilerScenarioDescriptor {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new ToppleCatException("Unsupported compiler scenario schema: " + schemaVersion);
        }
        requireText(acId, "acId");
        requireText(title, "title");
        requireText(scenarioId, "scenarioId");
        requireText(declaringBinaryName, "declaringBinaryName");
        requireText(methodName, "methodName");
        requireText(methodDescriptor, "methodDescriptor");
        sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (steps.isEmpty()) {
            throw new ToppleCatException("Compiler scenario " + scenarioId + " requires at least one step.");
        }
    }

    public ScenarioTemplate scenario() {
        return new ScenarioTemplate(scenarioId, declaringBinaryName + "#" + methodName + methodDescriptor,
                sourceRef, steps);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ToppleCatException("Compiler scenario " + field + " is required.");
        }
    }
}
