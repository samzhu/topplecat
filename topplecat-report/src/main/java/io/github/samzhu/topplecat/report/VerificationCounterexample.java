package io.github.samzhu.topplecat.report;

import java.util.List;

/** Canonical generator-choice JSON for one Property failure in Verification Evidence. */
public record VerificationCounterexample(String choicesJson, List<Integer> shrinkPath) {
  public VerificationCounterexample {
    if (choicesJson == null || choicesJson.isBlank()) {
      throw new IllegalArgumentException(
          "Verification Property counterexample choices are required.");
    }
    shrinkPath = List.copyOf(shrinkPath == null ? List.of() : shrinkPath);
  }
}
