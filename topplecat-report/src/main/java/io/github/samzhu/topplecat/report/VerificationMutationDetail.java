package io.github.samzhu.topplecat.report;

/**
 * One reviewer-facing mutation that a specific AC's public Acceptance Method did not detect.
 *
 * <p>This is derived from the assessed mutation evidence. It deliberately omits PIT test selectors
 * and mutator identities; those remain in the collapsed technical evidence.
 */
public record VerificationMutationDetail(
    int ordinal,
    String pitStatus,
    boolean pitDetected,
    String mutatedClass,
    String sourceFile,
    String mutatedMethod,
    String methodDescription,
    Integer lineNumber,
    Integer block,
    Integer index,
    String description,
    String originalSourceLine,
    String replacementBefore,
    String replacementAfter) {
  public VerificationMutationDetail {
    if (ordinal < 1
        || pitStatus == null
        || pitStatus.isBlank()
        || mutatedClass == null
        || mutatedClass.isBlank()
        || description == null
        || description.isBlank()) {
      throw new IllegalArgumentException("Verification mutation detail is incomplete.");
    }
    sourceFile = optionalText(sourceFile);
    mutatedMethod = optionalText(mutatedMethod);
    methodDescription = optionalText(methodDescription);
    originalSourceLine = optionalText(originalSourceLine);
    replacementBefore = optionalText(replacementBefore);
    replacementAfter = optionalText(replacementAfter);
    if ((replacementBefore == null) != (replacementAfter == null)) {
      throw new IllegalArgumentException(
          "Verification mutation replacement must contain both sides or neither side.");
    }
    if (lineNumber != null && lineNumber < 1) {
      throw new IllegalArgumentException("Verification mutation line number must be positive.");
    }
    if (block != null && block < 0) {
      throw new IllegalArgumentException("Verification mutation block must not be negative.");
    }
    if (index != null && index < 0) {
      throw new IllegalArgumentException("Verification mutation index must not be negative.");
    }
  }

  private static String optionalText(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
