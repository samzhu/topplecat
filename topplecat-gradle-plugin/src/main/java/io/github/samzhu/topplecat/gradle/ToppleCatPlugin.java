package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.junit.ToppleJunit;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gradle entry point for the rebuilt ToppleCat verification workflow.
 */
public final class ToppleCatPlugin implements Plugin<Project> {
    private static final String PITEST_PLUGIN_ID = "info.solidsoft.pitest";
    private static final Pattern JAVA_PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;");

    @Override
    public void apply(Project project) {
        ToppleCatExtension extension = project.getExtensions().create("toppleCat", ToppleCatExtension.class);
        extension.getPublicCaseRoot().convention(project.getLayout().getProjectDirectory().dir("src/test/resources/topplecat/cases"));
        extension.getHiddenSourceRoot().convention(project.getLayout().getProjectDirectory().dir("src/hiddenTest"));
        extension.getAdversarial().getEnabled().convention(true);
        extension.getAdversarial().getHiddenRetest().getEnabled().convention(true);
        extension.getAdversarial().getMutation().getEnabled().convention(true);
        extension.getAdversarial().getMutation().getThreshold().convention(100);
        extension.getAdversarial().getMutation().getProducerTask().convention("pitest");
        extension.getAdversarial().getMutation().getReportFile()
                .convention(project.getLayout().getBuildDirectory().file("reports/pitest/mutations.xml"));
        extension.getAdversarial().getExpectedConsumption().getEnabled().convention(true);
        project.getPluginManager().withPlugin("java", ignored -> configureJavaProject(project, extension));
    }

    private static void configureJavaProject(Project project, ToppleCatExtension extension) {
        Directory runDirectory = project.getLayout().getBuildDirectory()
                .dir("topplecat/runs/current").get();
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        SourceSet hidden = sourceSets.maybeCreate("hiddenTest");
        hidden.setCompileClasspath(hidden.getCompileClasspath().plus(main.getOutput()).plus(test.getOutput())
                .plus(test.getCompileClasspath()));
        hidden.setRuntimeClasspath(hidden.getRuntimeClasspath().plus(hidden.getOutput()).plus(main.getOutput())
                .plus(test.getRuntimeClasspath()));

        project.getTasks().withType(Test.class).configureEach(task -> {
            if (task.getName().equals("test")) {
                task.systemProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY,
                        extension.getPublicCaseRoot().get().getAsFile().getAbsolutePath());
                task.systemProperty(ToppleJunit.INCLUDE_HIDDEN_CASES_PROPERTY, "false");
                task.systemProperty(ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY, "true");
            }
        });

