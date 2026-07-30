package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowManifest;
import io.github.samzhu.topplecat.core.EscrowService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Seals reviewer-only source into local custody before implementation work. */
public abstract class ToppleCatSealTask extends ToppleCatScopedTask
    implements ToppleCatApprovalInputs {
  @Internal
  public abstract DirectoryProperty getProjectRoot();

  @Internal
  public abstract DirectoryProperty getHiddenSourceRoot();

  @TaskAction
  public void hide() {
    var root = getProjectRoot().get().getAsFile().toPath();
    Path hiddenSource = getHiddenSourceRoot().get().getAsFile().toPath();
    EscrowService escrow = new EscrowService();
    EscrowManifest manifest = escrow.hide(root, hiddenSource, currentApproval());
    clearTransientHiddenCompilerOutput(root.resolve("build/topplecat/compiler-hidden"));
    getLogger()
        .lifecycle(
            "ToppleCat seal complete: {} reviewer files moved to local hidden storage.",
            manifest.entries().size());
    getLogger()
        .lifecycle("Local hidden storage is plaintext custody state, not a secrecy boundary.");
    getLogger()
        .lifecycle(
            "Never expose reviewer source through agent-readable Git history. Give the"
                + " implementation agent a public export without .git, .topplecat, or build/, or"
                + " use an isolated environment whose history never contained it.");
  }

  private static void clearTransientHiddenCompilerOutput(Path output) {
    if (!Files.exists(output)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(output)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    } catch (IOException exception) {
      throw new org.gradle.api.GradleException(
          "Cannot clear transient reviewer compiler output.", exception);
    }
  }
}
