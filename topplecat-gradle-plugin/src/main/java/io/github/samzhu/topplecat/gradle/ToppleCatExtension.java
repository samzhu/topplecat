package io.github.samzhu.topplecat.gradle;

import org.gradle.api.Action;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Nested;

/** Gradle layout convention for the pure Java ToppleCat workflow. */
public abstract class ToppleCatExtension {
    /** Public JSON/YAML cases visible to the implementer. */
    public abstract DirectoryProperty getPublicCaseRoot();

    /** Complete reviewer-only source set moved by {@code toppleCatHide}. */
    public abstract DirectoryProperty getHiddenSourceRoot();

    /** Optional external Markdown documents that provide public SDD context for report projections. */
    public abstract ConfigurableFileCollection getSpecDocs();

    /** Adversarial verification controls. All safeguards are enabled by default. */
    @Nested
    public abstract ToppleCatAdversarialExtension getAdversarial();

    /** Configures adversarial verification in Groovy and Kotlin build scripts. */
    public void adversarial(Action<? super ToppleCatAdversarialExtension> action) {
        action.execute(getAdversarial());
    }
}
