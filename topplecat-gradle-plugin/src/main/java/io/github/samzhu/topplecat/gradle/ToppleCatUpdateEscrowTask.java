package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowManifest;
import io.github.samzhu.topplecat.core.EscrowService;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Explicit reviewer-custody task that replaces a restored escrowed reviewer suite after review. */
public abstract class ToppleCatUpdateEscrowTask extends DefaultTask implements ToppleCatApprovalInputs {
    @Internal
    public abstract DirectoryProperty getProjectRoot();

    @Internal
    public abstract DirectoryProperty getHiddenSourceRoot();

    @TaskAction
    public void update() {
        EscrowManifest manifest = new EscrowService().update(getProjectRoot().get().getAsFile().toPath(),
                getHiddenSourceRoot().get().getAsFile().toPath(), currentApproval());
        getLogger().lifecycle("ToppleCat escrow update complete: {} reviewer files are hidden.", manifest.entries().size());
    }
}
