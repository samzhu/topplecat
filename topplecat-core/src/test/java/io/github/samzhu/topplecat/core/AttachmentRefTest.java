package io.github.samzhu.topplecat.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttachmentRefTest {
    private static final String DIGEST = "a".repeat(64);

    @Test
    void permitsOnlyContentAddressedAllowlistedAssets() {
        assertDoesNotThrow(() -> new AttachmentRef(DIGEST, "receipt", "image/png", 12,
                CaseVisibility.HIDDEN, "attachments/" + DIGEST + ".png"));
        assertThrows(ToppleCatException.class, () -> new AttachmentRef(DIGEST, "receipt", "image/svg+xml", 12,
                CaseVisibility.HIDDEN, "attachments/" + DIGEST + ".svg"));
        assertThrows(ToppleCatException.class, () -> new AttachmentRef(DIGEST, "receipt", "image/png", 12,
                CaseVisibility.HIDDEN, "attachments/../receipt.png"));
    }
}
