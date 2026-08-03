package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** Reviewer-only raw PIT mutation relation with its exact public AC attribution. */
public record PitMutationEvidence(
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
    List<String> succeedingTests,
    List<String> attributedAcceptanceConditionIds,
    List<String> detectedAcceptanceConditionIds,
    String originalSourceLine) {
  /** Compatibility constructor for reviewer fixtures without PIT location fields. */
  public PitMutationEvidence(
      boolean detected,
      String status,
      String mutatedClass,
      String mutator,
      String description,
      List<String> coveringTests,
      List<String> killingTests,
      List<String> succeedingTests,
      List<String> attributedAcceptanceConditionIds) {
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
        succeedingTests,
        attributedAcceptanceConditionIds,
        killingTests == null || killingTests.isEmpty()
            ? List.of()
            : attributedAcceptanceConditionIds,
        null);
  }

  public PitMutationEvidence {
    if (status == null
        || status.isBlank()
        || mutatedClass == null
        || mutatedClass.isBlank()
        || mutator == null
        || mutator.isBlank()
        || description == null
        || description.isBlank()) {
      throw new ToppleCatException("PIT mutation evidence is incomplete.");
    }
    sourceFile = optionalText(sourceFile);
    mutatedMethod = optionalText(mutatedMethod);
    methodDescription = optionalText(methodDescription);
    validateCoordinate(lineNumber, "lineNumber", 1);
    validateCoordinate(block, "block", 0);
    validateCoordinate(index, "index", 0);
    originalSourceLine = optionalText(originalSourceLine);
    coveringTests = List.copyOf(coveringTests == null ? List.of() : coveringTests);
    killingTests = List.copyOf(killingTests == null ? List.of() : killingTests);
    succeedingTests = List.copyOf(succeedingTests == null ? List.of() : succeedingTests);
    attributedAcceptanceConditionIds =
        List.copyOf(
            attributedAcceptanceConditionIds == null
                ? List.of()
                : attributedAcceptanceConditionIds);
    detectedAcceptanceConditionIds =
        List.copyOf(
            detectedAcceptanceConditionIds == null ? List.of() : detectedAcceptanceConditionIds);
  }

  private static String optionalText(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static void validateCoordinate(Integer value, String field, int minimum) {
    if (value != null && value < minimum) {
      throw new ToppleCatException(
          "PIT mutation evidence " + field + " must be at least " + minimum + ".");
    }
  }
}
