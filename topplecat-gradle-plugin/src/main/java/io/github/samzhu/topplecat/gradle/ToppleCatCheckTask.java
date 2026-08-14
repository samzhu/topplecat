package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.CompilerPropertyDescriptor;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ContractQualityAdvisor;
import io.github.samzhu.topplecat.core.ContractQualityAdvisory;
import io.github.samzhu.topplecat.core.SelectedSpecScopeJson;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCaseReader;
import io.github.samzhu.topplecat.core.ToppleCaseSource;
import io.github.samzhu.topplecat.core.ToppleCatException;
import io.github.samzhu.topplecat.report.ReportJson;
import io.github.samzhu.topplecat.report.SelectedSpecProjection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Builds the sole ContractDefinition from javac descriptors and typed public/reviewer case data.
 */
public abstract class ToppleCatCheckTask extends ToppleCatScopedTask {
  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getProjectRoot();

  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getPublicTestSourceRoot();

  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getPublicCaseRoot();

  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getHiddenSourceRoot();

  /** Existing case roots are inputs without requiring an optional directory to exist. */
  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getCaseSources();

  /** Class output roots that contain the processor-owned descriptor index. */
  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getDescriptorClassDirectories();

  /** Compiler output used solely to reject forbidden reviewer-side Property declarations. */
  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getForbiddenHiddenPropertyDescriptorClassDirectories();

  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getReviewRoot();

  @OutputFile
  public abstract RegularFileProperty getDefinitionFile();

  @OutputFile
  public abstract RegularFileProperty getSelectedSpecScopeFile();

  @OutputFile
  public abstract RegularFileProperty getSelectedSpecProjectionFile();

  @org.gradle.api.tasks.Input
  public abstract ListProperty<String> getSelectedAcceptanceConditionIds();

  @org.gradle.api.tasks.Input
  public abstract Property<Boolean> getAcceptanceConditionsOptionProvided();

  @TaskAction
  public void check() {
    clearSelectedSpecHandoffs();
    if (!formalVerifyRequested()) {
      deleteReview(getReviewRoot().get().getAsFile().toPath());
      deleteReview(
          getReviewRoot().get().getAsFile().toPath().getParent().getParent().resolve("review"));
    }
    Path publicCases = getPublicCaseRoot().get().getAsFile().toPath();
    Path hiddenSource = getHiddenSourceRoot().get().getAsFile().toPath();
    validateCaseFileExtensions(publicCases);
    List<ToppleCaseSource> sources =
        new ArrayList<>(List.of(new ToppleCaseSource(publicCases, CaseVisibility.PUBLIC)));
    Path hiddenCases = hiddenSource.resolve("resources/topplecat/cases");
    if (Files.exists(hiddenCases)) {
      validateCaseFileExtensions(hiddenCases);
      sources.add(new ToppleCaseSource(hiddenCases, CaseVisibility.HIDDEN));
    }
    List<ToppleCaseData> cases;
    try {
      cases = ToppleCaseReader.readAll(sources);
    } catch (ToppleCatException exception) {
      throw new ToppleCatException(
          "ToppleCat check could not read case data: "
              + exception.getMessage()
              + " Fix the named JSON/YAML file and row, then run toppleCatCheck again.",
          exception);
    }
    if (cases.stream().noneMatch(testCase -> testCase.visibility() == CaseVisibility.PUBLIC)) {
      throw new ToppleCatException(
          "No public ToppleCat JSON/YAML cases found under " + publicCases);
    }
    List<CompilerScenarioDescriptor> descriptors =
        CompilerDescriptorReader.read(
            getDescriptorClassDirectories().getFiles().stream()
                .map(file -> file.toPath())
                .toList());
    List<CompilerPropertyDescriptor> publicProperties =
        CompilerDescriptorReader.readProperties(
            getDescriptorClassDirectories().getFiles().stream()
                .map(file -> file.toPath())
                .toList());
    List<CompilerPropertyDescriptor> forbiddenHiddenProperties =
        CompilerDescriptorReader.readProperties(
            getForbiddenHiddenPropertyDescriptorClassDirectories().getFiles().stream()
                .map(file -> file.toPath())
                .toList());
    if (!forbiddenHiddenProperties.isEmpty()) {
      throw new ToppleCatException(
          "@ToppleProperty is supported only under src/test. Remove it from src/hiddenTest; "
              + "reviewer custody supplies hidden typed rows only.");
    }
    ContractDefinition definition =
        ContractDefinitionBuilder.build(descriptors, publicProperties, cases);
    SpecScopeResolver.ResolvedSpecScope scope =
        SpecScopeResolver.resolve(
            getProjectRoot().get().getAsFile().toPath(),
            getSelectedSpecPaths().getOrElse(List.of()),
            getSpecOptionProvided().getOrElse(false),
            getSelectedAcceptanceConditionIds().getOrElse(List.of()),
            getAcceptanceConditionsOptionProvided().getOrElse(false));
    validateSelectedBindings(scope, descriptors);
    writeDefinition(definition);
    writeScope(scope);
    writeSelectedSpecProjection(scope, descriptors, publicProperties);
    if (!formalVerifyRequested()) {
      reviewerAdvisories(definition, scope)
          .forEach(
              advisory ->
                  getLogger()
                      .warn(
                          "ToppleCat reviewer advisory {} for {} at {} ({} public, {} hidden).",
                          advisory.ruleCode(),
                          advisory.acId(),
                          advisory.expectedPath(),
                          advisory.publicCount(),
                          advisory.hiddenCount()));
    }
    getLogger()
        .lifecycle(
            "ToppleCat check passed: {} ACs, {} case rows, {} Properties, definition {}.",
            definition.acceptanceConditions().size(),
            cases.size(),
            publicProperties.size(),
            definition.digest());
  }

