package io.github.samzhu.topplecat.gradle;

import org.gradle.api.provider.Property;

/** Controls reviewer-only hidden-case restoration and retesting. */
public abstract class ToppleCatHiddenRetestExtension {
    public abstract Property<Boolean> getEnabled();
}
