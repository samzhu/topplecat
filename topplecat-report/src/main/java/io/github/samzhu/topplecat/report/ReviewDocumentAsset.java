package io.github.samzhu.topplecat.report;

/** A repository-local image copied into the offline review bundle. */
public record ReviewDocumentAsset(String sourcePath, String bundlePath, String mediaType) {
  public ReviewDocumentAsset {
    if (sourcePath == null
        || sourcePath.isBlank()
        || sourcePath.startsWith("/")
        || sourcePath.contains("\\")
        || bundlePath == null
        || !bundlePath.matches("assets/spec/[a-f0-9]{64}\\.[a-z0-9]+")
        || mediaType == null
        || !mediaType.startsWith("image/")) {
      throw new IllegalArgumentException("Review document asset is invalid.");
    }
  }
}
