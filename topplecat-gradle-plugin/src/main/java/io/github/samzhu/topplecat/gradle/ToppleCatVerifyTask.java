package io.github.samzhu.topplecat.gradle;

import org.gradle.api.tasks.options.Option;

/** Entry task for a selected delivery verification run. */
public abstract class ToppleCatVerifyTask extends ToppleCatScopedTask {
    @Option(option = "all-hidden", description = "Escalate this verification run to every AC-bound hidden check.")
    public void setAllHidden(boolean allHidden) {
        getProject().getExtensions().getByType(ToppleCatExtension.class).getAllHiddenRequested().set(allHidden);
    }
}
