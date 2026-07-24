package io.github.samzhu.topplecat.gradle;

import org.gradle.api.provider.Property;

/** Controls runtime enforcement of declared expected-value consumption. */
public abstract class ToppleCatExpectedConsumptionExtension {
    public abstract Property<Boolean> getEnabled();
}
