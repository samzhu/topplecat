package io.github.samzhu.topplecat.report;

/** Position of an AC load point in a selected Spec document. */
public record ReviewAcLocation(String documentPath, int documentPosition, String insertionPointId) {
  public ReviewAcLocation {
    documentPath = documentPath == null ? "" : documentPath;
    if (documentPosition < 0 || (!documentPath.isBlank() && documentPosition == 0)) {
      throw new IllegalArgumentException(
          "Selected Spec AC position must be one-based or unavailable.");
    }
    insertionPointId = insertionPointId == null ? "" : insertionPointId;
  }

  public ReviewAcLocation(String documentPath, int documentPosition) {
    this(documentPath, documentPosition, "");
  }

  public static ReviewAcLocation unavailable() {
    return new ReviewAcLocation("", 0, "");
  }
}
