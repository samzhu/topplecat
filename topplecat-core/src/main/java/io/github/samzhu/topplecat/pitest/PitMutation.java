package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** One mutant from a PIT {@code mutations.xml} report. */
public record PitMutation(
    boolean detected,
    String status,
    String mutatedClass,
    List<String> coveringTests,
    List<String> killingTests,
    List<String> succeedingTests) {
  public PitMutation {
    requireText(status, "status");
    requireText(mutatedClass, "mutatedClass");
    coveringTests = normalized(coveringTests);
    killingTests = normalized(killingTests);
    succeedingTests = normalized(succeedingTests);
  }

  private static List<String> normalized(List<String> selectors) {
    return (selectors == null ? List.<String>of() : selectors)
        .stream()
            .filter(selector -> selector != null && !selector.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("PIT mutation " + field + " is required.");
    }
  }
}
