package io.github.samzhu.topplecat.report;

/** Public static metadata for a supplementary Property; it intentionally has no execution data. */
public record SpecProperty(
    String title,
    String methodIdentity,
    String sourceFile,
    long sourceLine,
    int tries,
    int maxDiscards,
    int maxShrinks) {
  public SpecProperty {
    if (blank(title)
        || blank(methodIdentity)
        || blank(sourceFile)
        || sourceLine < 1
        || tries < 1
        || maxDiscards < 0
        || maxShrinks < 0) {
      throw new IllegalArgumentException("Spec Property metadata is invalid.");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
