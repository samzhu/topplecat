package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** Reviewer-only count of the raw PIT results emitted by one mutator identity. */
public record PitMutatorSummary(
    String mutator, int mutantCount, List<PitOutcomeCount> outcomeCounts) {
  public PitMutatorSummary {
    if (mutator == null || mutator.isBlank() || mutantCount < 0) {
      throw new ToppleCatException("PIT per-mutator summary is invalid.");
    }
    outcomeCounts = List.copyOf(outcomeCounts == null ? List.of() : outcomeCounts);
  }
}
