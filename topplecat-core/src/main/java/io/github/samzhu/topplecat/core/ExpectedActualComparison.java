package io.github.samzhu.topplecat.core;

import java.util.List;

/** Reviewer-only structured evidence for one failed {@code ToppleCase.verify(...)} call. */
public record ExpectedActualComparison(
    String expectedKey, List<ExpectedActualDifference> differences) {
  public ExpectedActualComparison {
    if (expectedKey == null || expectedKey.isBlank()) {
      throw new ToppleCatException("Expected/actual comparison key is required.");
    }
    differences = List.copyOf(differences == null ? List.of() : differences);
    if (differences.isEmpty()) {
      throw new ToppleCatException("Expected/actual comparison requires a difference.");
    }
  }
}
