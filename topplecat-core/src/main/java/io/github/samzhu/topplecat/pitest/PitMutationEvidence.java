package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** Reviewer-only raw PIT mutation relation with its exact public AC attribution. */
public record PitMutationEvidence(
    boolean detected,
    String status,
    String mutatedClass,
    String mutator,
    String description,
    List<String> coveringTests,
    List<String> killingTests,
    List<String> succeedingTests,
    List<String> attributedAcceptanceConditionIds) {
  public PitMutationEvidence {
    if (status == null
        || status.isBlank()
        || mutatedClass == null
        || mutatedClass.isBlank()
        || mutator == null
        || mutator.isBlank()
        || description == null
        || description.isBlank()) {
      throw new ToppleCatException("PIT mutation evidence is incomplete.");
    }
    coveringTests = List.copyOf(coveringTests == null ? List.of() : coveringTests);
    killingTests = List.copyOf(killingTests == null ? List.of() : killingTests);
    succeedingTests = List.copyOf(succeedingTests == null ? List.of() : succeedingTests);
    attributedAcceptanceConditionIds =
        List.copyOf(
            attributedAcceptanceConditionIds == null
                ? List.of()
                : attributedAcceptanceConditionIds);
  }
}
