package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EscrowProjectLock;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/** Holds reviewer custody for an entire verification build, including failing task graphs. */
public abstract class ToppleCatCustodyBuildService
    implements BuildService<ToppleCatCustodyBuildService.Parameters>, AutoCloseable {
  /** Parameters fixed when the service is registered for one Gradle project. */
  public interface Parameters extends BuildServiceParameters {
    DirectoryProperty getProjectRoot();
  }

  private boolean acquired;

  public synchronized void acquire() {
    if (!acquired) {
      EscrowProjectLock.acquireForVerification(
          getParameters().getProjectRoot().get().getAsFile().toPath());
      acquired = true;
    }
  }

  @Override
  public synchronized void close() {
    if (acquired) {
      EscrowProjectLock.releaseVerification(
          getParameters().getProjectRoot().get().getAsFile().toPath());
      acquired = false;
    }
  }
}
