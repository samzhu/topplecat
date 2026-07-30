package io.github.samzhu.topplecat.core;

/** Resolved verification settings sealed together with the approved public contract. */
public record VerificationPolicy(
    String toppleCatVersion,
    boolean hiddenTestsEnabled,
    boolean expectedConsumptionEnabled,
    boolean propertyBasedTestingEnabled,
    boolean mutationEnabled,
    int mutationThreshold,
    MutationProducerKind mutationProducerKind,
    String mutationProducerTaskPath) {
  public VerificationPolicy {
    if (toppleCatVersion == null
        || toppleCatVersion.isBlank()
        || mutationThreshold < 0
        || mutationThreshold > 100
        || mutationProducerKind == null) {
      throw new ToppleCatException("Verification policy is invalid.");
    }
    if (mutationProducerKind == MutationProducerKind.DEFAULT && mutationProducerTaskPath != null) {
      throw new ToppleCatException(
          "The default mutation producer must not declare a custom task path.");
    }
    if (mutationProducerKind == MutationProducerKind.CUSTOM
        && (mutationProducerTaskPath == null || mutationProducerTaskPath.isBlank())) {
      throw new ToppleCatException("A custom mutation producer requires its resolved task path.");
    }
  }
}
