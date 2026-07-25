package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.AgentFeedback;
import io.github.samzhu.topplecat.core.AgentFeedbackJson;
import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.CaseDefinition;
import io.github.samzhu.topplecat.core.CaseRun;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ContractIntegrityResult;
import io.github.samzhu.topplecat.core.ContractIntegrityResultJson;
import io.github.samzhu.topplecat.core.EvidenceGate;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ExpectedConsumptionExecution;
import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.NarrativeExecution;
import io.github.samzhu.topplecat.core.StepRun;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleEvidence;
import io.github.samzhu.topplecat.core.ToppleEvidenceJson;
import io.github.samzhu.topplecat.core.VerificationRun;
import io.github.samzhu.topplecat.core.VerificationRunJson;
import io.github.samzhu.topplecat.report.CaseResultStatus;
import io.github.samzhu.topplecat.report.ReportJson;
import io.github.samzhu.topplecat.report.ReportViews;
import io.github.samzhu.topplecat.report.SpecView;
import io.github.samzhu.topplecat.report.HtmlBundleWriter;
import io.github.samzhu.topplecat.report.VerificationView;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tools.jackson.databind.json.JsonMapper;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Writes safe and reviewer-only reports after verification test tasks, then feeds evidence. */
public abstract class ToppleCatReportTask extends DefaultTask {
    private static final Pattern CASE_DISPLAY_NAME = Pattern.compile("\\[case:([^]]+)]");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String CONTRACT_CHANGED =
            "The public executable contract or verification policy changed after reviewer approval.";
    private static final String APPROVAL_MISSING =
            "Reviewer approval evidence is missing; an authorized reviewer must review and reseal the contract.";
    private static final String INTEGRITY_PRECONDITION =
            "The contract-integrity gate did not permit downstream verification in this run.";

    @Internal
    public abstract DirectoryProperty getProjectRoot();

    @Internal
    public abstract DirectoryProperty getPublicCaseRoot();

    @Internal
    public abstract DirectoryProperty getHiddenSourceRoot();

    @org.gradle.api.tasks.InputFile
    public abstract RegularFileProperty getDefinitionFile();

    /** Reviewer-only definition built after hidden source has been restored for this exact run. */
    @Internal
    public abstract RegularFileProperty getReviewerDefinitionFile();

    @Internal
    public abstract DirectoryProperty getRunDirectory();

    @Internal
    public abstract RegularFileProperty getContractIntegrityResultFile();

    @Internal
    public abstract RegularFileProperty getMutationResultsFile();

    @Input
    public abstract Property<Boolean> getHiddenRetestEnabled();

    @Input
    public abstract Property<String> getHiddenRetestDisabledReason();

    @Input
    public abstract Property<Boolean> getMutationEnabled();

    @Input
    public abstract Property<String> getMutationDisabledReason();

    @Input
    public abstract Property<String> getMutationIncompleteReason();

    @Input
    public abstract Property<Boolean> getExpectedConsumptionEnabled();

    @Input
    public abstract Property<String> getExpectedConsumptionDisabledReason();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecDocs();

