package io.github.samzhu.topplecat.core;

/** Current-run count and declared coverage requirement for one Property classification. */
public record PropertyClassification(
    String label, int count, double percent, Double minimumPercent) {
  public PropertyClassification {
    if (label == null
        || label.isBlank()
        || count < 0
        || !percentage(percent)
        || minimumPercent != null && !percentage(minimumPercent)) {
      throw new ToppleCatException("Property classification is invalid.");
    }
  }

  private static boolean percentage(double value) {
    return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0 && value <= 100.0;
  }
}
