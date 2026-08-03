package io.github.samzhu.topplecat.core;

/** Resolved verification settings sealed together with the approved public contract. */
public record VerificationPolicy(
    String toppleCatVersion,
    boolean hiddenTestsEnabled,
    boolean expectedConsumptionEnabled,
    boolean propertyBasedTestingEnabled,
    boolean mutationEnabled) {
  /**
   * Compatibility constructor for approvals authored before mutation became AC-scoped.
   *
   * <p>The former percentage is deliberately ignored and is not part of the sealed policy.
   */
  public VerificationPolicy(
      String toppleCatVersion,
      boolean hiddenTestsEnabled,
      boolean expectedConsumptionEnabled,
      boolean propertyBasedTestingEnabled,
      boolean mutationEnabled,
      int ignoredMutationThreshold) {
    this(
        toppleCatVersion,
        hiddenTestsEnabled,
        expectedConsumptionEnabled,
        propertyBasedTestingEnabled,
        mutationEnabled);
  }

  public VerificationPolicy {
    if (toppleCatVersion == null || toppleCatVersion.isBlank()) {
      throw new ToppleCatException("Verification policy is invalid.");
    }
  }
}
