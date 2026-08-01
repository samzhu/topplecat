package io.github.samzhu.topplecat.core;

/** Resolved verification settings sealed together with the approved public contract. */
public record VerificationPolicy(
    String toppleCatVersion,
    boolean hiddenTestsEnabled,
    boolean expectedConsumptionEnabled,
    boolean propertyBasedTestingEnabled,
    boolean mutationEnabled,
    int mutationThreshold) {
  public VerificationPolicy {
    if (toppleCatVersion == null
        || toppleCatVersion.isBlank()
        || mutationThreshold < 0
        || mutationThreshold > 100) {
      throw new ToppleCatException("Verification policy is invalid.");
    }
  }
}
