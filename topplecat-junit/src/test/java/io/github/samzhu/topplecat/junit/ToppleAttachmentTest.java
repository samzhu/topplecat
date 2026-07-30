package io.github.samzhu.topplecat.junit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ToppleAttachmentTest {
  @Test
  void rejectsPerFileOversizeContentAndKeepsUserTitlesOutOfAssetNaming() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ToppleAttachment.png("too large", new byte[(int) ToppleAttachment.MAX_BYTES + 1]));
    assertDoesNotThrow(() -> ToppleAttachment.json("../../untrusted-name.json", "{\"ok\":true}"));
  }
}
