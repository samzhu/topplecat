package io.github.samzhu.topplecat.report;

/** Reviewer-only observed Property classification. */
public record VerificationClassification(
    String label, int count, double percent, Double minimumPercent) {
  public VerificationClassification {
    if (label == null
        || label.isBlank()
        || count < 0
        || Double.isNaN(percent)
        || percent < 0
        || percent > 100
        || minimumPercent != null
            && (Double.isNaN(minimumPercent) || minimumPercent < 0 || minimumPercent > 100)) {
      throw new IllegalArgumentException("Verification Property classification is invalid.");
    }
  }
}
