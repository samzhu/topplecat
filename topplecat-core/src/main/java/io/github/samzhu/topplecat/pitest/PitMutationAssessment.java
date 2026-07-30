package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** Reviewer-visible per-AC mutation result derived from PIT's full mutation matrix. */
public record PitMutationAssessment(
    String acId,
    List<String> testClasses,
    int threshold,
    int totalMutations,
    int detectedMutations,
    int mutationScore,
    EvidenceVerdict verdict) {
  public PitMutationAssessment {
    if (acId == null || acId.isBlank()) {
      throw new ToppleCatException("Mutation assessment AC id is required.");
    }
    if (threshold < 0 || threshold > 100) {
      throw new ToppleCatException("Mutation assessment threshold must be between 0 and 100.");
    }
    if (totalMutations < 0 || detectedMutations < 0 || detectedMutations > totalMutations) {
      throw new ToppleCatException("Mutation assessment counts are invalid for " + acId + ".");
    }
    testClasses = List.copyOf(testClasses == null ? List.of() : testClasses);
    if (verdict == null) {
      throw new ToppleCatException("Mutation assessment verdict is required.");
    }
  }
}
