package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.StepPhase;
import java.util.Objects;

/** One compiler-template step resolved against one reviewer-visible case row. */
public record ReviewScenarioStep(StepPhase phase, String sentence) {
  public ReviewScenarioStep {
    phase = Objects.requireNonNull(phase, "phase");
    sentence = Objects.requireNonNull(sentence, "sentence");
  }
}
