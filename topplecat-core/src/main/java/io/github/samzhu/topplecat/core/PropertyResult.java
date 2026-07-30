package io.github.samzhu.topplecat.core;

import java.util.Comparator;
import java.util.List;

/** One terminal Property execution result, deliberately separate from authored case-row runs. */
public record PropertyResult(
    String acId,
    String methodIdentity,
    PropertyExecutionState state,
    int requestedTrials,
    int completedTrials,
    int edgeTrials,
    int randomTrials,
    int discards,
    List<PropertyClassification> classifications,
    long seed,
    boolean replayVerified,
    String replayToken,
    PropertyCounterexample originalCounterexample,
    PropertyCounterexample shrunkCounterexample,
    int shrinkAttempts,
    boolean shrinkComplete,
    String incompleteReason) {
  public PropertyResult {
    required(acId, "acId");
    required(methodIdentity, "methodIdentity");
    if (state == null
        || !state.terminal()
        || requestedTrials < 1
        || completedTrials < 0
        || completedTrials > requestedTrials
        || edgeTrials < 0
        || randomTrials < 0
        || discards < 0
        || edgeTrials + randomTrials != completedTrials
        || shrinkAttempts < 0) {
      throw new ToppleCatException("Property result is invalid.");
    }
    classifications =
        classifications == null
            ? List.of()
            : classifications.stream()
                .sorted(Comparator.comparing(PropertyClassification::label))
                .toList();
    if (classifications.stream().map(PropertyClassification::label).distinct().count()
        != classifications.size()) {
      throw new ToppleCatException("Property result has duplicate classification labels.");
    }
    switch (state) {
      case COMPLETED_PASS -> {
        if (completedTrials != requestedTrials
            || originalCounterexample != null
            || shrunkCounterexample != null
            || shrinkAttempts != 0
            || shrinkComplete
            || incompleteReason != null) {
          throw new ToppleCatException(
              "A passing Property result must contain all passing trials only.");
        }
      }
      case COMPLETED_COUNTEREXAMPLE -> {
        if (!replayVerified
            || originalCounterexample == null
            || shrunkCounterexample == null
            || incompleteReason != null) {
          throw new ToppleCatException(
              "A counterexample result must be reproducible and include both presentations.");
        }
      }
      case COMPLETED_INCOMPLETE -> {
        required(incompleteReason, "incompleteReason");
      }
      case STARTED -> throw new ToppleCatException("A Property result requires a terminal state.");
    }
  }

  private static void required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("Property result " + field + " is required.");
    }
  }
}