        project.getTasks().register("toppleCatInit", ToppleCatInitTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Creates a minimal ToppleCat authoring skeleton without overwriting files.");
            task.getProjectRoot().set(project.getLayout().getProjectDirectory());
        });
        TaskProvider<ToppleCatPrepareRunTask> prepareRun = project.getTasks().register(
                "toppleCatPrepareRun", ToppleCatPrepareRunTask.class, task -> {
                    task.setDescription("Prepares the internal workspace for one ToppleCat verification run.");
                    task.getRunDirectory().set(runDirectory);
                });
        TaskProvider<ToppleCatCompileContractsTask> compileContracts = project.getTasks().register(
                "toppleCatCompileContracts", ToppleCatCompileContractsTask.class, task -> {
                    task.setGroup("verification");
                    task.setDescription("Uses javac symbols to validate @ToppleTest scenarios and emit descriptors.");
                    task.getSourceFiles().from(test.getAllJava());
                    task.getCompileClasspath().from(test.getCompileClasspath());
                    task.getDescriptorClassesDirectory().set(project.getLayout().getBuildDirectory().dir("topplecat/compiler"));
                    task.dependsOn(test.getCompileJavaTaskName());
                });
        TaskProvider<ToppleCatCheckTask> check = project.getTasks().register("toppleCatCheck", ToppleCatCheckTask.class,
                task -> {
                    task.setGroup("verification");
                    task.setDescription("Validates ToppleCat Java bindings and public/reviewer case data without execution.");
                    task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                    task.getPublicCaseRoot().set(extension.getPublicCaseRoot());
                    task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                    task.getCaseSources().from(extension.getPublicCaseRoot(), extension.getHiddenSourceRoot());
                    task.getDescriptorClassDirectories().from(compileContracts.flatMap(
                            ToppleCatCompileContractsTask::getDescriptorClassesDirectory));
                    task.getSpecDocs().from(extension.getSpecDocs());
                    task.getReviewRoot().set(project.getLayout().getBuildDirectory().dir("topplecat/reports/review"));
                    task.getDefinitionFile().set(project.getLayout().getBuildDirectory().file("topplecat/contract-definition.json"));
                });
        check.configure(task -> task.dependsOn(compileContracts));
        project.getTasks().named("test", Test.class).configure(task -> {
            task.dependsOn(check);
            task.systemProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                    project.getLayout().getBuildDirectory().file("topplecat/contract-definition.json").get().getAsFile()
                            .getAbsolutePath());
        });
        TaskProvider<ToppleCatReviewTask> review = project.getTasks().register("toppleCatReview", ToppleCatReviewTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Writes a reviewer-only static contract review without executing tests.");
            task.dependsOn(check);
            task.getProjectRoot().set(project.getLayout().getProjectDirectory());
            task.getPublicTestSourceRoot().set(project.getLayout().getProjectDirectory().dir("src/test/java"));
            task.getReviewRoot().set(project.getLayout().getBuildDirectory().dir("topplecat/reports/review"));
            task.getDefinitionFile().set(project.getLayout().getBuildDirectory().file("topplecat/contract-definition.json"));
            task.getSpecDocs().from(extension.getSpecDocs());
        });
        TaskProvider<ToppleCatHideTask> hide = project.getTasks().register("toppleCatHide", ToppleCatHideTask.class,
                task -> {
                    task.setGroup("verification");
                    task.setDescription("Moves the complete reviewer-only src/hiddenTest source set into local hidden storage.");
                    task.dependsOn(check);
                    task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                    task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                });
        Provider<ToppleCatCustodyBuildService> custodyService = project.getGradle().getSharedServices()
                .registerIfAbsent("toppleCatCustody-" + project.getPath().replace(':', '_'),
                        ToppleCatCustodyBuildService.class,
                        spec -> spec.getParameters().getProjectRoot().set(project.getLayout().getProjectDirectory()));
        TaskProvider<ToppleCatAcquireCustodyTask> acquireCustody = project.getTasks().register(
                "toppleCatAcquireCustody", ToppleCatAcquireCustodyTask.class, task -> {
                    task.getCustodyService().set(custodyService);
                    task.usesService(custodyService);
                });
        hide.configure(task -> {
            task.mustRunAfter(acquireCustody);
            task.usesService(custodyService);
        });
        TaskProvider<ToppleCatRestoreTask> restore = project.getTasks().register("toppleCatRestore",
                ToppleCatRestoreTask.class, task -> {
                    task.setGroup("verification");
                    task.setDescription("Restores the complete reviewer-only source set from validated local hidden storage.");
                    task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                });
        restore.configure(task -> {
            task.mustRunAfter(acquireCustody, hide);
            task.usesService(custodyService);
        });
        TaskProvider<ToppleCatUpdateEscrowTask> updateEscrow = project.getTasks().register(
                "toppleCatUpdateEscrow", ToppleCatUpdateEscrowTask.class, task -> {
                    task.setGroup("verification");
                    task.setDescription("Validates, reviews, and explicitly updates reviewer-only local escrow custody.");
                    task.dependsOn(review);
                    task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                    task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                });
        updateEscrow.configure(task -> {
            task.mustRunAfter(acquireCustody, restore);
            task.usesService(custodyService);
        });
        TaskProvider<ToppleCatReviewerDefinitionTask> reviewerDefinition = project.getTasks().register(
                "toppleCatReviewerDefinition", ToppleCatReviewerDefinitionTask.class, task -> {
                    task.setDescription("Builds the run-scoped reviewer definition from restored source before verification.");
                    task.getPublicCaseRoot().set(extension.getPublicCaseRoot());
                    task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                    task.getDescriptorClassDirectories().from(compileContracts.flatMap(
                            ToppleCatCompileContractsTask::getDescriptorClassesDirectory));
                    task.getReviewerDefinitionFile().set(runDirectory.file("reviewer-definition.json"));
                    task.dependsOn(prepareRun, restore, check);
                    task.mustRunAfter(acquireCustody, hide);
                    task.usesService(custodyService);
                });
        project.getTasks().named(hidden.getCompileJavaTaskName()).configure(task -> task.dependsOn(restore));
        project.getTasks().named(hidden.getProcessResourcesTaskName()).configure(task -> task.dependsOn(restore));
        TaskProvider<ToppleCatRehideTask> rehide = project.getTasks().register("toppleCatRehide", ToppleCatRehideTask.class,
                task -> {
                    task.setGroup("verification");
                    task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                });

        TaskProvider<Test> verificationTest = project.getTasks().register("toppleCatVerificationTest", Test.class, task -> {
            task.setGroup("verification");
            task.setDescription("Runs public JUnit verification and, when enabled, restored reviewer case rows.");
            task.dependsOn(prepareRun, check, test.getClassesTaskName());
            task.setTestClassesDirs(test.getOutput().getClassesDirs());
            task.setClasspath(test.getRuntimeClasspath());
            task.useJUnitPlatform();
            configureCaseProperties(task, extension, false, true);
            configureVerificationArtifacts(task, runDirectory, VerificationRunArtifacts.JUNIT);
            configureNarrativeEvents(task, runDirectory, true);
            requireFreshVerificationExecution(task);
            task.systemProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                    project.getLayout().getBuildDirectory().file("topplecat/contract-definition.json").get().getAsFile()
                            .getAbsolutePath());
        });
        verificationTest.configure(task -> {
            task.mustRunAfter(acquireCustody);
            task.usesService(custodyService);
        });
        TaskProvider<Test> hiddenTest = project.getTasks().register("hiddenTest", Test.class, task -> {
            task.setGroup("verification");
            task.setDescription("Runs restored reviewer-only JUnit tests.");
            task.dependsOn(prepareRun, restore, hidden.getClassesTaskName());
            task.setTestClassesDirs(hidden.getOutput().getClassesDirs());
            task.setClasspath(hidden.getRuntimeClasspath());
            task.useJUnitPlatform();
            configureCaseProperties(task, extension, true, true);
            configureVerificationArtifacts(task, runDirectory, VerificationRunArtifacts.REVIEWER_JUNIT);
            configureNarrativeEvents(task, runDirectory, false);
            requireFreshVerificationExecution(task);
            task.systemProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                    project.getLayout().getBuildDirectory().file("topplecat/contract-definition.json").get().getAsFile()
                            .getAbsolutePath());
            task.usesService(custodyService);
        });
        TaskProvider<ToppleCatMutationGateTask> mutationGate = project.getTasks().register("toppleCatMutationGate",
                ToppleCatMutationGateTask.class, task -> {
                    task.setGroup("verification");
                    task.setDescription("Evaluates public executable contract mutation strength for each @ToppleTest acceptance condition.");
                    task.getPublicTestSourceRoot().set(project.getLayout().getProjectDirectory().dir("src/test/java"));
                    task.getDefinitionFile().set(project.getLayout().getBuildDirectory().file("topplecat/contract-definition.json"));
                    task.getPitReportFile().set(extension.getAdversarial().getMutation().getReportFile());
                    task.getResultsFile().set(runDirectory.file("mutation-results.json"));
                    task.getRunDirectory().set(runDirectory);
                    task.getThreshold().set(extension.getAdversarial().getMutation().getThreshold());
                    task.getProducerTaskName().set(extension.getAdversarial().getMutation().getProducerTask());
                    task.getProducerAvailable().convention(false);
                    task.dependsOn(prepareRun);
                    task.mustRunAfter(verificationTest, hiddenTest);
                    task.usesService(custodyService);
                });
        TaskProvider<ToppleCatReportTask> report = project.getTasks().register("toppleCatReport", ToppleCatReportTask.class,
                task -> {
                    task.setGroup("verification");
                    task.setDescription("Writes safe Spec and reviewer-only Verification reports plus evidence.");
                    task.getProjectRoot().set(project.getLayout().getProjectDirectory());
                    task.getPublicCaseRoot().set(extension.getPublicCaseRoot());
                    task.getHiddenSourceRoot().set(extension.getHiddenSourceRoot());
                    task.getDefinitionFile().set(project.getLayout().getBuildDirectory().file("topplecat/contract-definition.json"));
                    task.getSpecDocs().from(extension.getSpecDocs());
                    task.getRunDirectory().set(runDirectory);
                    task.getMutationResultsFile().set(mutationGate.flatMap(ToppleCatMutationGateTask::getResultsFile));
                    task.getMutationIncompleteReason().convention("");
                    task.mustRunAfter(verificationTest, hiddenTest, mutationGate);
                    task.usesService(custodyService);
                });
        verificationTest.configure(task -> task.finalizedBy(report));
        hiddenTest.configure(task -> task.finalizedBy(report));
        mutationGate.configure(task -> task.finalizedBy(report));
        hiddenTest.configure(task -> task.mustRunAfter(verificationTest));
        rehide.configure(task -> {
            task.mustRunAfter(acquireCustody, report);
            task.usesService(custodyService);
        });

        TaskProvider<Task> verify = project.getTasks().register("toppleCatVerify", task -> {
            task.setGroup("verification");
            task.setDescription("Runs configured adversarial verification, then re-hides reviewer source.");
            task.dependsOn(acquireCustody, hide, verificationTest);
            task.finalizedBy(report);
        });
        project.afterEvaluate(ignored -> configureAdversarialVerification(project, extension, restore, reviewerDefinition,
                verificationTest, hiddenTest, mutationGate, report, rehide, verify));
    }

    private static void configureCaseProperties(
            Test task,
            ToppleCatExtension extension,
            boolean includeHidden,
            boolean expectedConsumptionEnforced
    ) {
        task.systemProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY,
                extension.getPublicCaseRoot().get().getAsFile().getAbsolutePath());
        task.systemProperty(ToppleJunit.HIDDEN_CASE_SOURCES_PROPERTY,
                extension.getHiddenSourceRoot().get().getAsFile().toPath().resolve("resources/topplecat/cases").toString());
        task.systemProperty(ToppleJunit.INCLUDE_HIDDEN_CASES_PROPERTY, Boolean.toString(includeHidden));
        task.systemProperty(ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY,
                Boolean.toString(expectedConsumptionEnforced));
    }

    private static void configureVerificationArtifacts(Test task, Directory runDirectory, String gate) {
        task.getReports().getJunitXml().getOutputLocation().set(runDirectory.dir("junit/" + gate));
        task.addTestListener(new TestListener() {
            @Override
            public void beforeSuite(TestDescriptor suite) {
            }

            @Override
            public void afterSuite(TestDescriptor suite, TestResult result) {
                if (suite.getParent() == null) {
                    VerificationRunArtifacts.markCompleted(runDirectory.getAsFile().toPath(), gate);
                }
            }

            @Override
            public void beforeTest(TestDescriptor testDescriptor) {
            }

            @Override
            public void afterTest(TestDescriptor testDescriptor, TestResult result) {
            }
        });
    }

    private static void requireFreshVerificationExecution(Test task) {
        task.getOutputs().upToDateWhen(ignored -> false);
        task.getOutputs().doNotCacheIf("ToppleCat verification evidence is run-scoped.", ignored -> true);
    }

    private static void configureNarrativeEvents(Test task, Directory runDirectory, boolean clearBeforeTask) {
        Path narrative = runDirectory.file("narrative-executions.jsonl").getAsFile().toPath();
        Path consumption = runDirectory.file("expected-consumption-executions.jsonl").getAsFile().toPath();
        task.systemProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, narrative.toAbsolutePath().toString());
        task.systemProperty(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY, consumption.toAbsolutePath().toString());
        task.systemProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY,
                runDirectory.dir("attachments").getAsFile().toPath().toAbsolutePath().toString());
        if (clearBeforeTask) {
            task.doFirst(ignored -> {
                try {
                    Files.deleteIfExists(narrative);
                    Files.deleteIfExists(consumption);
                } catch (IOException exception) {
                    throw new GradleException("Cannot clear ToppleCat verification sidecars in " + runDirectory, exception);
                }
            });
        }
    }

    private static void configureAdversarialVerification(
            Project project,
            ToppleCatExtension extension,
            TaskProvider<ToppleCatRestoreTask> restore,
            TaskProvider<ToppleCatReviewerDefinitionTask> reviewerDefinition,
            TaskProvider<Test> verificationTest,
            TaskProvider<Test> hiddenTest,
            TaskProvider<ToppleCatMutationGateTask> mutationGate,
            TaskProvider<ToppleCatReportTask> report,
            TaskProvider<ToppleCatRehideTask> rehide,
            TaskProvider<Task> verify
    ) {
        AdversarialConfiguration configuration = AdversarialConfiguration.resolve(extension);
        project.getTasks().withType(Test.class).configureEach(task -> {
            if (task.getName().equals("test")) {
                task.systemProperty(ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY,
                        Boolean.toString(configuration.expectedConsumptionEnabled()));
            }
        });
        verificationTest.configure(task -> {
            configureCaseProperties(task, extension, configuration.hiddenRetestEnabled(),
                    configuration.expectedConsumptionEnabled());
            if (configuration.hiddenRetestEnabled()) {
                task.dependsOn(reviewerDefinition);
                task.systemProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                        reviewerDefinition.get().getReviewerDefinitionFile().get().getAsFile().getAbsolutePath());
            }
        });
        hiddenTest.configure(task -> {
            task.systemProperty(ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY,
                    Boolean.toString(configuration.expectedConsumptionEnabled()));
            if (configuration.hiddenRetestEnabled()) {
                task.dependsOn(reviewerDefinition);
                task.systemProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY,
                        reviewerDefinition.get().getReviewerDefinitionFile().get().getAsFile().getAbsolutePath());
            }
        });
        report.configure(task -> {
            task.getHiddenRetestEnabled().set(configuration.hiddenRetestEnabled());
            task.getHiddenRetestDisabledReason().set(configuration.hiddenRetestDisabledReason());
            task.getMutationEnabled().set(configuration.mutationEnabled());
            task.getMutationDisabledReason().set(configuration.mutationDisabledReason());
            task.getExpectedConsumptionEnabled().set(configuration.expectedConsumptionEnabled());
            task.getExpectedConsumptionDisabledReason().set(configuration.expectedConsumptionDisabledReason());
            if (configuration.hiddenRetestEnabled()) {
                task.getReviewerDefinitionFile().set(reviewerDefinition.flatMap(
                        ToppleCatReviewerDefinitionTask::getReviewerDefinitionFile));
                task.mustRunAfter(reviewerDefinition);
            }
        });
        if (configuration.hiddenRetestEnabled()) {
            verify.configure(task -> task.dependsOn(hiddenTest));
            report.configure(task -> task.finalizedBy(rehide));
        }
        configureMutationGate(project, extension, configuration, mutationGate, report, verify);
    }

    private static void configureMutationGate(
            Project project,
            ToppleCatExtension extension,
            AdversarialConfiguration configuration,
            TaskProvider<ToppleCatMutationGateTask> mutationGate,
            TaskProvider<ToppleCatReportTask> report,
            TaskProvider<Task> verify
    ) {
        if (!configuration.mutationEnabled()) {
            return;
        }
        ProductionPackages productionPackages = productionPackages(project);
        if (productionPackages.targets().isEmpty()) {
            String reason = productionPackages.sourcesFound()
                    ? "no production packages found under src/main/java; the mutation gate cannot run."
                    : "no production sources found under src/main/java; the mutation gate cannot run.";
            report.configure(task -> task.getMutationIncompleteReason().set(reason));
            return;
        }
        String producerName = extension.getAdversarial().getMutation().getProducerTask().get().trim();
        if (producerName.equals("pitest") && project.getTasks().findByName(producerName) == null) {
            project.getPluginManager().apply(PITEST_PLUGIN_ID);
            configureDefaultPit(project, productionPackages.targets());
        }
        Task producer = project.getTasks().findByName(producerName);
        mutationGate.configure(task -> {
            task.getProducerAvailable().set(producer != null);
            if (producer != null) {
                task.dependsOn(producer);
            }
        });
        if (producer != null) {
            configureFullMutationMatrix(project, producer);
        }
        verify.configure(task -> task.dependsOn(mutationGate));
    }

    private static void configureDefaultPit(Project project, Set<String> targetClasses) {
        Object pitest = project.getExtensions().findByName("pitest");
        if (pitest == null) {
            throw new GradleException("ToppleCat could not configure its default PIT producer. "
                    + "Apply info.solidsoft.pitest explicitly or set toppleCat.adversarial.mutation.producerTask.");
        }
        setPitProperty(pitest, "getPitestVersion", "1.25.5");
        setPitProperty(pitest, "getJunit5PluginVersion", "1.2.3");
        setPitProperty(pitest, "getTargetClasses", targetClasses);
        SourceSet publicTests = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
                .getByName(SourceSet.TEST_SOURCE_SET_NAME);
        setPitProperty(pitest, "getTestSourceSets", Set.of(publicTests));
        setPitProperty(pitest, "getFullMutationMatrix", true);
        setPitProperty(pitest, "getOutputFormats", Set.of("XML"));
        setPitProperty(pitest, "getTimestampedReports", false);
        setPitProperty(pitest, "getMutationThreshold", 0);
        setPitProperty(pitest, "getFailWhenNoMutations", false);
        project.getLogger().lifecycle("ToppleCat configured the default PIT producer with a full mutation matrix against sourceSets.test.");
    }

    private static void configureFullMutationMatrix(Project project, Task producer) {
        Object pitest = project.getExtensions().findByName("pitest");
        if (pitest == null) {
            return;
        }
        try {
            setPitProperty(pitest, "getFullMutationMatrix", true);
            project.getLogger().lifecycle("ToppleCat enables PIT fullMutationMatrix=true for {}.", producer.getPath());
        } catch (GradleException exception) {
            project.getLogger().warn("ToppleCat could not enable PIT fullMutationMatrix=true for {}. "
                    + "Configure it in the PIT extension before verification.", producer.getPath());
        }
    }

    private static void setPitProperty(Object pitest, String getter, Object value) {
        try {
            Object property = pitest.getClass().getMethod(getter).invoke(pitest);
            java.lang.reflect.Method setter = java.util.Arrays.stream(property.getClass().getMethods())
                    .filter(method -> method.getName().equals("set") && method.getParameterCount() == 1
                            && method.getParameterTypes()[0].isAssignableFrom(value.getClass()))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("set(value) on " + property.getClass().getName()));
            setter.invoke(property, value);
        } catch (ReflectiveOperationException exception) {
            throw new GradleException("ToppleCat could not configure PIT via " + getter + ".", exception);
        }
    }

    private static ProductionPackages productionPackages(Project project) {
        Path sourceRoot = project.getLayout().getProjectDirectory().dir("src/main/java").getAsFile().toPath();
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
            throw new GradleException("ToppleCat could not inspect src/main/java to configure default PIT targets.", exception);
        }
        return new ProductionPackages(sourcesFound, Set.copyOf(patterns));
    }

    private record ProductionPackages(boolean sourcesFound, Set<String> targets) {
    }

    private record AdversarialConfiguration(
            boolean hiddenRetestEnabled,
            String hiddenRetestDisabledReason,
            boolean mutationEnabled,
            String mutationDisabledReason,
            boolean expectedConsumptionEnabled,
            String expectedConsumptionDisabledReason
    ) {
        private static AdversarialConfiguration resolve(ToppleCatExtension extension) {
            boolean global = extension.getAdversarial().getEnabled().getOrElse(true);
            boolean hiddenRetest = global && extension.getAdversarial().getHiddenRetest().getEnabled().getOrElse(true);
            boolean mutation = global && extension.getAdversarial().getMutation().getEnabled().getOrElse(true);
            boolean expectedConsumption = global && extension.getAdversarial().getExpectedConsumption().getEnabled().getOrElse(true);
            String globalReason = "disabled by toppleCat.adversarial.enabled=false";
            return new AdversarialConfiguration(
                    hiddenRetest, hiddenRetest ? "" : global ?
                            "disabled by toppleCat.adversarial.hiddenRetest.enabled=false" : globalReason,
                    mutation, mutation ? "" : global ?
                            "disabled by toppleCat.adversarial.mutation.enabled=false" : globalReason,
                    expectedConsumption, expectedConsumption ? "" : global ?
                            "disabled by toppleCat.adversarial.expectedConsumption.enabled=false" : globalReason
            );
        }
    }
}
