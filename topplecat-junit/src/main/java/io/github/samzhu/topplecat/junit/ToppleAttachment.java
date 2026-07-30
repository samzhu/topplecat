package io.github.samzhu.topplecat.junit;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A small allowlisted, step-scoped attachment request. Visibility is inherited from the current
 * case.
 */
public final class ToppleAttachment {
  static final long MAX_BYTES = 10L * 1024 * 1024;
  private final String title;
  private final String mediaType;
  private final String extension;
  private final byte[] content;

  private ToppleAttachment(String title, String mediaType, String extension, byte[] content) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("Topple attachment title is required.");
    }
    this.title = title;
    this.mediaType = mediaType;
    this.extension = extension;
    this.content = Objects.requireNonNull(content, "content").clone();
    if (this.content.length > MAX_BYTES) {
      throw new IllegalArgumentException(
          "Topple attachment " + title + " exceeds the 10 MiB per-file limit.");
    }
  }

  public static ToppleAttachment png(String title, byte[] bytes) {
    return new ToppleAttachment(title, "image/png", "png", bytes);
  }

  public static ToppleAttachment jpeg(String title, byte[] bytes) {
    return new ToppleAttachment(title, "image/jpeg", "jpg", bytes);
  }

  public static ToppleAttachment json(String title, String value) {
    return textLike(title, "application/json", "json", value, UnaryOperator.identity());
  }

  public static ToppleAttachment text(String title, String value) {
    return textLike(title, "text/plain; charset=utf-8", "txt", value, UnaryOperator.identity());
  }

  /** Allows the caller to apply domain-specific redaction before ToppleCat's default masking. */
  public static ToppleAttachment text(String title, String value, UnaryOperator<String> redactor) {
    return textLike(title, "text/plain; charset=utf-8", "txt", value, redactor);
  }

  /** Allows the caller to apply domain-specific redaction before ToppleCat's default masking. */
  public static ToppleAttachment json(String title, String value, UnaryOperator<String> redactor) {
    return textLike(title, "application/json", "json", value, redactor);
  }

  private static ToppleAttachment textLike(
      String title,
      String mediaType,
      String extension,
      String value,
      UnaryOperator<String> redactor) {
    String redacted =
        Objects.requireNonNull(redactor, "redactor").apply(Objects.requireNonNull(value, "value"));
    return new ToppleAttachment(
        title, mediaType, extension, redacted.getBytes(StandardCharsets.UTF_8));
  }

  String title() {
    return title;
  }

  String mediaType() {
    return mediaType;
  }

  String extension() {
    return extension;
  }

  byte[] content() {
    return content.clone();
  }
}
