package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.SelectedSpecScope;
import io.github.samzhu.topplecat.core.SelectedSpecScopeJson;
import io.github.samzhu.topplecat.pitest.ToppleCatManagedMutationProfile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/**
 * ToppleCat's formal PIT producer.
 *
 * <p>This intentionally is not a {@code PitestTask}. A consumer commonly applies project-wide
 * {@code tasks.withType(PitestTask)} conventions for its own mutation workflow. Formal Verify must
 * not inherit those conventions: its producer, target methods, report location, runtime, output
 * matrix, and managed profile belong solely to ToppleCat.
 */
public abstract class ToppleCatManagedPitTask extends DefaultTask {
  private final ExecOperations execOperations;

  @Inject
  public ToppleCatManagedPitTask(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  @Internal
  public abstract DirectoryProperty getReportDirectory();

  @Internal
  public abstract SetProperty<String> getTargetClasses();

  @Internal
  public abstract DirectoryProperty getDescriptorDirectory();

  @org.gradle.api.tasks.InputFile
  @org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.NONE)
  public abstract RegularFileProperty getSelectedSpecScopeFile();

  @Internal
  public abstract DirectoryProperty getWorkingDirectory();

  @Internal
  public abstract ConfigurableFileCollection getSourceDirectories();

  @Internal
  public abstract ConfigurableFileCollection getMutableCodePaths();

  @Internal
  public abstract ConfigurableFileCollection getAdditionalClasspath();

  @Internal
  public abstract RegularFileProperty getAdditionalClasspathFile();

  @Internal
  public abstract ConfigurableFileCollection getLaunchClasspath();

  @Internal
  public abstract ListProperty<String> getChildProcessJvmArgs();

  @TaskAction
  public void runManagedPit() {
    AcceptanceTestTargets targets = acceptanceTestTargets();
    Path report = getReportDirectory().get().getAsFile().toPath().resolve("mutations.xml");
    writeAdditionalClasspath();
    execOperations.javaexec(
        specification -> {
          specification
              .getMainClass()
              .set("org.pitest.mutationtest.commandline.MutationCoverageReport");
          specification.setClasspath(getLaunchClasspath());
          specification.setWorkingDir(getWorkingDirectory().get().getAsFile());
          specification.args(arguments(targets));
        });
    writeEmptyMutationMatrixWhenPitFoundNoMutants(report);
  }

  private List<String> arguments(AcceptanceTestTargets targets) {
    List<String> arguments = new ArrayList<>();
    arguments.add("--reportDir=" + getReportDirectory().get().getAsFile());
    arguments.add("--targetClasses=" + String.join(",", new TreeSet<>(getTargetClasses().get())));
    arguments.add("--targetTests=" + String.join(",", targets.classes()));
    arguments.add("--includedTestMethods=" + String.join(",", targets.methods()));
    arguments.add("--mutators=" + String.join(",", ToppleCatManagedMutationProfile.operatorIds()));
    arguments.add("--outputFormats=XML");
    arguments.add("--fullMutationMatrix=true");
    arguments.add("--timestampedReports=false");
    arguments.add("--failWhenNoMutations=false");
    arguments.add("--mutationThreshold=0");
    arguments.add("--coverageThreshold=0");
    arguments.add("--testStrengthThreshold=0");
    arguments.add("--classPathFile=" + getAdditionalClasspathFile().get().getAsFile());
    arguments.add("--sourceDirs=" + commaSeparatedPaths(getSourceDirectories()));
    arguments.add("--mutableCodePaths=" + commaSeparatedPaths(getMutableCodePaths()));
    arguments.add("--jvmArgs=" + String.join(",", getChildProcessJvmArgs().get()));
    return List.copyOf(arguments);
  }

