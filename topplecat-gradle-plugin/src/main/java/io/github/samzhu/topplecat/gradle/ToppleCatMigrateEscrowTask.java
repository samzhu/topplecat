package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowService;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * Explicit reviewer custody migration task that moves legacy project-local escrow
 * into the configured reviewer-local state root.
 */
public abstract class ToppleCatMigrateEscrowTask extends DefaultTask {
    @Internal
    public abstract DirectoryProperty getProjectRoot();

    @TaskAction
    public void migrateEscrow() {
        new EscrowService().migrateLegacyEscrow(getProjectRoot().get().getAsFile().toPath());
        getLogger().lifecycle("ToppleCat migrated legacy local escrow state to reviewer-local custody.");
    }
}
