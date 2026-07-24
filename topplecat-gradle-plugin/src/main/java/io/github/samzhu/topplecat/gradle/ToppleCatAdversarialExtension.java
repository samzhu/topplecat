package io.github.samzhu.topplecat.gradle;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

/** Top-level switches for ToppleCat's adversarial verification safeguards. */
public abstract class ToppleCatAdversarialExtension {
    /** Enables every adversarial safeguard unless an individual category is disabled. */
    public abstract Property<Boolean> getEnabled();

    @Nested
    public abstract ToppleCatHiddenRetestExtension getHiddenRetest();

    @Nested
    public abstract ToppleCatAdversarialMutationExtension getMutation();

    @Nested
    public abstract ToppleCatExpectedConsumptionExtension getExpectedConsumption();

    public void hiddenRetest(Action<? super ToppleCatHiddenRetestExtension> action) {
        action.execute(getHiddenRetest());
    }

    public void mutation(Action<? super ToppleCatAdversarialMutationExtension> action) {
        action.execute(getMutation());
    }

    public void expectedConsumption(Action<? super ToppleCatExpectedConsumptionExtension> action) {
        action.execute(getExpectedConsumption());
    }
}
