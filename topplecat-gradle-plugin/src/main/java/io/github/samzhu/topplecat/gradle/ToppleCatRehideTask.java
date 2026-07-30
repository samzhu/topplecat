package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowService;
import java.nio.file.Files;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Internal finalizer that validates and removes restored reviewer source. */
public abstract class ToppleCatRehideTask extends DefaultTask {
  @Internal
  public abstract DirectoryProperty getProjectRoot();

  @TaskAction
  public void rehide() {
    var root = getProjectRoot().get().getAsFile().toPath();
    var manifest =
        EscrowService.reviewerStatePath(root, EscrowService.defaultReviewerStateRoot())
            .resolve("manifest.json");
    if (!Files.isRegularFile(manifest)) {
      getLogger()
          .lifecycle("ToppleCat rehide skipped because Verify found no existing Mechanical Seal.");
      return;
    }
    new EscrowService().rehide(root);
  }
}
