package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.junit.ToppleJunit;
import io.github.samzhu.topplecat.pitest.ToppleCatManagedMutationProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

/** Gradle entry point for the rebuilt ToppleCat verification workflow. */
public final class ToppleCatPlugin implements Plugin<Project> {
  private static final Pattern JAVA_PACKAGE =
      Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;");

  @Override
  public void apply(Project project) {
    ToppleCatExtension extension =
        project.getExtensions().create("toppleCat", ToppleCatExtension.class);
    extension
        .getPublicCaseRoot()
        .convention(
            project.getLayout().getProjectDirectory().dir("src/test/resources/topplecat/cases"));
    extension
        .getHiddenSourceRoot()
        .convention(project.getLayout().getProjectDirectory().dir("src/hiddenTest"));
    extension.getHiddenTests().getEnabled().convention(true);
    extension.getMutationTesting().getEnabled().convention(true);
    extension.getMutationTesting().getThreshold().convention(100);
    extension.getExpectedConsumption().getEnabled().convention(true);
    extension.getPropertyBasedTesting().getEnabled().convention(true);
    extension.getCommandLineSpecPaths().convention(List.of());
    extension.getCommandLineSpecProvided().convention(false);
    extension.getAllHiddenRequested().convention(false);
    project
        .getPluginManager()
        .withPlugin("java", ignored -> configureJavaProject(project, extension));
  }

