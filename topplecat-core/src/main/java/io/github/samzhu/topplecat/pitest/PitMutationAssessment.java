package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** Reviewer-visible per-AC mutation result derived from PIT's full mutation matrix. */
public record PitMutationAssessment(
    String acId,
    List<String> acceptanceMethods,
    int coveredMutantCount,
    int killedByAcceptanceMethodMutantCount,
    List<PitOutcomeCount> pitOutcomeCounts,
    boolean attributionGap) {
  /** Compatibility constructor for the retired percentage projection. */
  public PitMutationAssessment(
      String acId,
      List<String> acceptanceMethods,
      int coveredMutantCount,
      int killedByAcceptanceMethodMutantCount,
      int ignoredSealedThreshold,
      int ignoredDetectionRate,
      List<PitOutcomeCount> pitOutcomeCounts,
      boolean attributionGap) {
    this(
        acId,
        acceptanceMethods,
        coveredMutantCount,
        killedByAcceptanceMethodMutantCount,
        pitOutcomeCounts,
        attributionGap);
  }

  public PitMutationAssessment {
    if (acId == null || acId.isBlank()) {
      throw new ToppleCatException("Mutation assessment AC id is required.");
    }
    if (coveredMutantCount < 0
        || killedByAcceptanceMethodMutantCount < 0
        || killedByAcceptanceMethodMutantCount > coveredMutantCount) {
      throw new ToppleCatException("Mutation assessment counts are invalid for " + acId + ".");
    }
    acceptanceMethods = List.copyOf(acceptanceMethods == null ? List.of() : acceptanceMethods);
    pitOutcomeCounts = List.copyOf(pitOutcomeCounts == null ? List.of() : pitOutcomeCounts);
    if (attributionGap != (coveredMutantCount == 0)) {
      throw new ToppleCatException(
          "Mutation assessment attribution-gap state must match its covered mutant count.");
    }
  }

  /** Number of covered mutations the owning Acceptance Method detected. */
  public int detectedCount() {
    return killedByAcceptanceMethodMutantCount;
  }

  /**
   * @deprecated percentage scores are no longer a Mutation Gate policy.
   */
  @Deprecated
  public int sealedThreshold() {
    return 100;
  }

  /**
   * @deprecated percentage scores are no longer a Mutation Gate policy.
   */
  @Deprecated
  public int detectionRate() {
    return coveredMutantCount == 0
        ? 0
        : (killedByAcceptanceMethodMutantCount * 100) / coveredMutantCount;
  }
}
