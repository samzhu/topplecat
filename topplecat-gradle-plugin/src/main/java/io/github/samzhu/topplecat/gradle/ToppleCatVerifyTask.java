package io.github.samzhu.topplecat.gradle;

import org.gradle.api.tasks.options.Option;

/** Entry task for a selected delivery verification run. */
public abstract class ToppleCatVerifyTask extends ToppleCatScopedTask {
  @Option(
      option = "all-hidden-tests",
      description = "Escalate this verification run to every hidden typed-row check.")
  public void setAllHiddenTests(boolean allHidden) {
    getProject()
        .getExtensions()
        .getByType(ToppleCatExtension.class)
        .getAllHiddenRequested()
        .set(allHidden);
  }
}
