package io.github.samzhu.topplecat.report;

import java.util.List;

/** Current-run public Property evidence, kept separate from case-row execution. */
public record VerificationProperty(
    String title,
    String methodIdentity,
    String status,
    int requestedTrials,
    int completedTrials,
    int edgeTrials,
    int randomTrials,
    int discards,
    List<VerificationClassification> classifications,
    long seed,
    boolean replayVerified,
    String replayToken,
    VerificationCounterexample originalCounterexample,
    VerificationCounterexample shrunkCounterexample,
    int shrinkAttempts,
    boolean shrinkComplete,
    List<VerificationDiscardedInput> discardedInputs,
    String incompleteReason) {
  public VerificationProperty(
      String title,
      String methodIdentity,
      String status,
      int requestedTrials,
      int completedTrials,
      int edgeTrials,
      int randomTrials,
      int discards,
      List<VerificationClassification> classifications,
      long seed,
      boolean replayVerified,
      String replayToken,
      VerificationCounterexample originalCounterexample,
      VerificationCounterexample shrunkCounterexample,
      int shrinkAttempts,
      boolean shrinkComplete,
      String incompleteReason) {
    this(
        title,
        methodIdentity,
        status,
        requestedTrials,
        completedTrials,
        edgeTrials,
        randomTrials,
        discards,
        classifications,
        seed,
        replayVerified,
        replayToken,
        originalCounterexample,
        shrunkCounterexample,
        shrinkAttempts,
        shrinkComplete,
        List.of(),
        incompleteReason);
  }

  public VerificationProperty {
    if (title == null
        || title.isBlank()
        || methodIdentity == null
        || methodIdentity.isBlank()
        || !(status.equals("PASS") || status.equals("FAIL") || status.equals("INCOMPLETE"))
        || requestedTrials < 1
        || completedTrials < 0
        || completedTrials > requestedTrials
        || edgeTrials < 0
        || randomTrials < 0
        || discards < 0
        || edgeTrials + randomTrials != completedTrials
        || shrinkAttempts < 0) {
      throw new IllegalArgumentException("Verification Property is invalid.");
    }
    classifications = List.copyOf(classifications == null ? List.of() : classifications);
    discardedInputs = List.copyOf(discardedInputs == null ? List.of() : discardedInputs);
    if (discards != discardedInputs.size()) {
      throw new IllegalArgumentException(
          "Verification Property discard count must match discarded inputs.");
    }
  }
}
