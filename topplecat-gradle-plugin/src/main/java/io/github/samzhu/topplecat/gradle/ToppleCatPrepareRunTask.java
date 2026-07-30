package io.github.samzhu.topplecat.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Prepares the execution workspace before Gradle creates verification-test outputs. */
public abstract class ToppleCatPrepareRunTask extends DefaultTask {
  @Internal
  public abstract DirectoryProperty getRunDirectory();

  /** Configured producer output that must never supply stale mutation evidence to this run. */
  @Internal
  public abstract RegularFileProperty getMutationProducerReportFile();

  @TaskAction
  public void prepare() {
    var directory = getRunDirectory().get().getAsFile().toPath();
    VerificationRunWorkspace.start(directory);
    try {
      if (getMutationProducerReportFile().isPresent()) {
        Files.deleteIfExists(getMutationProducerReportFile().get().getAsFile().toPath());
      }
      Files.writeString(directory.resolve("run-id"), UUID.randomUUID().toString());
    } catch (IOException exception) {
      throw new org.gradle.api.GradleException(
          "Cannot create ToppleCat run identifier.", exception);
    }
  }
}