  private AcceptanceTestTargets acceptanceTestTargets() {
    SelectedSpecScope scope;
    Set<String> selectedAcIds;
    try {
      scope =
          SelectedSpecScopeJson.read(
              Files.readString(getSelectedSpecScopeFile().get().getAsFile().toPath()));
      selectedAcIds = Set.copyOf(scope.acceptanceConditionIds());
    } catch (IOException | RuntimeException exception) {
      throw new GradleException(
          "ToppleCat managed PIT could not read the selected Delivery Scope.", exception);
    }
    List<CompilerScenarioDescriptor> descriptors =
        CompilerDescriptorReader.read(List.of(getDescriptorDirectory().get().getAsFile().toPath()));
    rejectAmbiguousSelectedOverloads(scope, selectedAcIds, descriptors);

    Set<String> classes = new TreeSet<>();
    Set<String> methods = new TreeSet<>();
    for (CompilerScenarioDescriptor descriptor : descriptors) {
      if (scope.selected() && !selectedAcIds.contains(descriptor.acId())) {
        continue;
      }
      classes.add(descriptor.declaringBinaryName());
      methods.add(descriptor.methodName());
    }
    if (classes.isEmpty() || methods.isEmpty()) {
      throw new GradleException(
          "ToppleCat compiler emitted no public @ToppleAcceptanceTest descriptors for PIT.");
    }
    return new AcceptanceTestTargets(List.copyOf(classes), List.copyOf(methods));
  }

  /**
   * PIT's JUnit 5 producer cannot select a compiler-defined Acceptance Method by its JVM
   * descriptor. A selected delivery must therefore stop before PIT when its target test class also
   * contains an unselected public Acceptance Method.
   */
  private static void rejectAmbiguousSelectedOverloads(
      SelectedSpecScope scope,
      Set<String> selectedAcIds,
      List<CompilerScenarioDescriptor> descriptors) {
    if (!scope.selected()) {
      return;
    }
    Map<String, List<CompilerScenarioDescriptor>> descriptorsByClass = new HashMap<>();
    for (CompilerScenarioDescriptor descriptor : descriptors) {
      descriptorsByClass
          .computeIfAbsent(descriptor.declaringBinaryName(), ignored -> new ArrayList<>())
          .add(descriptor);
    }
    for (Map.Entry<String, List<CompilerScenarioDescriptor>> entry :
        descriptorsByClass.entrySet()) {
      boolean selected =
          entry.getValue().stream()
              .anyMatch(descriptor -> selectedAcIds.contains(descriptor.acId()));
      boolean unselected =
          entry.getValue().stream()
              .anyMatch(descriptor -> !selectedAcIds.contains(descriptor.acId()));
      if (selected && unselected) {
        throw new GradleException(
            "ToppleCat managed PIT cannot safely target selected Acceptance Methods when "
                + entry.getKey()
                + " has both selected and unselected Acceptance Methods. PIT cannot filter its "
                + "JUnit 5 producer by compiler method descriptor, so select every Acceptance "
                + "Method in that class or move the selected ACs to a dedicated class.");
      }
    }
  }

  private void writeAdditionalClasspath() {
    Path classpathFile = getAdditionalClasspathFile().get().getAsFile().toPath();
    try {
      Files.createDirectories(classpathFile.getParent());
      Files.writeString(
          classpathFile,
          getAdditionalClasspath().getFiles().stream()
              .map(File::getAbsolutePath)
              .sorted()
              .reduce((left, right) -> left + System.lineSeparator() + right)
              .orElse(""));
    } catch (IOException exception) {
      throw new GradleException(
          "ToppleCat managed PIT producer could not write its test runtime classpath.", exception);
    }
  }

  private static String commaSeparatedPaths(ConfigurableFileCollection files) {
    return files.getFiles().stream()
        .map(File::getAbsolutePath)
        .sorted()
        .reduce((left, right) -> left + "," + right)
        .orElse("");
  }

  private static void writeEmptyMutationMatrixWhenPitFoundNoMutants(Path report) {
    if (Files.isRegularFile(report)) {
      return;
    }
    try {
      Files.createDirectories(report.getParent());
      Files.writeString(report, "<mutations></mutations>\n");
    } catch (IOException exception) {
      throw new GradleException(
          "ToppleCat managed PIT producer could not record its zero-mutant matrix.", exception);
    }
  }

  private record AcceptanceTestTargets(List<String> classes, List<String> methods) {}
}
