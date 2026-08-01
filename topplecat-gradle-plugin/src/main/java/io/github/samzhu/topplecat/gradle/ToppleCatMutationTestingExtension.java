package io.github.samzhu.topplecat.gradle;

import org.gradle.api.provider.Property;

/** Controls the sealed policy for ToppleCat's managed PIT verification producer. */
public abstract class ToppleCatMutationTestingExtension {
  public abstract Property<Boolean> getEnabled();

  /** Minimum per-AC score. A 100% default makes every surviving attributed mutant visible. */
  public abstract Property<Integer> getThreshold();
}
