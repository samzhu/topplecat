package io.github.samzhu.topplecat.core;

/** Natural-language phase inferred from the Stage field name and/or method owner. */
public enum StepPhase {
  GIVEN,
  WHEN,
  THEN,
  AND
}
