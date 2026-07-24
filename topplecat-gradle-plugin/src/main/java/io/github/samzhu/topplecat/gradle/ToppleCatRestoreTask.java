package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowService;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Restores reviewer source from validated local hidden storage. */
public abstract class ToppleCatRestoreTask extends DefaultTask {
    @Internal
    public abstract DirectoryProperty getProjectRoot();

    @TaskAction
    public void restore() {
        var manifest = new EscrowService().restore(getProjectRoot().get().getAsFile().toPath());
        getLogger().lifecycle("ToppleCat restore complete: {} reviewer files are available to the reviewer.",
                manifest.entries().size());
    }
}
