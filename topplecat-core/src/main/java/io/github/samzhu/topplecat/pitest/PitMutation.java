package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** One mutant from a PIT {@code mutations.xml} report. */
public record PitMutation(
    boolean detected,
    String status,
    String mutatedClass,
    String mutator,
    String description,
    List<String> coveringTests,
    List<String> killingTests,
    List<String> succeedingTests) {
  public PitMutation {
    requireText(status, "status");
    requireText(mutatedClass, "mutatedClass");
    requireText(mutator, "mutator");
    requireText(description, "description");
    coveringTests = normalized(coveringTests);
    killingTests = normalized(killingTests);
    succeedingTests = normalized(succeedingTests);
  }

  private static List<String> normalized(List<String> selectors) {
    List<String> values = selectors == null ? List.of() : selectors;
    if (values.stream().anyMatch(selector -> selector == null || selector.isBlank())) {
      throw new ToppleCatException("PIT mutation selector must be nonblank when present.");
    }
    return List.copyOf(values);
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("PIT mutation " + field + " is required.");
    }
  }
}
