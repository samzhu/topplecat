package io.github.samzhu.topplecat.report;

/** Position of an AC anchor in a selected Spec document; it does not select a second scope. */
public record ReviewAcLocation(String documentPath, int documentPosition) {
  public ReviewAcLocation {
    documentPath = documentPath == null ? "" : documentPath;
    if (documentPosition < 0) {
      throw new IllegalArgumentException("Selected Spec AC position cannot be negative.");
    }
  }

  public static ReviewAcLocation unavailable() {
    return new ReviewAcLocation("", 0);
  }
}
