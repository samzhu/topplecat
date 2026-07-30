package io.github.samzhu.topplecat.core;

import java.util.Objects;

/** Checked static definition of one supplementary property bound to an acceptance condition. */
public record PropertyDefinition(
    String acId,
    String methodIdentity,
    String title,
    int tries,
    int maxDiscards,
    int maxShrinks,
    SourceRef sourceRef,
    String sourceDigest) {
  public PropertyDefinition {
    requireText(acId, "acId");
    requireText(methodIdentity, "methodIdentity");
    requireText(title, "title");
    sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
    if (tries < 1
        || tries > 100_000
        || maxDiscards < 0
        || maxShrinks < 0
        || sourceDigest == null
        || !sourceDigest.matches("[0-9a-f]{64}")) {
      throw new ToppleCatException("Property definition is invalid.");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("Property definition " + field + " is required.");
    }
  }
}
