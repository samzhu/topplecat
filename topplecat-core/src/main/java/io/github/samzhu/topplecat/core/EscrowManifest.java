package io.github.samzhu.topplecat.core;

import java.util.List;

/** Versioned description of the reviewer-only source and its active reviewer approval epoch. */
public record EscrowManifest(
    String schemaVersion,
    EscrowState state,
    List<EscrowEntry> entries,
    ReviewerContractApproval approval) {
  public static final String SCHEMA_VERSION_V2 = "topplecat.escrow.v2";
  public static final String SCHEMA_VERSION = SCHEMA_VERSION_V2;

  public EscrowManifest {
    if (!SCHEMA_VERSION_V2.equals(schemaVersion)) {
      throw new ToppleCatException("Unsupported escrow schema: " + schemaVersion);
    }
    if (state == null || approval == null) {
      throw new ToppleCatException("Escrow manifest state or approval is invalid.");
    }
    entries = List.copyOf(entries == null ? List.of() : entries);
  }
}
