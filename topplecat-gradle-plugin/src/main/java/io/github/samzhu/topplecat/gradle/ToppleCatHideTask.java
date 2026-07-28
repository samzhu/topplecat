package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowManifest;
import io.github.samzhu.topplecat.core.EscrowService;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Moves reviewer-only source to local hidden storage before implementation work. */
public abstract class ToppleCatHideTask extends ToppleCatScopedTask implements ToppleCatApprovalInputs {
    @Internal
    public abstract DirectoryProperty getProjectRoot();

    @Internal
    public abstract DirectoryProperty getHiddenSourceRoot();

    @TaskAction
    public void hide() {
        EscrowManifest manifest = new EscrowService().hide(getProjectRoot().get().getAsFile().toPath(),
                getHiddenSourceRoot().get().getAsFile().toPath(), currentApproval());
        getLogger().lifecycle("ToppleCat hide complete: {} reviewer files moved to local hidden storage.",
                manifest.entries().size());
        getLogger().lifecycle("Local hidden storage is plaintext custody state, not a secrecy boundary.");
        getLogger().lifecycle("Never expose reviewer source through agent-readable Git history. Give the implementation agent "
                + "a public export without .git, .topplecat, or build/, or use an isolated environment whose history never contained it.");
    }
}
