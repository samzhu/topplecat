package io.github.samzhu.topplecat.core;

/** Immutable reviewer-only reference to a content-addressed attachment asset. */
public record AttachmentRef(
    String sha256,
    String title,
    String mediaType,
    long size,
    CaseVisibility visibility,
    String relativePath) {
  public AttachmentRef {
    if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
      throw new ToppleCatException("Attachment sha256 must be a lowercase SHA-256 digest.");
    }
    if (title == null
        || title.isBlank()
        || mediaType == null
        || mediaType.isBlank()
        || size < 0
        || relativePath == null
        || relativePath.isBlank()) {
      throw new ToppleCatException("Attachment reference is incomplete.");
    }
    visibility = java.util.Objects.requireNonNull(visibility, "visibility");
    String extension =
        switch (mediaType) {
          case "image/png" -> "png";
          case "image/jpeg" -> "jpg";
          case "application/json" -> "json";
          case "text/plain; charset=utf-8" -> "txt";
          default ->
              throw new ToppleCatException(
                  "Attachment media type is not allowlisted: " + mediaType);
        };
    if (!relativePath.equals("attachments/" + sha256 + "." + extension)) {
      throw new ToppleCatException(
          "Attachment reference must use its content-addressed local asset path.");
    }
  }
}
