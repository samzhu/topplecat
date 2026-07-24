package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;

import java.util.List;

/** Presentation-only rendering of the compiler descriptor; it never reads Java expressions. */
final class ScenarioText {
    private ScenarioText() {
    }

    static List<String> render(List<StepTemplate> steps) {
        return steps.stream().map(ScenarioText::render).toList();
    }

    static String render(StepTemplate step) {
        StringBuilder text = new StringBuilder(phase(step));
        for (StepToken token : step.tokens()) {
            if (token.kind() == StepTokenKind.PHASE) {
                continue;
            }
            if (token.kind() == StepTokenKind.ARGUMENT) {
                int index = Integer.parseInt(token.value());
                String name = index < step.argumentBindings().size()
                        ? step.argumentBindings().get(index).displayName() : "value" + index;
                text.append('<').append(name).append('>');
            } else {
                text.append(token.value());
            }
        }
        return text.toString().trim();
    }

    private static String phase(StepTemplate step) {
        return switch (step.phase()) {
            case GIVEN -> "Given ";
            case WHEN -> "When ";
            case THEN -> "Then ";
            case AND -> "And ";
        };
    }
}
