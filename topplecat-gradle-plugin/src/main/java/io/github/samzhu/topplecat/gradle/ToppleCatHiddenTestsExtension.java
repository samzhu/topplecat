package io.github.samzhu.topplecat.gradle;

import org.gradle.api.provider.Property;

/** Controls reviewer-only hidden typed-row restoration and retesting. */
public abstract class ToppleCatHiddenTestsExtension {
  public abstract Property<Boolean> getEnabled();
}
