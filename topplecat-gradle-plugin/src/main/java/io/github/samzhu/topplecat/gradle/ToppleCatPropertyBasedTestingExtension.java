package io.github.samzhu.topplecat.gradle;

import org.gradle.api.provider.Property;

/** Controls the sealed deterministic Property-based testing safeguard. */
public abstract class ToppleCatPropertyBasedTestingExtension {
  public abstract Property<Boolean> getEnabled();
}
