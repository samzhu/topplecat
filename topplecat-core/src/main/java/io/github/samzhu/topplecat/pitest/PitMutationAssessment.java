package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** Reviewer-visible per-AC mutation result derived from PIT's full mutation matrix. */
public record PitMutationAssessment(
    String acId,
    List<String> acceptanceMethods,
    int coveredMutantCount,
    int killedByAcceptanceMethodMutantCount,
    int sealedThreshold,
    int detectionRate,
    List<PitOutcomeCount> pitOutcomeCounts,
    boolean attributionGap) {
  public PitMutationAssessment {
    if (acId == null || acId.isBlank()) {
      throw new ToppleCatException("Mutation assessment AC id is required.");
    }
    if (sealedThreshold < 0 || sealedThreshold > 100) {
      throw new ToppleCatException("Mutation assessment threshold must be between 0 and 100.");
    }
    if (coveredMutantCount < 0
        || killedByAcceptanceMethodMutantCount < 0
        || killedByAcceptanceMethodMutantCount > coveredMutantCount
        || detectionRate < 0
        || detectionRate > 100) {
      throw new ToppleCatException("Mutation assessment counts are invalid for " + acId + ".");
    }
    acceptanceMethods = List.copyOf(acceptanceMethods == null ? List.of() : acceptanceMethods);
    pitOutcomeCounts = List.copyOf(pitOutcomeCounts == null ? List.of() : pitOutcomeCounts);
    if (attributionGap != (coveredMutantCount == 0)) {
      throw new ToppleCatException(
          "Mutation assessment attribution-gap state must match its covered mutant count.");
    }
  }
}
