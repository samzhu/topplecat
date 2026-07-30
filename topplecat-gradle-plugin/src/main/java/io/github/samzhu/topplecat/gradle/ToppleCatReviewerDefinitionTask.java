package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.CompilerPropertyDescriptor;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCaseReader;
import io.github.samzhu.topplecat.core.ToppleCaseSource;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Builds the reviewer-only definition for one verification run after custody has restored hidden
 * source.
 *
 * <p>This task deliberately reads {@code src/hiddenTest} rather than escrow. Restore remains the
 * custody boundary, and the generated definition is retained only under the transient run
 * workspace.
 */
public abstract class ToppleCatReviewerDefinitionTask extends DefaultTask {
  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getPublicCaseRoot();

  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getHiddenSourceRoot();

  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getDescriptorClassDirectories();

  @OutputFile
  public abstract RegularFileProperty getReviewerDefinitionFile();

  @TaskAction
  public void buildDefinition() {
    Path publicCases = getPublicCaseRoot().get().getAsFile().toPath();
    Path hiddenCases =
        getHiddenSourceRoot().get().getAsFile().toPath().resolve("resources/topplecat/cases");
    validateCaseFileExtensions(publicCases);
    List<ToppleCaseSource> sources =
        new ArrayList<>(List.of(new ToppleCaseSource(publicCases, CaseVisibility.PUBLIC)));
    if (Files.exists(hiddenCases)) {
      validateCaseFileExtensions(hiddenCases);
      sources.add(new ToppleCaseSource(hiddenCases, CaseVisibility.HIDDEN));
    }
    List<ToppleCaseData> cases;
    try {
      cases = ToppleCaseReader.readAll(sources);
    } catch (ToppleCatException exception) {
      throw new ToppleCatException(
          "ToppleCat reviewer definition could not read restored case data: "
              + exception.getMessage(),
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
    ContractDefinition definition =
        ContractDefinitionBuilder.build(descriptors, publicProperties, cases);
    Path output = getReviewerDefinitionFile().get().getAsFile().toPath();
    try {
      Files.createDirectories(output.getParent());
      Files.writeString(
          output,
          ContractDefinitionJson.write(definition),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot write run-scoped ToppleCat reviewer definition "
              + output
              + ": "
              + exception.getMessage(),
          exception);
    }
    getLogger()
        .lifecycle(
            "ToppleCat reviewer definition built: {} ACs, {} case rows, {} Properties, digest {}.",
            definition.acceptanceConditions().size(),
            cases.size(),
            publicProperties.size(),
            definition.digest());
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
