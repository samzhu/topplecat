package io.github.samzhu.topplecat.core;

/** A compiler-validated argument slot and, when known, its case-data JSON pointer. */
public record ArgumentBinding(int index, String displayName, String jsonPointer) {
  public ArgumentBinding {
    if (index < 0) {
      throw new ToppleCatException("Argument binding index must not be negative.");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new ToppleCatException("Argument binding display name is required.");
    }
    jsonPointer = jsonPointer == null ? "" : jsonPointer;
  }
}
