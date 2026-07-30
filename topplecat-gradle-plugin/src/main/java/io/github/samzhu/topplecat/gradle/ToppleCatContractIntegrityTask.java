package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.ContractIntegrityResult;
import io.github.samzhu.topplecat.core.ContractIntegrityResultJson;
import io.github.samzhu.topplecat.core.EscrowManifest;
import io.github.samzhu.topplecat.core.EscrowService;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ReviewerContractApproval;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Fresh, run-scoped proof that the public contract still matches the reviewer-approved epoch. */
public abstract class ToppleCatContractIntegrityTask extends DefaultTask
    implements ToppleCatApprovalInputs {
  @Internal
  public abstract DirectoryProperty getProjectRoot();

  @OutputFile
  public abstract RegularFileProperty getResultFile();

  @TaskAction
  public void verifyIntegrity() {
    ContractIntegrityResult result;
    try {
      ReviewerContractApproval current = currentApproval();
      EscrowManifest manifest =
          new EscrowService().manifest(getProjectRoot().get().getAsFile().toPath());
      result = ContractApprovalFactory.compare(manifest.approval(), current);
    } catch (RuntimeException exception) {
      getLogger()
          .warn(
              "ToppleCat could not establish current reviewer approval integrity: {}",
              exception.getMessage());
      result =
          new ContractIntegrityResult(
              ContractIntegrityResult.SCHEMA_VERSION,
              EvidenceVerdict.INCOMPLETE,
              null,
              null,
              List.of(),
              List.of(),
              List.of(),
              false,
              List.of());
    }
    write(result);
    getLogger().lifecycle("ToppleCat contract integrity: {}", result.verdict());
  }

  private void write(ContractIntegrityResult result) {
    Path output = getResultFile().get().getAsFile().toPath();
    try {
      Files.createDirectories(output.getParent());
      Files.writeString(
          output,
          ContractIntegrityResultJson.write(result),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot write ToppleCat contract-integrity result "
              + output
              + ": "
              + exception.getMessage(),
          exception);
    }
  }

  static boolean passed(Path resultFile) {
    if (!Files.isRegularFile(resultFile)) {
      return false;
    }
    try {
      return ContractIntegrityResultJson.read(Files.readString(resultFile)).verdict()
          == EvidenceVerdict.PASS;
    } catch (IOException | RuntimeException ignored) {
      return false;
    }
  }
}
