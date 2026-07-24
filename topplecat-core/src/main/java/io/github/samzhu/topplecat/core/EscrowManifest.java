package io.github.samzhu.topplecat.core;

import java.util.List;

/** Versioned description of the reviewer-only source held by escrow. */
public record EscrowManifest(String schemaVersion, EscrowState state, List<EscrowEntry> entries) {
    public static final String SCHEMA_VERSION = "topplecat.escrow.v1";

    public EscrowManifest {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new ToppleCatException("Unsupported escrow schema: " + schemaVersion);
        }
        if (state == null) {
            throw new ToppleCatException("Escrow state is required.");
        }
        entries = List.copyOf(entries == null ? List.of() : entries);
    }
}
