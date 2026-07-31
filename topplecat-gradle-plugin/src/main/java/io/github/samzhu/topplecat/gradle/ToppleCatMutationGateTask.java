package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.pitest.PitMutationAttribution;
import io.github.samzhu.topplecat.pitest.PitMutationAttributor;
import io.github.samzhu.topplecat.pitest.PitMutationParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Turns a public-contract PIT full mutation matrix into automatic, reviewer-visible AC results. */
public abstract class ToppleCatMutationGateTask extends DefaultTask {
  @Internal
  public abstract DirectoryProperty getPublicTestSourceRoot();

  @org.gradle.api.tasks.InputFile
  public abstract RegularFileProperty getDefinitionFile();

  /** Configured location used by the action to report a missing current PIT producer output. */
  @Internal
  public abstract RegularFileProperty getPitReportFile();

  /**
   * Current PIT producer output. This is a real tracked input when it exists; an absent optional
   * report deliberately reaches the task action so it can mark the current run incomplete.
   */
  @org.gradle.api.tasks.InputFile
  @Optional
  @PathSensitive(PathSensitivity.NONE)
  public Provider<RegularFile> getPitReportInput() {
    return getProject()
        .provider(
            () -> {
              RegularFile report = getPitReportFile().getOrNull();
              return report != null && report.getAsFile().isFile() ? report : null;
            });
  }

  @OutputFile
  public abstract RegularFileProperty getResultsFile();

  @Internal
  public abstract DirectoryProperty getRunDirectory();

  @Input
  public abstract Property<Integer> getThreshold();

  @Input
  public abstract Property<String> getProducerTaskName();

  @Input
  public abstract Property<Boolean> getProducerAvailable();

  /** Internal Verify-only switch: direct diagnostic execution still fails at this task. */
  @Internal
  public abstract Property<Boolean> getContinueAfterFailure();

  @TaskAction
  public void evaluateMutationGate() {
    String producer = getProducerTaskName().get();
    if (!getProducerAvailable().get()) {
      deferOrThrow(
          "ToppleCat mutation is enabled, but producer task '"
              + producer
              + "' was not found. ToppleCat configures PIT automatically for the default 'pitest'"
              + " task; otherwise set toppleCat.mutationTesting.producerTask to a task that writes"
              + " mutations.xml.");
      return;
    }
    Path report = getPitReportFile().get().getAsFile().toPath();
    if (!Files.isRegularFile(report)) {
      VerificationRunArtifacts.markCompleted(
          getRunDirectory().get().getAsFile().toPath(), VerificationRunArtifacts.MUTATION);
      deferOrThrow(
          "ToppleCat mutation producer '"
              + producer
              + "' did not write PIT mutations.xml at "
              + report
              + ". Enable PIT fullMutationMatrix=true and configure"
              + " toppleCat.mutationTesting.reportFile if needed.");
      return;
    }
    Map<String, Set<String>> testsByAc = canonicalMethodsByAc();
    PitMutationAttribution attribution;
    try {
      attribution =
          PitMutationAttributor.attribute(
              new PitMutationParser().parse(report), testsByAc, getThreshold().get());
    } catch (RuntimeException exception) {
      VerificationRunArtifacts.markCompleted(
          getRunDirectory().get().getAsFile().toPath(), VerificationRunArtifacts.MUTATION);
      deferOrThrow(
          "ToppleCat mutation producer '"
              + producer
              + "' wrote an unusable current-run PIT mutations.xml report.");
      return;
    }
    Path output = getResultsFile().get().getAsFile().toPath();
    try {
      Files.createDirectories(output.getParent());
      Files.writeString(output, MutationGateResults.write(MutationGateResults.from(attribution)));
    } catch (IOException exception) {
      throw new GradleException("Cannot write ToppleCat mutation results: " + output, exception);
    }
    VerificationRunArtifacts.markCompleted(
        getRunDirectory().get().getAsFile().toPath(), VerificationRunArtifacts.MUTATION);
    MutationGateResults results = MutationGateResults.from(attribution);
    if (results.verdict() != io.github.samzhu.topplecat.core.EvidenceVerdict.PASS) {
      deferOrThrow(
          "ToppleCat mutation gate did not meet the sealed public-contract detection policy."
              + " Inspect "
              + output
              + " for exact per-AC coverage, detection, and PIT producer outcomes.");
      return;
    }
    getLogger()
        .lifecycle("ToppleCat mutation gate passed for {} ACs.", attribution.assessments().size());
  }

  private void deferOrThrow(String message) {
    if (getContinueAfterFailure().getOrElse(false)) {
      getLogger().lifecycle("{}", message);
      return;
    }
    throw new GradleException(message);
  }

  private Map<String, Set<String>> canonicalMethodsByAc() {
    Map<String, Set<String>> result = new LinkedHashMap<>();
    Path definition = getDefinitionFile().get().getAsFile().toPath();
    try {
      ContractDefinitionJson.read(Files.readString(definition))
          .acceptanceConditions()
          .forEach(
              contract ->
                  result
                      .computeIfAbsent(contract.acId(), ignored -> new LinkedHashSet<>())
                      .add(contract.scenario().acceptanceTestMethodIdentity()));
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot read ToppleCat contract definition " + definition + ": " + exception.getMessage(),
          exception);
    }
    return result;
  }
}
