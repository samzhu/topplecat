package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import io.github.samzhu.topplecat.pitest.PitMutationAttributor;
import io.github.samzhu.topplecat.pitest.PitMutationParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Turns a public-contract PIT full mutation matrix into automatic, reviewer-visible AC results. */
public abstract class ToppleCatMutationGateTask extends DefaultTask {
  @Internal
  public abstract DirectoryProperty getPublicTestSourceRoot();

  @org.gradle.api.tasks.InputFile
  public abstract RegularFileProperty getDefinitionFile();

  @Internal
  public abstract RegularFileProperty getPitReportFile();

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
    List<PitMutationAssessment> assessments;
    try {
      assessments =
          PitMutationAttributor.assess(
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
      Files.writeString(
          output,
          MutationGateResults.write(
              new MutationGateResults(MutationGateResults.SCHEMA_VERSION, assessments)));
    } catch (IOException exception) {
      throw new GradleException("Cannot write ToppleCat mutation results: " + output, exception);
    }
    VerificationRunArtifacts.markCompleted(
        getRunDirectory().get().getAsFile().toPath(), VerificationRunArtifacts.MUTATION);
    List<String> failed =
        assessments.stream()
            .filter(result -> result.verdict() != EvidenceVerdict.PASS)
            .map(PitMutationAssessment::acId)
            .toList();
    if (!failed.isEmpty()) {
      deferOrThrow(
          "ToppleCat mutation gate failed for "
              + String.join(", ", failed)
              + ". Inspect "
              + output
              + " for the per-AC mutation score and test attribution.");
      return;
    }
    getLogger().lifecycle("ToppleCat mutation gate passed for {} ACs.", assessments.size());
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