    @TaskAction
    public void report() {
        Path root = getProjectRoot().get().getAsFile().toPath();
        Path runDirectory = getRunDirectory().get().getAsFile().toPath();
        VerificationRunWorkspace.prepare(runDirectory);
        ExternalSpecDocumentReader.ParsedSpecs specDocs = ExternalSpecDocumentReader.read(root,
                getSpecDocs().getFiles().stream().map(file -> file.toPath()).toList());
        ContractDefinition publicDefinition = definition(getDefinitionFile(), "public");
        GateOutcome integrity = contractIntegrityVerdict();
        boolean integrityPassed = integrity.verdict() == EvidenceVerdict.PASS;
        ContractDefinition reviewerDefinition = integrityPassed && getHiddenRetestEnabled().get()
                ? reviewerDefinition() : publicDefinition;
        List<ToppleCaseData> publicCases = publicDefinition.acceptanceConditions().stream()
                .flatMap(contract -> contract.cases().stream()).map(ToppleCatReportTask::caseData)
                .filter(testCase -> testCase.visibility() == CaseVisibility.PUBLIC).toList();
        List<ToppleCaseData> verificationCases = reviewerDefinition.acceptanceConditions().stream()
                .flatMap(contract -> contract.cases().stream()).map(ToppleCatReportTask::caseData)
                .filter(testCase -> getHiddenRetestEnabled().get() || testCase.visibility() == CaseVisibility.PUBLIC).toList();
        Map<String, String> publicTitles = publicDefinition.acceptanceConditions().stream().collect(java.util.stream.Collectors.toMap(
                AcceptanceContract::acId, AcceptanceContract::title, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<String>> publicScenarios = publicDefinition.acceptanceConditions().stream().collect(java.util.stream.Collectors.toMap(
                AcceptanceContract::acId, contract -> contract.scenario().steps().stream()
                        .map(io.github.samzhu.topplecat.core.ScenarioTemplateRenderer::template).toList(),
                (left, right) -> left, LinkedHashMap::new));
        Map<String, String> reviewerTitles = reviewerDefinition.acceptanceConditions().stream().collect(java.util.stream.Collectors.toMap(
                AcceptanceContract::acId, AcceptanceContract::title, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<io.github.samzhu.topplecat.core.StepTemplate>> reviewerScenarioTemplates = reviewerDefinition.acceptanceConditions()
                .stream().collect(java.util.stream.Collectors.toMap(AcceptanceContract::acId,
                        contract -> contract.scenario().steps(), (left, right) -> left, LinkedHashMap::new));
        Set<String> definedCaseIds = verificationCases.stream().map(ToppleCaseData::caseId).collect(java.util.stream.Collectors.toSet());
        ExecutionSummary executionSummary = executions(runDirectory, reviewerDefinition.digest(), definedCaseIds);
        Map<String, ReportViews.CaseExecution> executions = executionSummary.executions();
        Instant generatedAt = Instant.now();
        GateOutcome junit = integrityPassed ? testVerdict(runDirectory, "the public verification", VerificationRunArtifacts.JUNIT,
                executionSummary.junit()) : GateOutcome.incomplete(INTEGRITY_PRECONDITION);
        GateOutcome reviewer = !integrityPassed ? GateOutcome.incomplete(INTEGRITY_PRECONDITION)
                : getHiddenRetestEnabled().get()
                ? testVerdict(runDirectory, "the reviewer retest", VerificationRunArtifacts.REVIEWER_JUNIT,
                        executionSummary.reviewerJUnit())
                : GateOutcome.disabled(getHiddenRetestDisabledReason().get());
        GateOutcome expectedConsumption = !integrityPassed ? GateOutcome.incomplete(INTEGRITY_PRECONDITION)
                : getExpectedConsumptionEnabled().get()
                ? expectedConsumptionVerdict(junit, reviewer, verificationCases, executions)
                : GateOutcome.disabled(getExpectedConsumptionDisabledReason().get());
        GateOutcome mutation = !integrityPassed ? GateOutcome.incomplete(INTEGRITY_PRECONDITION)
                : getMutationEnabled().get()
                ? mutationVerdict(runDirectory)
                : GateOutcome.disabled(getMutationDisabledReason().get());
        List<EvidenceGate> reviewerGates = List.of(
                integrity.asEvidenceGate(VerificationRunArtifacts.CONTRACT_INTEGRITY),
                junit.asEvidenceGate(VerificationRunArtifacts.JUNIT),
                reviewer.asEvidenceGate(VerificationRunArtifacts.REVIEWER_JUNIT),
                expectedConsumption.asEvidenceGate(VerificationRunArtifacts.EXPECTED_CONSUMPTION),
                mutation.asEvidenceGate(VerificationRunArtifacts.MUTATION)
        );
        SpecView spec = ReportViews.spec(publicTitles, publicCases, specDocs.narratives(), publicScenarios, generatedAt);
        VerificationView verification = ReportViews.verificationFromTemplates(reviewerTitles, verificationCases, executions,
                specDocs.narratives(), reviewerScenarioTemplates,
                getExpectedConsumptionEnabled().get(), reviewerGates, generatedAt);
        Path reports = runDirectory.resolve("reports");
        if (integrityPassed) {
            HtmlBundleWriter.spec(reports.resolve("spec"), spec);
        }
        HtmlBundleWriter.verification(reports.resolve("verification"), verification);
        copyAttachments(verification, runDirectory, reports.resolve("verification"));

        EvidenceVerdict verdict = reviewerGates.stream().anyMatch(gate -> gate.verdict() == EvidenceVerdict.FAIL)
                || verification.verdict() == CaseResultStatus.FAIL ? EvidenceVerdict.FAIL
                : reviewerGates.stream().allMatch(gate -> gate.verdict() == EvidenceVerdict.PASS
                || gate.verdict() == EvidenceVerdict.DISABLED)
                && verification.verdict() == CaseResultStatus.PASS ? EvidenceVerdict.PASS : EvidenceVerdict.INCOMPLETE;
        String runId = java.util.UUID.randomUUID().toString();
        VerificationRun executionRun = new VerificationRun(VerificationRun.SCHEMA_VERSION, reviewerDefinition.digest(), runId,
                runStartedAt(runDirectory), generatedAt, verdict,
                verificationCases.stream().map(testCase -> caseRun(testCase, executions)).toList());
        write(runDirectory.resolve("verification-run.json"), VerificationRunJson.write(executionRun));
        Map<String, String> digests = artifactDigests(root, reports, getMutationResultsFile().get().getAsFile().toPath(),
                getContractIntegrityResultFile().get().getAsFile().toPath());
        ToppleEvidence evidence = ToppleEvidenceJson.create(runId, generatedAt.toString(), verdict,
                reviewerGates, digests);
        Path evidencePath = runDirectory.resolve("evidence.json");
        write(evidencePath, ToppleEvidenceJson.write(evidence));
        Path feedbackPath = runDirectory.resolve("agent-feedback.json");
        write(feedbackPath, AgentFeedbackJson.write(new AgentFeedback(
                AgentFeedback.SCHEMA_VERSION, verdict, reviewerGates)));
        publishStableArtifacts(root, reports, evidencePath, feedbackPath, getMutationResultsFile().get().getAsFile().toPath(),
                integrityPassed);
        VerificationRunWorkspace.archive(runDirectory, runId);
        getLogger().lifecycle("ToppleCat verification report written: {}", verdict);
        if (verdict != EvidenceVerdict.PASS) {
            throw new GradleException("ToppleCat verification verdict is " + verdict
                    + "; see " + evidencePath + " for gate-level detail.");
        }
    }

    private ContractDefinition reviewerDefinition() {
        if (!getReviewerDefinitionFile().isPresent()) {
            throw new IllegalStateException("ToppleCat hidden verification requires a run-scoped reviewer definition.");
        }
        return definition(getReviewerDefinitionFile(), "reviewer");
    }

    private GateOutcome contractIntegrityVerdict() {
        Path resultFile = getContractIntegrityResultFile().get().getAsFile().toPath();
        if (!Files.isRegularFile(resultFile)) {
            return GateOutcome.incomplete(APPROVAL_MISSING);
        }
        try {
            ContractIntegrityResult result = ContractIntegrityResultJson.read(Files.readString(resultFile));
            return switch (result.verdict()) {
                case PASS -> GateOutcome.pass();
                case FAIL -> GateOutcome.fail(CONTRACT_CHANGED);
                case INCOMPLETE -> GateOutcome.incomplete(APPROVAL_MISSING);
                case DISABLED -> GateOutcome.incomplete(APPROVAL_MISSING);
            };
        } catch (IOException | RuntimeException exception) {
            return GateOutcome.incomplete(APPROVAL_MISSING);
        }
    }

    private static ContractDefinition definition(RegularFileProperty definitionFile, String projection) {
        Path file = definitionFile.get().getAsFile().toPath();
        try {
            return ContractDefinitionJson.read(Files.readString(file));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + projection + " ToppleCat contract definition " + file + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static ToppleCaseData caseData(CaseDefinition testCase) {
        return new ToppleCaseData(testCase.caseId(), testCase.acId(), testCase.visibility(), testCase.inputs(),
                testCase.expected(), Path.of("contract-definition.json"));
    }

    private static CaseRun caseRun(ToppleCaseData testCase, Map<String, ReportViews.CaseExecution> executions) {
        ReportViews.CaseExecution execution = executions.getOrDefault(testCase.caseId(),
                ReportViews.CaseExecution.notReported());
        List<StepRun> steps = execution.steps().stream().map(step -> new StepRun(step.stepId(), step.status(),
                Duration.ofNanos(step.durationNanos()), step.actualArguments().stream().map(argument -> (Object) argument).toList(),
                step.attachments(), step.failureRef())).toList();
        long durationNanos = steps.stream().mapToLong(step -> step.duration().toNanos()).sum();
        return new CaseRun(testCase.caseId(), switch (execution.status()) {
            case PASS -> io.github.samzhu.topplecat.core.NarrativeStepStatus.PASS;
            case FAIL -> io.github.samzhu.topplecat.core.NarrativeStepStatus.FAIL;
            case NOT_REPORTED -> io.github.samzhu.topplecat.core.NarrativeStepStatus.SKIPPED;
        }, Duration.ofNanos(durationNanos), steps, execution.expectedConsumption(),
                execution.failure() == null || execution.failure().isBlank() ? "" : "case-failure:" + testCase.caseId());
    }

    private static Instant runStartedAt(Path runDirectory) {
        try {
            return Files.getLastModifiedTime(runDirectory.resolve(".active")).toInstant();
        } catch (IOException ignored) {
            return Instant.now();
        }
    }

    private ExecutionSummary executions(Path runDirectory, String definitionDigest, Set<String> definedCaseIds) {
        Map<String, ReportViews.CaseExecution> result = new LinkedHashMap<>();
        TestResults junit = readTestResults(runDirectory.resolve("junit/").resolve(VerificationRunArtifacts.JUNIT), result);
        TestResults reviewer = readTestResults(runDirectory.resolve("junit/").resolve(VerificationRunArtifacts.REVIEWER_JUNIT), result);
        readNarrativeExecutions(runDirectory.resolve("narrative-executions.jsonl"), result, definitionDigest);
        readExpectedConsumptionExecutions(runDirectory.resolve("expected-consumption-executions.jsonl"), result);
        result.keySet().stream().filter(caseId -> !definedCaseIds.contains(caseId)).findFirst().ifPresent(caseId -> {
            throw new IllegalStateException("ToppleCat report cannot project execution event for case " + caseId
                    + " because no matching case exists in the run-scoped contract definition " + definitionDigest + ".");
        });
        return new ExecutionSummary(result, junit, reviewer);
    }

    private static void readNarrativeExecutions(Path file, Map<String, ReportViews.CaseExecution> result,
                                                String definitionDigest) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) {
                    continue;
                }
                NarrativeExecution narrative = JSON.readValue(line, NarrativeExecution.class);
                if (!definitionDigest.equals(narrative.definitionDigest())) {
                    throw new IllegalStateException("ToppleCat runtime narrative for " + narrative.caseId()
                            + " was produced from a different contract definition digest.");
                }
                ReportViews.CaseExecution execution = result.getOrDefault(narrative.caseId(),
                        ReportViews.CaseExecution.notReported());
                result.put(narrative.caseId(), new ReportViews.CaseExecution(execution.status(), execution.failure(),
                        narrative.steps(), execution.expectedConsumption()));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read ToppleCat narrative sidecar " + file + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static void readExpectedConsumptionExecutions(Path file, Map<String, ReportViews.CaseExecution> result) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) {
                    continue;
                }
                ExpectedConsumptionExecution consumption = JSON.readValue(line, ExpectedConsumptionExecution.class);
                ReportViews.CaseExecution execution = result.getOrDefault(consumption.caseId(),
                        ReportViews.CaseExecution.notReported());
                result.put(consumption.caseId(), new ReportViews.CaseExecution(execution.status(), execution.failure(),
                        execution.steps(), consumption.expectedConsumption()));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read ToppleCat expected-consumption sidecar " + file + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static TestResults readTestResults(Path directory, Map<String, ReportViews.CaseExecution> result) {
        if (!Files.exists(directory)) {
            return TestResults.NONE;
        }
        boolean foundXml = false;
        boolean failures = false;
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
                foundXml = true;
                NodeList testCases = parseCompletedTestResults(file);
                for (int index = 0; index < testCases.getLength(); index++) {
                    Element testCase = (Element) testCases.item(index);
                    Element failure = failure(testCase);
                    failures |= failure != null;
                    Matcher matcher = CASE_DISPLAY_NAME.matcher(testCase.getAttribute("name"));
                    if (!matcher.find()) {
                        continue;
                    }
                    result.put(matcher.group(1), failure == null
                            ? new ReportViews.CaseExecution(CaseResultStatus.PASS, null, List.of())
                            : new ReportViews.CaseExecution(CaseResultStatus.FAIL, failure.getAttribute("message"), List.of()));
                }
            }
        } catch (IOException | SAXException exception) {
            throw new IllegalStateException("Cannot read JUnit XML results from " + directory + ": " + exception.getMessage(), exception);
        }
        return new TestResults(foundXml, failures);
    }

    /** Gradle can expose a test XML path to a finalizer just before its writer flushes the document. */
    private static NodeList parseCompletedTestResults(Path file) throws SAXException {
        SAXException lastFailure = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return builder().parse(file.toFile()).getElementsByTagName("testcase");
            } catch (SAXException exception) {
                lastFailure = exception;
                if (attempt == 4) {
                    break;
                }
                try {
                    Thread.sleep(25L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for JUnit XML results: " + file, interrupted);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read JUnit XML results from " + file + ": " + exception.getMessage(), exception);
            }
        }
        throw lastFailure;
    }

