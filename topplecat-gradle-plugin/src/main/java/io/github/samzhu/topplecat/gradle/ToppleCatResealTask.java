package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowManifest;
import io.github.samzhu.topplecat.core.EscrowService;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Explicit reviewer-custody task that replaces a restored escrowed reviewer suite after review. */
public abstract class ToppleCatResealTask extends ToppleCatReviewerPresentationTask
    implements ToppleCatApprovalInputs {
  @Internal
  public abstract DirectoryProperty getProjectRoot();

  @Internal
  public abstract DirectoryProperty getHiddenSourceRoot();

  @TaskAction
  public void update() {
    var root = getProjectRoot().get().getAsFile().toPath();
    EscrowService escrow = new EscrowService();
    EscrowManifest manifest =
        escrow.update(root, getHiddenSourceRoot().get().getAsFile().toPath(), currentApproval());
    getLogger()
        .lifecycle(
            "ToppleCat escrow update complete: {} reviewer files are hidden.",
            manifest.entries().size());
  }
}
