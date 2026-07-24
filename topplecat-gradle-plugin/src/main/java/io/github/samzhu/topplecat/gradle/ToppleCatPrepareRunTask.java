package io.github.samzhu.topplecat.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Prepares the execution workspace before Gradle creates verification-test outputs. */
public abstract class ToppleCatPrepareRunTask extends DefaultTask {
    @Internal
    public abstract DirectoryProperty getRunDirectory();

    @TaskAction
    public void prepare() {
        VerificationRunWorkspace.prepare(getRunDirectory().get().getAsFile().toPath());
    }
}