    private static javax.xml.parsers.DocumentBuilder builder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            throw new IllegalStateException("Cannot configure secure XML parser", exception);
        }
    }

    private static Element failure(Element testCase) {
        NodeList children = testCase.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element element && (element.getTagName().equals("failure") || element.getTagName().equals("error"))) {
                return element;
            }
        }
        return null;
    }

    private static GateOutcome testVerdict(Path runDirectory, String taskName, String gate, TestResults results) {
        if (!VerificationRunArtifacts.completed(runDirectory, gate)) {
            return GateOutcome.incomplete(taskName + " did not complete in this verification run.");
        }
        if (!results.foundXml()) {
            return GateOutcome.incomplete(taskName + " completed without JUnit XML results.");
        }
        return results.failures() ? GateOutcome.fail() : GateOutcome.pass();
    }

    private GateOutcome mutationVerdict(Path runDirectory) {
        String incompleteReason = getMutationIncompleteReason().getOrElse("").trim();
        if (!incompleteReason.isEmpty()) {
            return GateOutcome.incomplete(incompleteReason);
        }
        if (!VerificationRunArtifacts.completed(runDirectory, VerificationRunArtifacts.MUTATION)) {
            return GateOutcome.incomplete("the mutation gate did not complete in this verification run.");
        }
        Path results = getMutationResultsFile().get().getAsFile().toPath();
        if (!Files.isRegularFile(results)) {
            return GateOutcome.incomplete("the mutation gate completed without mutation results.");
        }
        try {
            return MutationGateResults.read(Files.readString(results)).verdict() == EvidenceVerdict.PASS
                    ? GateOutcome.pass() : GateOutcome.fail();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read ToppleCat mutation results " + results, exception);
        }
    }

    private static GateOutcome expectedConsumptionVerdict(
            GateOutcome junit,
            GateOutcome reviewer,
            List<ToppleCaseData> cases,
            Map<String, ReportViews.CaseExecution> executions
    ) {
        if (junit.verdict() == EvidenceVerdict.INCOMPLETE) {
            return GateOutcome.incomplete("the public verification did not complete expected-consumption tracking.");
        }
        if (reviewer.verdict() == EvidenceVerdict.INCOMPLETE) {
            return GateOutcome.incomplete("the reviewer retest did not complete expected-consumption tracking.");
        }
        boolean unasserted = false;
        boolean unknown = false;
        for (ToppleCaseData testCase : cases) {
            ReportViews.CaseExecution execution = executions.getOrDefault(testCase.caseId(),
                    ReportViews.CaseExecution.notReported());
            for (String key : testCase.expected().propertyNames()) {
                String state = execution.expectedConsumption().getOrDefault(key, "UNKNOWN");
                if (state.equals("UNKNOWN")) {
                    unknown = true;
                } else if (!state.equals("ASSERTED")) {
                    unasserted = true;
                }
            }
        }
        if (unasserted) {
            return GateOutcome.fail();
        }
        return unknown ? GateOutcome.incomplete("Expected-consumption sidecar data was missing for one or more declared values.")
                : GateOutcome.pass();
    }

    private static Map<String, String> artifactDigests(Path root, Path reports, Path mutationResults, Path integrityResult) {
        Map<String, String> digests = new LinkedHashMap<>();
        if (Files.isRegularFile(reports.resolve("spec/data.json"))) {
            digest(digests, "spec-data.json", reports.resolve("spec/data.json"));
        }
        digest(digests, "verification-data.json", reports.resolve("verification/data.json"));
        digest(digests, "contract-integrity.json", integrityResult);
        Path manifest = root.resolve(".topplecat/escrow/manifest.json");
        if (Files.exists(manifest)) {
            digest(digests, "escrow-manifest.json", manifest);
        }
        if (Files.isRegularFile(mutationResults)) {
            digest(digests, "mutation-results.json", mutationResults);
        }
        return digests;
    }

    private static void digest(Map<String, String> result, String name, Path path) {
        try {
            result.put(name, Hashing.sha256(Files.readAllBytes(path)));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot hash " + path, exception);
        }
    }

    private static void write(Path path, String source) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, source);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write report " + path, exception);
        }
    }

    private static void publishStableArtifacts(
            Path root,
            Path reports,
            Path evidence,
            Path feedback,
            Path mutationResults,
            boolean publishPublicSpec
    ) {
        Path stable = root.resolve("build/topplecat");
        if (publishPublicSpec) {
            replaceTree(reports.resolve("spec"), stable.resolve("reports/spec"));
        } else {
            deleteTree(stable.resolve("reports/spec"));
        }
        replaceTree(reports.resolve("verification"), stable.resolve("reports/verification"));
        copy(evidence, stable.resolve("evidence.json"));
        copy(feedback, stable.resolve("agent-feedback.json"));
        if (Files.isRegularFile(mutationResults)) {
            copy(mutationResults, stable.resolve("mutation-results.json"));
        }
    }

    static void replaceTree(Path source, Path target) {
        deleteTree(target);
        copyTree(source, target);
    }

    private static void deleteTree(Path target) {
        if (!Files.exists(target)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(target)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot replace ToppleCat report bundle " + target, exception);
        }
    }

    private static void copy(Path source, Path target) {
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot publish ToppleCat artifact " + source + " to " + target, exception);
        }
    }

    private static void copyTree(Path source, Path target) {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path targetPath = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(targetPath);
                } else {
                    copy(path, targetPath);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot publish ToppleCat report bundle " + source + " to " + target, exception);
        }
    }

    private static void copyAttachments(VerificationView view, Path runDirectory, Path bundle) {
        view.acceptanceConditions().stream().flatMap(condition -> condition.cases().stream())
                .flatMap(testCase -> testCase.steps().stream()).flatMap(step -> step.attachments().stream()).forEach(ref -> {
                    Path source = runDirectory.resolve(ref.relativePath()).normalize();
                    Path target = bundle.resolve(ref.relativePath()).normalize();
                    if (!source.startsWith(runDirectory) || !target.startsWith(bundle)) {
                        throw new IllegalStateException("Topple attachment reference escaped its report boundary.");
                    }
                    if (!Files.isRegularFile(source)) {
                        throw new IllegalStateException("Topple attachment asset is missing: " + ref.sha256());
                    }
                    copy(source, target);
                });
    }

    private record ExecutionSummary(
            Map<String, ReportViews.CaseExecution> executions,
            TestResults junit,
            TestResults reviewerJUnit
    ) {
        private ExecutionSummary {
            executions = Map.copyOf(executions);
        }
    }

    private record TestResults(boolean foundXml, boolean failures) {
        private static final TestResults NONE = new TestResults(false, false);
    }

    private record GateOutcome(EvidenceVerdict verdict, String reason) {
        private static GateOutcome pass() {
            return new GateOutcome(EvidenceVerdict.PASS, null);
        }

        private static GateOutcome fail() {
            return new GateOutcome(EvidenceVerdict.FAIL, null);
        }

        private static GateOutcome fail(String reason) {
            return new GateOutcome(EvidenceVerdict.FAIL, reason);
        }

        private static GateOutcome incomplete(String reason) {
            return new GateOutcome(EvidenceVerdict.INCOMPLETE, reason);
        }

        private static GateOutcome disabled(String reason) {
            return new GateOutcome(EvidenceVerdict.DISABLED, reason);
        }

        private EvidenceGate asEvidenceGate(String name) {
            return new EvidenceGate(name, verdict, reason);
        }
    }
}
