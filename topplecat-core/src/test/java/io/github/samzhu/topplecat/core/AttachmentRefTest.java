package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AttachmentRefTest {
  private static final String DIGEST = "a".repeat(64);

  @Test
  void permitsOnlyContentAddressedAllowlistedAssets() {
    assertDoesNotThrow(
        () ->
            new AttachmentRef(
                DIGEST,
                "receipt",
                "image/png",
                12,
                CaseVisibility.HIDDEN,
                "attachments/" + DIGEST + ".png"));
    assertThrows(
        ToppleCatException.class,
        () ->
            new AttachmentRef(
                DIGEST,
                "receipt",
                "image/svg+xml",
                12,
                CaseVisibility.HIDDEN,
                "attachments/" + DIGEST + ".svg"));
    assertThrows(
        ToppleCatException.class,
        () ->
            new AttachmentRef(
                DIGEST,
                "receipt",
                "image/png",
                12,
                CaseVisibility.HIDDEN,
                "attachments/../receipt.png"));
  }
}
