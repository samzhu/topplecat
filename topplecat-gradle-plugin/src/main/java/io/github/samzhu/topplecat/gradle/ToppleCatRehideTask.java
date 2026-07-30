package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowService;
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
    new EscrowService().rehide(getProjectRoot().get().getAsFile().toPath());
  }
}
