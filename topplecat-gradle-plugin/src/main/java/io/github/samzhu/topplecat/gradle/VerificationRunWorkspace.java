package io.github.samzhu.topplecat.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.api.GradleException;

/** Manages one execution-time workspace and the bounded archive of completed verification runs. */
final class VerificationRunWorkspace {
  private static final String ACTIVE_MARKER = ".active";
  private static final String WORKSPACE_NAME = "current";
  private static final int RETAINED_ARCHIVES = 3;

  private VerificationRunWorkspace() {}

  static void prepare(Path workspace) {
    if (Files.isRegularFile(workspace.resolve(ACTIVE_MARKER))) {
      return;
    }
    start(workspace);
  }

  /** Starts a new run and discards any unarchived workspace from an earlier invocation. */
  static void start(Path workspace) {
    Path runsDirectory = workspace.getParent();
    try {
      Files.createDirectories(runsDirectory);
      // A remaining .active marker means a prior run did not reach archival. Nothing under it
      // is current-run evidence for the next Verify, so discard it before any producer or gate.
      deleteTree(workspace);
      pruneArchivedRuns(runsDirectory, RETAINED_ARCHIVES - 1);
      Files.createDirectories(workspace);
      Files.writeString(workspace.resolve(ACTIVE_MARKER), "active\n");
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot start ToppleCat verification run workspace "
              + workspace
              + ". Clean the project's build/topplecat/runs directory and retry.",
          exception);
    }
  }

  static void archive(Path workspace, String runId) {
    Path archive = workspace.resolveSibling(runId);
    try {
      Files.deleteIfExists(workspace.resolve(ACTIVE_MARKER));
      Files.move(workspace, archive, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
      try {
        Files.move(workspace, archive);
      } catch (IOException exception) {
        throw archiveFailure(workspace, archive, exception);
      }
    } catch (IOException exception) {
      throw archiveFailure(workspace, archive, exception);
    }
  }

  private static GradleException archiveFailure(
      Path workspace, Path archive, IOException exception) {
    return new GradleException(
        "Cannot archive ToppleCat verification run from "
            + workspace
            + " to "
            + archive
            + ". Inspect the build/topplecat/runs directory and retry.",
        exception);
  }

  private static void pruneArchivedRuns(Path runsDirectory, int retained) throws IOException {
    try (Stream<Path> paths = Files.list(runsDirectory)) {
      List<Path> archives =
          paths
              .filter(Files::isDirectory)
              .filter(path -> !WORKSPACE_NAME.equals(path.getFileName().toString()))
              .sorted(Comparator.comparing(VerificationRunWorkspace::lastModified).reversed())
              .toList();
      for (int index = retained; index < archives.size(); index++) {
        deleteTree(archives.get(index));
      }
    }
  }

  private static long lastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException exception) {
      throw new GradleException("Cannot inspect ToppleCat verification run " + path, exception);
    }
  }

  private static void deleteTree(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(path)) {
      for (Path entry : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(entry);
      }
    }
  }
}
