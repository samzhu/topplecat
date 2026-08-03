package io.github.samzhu.topplecat.report;

/** Reader-facing meaning of one AC safeguard, separate from its canonical Gate verdict. */
public enum VerificationSafeguardOutcome {
  PASSED,
  PROBLEM_FOUND,
  COMPARISON_COMPLETED,
  UNABLE_TO_ASSESS,
  DISABLED,
  NOT_APPLICABLE
}
