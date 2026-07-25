package io.github.samzhu.topplecat.core;

import java.util.List;

/** Versioned description of the reviewer-only source and its active reviewer approval epoch. */
public record EscrowManifest(
        String schemaVersion,
        EscrowState state,
        List<EscrowEntry> entries,
        ReviewerContractApproval approval
) {
    public static final String SCHEMA_VERSION_V1 = "topplecat.escrow.v1";
    public static final String SCHEMA_VERSION_V2 = "topplecat.escrow.v2";
    /** @deprecated Use the explicit version constants when constructing a manifest. */
    @Deprecated(forRemoval = false)
    public static final String SCHEMA_VERSION = SCHEMA_VERSION_V1;

    public EscrowManifest(String schemaVersion, EscrowState state, List<EscrowEntry> entries) {
        this(schemaVersion, state, entries, null);
    }

    public EscrowManifest {
        if (!SCHEMA_VERSION_V1.equals(schemaVersion) && !SCHEMA_VERSION_V2.equals(schemaVersion)) {
            throw new ToppleCatException("Unsupported escrow schema: " + schemaVersion);
        }
        if (state == null || SCHEMA_VERSION_V1.equals(schemaVersion) && approval != null
                || SCHEMA_VERSION_V2.equals(schemaVersion) && approval == null) {
            throw new ToppleCatException("Escrow manifest state or approval is invalid.");
        }
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public boolean isLegacyVersionOne() {
        return SCHEMA_VERSION_V1.equals(schemaVersion);
    }
}
