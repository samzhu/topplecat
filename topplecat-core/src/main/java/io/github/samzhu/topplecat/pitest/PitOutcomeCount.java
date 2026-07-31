package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;

/** Count of one unmodified PIT {@code (status, detected)} outcome pair. */
public record PitOutcomeCount(String status, boolean detected, int count) {
  public PitOutcomeCount {
    if (status == null || status.isBlank() || count < 0) {
      throw new ToppleCatException("PIT outcome count is invalid.");
    }
  }
}
