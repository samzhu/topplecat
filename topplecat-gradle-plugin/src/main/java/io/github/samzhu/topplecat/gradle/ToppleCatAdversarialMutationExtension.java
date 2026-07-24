package io.github.samzhu.topplecat.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/** Controls PIT-backed mutation verification and its producer contract. */
public abstract class ToppleCatAdversarialMutationExtension {
    public abstract Property<Boolean> getEnabled();

    /** Minimum per-AC score. A 100% default makes every surviving attributed mutant visible. */
    public abstract Property<Integer> getThreshold();

    /** Task that writes the PIT-compatible XML report. Defaults to {@code pitest}. */
    public abstract Property<String> getProducerTask();

    /** XML report consumed by the ToppleCat mutation gate. */
    public abstract RegularFileProperty getReportFile();
}
