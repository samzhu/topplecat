package io.github.samzhu.topplecat.core;

import java.util.Objects;

/** Structured presentation token; it is intentionally not one pre-rendered sentence. */
public record StepToken(StepTokenKind kind, String value) {
  public StepToken {
    kind = Objects.requireNonNull(kind, "kind");
    if (value == null) {
      throw new ToppleCatException("Step token value is required.");
    }
  }
}
