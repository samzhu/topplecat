package io.github.samzhu.topplecat.core;

import java.util.List;

/** Renders only compiler-owned template tokens; it has no Java-source interpretation path. */
public final class ScenarioTemplateRenderer {
    private ScenarioTemplateRenderer() {
    }

    public static String render(StepTemplate step, List<String> values) {
        StringBuilder result = new StringBuilder(phase(step.phase()));
        for (StepToken token : step.tokens()) {
            if (token.kind() == StepTokenKind.PHASE) {
                continue;
            }
            if (token.kind() == StepTokenKind.ARGUMENT) {
                int index = Integer.parseInt(token.value());
                result.append(index < values.size() ? values.get(index) : "<value" + index + ">");
            } else {
                result.append(token.value());
            }
        }
        return result.toString().trim();
    }

    public static String template(StepTemplate step) {
        return render(step, step.argumentBindings().stream().map(binding -> "<" + binding.displayName() + ">").toList());
    }

    private static String phase(StepPhase phase) {
        return switch (phase) {
            case GIVEN -> "Given ";
            case WHEN -> "When ";
            case THEN -> "Then ";
            case AND -> "And ";
        };
    }
}
