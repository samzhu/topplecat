package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.EvidenceVerdict;

/** Assessed result of one fixed safeguard section inside an AC card. */
public record VerificationSafeguard(
    String name, EvidenceVerdict verdict, String explanation, String technicalGate) {
  public VerificationSafeguard {
    if (name == null || name.isBlank() || verdict == null) {
      throw new IllegalArgumentException("Verification safeguard name and verdict are required.");
    }
    explanation = explanation == null ? "" : explanation;
    technicalGate = technicalGate == null ? "" : technicalGate;
  }
}
