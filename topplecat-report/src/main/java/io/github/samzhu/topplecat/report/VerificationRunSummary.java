package io.github.samzhu.topplecat.report;

import java.time.Instant;

/** Current-run metadata shown at the head of the reviewer-only Verification Report. */
public record VerificationRunSummary(
    String runId,
    Instant startedAt,
    Instant finishedAt,
    int failedGateCount,
    int incompleteGateCount,
    int failedAcceptanceConditionCount,
    int failedCaseCount,
    int passedAcceptanceConditionCount,
    int incompleteAcceptanceConditionCount) {
  public VerificationRunSummary(
      String runId,
      Instant startedAt,
      Instant finishedAt,
      int failedGateCount,
      int incompleteGateCount,
      int failedAcceptanceConditionCount,
      int failedCaseCount) {
    this(
        runId,
        startedAt,
        finishedAt,
        failedGateCount,
        incompleteGateCount,
        failedAcceptanceConditionCount,
        failedCaseCount,
        0,
        0);
  }

  public VerificationRunSummary {
    runId = runId == null ? "" : runId;
    failedGateCount = nonNegative(failedGateCount);
    incompleteGateCount = nonNegative(incompleteGateCount);
    failedAcceptanceConditionCount = nonNegative(failedAcceptanceConditionCount);
    failedCaseCount = nonNegative(failedCaseCount);
    passedAcceptanceConditionCount = nonNegative(passedAcceptanceConditionCount);
    incompleteAcceptanceConditionCount = nonNegative(incompleteAcceptanceConditionCount);
  }

  public static VerificationRunSummary unavailable() {
    return new VerificationRunSummary("", null, null, 0, 0, 0, 0, 0, 0);
  }

  private static int nonNegative(int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Verification run count cannot be negative.");
    }
    return value;
  }
}
