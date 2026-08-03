package io.github.samzhu.topplecat.report;

/** Reviewer-only canonical presentation of one generator choice discarded by a Property. */
public record VerificationDiscardedInput(String choicesJson) {
  public VerificationDiscardedInput {
    if (choicesJson == null || choicesJson.isBlank()) {
      throw new IllegalArgumentException("A discarded Property input requires choices.");
    }
  }
}
