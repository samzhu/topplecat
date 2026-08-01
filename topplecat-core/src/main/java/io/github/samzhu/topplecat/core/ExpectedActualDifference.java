package io.github.samzhu.topplecat.core;

import tools.jackson.databind.JsonNode;

/** One deterministic field-level difference from a typed expected-value comparison. */
public record ExpectedActualDifference(String path, Kind kind, JsonNode expected, JsonNode actual) {
  public ExpectedActualDifference {
    if (path == null || path.isBlank()) {
      throw new ToppleCatException("Expected/actual comparison path is required.");
    }
    if (kind == null) {
      throw new ToppleCatException("Expected/actual comparison kind is required.");
    }
    expected = expected == null ? null : expected.deepCopy();
    actual = actual == null ? null : actual.deepCopy();
  }

  /** The relationship of one field in the declared and observed JSON values. */
  public enum Kind {
    /** A declared expected field was absent from the observed value. */
    MISSING_EXPECTED,
    /** An observed field was not present in the declared expected value. */
    UNEXPECTED_ACTUAL,
    /** Both sides had a value, but the values differ. */
    CHANGED
  }
}