  private void clearSelectedSpecHandoffs() {
    for (Path output :
        List.of(
            getSelectedSpecScopeFile().get().getAsFile().toPath(),
            getSelectedSpecProjectionFile().get().getAsFile().toPath())) {
      try {
        Files.deleteIfExists(output);
      } catch (IOException exception) {
        throw new GradleException(
            "Cannot clear stale selected ToppleCat Spec handoff "
                + output
                + ": "
                + exception.getMessage(),
            exception);
      }
    }
  }

  /** Verify rechecks the current public contract but must not erase the prior reviewer handoff. */
  private boolean formalVerifyRequested() {
    return getProject().getGradle().getTaskGraph().getAllTasks().stream()
        .anyMatch(
            task ->
                task.getProject().equals(getProject()) && task.getName().equals("toppleCatVerify"));
  }

  private void writeScope(SpecScopeResolver.ResolvedSpecScope scope) {
    Path output = getSelectedSpecScopeFile().get().getAsFile().toPath();
    try {
      Files.createDirectories(output.getParent());
      Files.writeString(
          output,
          SelectedSpecScopeJson.write(scope.scope()),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot write selected ToppleCat Spec scope " + output + ": " + exception.getMessage(),
          exception);
    }
  }

  private void writeSelectedSpecProjection(
      SpecScopeResolver.ResolvedSpecScope scope,
      List<CompilerScenarioDescriptor> descriptors,
      List<CompilerPropertyDescriptor> properties) {
    Path output = getSelectedSpecProjectionFile().get().getAsFile().toPath();
    SelectedSpecProjection projection =
        new SelectedSpecProjection(
            SelectedSpecProjection.SCHEMA_VERSION,
            scope.parsedSpecs().documents(),
            scope.parsedSpecs().locations(),
            descriptors.stream()
                .collect(
                    Collectors.toMap(
                        CompilerScenarioDescriptor::acId,
                        descriptor ->
                            JavaSourceSnapshot.read(
                                getPublicTestSourceRoot().get().getAsFile().toPath(),
                                descriptor.sourceRef().file(),
                                descriptor.sourceRef().line()),
                        (left, right) -> left,
                        LinkedHashMap::new)),
            properties.stream()
                .collect(
                    Collectors.toMap(
                        CompilerPropertyDescriptor::methodIdentity,
                        property ->
                            JavaSourceSnapshot.read(
                                getPublicTestSourceRoot().get().getAsFile().toPath(),
                                property.sourceRef().file(),
                                property.sourceRef().line()),
                        (left, right) -> left,
                        LinkedHashMap::new)));
    try {
      Files.createDirectories(output.getParent());
      Files.writeString(
          output,
          ReportJson.writeSelectedSpecProjection(projection),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot write checked Selected Spec projection " + output + ": " + exception.getMessage(),
          exception);
    }
  }

  private void writeDefinition(ContractDefinition definition) {
    Path output = getDefinitionFile().get().getAsFile().toPath();
    try {
      Files.createDirectories(output.getParent());
      Files.writeString(
          output,
          ContractDefinitionJson.write(definition),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot write ToppleCat contract definition " + output + ": " + exception.getMessage(),
          exception);
    }
  }

  private void validateSelectedBindings(
      SpecScopeResolver.ResolvedSpecScope scope, List<CompilerScenarioDescriptor> descriptors) {
    if (!scope.scope().selected()) {
      return;
    }
    Set<String> acIds =
        descriptors.stream().map(CompilerScenarioDescriptor::acId).collect(Collectors.toSet());
    scope.scope().acceptanceConditionIds().stream()
        .filter(acId -> !acIds.contains(acId))
        .findFirst()
        .ifPresent(
            acId -> {
              throw new ToppleCatException(
                  "[TC-SPEC-AC-BINDING-MISSING] Selected ToppleCat AC "
                      + acId
                      + " has no @ToppleAcceptanceTest binding. Add"
                      + " @ToppleAcceptanceTest(\""
                      + acId
                      + "\") before review.");
            });
  }

  private static List<ContractQualityAdvisory> reviewerAdvisories(
      ContractDefinition definition, SpecScopeResolver.ResolvedSpecScope scope) {
    return ContractQualityAdvisor.analyze(
        definition.acceptanceConditions().stream()
            .filter(
                contract ->
                    !scope.scope().selected()
                        || scope.scope().acceptanceConditionIds().contains(contract.acId()))
            .flatMap(contract -> contract.cases().stream())
            .toList());
  }

  private static void deleteReview(Path reviewRoot) {
    if (!Files.exists(reviewRoot)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(reviewRoot)) {
      for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot clear stale ToppleCat reviewer review "
              + reviewRoot
              + ": "
              + exception.getMessage(),
          exception);
    }
  }

  private static void validateCaseFileExtensions(Path root) {
    if (!Files.exists(root)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(root)) {
      Path unsupported =
          paths
              .filter(Files::isRegularFile)
              .filter(path -> !path.getFileName().toString().matches(".*\\.(json|ya?ml)"))
              .findFirst()
              .orElse(null);
      if (unsupported != null) {
        throw new ToppleCatException("Topple case source must be JSON or YAML: " + unsupported);
      }
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot inspect Topple case root " + root + ": " + exception.getMessage(), exception);
    }
  }
}
