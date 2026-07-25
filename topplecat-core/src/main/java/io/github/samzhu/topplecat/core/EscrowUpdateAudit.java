package io.github.samzhu.topplecat.core;

import java.time.Instant;

/** Reviewer-local summary of one explicit escrow custody update. */
record EscrowUpdateAudit(
        String schemaVersion,
        Instant updatedAt,
        String previousManifestSha256,
        String newManifestSha256,
        int added,
        int changed,
        int removed
) {
    static final String SCHEMA_VERSION = "topplecat.escrow-update.v1";

    EscrowUpdateAudit {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new ToppleCatException("Unsupported escrow update audit schema: " + schemaVersion);
        }
        if (updatedAt == null || !digest(previousManifestSha256) || !digest(newManifestSha256)) {
            throw new ToppleCatException("Escrow update audit metadata is invalid.");
        }
        if (added < 0 || changed < 0 || removed < 0) {
            throw new ToppleCatException("Escrow update audit counts cannot be negative.");
        }
    }

    private static boolean digest(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }
}
