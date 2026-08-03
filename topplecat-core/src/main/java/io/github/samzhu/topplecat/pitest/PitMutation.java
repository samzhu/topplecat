package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** One mutant from a PIT {@code mutations.xml} report. */
public record PitMutation(
    boolean detected,
    String status,
    String mutatedClass,
    String sourceFile,
    String mutatedMethod,
    String methodDescription,
    Integer lineNumber,
    Integer block,
    Integer index,
    String mutator,
    String description,
    List<String> coveringTests,
    List<String> killingTests,
    List<String> succeedingTests) {
  /** Compatibility constructor for producer fixtures without PIT location fields. */
  public PitMutation(
      boolean detected,
      String status,
      String mutatedClass,
      String mutator,
      String description,
      List<String> coveringTests,
      List<String> killingTests,
      List<String> succeedingTests) {
    this(
        detected,
        status,
        mutatedClass,
        null,
        null,
        null,
        null,
        null,
        null,
        mutator,
        description,
        coveringTests,
        killingTests,
        succeedingTests);
  }

  public PitMutation {
    requireText(status, "status");
    requireText(mutatedClass, "mutatedClass");
    sourceFile = optionalText(sourceFile);
    mutatedMethod = optionalText(mutatedMethod);
    methodDescription = optionalText(methodDescription);
    validateCoordinate(lineNumber, "lineNumber", 1);
    validateCoordinate(block, "block", 0);
    validateCoordinate(index, "index", 0);
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

  private static String optionalText(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static void validateCoordinate(Integer value, String field, int minimum) {
    if (value != null && value < minimum) {
      throw new ToppleCatException("PIT mutation " + field + " must be at least " + minimum + ".");
    }
  }
}
