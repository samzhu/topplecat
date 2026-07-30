package io.github.samzhu.topplecat.report;

/** Static public Property review card. */
public record ReviewProperty(
    String title,
    String methodIdentity,
    String sourceFile,
    long sourceLine,
    int tries,
    int maxDiscards,
    int maxShrinks,
    String sourceCode) {
  public ReviewProperty {
    if (title == null
        || title.isBlank()
        || methodIdentity == null
        || methodIdentity.isBlank()
        || sourceLine < 1
        || tries < 1
        || maxDiscards < 0
        || maxShrinks < 0
        || sourceCode == null) {
      throw new IllegalArgumentException("Review Property metadata is invalid.");
    }
    if (sourceFile == null || sourceFile.isBlank()) {
      throw new IllegalArgumentException("Review Property metadata requires a public source file.");
    }
  }
}
