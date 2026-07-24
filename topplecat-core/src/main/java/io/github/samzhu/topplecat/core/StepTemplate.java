package io.github.samzhu.topplecat.core;

import java.util.List;
import java.util.Objects;

/** A single compiler-resolved Stage method invocation in source order. */
public record StepTemplate(String stepId, StepPhase phase, List<StepToken> tokens,
                           List<ArgumentBinding> argumentBindings, SourceRef sourceRef) {
    public StepTemplate {
        if (stepId == null || stepId.isBlank()) {
            throw new ToppleCatException("Step id is required.");
        }
        phase = Objects.requireNonNull(phase, "phase");
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        argumentBindings = List.copyOf(Objects.requireNonNull(argumentBindings, "argumentBindings"));
        sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
        for (StepToken token : tokens) {
            if (token.kind() != StepTokenKind.ARGUMENT) {
                continue;
            }
            try {
                int index = Integer.parseInt(token.value());
                if (index < 0 || index >= argumentBindings.size()) {
                    throw new ToppleCatException("Step template " + stepId + " references missing argument {" + token.value() + "}.");
                }
            } catch (NumberFormatException exception) {
                throw new ToppleCatException("Step template " + stepId + " has a non-numeric argument token.", exception);
            }
        }
    }
}