  private static void configureJavaProject(Project project, ToppleCatExtension extension) {
    Directory runDirectory =
        project.getLayout().getBuildDirectory().dir("topplecat/runs/current").get();
    Path contractIntegrityResult =
        runDirectory.file("contract-integrity.json").getAsFile().toPath();
    SourceSetContainer sourceSets =
        project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
    SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
    SourceSet hidden = sourceSets.maybeCreate("hiddenTest");
    hidden.setCompileClasspath(
        hidden
            .getCompileClasspath()
            .plus(main.getOutput())
            .plus(test.getOutput())
            .plus(test.getCompileClasspath()));
    hidden.setRuntimeClasspath(
        hidden
            .getRuntimeClasspath()
            .plus(hidden.getOutput())
            .plus(main.getOutput())
            .plus(test.getRuntimeClasspath()));

    project
        .getTasks()
        .withType(Test.class)
        .configureEach(
            task -> {
              if (task.getName().equals("test")) {
                task.systemProperty(
                    ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY,
                    extension.getPublicCaseRoot().get().getAsFile().getAbsolutePath());
                task.systemProperty(ToppleJunit.CASE_EXECUTION_SCOPE_PROPERTY, "PUBLIC_ONLY");
                task.systemProperty(ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY, "true");
                task.useJUnitPlatform(options -> options.excludeTags(ToppleJunit.PROPERTY_TAG));
              }
            });

    project
        .getTasks()
        .register(
            "toppleCatInit",
            ToppleCatInitTask.class,
            task -> {
              task.setGroup("verification");
              task.setDescription(
                  "Creates a minimal ToppleCat authoring skeleton without overwriting files.");
              task.getProjectRoot().set(project.getLayout().getProjectDirectory());
            });
    TaskProvider<ToppleCatPrepareRunTask> prepareRun =
        project
            .getTasks()
            .register(
                "toppleCatPrepareRun",
                ToppleCatPrepareRunTask.class,
                task -> {
                  task.setDescription(
                      "Prepares the internal workspace for one ToppleCat verification run.");
                  task.getRunDirectory().set(runDirectory);
                });
    TaskProvider<ToppleCatCompileContractsTask> compileContracts =
        project
            .getTasks()
            .register(
                "toppleCatCompileContracts",
                ToppleCatCompileContractsTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Uses javac symbols to validate @ToppleAcceptanceTest scenarios and emit"
                          + " descriptors.");
                  task.getSourceFiles().from(test.getAllJava());
                  task.getCompileClasspath().from(test.getCompileClasspath());
                  task.getDescriptorClassesDirectory()
                      .set(project.getLayout().getBuildDirectory().dir("topplecat/compiler"));
                  task.dependsOn(test.getCompileJavaTaskName());
                });
    project
        .getTasks()
        .named("test", Test.class)
        .configure(
            task -> {
              // New-style Scenario resolution consumes only compiler-owned descriptors. Make the
              // descriptor directory available to ordinary developer tests without creating a
              // formal definition, evidence run, or custody dependency.
              task.dependsOn(compileContracts);
              task.setClasspath(
                  task.getClasspath()
                      .plus(
                          project.files(
                              compileContracts.flatMap(
                                  ToppleCatCompileContractsTask::getDescriptorClassesDirectory))));
            });
    TaskProvider<ToppleCatCompileContractsTask> validateReviewerSource =
        project
            .getTasks()
            .register(
                "toppleCatValidateReviewerSource",
                ToppleCatCompileContractsTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Validates that reviewer-only source supplies typed rows, not Property"
                          + " declarations.");
                  task.getSourceFiles().from(hidden.getAllJava());
                  task.getCompileClasspath().from(hidden.getCompileClasspath());
                  task.getDescriptorClassesDirectory()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .dir("topplecat/forbidden-hidden-properties"));
                  task.dependsOn(test.getCompileJavaTaskName());
                });
    TaskProvider<ToppleCatCheckTask> check =
        project
            .getTasks()
            .register(
                "toppleCatCheck",
                ToppleCatCheckTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Validates ToppleCat Java bindings and public/reviewer case data without"
                          + " execution.");
                  task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                  task.getPublicCaseRoot().set(extension.getPublicCaseRoot());
                  task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                  task.getCaseSources()
                      .from(extension.getPublicCaseRoot(), extension.getHiddenSourceRoot());
                  task.getDescriptorClassDirectories()
                      .from(
                          compileContracts.flatMap(
                              ToppleCatCompileContractsTask::getDescriptorClassesDirectory));
                  task.getForbiddenHiddenPropertyDescriptorClassDirectories()
                      .from(
                          validateReviewerSource.flatMap(
                              ToppleCatCompileContractsTask::getDescriptorClassesDirectory));
                  configureScopeTask(task, extension);
                  task.getReviewRoot()
                      .set(project.getLayout().getBuildDirectory().dir("topplecat/reports/review"));
                  task.getDefinitionFile()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .file("topplecat/contract-definition.json"));
                  task.getSelectedSpecScopeFile()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .file("topplecat/selected-spec-scope.json"));
                  task.getOutputs().upToDateWhen(ignored -> false);
                  task.getOutputs()
                      .doNotCacheIf(
                          "ToppleCat contract inputs include reviewer custody state.",
                          ignored -> true);
                });
    check.configure(task -> task.dependsOn(compileContracts, validateReviewerSource));
    TaskProvider<ToppleCatReviewTask> review =
        project
            .getTasks()
            .register(
                "toppleCatReview",
                ToppleCatReviewTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Writes a reviewer-only static contract review without executing tests.");
                  task.dependsOn(check);
                  task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                  task.getPublicTestSourceRoot()
                      .set(project.getLayout().getProjectDirectory().dir("src/test/java"));
                  task.getReviewRoot()
                      .set(project.getLayout().getBuildDirectory().dir("topplecat/reports/review"));
                  task.getDefinitionFile()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .file("topplecat/contract-definition.json"));
                  configureScopeTask(task, extension);
                });
    TaskProvider<ToppleCatSealTask> hide =
        project
            .getTasks()
            .register(
                "toppleCatSeal",
                ToppleCatSealTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Moves the complete reviewer-only src/hiddenTest source set into local hidden"
                          + " storage.");
                  task.dependsOn(review);
                  task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                  task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                  configureScopeTask(task, extension);
                  configureApprovalInputs(task, project, test, extension);
                });
    Provider<ToppleCatCustodyBuildService> custodyService =
        project
            .getGradle()
            .getSharedServices()
            .registerIfAbsent(
                "toppleCatCustody-" + project.getPath().replace(':', '_'),
                ToppleCatCustodyBuildService.class,
                spec ->
                    spec.getParameters()
                        .getProjectRoot()
                        .set(project.getLayout().getProjectDirectory()));
    TaskProvider<ToppleCatAcquireCustodyTask> acquireCustody =
        project
            .getTasks()
            .register(
                "toppleCatAcquireCustody",
                ToppleCatAcquireCustodyTask.class,
                task -> {
                  task.getCustodyService().set(custodyService);
                  task.usesService(custodyService);
                });
    hide.configure(
        task -> {
          task.mustRunAfter(acquireCustody);
          task.usesService(custodyService);
        });
    TaskProvider<ToppleCatRestoreTask> restore =
        project
            .getTasks()
            .register(
                "toppleCatRestore",
                ToppleCatRestoreTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Restores the complete reviewer-only source set from validated local hidden"
                          + " storage.");
                  task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                  task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                });
    restore.configure(
        task -> {
          // Public contract checking reads the hidden-root path to reject unsupported reviewer
          // declarations. Restore only after that public check so Gradle sees one deliberate
          // custody transition rather than competing producers for src/hiddenTest.
          task.mustRunAfter(acquireCustody, hide, check, validateReviewerSource);
          task.usesService(custodyService);
          task.getOutputs().upToDateWhen(ignored -> false);
          task.getOutputs()
              .doNotCacheIf("Reviewer source restore is custody-state dependent.", ignored -> true);
        });
    TaskProvider<ToppleCatResealTask> updateEscrow =
        project
            .getTasks()
            .register(
                "toppleCatReseal",
                ToppleCatResealTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Validates, reviews, and explicitly updates reviewer-only local escrow"
                          + " custody.");
                  task.dependsOn(review);
                  task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                  task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                  configureScopeTask(task, extension);
                  configureApprovalInputs(task, project, test, extension);
                });
    updateEscrow.configure(
        task -> {
          task.mustRunAfter(acquireCustody, restore);
          task.usesService(custodyService);
        });
    TaskProvider<ToppleCatReviewerDefinitionTask> reviewerDefinition =
        project
            .getTasks()
            .register(
                "toppleCatReviewerDefinition",
                ToppleCatReviewerDefinitionTask.class,
                task -> {
                  task.setDescription(
                      "Builds the run-scoped reviewer definition from restored source before"
                          + " verification.");
                  task.getPublicCaseRoot().set(extension.getPublicCaseRoot());
                  task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                  task.getDescriptorClassDirectories()
                      .from(
                          compileContracts.flatMap(
                              ToppleCatCompileContractsTask::getDescriptorClassesDirectory));
                  task.getReviewerDefinitionFile()
                      .set(runDirectory.file("reviewer-definition.json"));
                  task.dependsOn(prepareRun, restore, check);
                  task.mustRunAfter(acquireCustody, hide);
                  task.usesService(custodyService);
                });
    TaskProvider<ToppleCatRehideTask> rehide =
        project
            .getTasks()
            .register(
                "toppleCatRehide",
                ToppleCatRehideTask.class,
                task -> {
                  task.setGroup("verification");
                  task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                });
    TaskProvider<ToppleCatContractIntegrityTask> contractIntegrity =
        project
            .getTasks()
            .register(
                "toppleCatContractIntegrity",
                ToppleCatContractIntegrityTask.class,
                task -> {
                  task.setDescription(
                      "Freshly compares the current public contract with the active reviewer"
                          + " approval.");
                  task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                  task.getResultFile().set(runDirectory.file("contract-integrity.json"));
                  configureApprovalInputs(task, project, test, extension);
                  // Verify reuses an existing Mechanical Seal. It must never schedule the public
                  // Seal workflow, because that would make an implementation run look like a new
                  // reviewer approval.
                  task.dependsOn(prepareRun, check);
                  task.getOutputs().upToDateWhen(ignored -> false);
                  task.getOutputs()
                      .doNotCacheIf(
                          "ToppleCat contract-integrity evidence is run-scoped.", ignored -> true);
                });
    contractIntegrity.configure(
        task -> {
          task.mustRunAfter(acquireCustody);
          task.usesService(custodyService);
        });

    TaskProvider<Test> verificationTest =
        project
            .getTasks()
            .register(
                "toppleCatVerificationTest",
                Test.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Runs only public @ToppleAcceptanceTest rows for formal verification.");
                  task.dependsOn(prepareRun, check, test.getClassesTaskName());
                  task.setTestClassesDirs(test.getOutput().getClassesDirs());
                  task.setClasspath(test.getRuntimeClasspath());
                  task.useJUnitPlatform();
                  configureCaseProperties(task, extension, "PUBLIC_ONLY", true);
                  configureVerificationArtifacts(
                      task, runDirectory, VerificationRunArtifacts.JUNIT);
                  configureNarrativeEvents(task, runDirectory, true);
                  requireFreshVerificationExecution(task);
                  task.systemProperty(
                      ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                      project
                          .getLayout()
                          .getBuildDirectory()
                          .file("topplecat/contract-definition.json")
                          .get()
                          .getAsFile()
                          .getAbsolutePath());
                });
    verificationTest.configure(
        task -> {
          task.dependsOn(contractIntegrity);
          task.onlyIf(ignored -> ToppleCatContractIntegrityTask.passed(contractIntegrityResult));
          task.mustRunAfter(acquireCustody);
          task.usesService(custodyService);
          configureSelectedAcceptanceScope(task, extension, project, false);
          task.useJUnitPlatform(options -> options.excludeTags(ToppleJunit.PROPERTY_TAG));
        });
    reviewerDefinition.configure(
        task -> {
          task.dependsOn(contractIntegrity);
          task.onlyIf(ignored -> ToppleCatContractIntegrityTask.passed(contractIntegrityResult));
        });
    TaskProvider<Test> hiddenTest =
        project
            .getTasks()
            .register(
                "toppleCatHiddenTest",
                Test.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Reuses public @ToppleAcceptanceTest methods with restored hidden typed rows"
                          + " only.");
                  task.dependsOn(prepareRun, restore, check, test.getClassesTaskName());
                  task.setTestClassesDirs(test.getOutput().getClassesDirs());
                  task.setClasspath(test.getRuntimeClasspath());
                  task.useJUnitPlatform(options -> options.includeTags(ToppleJunit.CONTRACT_TAG));
                  configureCaseProperties(task, extension, "HIDDEN_ONLY", true);
                  configureVerificationArtifacts(
                      task, runDirectory, VerificationRunArtifacts.REVIEWER_JUNIT);
                  configureNarrativeEvents(task, runDirectory, false);
                  requireFreshVerificationExecution(task);
                  task.systemProperty(
                      ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                      project
                          .getLayout()
                          .getBuildDirectory()
                          .file("topplecat/contract-definition.json")
                          .get()
                          .getAsFile()
                          .getAbsolutePath());
                  task.getFailOnNoDiscoveredTests().set(false);
                  task.usesService(custodyService);
                  configureSelectedAcceptanceScope(task, extension, project, true);
                  task.useJUnitPlatform(options -> options.excludeTags(ToppleJunit.PROPERTY_TAG));
                });
    TaskProvider<Test> propertyTest =
        project
            .getTasks()
            .register(
                "toppleCatPropertyTest",
                Test.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Runs all public ToppleCat Property declarations in an isolated current-run"
                          + " task.");
                  task.dependsOn(prepareRun, check, test.getClassesTaskName());
                  task.setTestClassesDirs(test.getOutput().getClassesDirs());
                  task.setClasspath(test.getRuntimeClasspath());
                  task.useJUnitPlatform(options -> options.includeTags(ToppleJunit.PROPERTY_TAG));
                  configureVerificationArtifacts(
                      task, runDirectory, VerificationRunArtifacts.PROPERTY_PUBLIC);
                  configurePropertyEvents(
                      task,
                      runDirectory,
                      project
                          .getLayout()
                          .getBuildDirectory()
                          .file("topplecat/contract-definition.json")
                          .get()
                          .getAsFile()
                          .toPath());
                  requireFreshVerificationExecution(task);
                  task.getFailOnNoDiscoveredTests().set(false);
                  task.systemProperty(
                      ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                      project
                          .getLayout()
                          .getBuildDirectory()
                          .file("topplecat/contract-definition.json")
                          .get()
                          .getAsFile()
                          .getAbsolutePath());
                });
    propertyTest.configure(
        task -> {
          task.dependsOn(contractIntegrity);
          task.onlyIf(ignored -> ToppleCatContractIntegrityTask.passed(contractIntegrityResult));
          task.mustRunAfter(acquireCustody, verificationTest, hiddenTest);
          task.usesService(custodyService);
          configureSelectedAcceptanceScope(task, extension, project, false);
        });
    TaskProvider<ToppleCatMutationGateTask> mutationGate =
        project
            .getTasks()
            .register(
                "toppleCatMutationGate",
                ToppleCatMutationGateTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Evaluates public executable contract mutation strength for each"
                          + " @ToppleAcceptanceTest acceptance condition.");
                  task.getPublicTestSourceRoot()
                      .set(project.getLayout().getProjectDirectory().dir("src/test/java"));
                  task.getDefinitionFile()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .file("topplecat/contract-definition.json"));
                  task.getPitReportFile().set(runDirectory.file("pit/mutations.xml"));
                  task.getResultsFile().set(runDirectory.file("mutation-results.json"));
                  task.getRunDirectory().set(runDirectory);
                  task.getThreshold().set(extension.getMutationTesting().getThreshold());
                  task.dependsOn(prepareRun, contractIntegrity);
                  task.mustRunAfter(verificationTest, hiddenTest, propertyTest);
                  task.usesService(custodyService);
                });
    mutationGate.configure(
        task ->
            task.onlyIf(ignored -> ToppleCatContractIntegrityTask.passed(contractIntegrityResult)));
    TaskProvider<ToppleCatReportTask> report =
        project
            .getTasks()
            .register(
                "toppleCatReport",
                ToppleCatReportTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Writes safe Spec and reviewer-only Verification reports plus evidence.");
                  task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                  task.getPublicCaseRoot().set(extension.getPublicCaseRoot());
                  task.getDefinitionFile()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .file("topplecat/contract-definition.json"));
                  task.getSelectedSpecPaths().set(extension.getCommandLineSpecPaths());
                  task.getSpecOptionProvided().set(extension.getCommandLineSpecProvided());
                  task.getAllHidden().set(extension.getAllHiddenRequested());
                  task.getRunDirectory().set(runDirectory);
                  task.getContractIntegrityResultFile()
                      .set(
                          contractIntegrity.flatMap(ToppleCatContractIntegrityTask::getResultFile));
                  task.getMutationResultsFile()
                      .set(mutationGate.flatMap(ToppleCatMutationGateTask::getResultsFile));
                  task.getMutationIncompleteReason().convention("");
                  task.getPropertyEnabled().convention(true);
                  task.getPropertyDisabledReason().convention("");
                  task.getReviewerDefinitionRequired().convention(false);
                  task.mustRunAfter(verificationTest, hiddenTest, propertyTest, mutationGate);
                  task.dependsOn(acquireCustody, contractIntegrity);
                  task.usesService(custodyService);
                });
    verificationTest.configure(task -> task.finalizedBy(report));
    hiddenTest.configure(task -> task.finalizedBy(report));
    propertyTest.configure(task -> task.finalizedBy(report));
    mutationGate.configure(task -> task.finalizedBy(report));
    hiddenTest.configure(task -> task.mustRunAfter(verificationTest));
    restore.configure(task -> task.mustRunAfter(verificationTest));
    rehide.configure(
        task -> {
          task.mustRunAfter(acquireCustody, report);
          task.usesService(custodyService);
        });

    TaskProvider<ToppleCatVerifyTask> verify =
        project
            .getTasks()
            .register(
                "toppleCatVerify",
                ToppleCatVerifyTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Runs the configured independent verification gates, then re-hides reviewer"
                          + " source.");
                  configureScopeTask(task, extension);
                  task.dependsOn(acquireCustody, contractIntegrity, verificationTest);
                  task.finalizedBy(report);
                });
    project.afterEvaluate(
        ignored ->
            configureVerificationGates(
                project,
                extension,
                restore,
                reviewerDefinition,
                verificationTest,
                hiddenTest,
                propertyTest,
                mutationGate,
                report,
                rehide,
                verify,
                hide,
                updateEscrow,
                contractIntegrity,
                contractIntegrityResult,
                compileContracts));
  }

  private static void configureCaseProperties(
      Test task,
      ToppleCatExtension extension,
      String executionScope,
      boolean expectedConsumptionEnforced) {
    task.systemProperty(
        ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY,
        extension.getPublicCaseRoot().get().getAsFile().getAbsolutePath());
    task.systemProperty(
        ToppleJunit.HIDDEN_CASE_SOURCES_PROPERTY,
        extension
            .getHiddenSourceRoot()
            .get()
            .getAsFile()
            .toPath()
            .resolve("resources/topplecat/cases")
            .toString());
    task.systemProperty(ToppleJunit.CASE_EXECUTION_SCOPE_PROPERTY, executionScope);
    task.systemProperty(
        ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY,
        Boolean.toString(expectedConsumptionEnforced));
  }

  private static void configureSelectedAcceptanceScope(
      Test task, ToppleCatExtension extension, Project project, boolean allowAllHiddenTests) {
    Path scope =
        project
            .getLayout()
            .getBuildDirectory()
            .getAsFile()
            .get()
            .toPath()
            .resolve("topplecat/selected-spec-scope.json")
            .toAbsolutePath();
    task.doFirst(
        ignored -> {
          boolean noSpecSelection = !extension.getCommandLineSpecProvided().getOrElse(false);
          boolean allSelected =
              noSpecSelection
                  || (allowAllHiddenTests && extension.getAllHiddenRequested().getOrElse(false));
          task.systemProperty(
              ToppleJunit.SELECTED_SCOPE_FILE_PROPERTY, allSelected ? "" : scope.toString());
          task.systemProperty(
              ToppleJunit.FILTER_ACCEPTANCE_TESTS_PROPERTY, Boolean.toString(!allSelected));
        });
  }

  private static void configureVerificationArtifacts(
      Test task, Directory runDirectory, String gate) {
    task.getReports().getJunitXml().getOutputLocation().set(runDirectory.dir("junit/" + gate));
    task.addTestListener(
        new TestListener() {
          @Override
          public void beforeSuite(TestDescriptor suite) {}

          @Override
          public void afterSuite(TestDescriptor suite, TestResult result) {
            if (suite.getParent() == null) {
              VerificationRunArtifacts.markCompleted(runDirectory.getAsFile().toPath(), gate);
            }
          }

          @Override
          public void beforeTest(TestDescriptor testDescriptor) {}

          @Override
          public void afterTest(TestDescriptor testDescriptor, TestResult result) {}
        });
  }

  private static void requireFreshVerificationExecution(Test task) {
    task.getOutputs().upToDateWhen(ignored -> false);
    task.getOutputs()
        .doNotCacheIf("ToppleCat verification evidence is run-scoped.", ignored -> true);
  }

  private static void configureNarrativeEvents(
      Test task, Directory runDirectory, boolean clearBeforeTask) {
    Path narrative = runDirectory.file("narrative-executions.jsonl").getAsFile().toPath();
    Path consumption =
        runDirectory.file("expected-consumption-executions.jsonl").getAsFile().toPath();
    task.systemProperty(
        ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, narrative.toAbsolutePath().toString());
    task.systemProperty(
        ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY,
        consumption.toAbsolutePath().toString());
    task.systemProperty(
        ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY,
        runDirectory.dir("attachments").getAsFile().toPath().toAbsolutePath().toString());
    if (clearBeforeTask) {
      task.doFirst(
          ignored -> {
            try {
              Files.deleteIfExists(narrative);
              Files.deleteIfExists(consumption);
            } catch (IOException exception) {
              throw new GradleException(
                  "Cannot clear ToppleCat verification sidecars in " + runDirectory, exception);
            }
          });
    }
  }

  private static void configurePropertyEvents(Test task, Directory runDirectory, Path definition) {
    Path events = runDirectory.file("public-property-events.jsonl").getAsFile().toPath();
    task.systemProperty(
        ToppleJunit.PROPERTY_EVENTS_FILE_PROPERTY, events.toAbsolutePath().toString());
    task.systemProperty(ToppleJunit.HIDDEN_CASE_SOURCES_PROPERTY, "");
    task.systemProperty(ToppleJunit.CASE_EXECUTION_SCOPE_PROPERTY, "PUBLIC_ONLY");
    // Verification must always make fresh managed evidence, regardless of a developer's diagnostic
    // replay JVM flag.
    task.systemProperty("topplecat.property.replay", "");
    task.doFirst(
        ignored -> {
          try {
            Files.deleteIfExists(events);
            task.systemProperty(
                "topplecat.property.runId",
                Files.readString(runDirectory.file("run-id").getAsFile().toPath()).trim());
            ContractDefinition checked = ContractDefinitionJson.read(Files.readString(definition));
            task.systemProperty(
                "topplecat.property.executionContext",
                Hashing.sha256(Files.readAllBytes(definition)));
            for (var contract : checked.acceptanceConditions()) {
              for (var property : contract.properties()) {
                task.systemProperty(
                    "topplecat.property.sourceDigest." + property.methodIdentity(),
                    property.sourceDigest());
              }
            }
          } catch (IOException exception) {
            throw new GradleException(
                "Cannot prepare ToppleCat Property event evidence in " + runDirectory, exception);
          }
        });
  }

  private static void configureVerificationGates(
      Project project,
      ToppleCatExtension extension,
      TaskProvider<ToppleCatRestoreTask> restore,
      TaskProvider<ToppleCatReviewerDefinitionTask> reviewerDefinition,
      TaskProvider<Test> verificationTest,
      TaskProvider<Test> hiddenTest,
      TaskProvider<Test> propertyTest,
      TaskProvider<ToppleCatMutationGateTask> mutationGate,
      TaskProvider<ToppleCatReportTask> report,
      TaskProvider<ToppleCatRehideTask> rehide,
      TaskProvider<ToppleCatVerifyTask> verify,
      TaskProvider<ToppleCatSealTask> hide,
      TaskProvider<ToppleCatResealTask> updateEscrow,
      TaskProvider<ToppleCatContractIntegrityTask> contractIntegrity,
      Path contractIntegrityResult,
      TaskProvider<ToppleCatCompileContractsTask> compileContracts) {
    VerificationConfiguration configuration = VerificationConfiguration.resolve(extension);
    configureApprovalPolicy(
        project, extension, configuration, hide, updateEscrow, contractIntegrity);
    project
        .getTasks()
        .withType(Test.class)
        .configureEach(
            task -> {
              if (task.getName().equals("test")) {
                task.systemProperty(
                    ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY,
                    Boolean.toString(configuration.expectedConsumptionEnabled()));
              }
            });
    verificationTest.configure(
        task -> {
          configureCaseProperties(
              task, extension, "PUBLIC_ONLY", configuration.expectedConsumptionEnabled());
        });
    hiddenTest.configure(
        task -> {
          task.dependsOn(contractIntegrity);
          task.onlyIf(ignored -> ToppleCatContractIntegrityTask.passed(contractIntegrityResult));
          task.systemProperty(
              ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY,
              Boolean.toString(configuration.expectedConsumptionEnabled()));
          if (configuration.hiddenTestsEnabled()) {
            task.dependsOn(reviewerDefinition);
            task.systemProperty(
                ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                reviewerDefinition
                    .get()
                    .getReviewerDefinitionFile()
                    .get()
                    .getAsFile()
                    .getAbsolutePath());
          }
        });
    propertyTest.configure(
        task -> {
          task.setEnabled(configuration.propertyBasedTestingEnabled());
        });
    report.configure(
        task -> {
          task.getHiddenTestsEnabled().set(configuration.hiddenTestsEnabled());
          task.getHiddenTestsDisabledReason().set(configuration.hiddenTestsDisabledReason());
          task.getMutationEnabled().set(configuration.mutationEnabled());
          task.getMutationDisabledReason().set(configuration.mutationDisabledReason());
          task.getExpectedConsumptionEnabled().set(configuration.expectedConsumptionEnabled());
          task.getExpectedConsumptionDisabledReason()
              .set(configuration.expectedConsumptionDisabledReason());
          task.getPropertyEnabled().set(configuration.propertyBasedTestingEnabled());
          task.getPropertyDisabledReason().set(configuration.propertyDisabledReason());
          boolean reviewerDefinitionRequired = configuration.hiddenTestsEnabled();
          task.getReviewerDefinitionRequired().set(reviewerDefinitionRequired);
          if (reviewerDefinitionRequired) {
            task.getReviewerDefinitionFile()
                .set(
                    reviewerDefinition.flatMap(
                        ToppleCatReviewerDefinitionTask::getReviewerDefinitionFile));
            task.mustRunAfter(reviewerDefinition);
          }
        });
    if (configuration.hiddenTestsEnabled()) {
      verify.configure(task -> task.dependsOn(hiddenTest));
    }
    if (configuration.propertyBasedTestingEnabled()) {
      verify.configure(
          task -> {
            task.dependsOn(propertyTest);
          });
    }
    configureFormalVerifyFailureDeferral(project, verificationTest, verify);
    configureFormalVerifyFailureDeferral(project, hiddenTest, verify);
    configureFormalVerifyFailureDeferral(project, propertyTest, verify);
    restore.configure(
        task ->
            task.onlyIf(
                ignored ->
                    !project.getGradle().getTaskGraph().hasTask(verify.get())
                        || ToppleCatContractIntegrityTask.passed(contractIntegrityResult)));
    mutationGate.configure(
        task ->
            task.doFirst(
                ignored ->
                    task.getContinueAfterFailure()
                        .set(project.getGradle().getTaskGraph().hasTask(verify.get()))));
    report.configure(task -> task.finalizedBy(rehide));
    configureMutationGate(
        project,
        extension,
        configuration,
        contractIntegrity,
        verificationTest,
        hiddenTest,
        propertyTest,
        mutationGate,
        report,
        verify,
        contractIntegrityResult,
        compileContracts);
  }

  /**
   * A direct diagnostic task preserves ordinary Gradle failure behavior. Inside the formal Verify
   * graph, assertion failures are evidence inputs rather than the aggregate failure exit.
   */
  private static void configureFormalVerifyFailureDeferral(
      Project project, TaskProvider<Test> gate, TaskProvider<ToppleCatVerifyTask> verify) {
    gate.configure(
        task ->
            task.doFirst(
                ignored ->
                    task.setIgnoreFailures(
                        project.getGradle().getTaskGraph().hasTask(verify.get()))));
  }

  private static void configureApprovalInputs(
      ToppleCatApprovalInputs task, Project project, SourceSet test, ToppleCatExtension extension) {
    task.getApprovalBuildRoot().set(project.getRootProject().getLayout().getProjectDirectory());
    task.getApprovalPublicSourceRoots().from(test.getAllSource().getSourceDirectories());
    task.getApprovalCompileClasspath().from(test.getCompileClasspath());
    task.getApprovalPublicCaseRoot().set(extension.getPublicCaseRoot());
    task.getApprovalDefinitionFile()
        .set(project.getLayout().getBuildDirectory().file("topplecat/contract-definition.json"));
    task.getApprovalHiddenTestsEnabled().convention(true);
    task.getApprovalExpectedConsumptionEnabled().convention(true);
    task.getApprovalPropertyEnabled().convention(true);
    task.getApprovalMutationEnabled().convention(true);
    task.getApprovalMutationThreshold().convention(100);
    task.getApprovalSelectedSpecPaths().set(extension.getCommandLineSpecPaths());
    task.getApprovalSpecOptionProvided().set(extension.getCommandLineSpecProvided());
  }

  private static void configureScopeTask(ToppleCatScopedTask task, ToppleCatExtension extension) {
    task.getSelectedSpecPaths().set(extension.getCommandLineSpecPaths());
    task.getSpecOptionProvided().set(extension.getCommandLineSpecProvided());
  }

  private static void configureApprovalPolicy(
      Project project,
      ToppleCatExtension extension,
      VerificationConfiguration configuration,
      TaskProvider<ToppleCatSealTask> hide,
      TaskProvider<ToppleCatResealTask> updateEscrow,
      TaskProvider<ToppleCatContractIntegrityTask> contractIntegrity) {
    for (TaskProvider<? extends ToppleCatApprovalInputs> provider :
        List.of(hide, updateEscrow, contractIntegrity)) {
      provider.configure(
          task -> {
            task.getApprovalHiddenTestsEnabled().set(configuration.hiddenTestsEnabled());
            task.getApprovalExpectedConsumptionEnabled()
                .set(configuration.expectedConsumptionEnabled());
            task.getApprovalPropertyEnabled().set(configuration.propertyBasedTestingEnabled());
            task.getApprovalMutationEnabled().set(configuration.mutationEnabled());
            task.getApprovalMutationThreshold().set(extension.getMutationTesting().getThreshold());
          });
    }
  }

  private static void configureMutationGate(
      Project project,
      ToppleCatExtension extension,
      VerificationConfiguration configuration,
      TaskProvider<ToppleCatContractIntegrityTask> contractIntegrity,
      TaskProvider<Test> verificationTest,
      TaskProvider<Test> hiddenTest,
      TaskProvider<Test> propertyTest,
      TaskProvider<ToppleCatMutationGateTask> mutationGate,
      TaskProvider<ToppleCatReportTask> report,
      TaskProvider<ToppleCatVerifyTask> verify,
      Path contractIntegrityResult,
      TaskProvider<ToppleCatCompileContractsTask> compileContracts) {
    if (!configuration.mutationEnabled()) {
      return;
    }
    ProductionPackages productionPackages = productionPackages(project);
    if (productionPackages.targets().isEmpty()) {
      String reason =
          productionPackages.sourcesFound()
              ? "no production packages found under src/main/java; the mutation gate cannot run."
              : "no production sources found under src/main/java; the mutation gate cannot run.";
      report.configure(task -> task.getMutationIncompleteReason().set(reason));
      return;
    }

    SourceSetContainer sourceSets =
        project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
    SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
    Configuration managedRuntime = managedPitRuntime(project);
    TaskProvider<ToppleCatManagedPitTask> producer =
        project
            .getTasks()
            .register(
                "toppleCatManagedPit",
                ToppleCatManagedPitTask.class,
                task -> {
                  task.setGroup("verification");
                  task.setDescription(
                      "Runs ToppleCat's managed PIT 1.25.5 mutation producer for formal Verify.");
                  task.getReportDirectory()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .dir("topplecat/runs/current/pit"));
                  task.getTargetClasses().set(productionPackages.targets());
                  task.getChildProcessJvmArgs()
                      .set(
                          List.of(
                              "-D"
                                  + ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY
                                  + "="
                                  + extension
                                      .getPublicCaseRoot()
                                      .get()
                                      .getAsFile()
                                      .getAbsolutePath(),
                              "-D" + ToppleJunit.CASE_EXECUTION_SCOPE_PROPERTY + "=PUBLIC_ONLY",
                              "-D"
                                  + ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY
                                  + "=true",
                              "-D"
                                  + ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY
                                  + "="
                                  + project
                                      .getLayout()
                                      .getBuildDirectory()
                                      .file("topplecat/contract-definition.json")
                                      .get()
                                      .getAsFile()
                                      .getAbsolutePath()));
                  task.getDescriptorDirectory()
                      .set(project.getLayout().getBuildDirectory().dir("topplecat/compiler"));
                  task.getWorkingDirectory().set(project.getLayout().getProjectDirectory());
                  task.getSourceDirectories().from(main.getAllJava().getSourceDirectories());
                  task.getMutableCodePaths().from(main.getOutput().getClassesDirs());
                  task.getAdditionalClasspath().from(test.getRuntimeClasspath());
                  task.getAdditionalClasspathFile()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .file("topplecat/managed-pit-classpath"));
                  task.getLaunchClasspath().from(managedRuntime);
                  task.dependsOn(
                      main.getClassesTaskName(), test.getClassesTaskName(), compileContracts);
                  task.mustRunAfter(contractIntegrity, verificationTest, hiddenTest, propertyTest);
                  task.mustRunAfter(project.getTasks().named("toppleCatPrepareRun"));
                  task.onlyIf(
                      ignored ->
                          !project.getGradle().getTaskGraph().hasTask(verify.get())
                              || ToppleCatContractIntegrityTask.passed(contractIntegrityResult));
                  task.getOutputs()
                      .upToDateWhen(
                          ignored -> !project.getGradle().getTaskGraph().hasTask(verify.get()));
                  task.getOutputs()
                      .doNotCacheIf(
                          "ToppleCat Verify requires current-run managed PIT producer evidence.",
                          ignored -> project.getGradle().getTaskGraph().hasTask(verify.get()));
                });
    mutationGate.configure(
        task -> {
          task.dependsOn(producer);
          task.getOutputs()
              .upToDateWhen(ignored -> !project.getGradle().getTaskGraph().hasTask(verify.get()));
          task.getOutputs()
              .doNotCacheIf(
                  "ToppleCat Verify requires current-run mutation gate evidence.",
                  ignored -> project.getGradle().getTaskGraph().hasTask(verify.get()));
        });
    project
        .getTasks()
        .named("toppleCatPrepareRun", ToppleCatPrepareRunTask.class)
        .configure(
            task ->
                task.getMutationProducerReportFile()
                    .set(
                        project
                            .getLayout()
                            .getBuildDirectory()
                            .file("topplecat/runs/current/pit/mutations.xml")));
    verify.configure(task -> task.dependsOn(mutationGate));
  }

  private static Configuration managedPitRuntime(Project project) {
    Configuration runtime = project.getConfigurations().maybeCreate("toppleCatManagedPitRuntime");
    runtime.setCanBeConsumed(false);
    runtime.setCanBeResolved(true);
    runtime.setVisible(false);
    runtime
        .getResolutionStrategy()
        .force(
            "org.pitest:pitest-command-line:" + ToppleCatManagedMutationProfile.PIT_VERSION,
            "org.pitest:pitest:" + ToppleCatManagedMutationProfile.PIT_VERSION,
            "org.pitest:pitest-entry:" + ToppleCatManagedMutationProfile.PIT_VERSION,
            "org.pitest:pitest-junit5-plugin:1.2.3");
    project
        .getDependencies()
        .add(
            runtime.getName(),
            "org.pitest:pitest-command-line:" + ToppleCatManagedMutationProfile.PIT_VERSION);
    project.getDependencies().add(runtime.getName(), "org.pitest:pitest-junit5-plugin:1.2.3");
    return runtime;
  }

  private static ProductionPackages productionPackages(Project project) {
    Path sourceRoot =
        project.getLayout().getProjectDirectory().dir("src/main/java").getAsFile().toPath();
    if (!Files.isDirectory(sourceRoot)) {
      return new ProductionPackages(false, Set.of());
    }
    Set<String> patterns = new LinkedHashSet<>();
    boolean sourcesFound = false;
    try (java.util.stream.Stream<Path> files = Files.walk(sourceRoot)) {
      for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        sourcesFound = true;
        Matcher matcher = JAVA_PACKAGE.matcher(Files.readString(source));
        if (matcher.find()) {
          patterns.add(matcher.group(1) + ".*");
        }
      }
    } catch (IOException exception) {
      throw new GradleException(
          "ToppleCat could not inspect src/main/java to configure default PIT targets.", exception);
    }
    return new ProductionPackages(sourcesFound, Set.copyOf(patterns));
  }

  private record ProductionPackages(boolean sourcesFound, Set<String> targets) {}

  private record VerificationConfiguration(
      boolean hiddenTestsEnabled,
      String hiddenTestsDisabledReason,
      boolean mutationEnabled,
      String mutationDisabledReason,
      boolean expectedConsumptionEnabled,
      String expectedConsumptionDisabledReason,
      boolean propertyBasedTestingEnabled,
      String propertyDisabledReason) {
    private static VerificationConfiguration resolve(ToppleCatExtension extension) {
      boolean hiddenTests = extension.getHiddenTests().getEnabled().getOrElse(true);
      boolean mutation = extension.getMutationTesting().getEnabled().getOrElse(true);
      boolean expectedConsumption = extension.getExpectedConsumption().getEnabled().getOrElse(true);
      boolean property = extension.getPropertyBasedTesting().getEnabled().getOrElse(true);
      return new VerificationConfiguration(
          hiddenTests, hiddenTests ? "" : "disabled by toppleCat.hiddenTests.enabled=false",
          mutation, mutation ? "" : "disabled by toppleCat.mutationTesting.enabled=false",
          expectedConsumption,
              expectedConsumption ? "" : "disabled by toppleCat.expectedConsumption.enabled=false",
          property, property ? "" : "disabled by toppleCat.propertyBasedTesting.enabled=false");
    }
  }
}
