package io.github.samzhu.topplecat.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
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
    var directory = getRunDirectory().get().getAsFile().toPath();
    VerificationRunWorkspace.prepare(directory);
    try {
      Files.writeString(directory.resolve("run-id"), UUID.randomUUID().toString());
    } catch (IOException exception) {
      throw new org.gradle.api.GradleException(
          "Cannot create ToppleCat run identifier.", exception);
    }
  }
}
