package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.AgentFeedback;
import io.github.samzhu.topplecat.core.AgentFeedbackJson;
import io.github.samzhu.topplecat.core.CaseDefinition;
import io.github.samzhu.topplecat.core.CaseRun;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ContractIntegrityResult;
import io.github.samzhu.topplecat.core.ContractIntegrityResultJson;
import io.github.samzhu.topplecat.core.EscrowService;
import io.github.samzhu.topplecat.core.EvidenceGate;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ExpectedConsumptionExecution;
import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.NarrativeExecution;
import io.github.samzhu.topplecat.core.PropertyDefinition;
import io.github.samzhu.topplecat.core.PropertyExecutionEvent;
import io.github.samzhu.topplecat.core.PropertyExecutionEventJson;
import io.github.samzhu.topplecat.core.PropertyExecutionState;
import io.github.samzhu.topplecat.core.PropertyResult;
import io.github.samzhu.topplecat.core.PropertyResults;
import io.github.samzhu.topplecat.core.PropertyResultsJson;
import io.github.samzhu.topplecat.core.StepRun;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleEvidence;
import io.github.samzhu.topplecat.core.ToppleEvidenceJson;
import io.github.samzhu.topplecat.core.VerificationRun;
import io.github.samzhu.topplecat.core.VerificationRunJson;
import io.github.samzhu.topplecat.core.VerificationScope;
import io.github.samzhu.topplecat.core.VerificationScopeJson;
import io.github.samzhu.topplecat.report.CaseResultStatus;
import io.github.samzhu.topplecat.report.DeliveryScope;
import io.github.samzhu.topplecat.report.HtmlBundleWriter;
import io.github.samzhu.topplecat.report.ReportViews;
import io.github.samzhu.topplecat.report.SpecProperty;
import io.github.samzhu.topplecat.report.SpecView;
import io.github.samzhu.topplecat.report.VerificationClassification;
import io.github.samzhu.topplecat.report.VerificationCounterexample;
import io.github.samzhu.topplecat.report.VerificationProperty;
import io.github.samzhu.topplecat.report.VerificationView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tools.jackson.databind.json.JsonMapper;

/** Writes safe and reviewer-only reports after verification test tasks, then feeds evidence. */
public abstract class ToppleCatReportTask extends DefaultTask {
  private static final Pattern CASE_DISPLAY_NAME = Pattern.compile("\\[case:([^]]+)]");
  private static final String MUTATION_COVERAGE_MISSING =
      "Mutation verification did not exercise the required public acceptance contract. "
          + "Check PIT test targeting and public acceptance coverage.";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String CONTRACT_CHANGED =
      "The public executable contract or verification policy changed after reviewer approval.";
  private static final String APPROVAL_MISSING =
      "An existing Mechanical Seal is missing or reviewer custody is not ready; run "
          + "toppleCatSeal before Verify. Verify did not update approval.";
  private static final String INTEGRITY_PRECONDITION =
      "The contract-integrity gate did not permit downstream verification in this run.";
  private static final String PROPERTY_COUNTEREXAMPLE =
      "Property-Based Testing found a counterexample in this run. Review the approved public "
          + "contract and implement the rule generally.";
  private static final String PROPERTY_TASK_INCOMPLETE =
      "Property-Based Testing did not complete in this verification run.";
  private static final String PROPERTY_EVIDENCE_INCOMPLETE =
      "Property-Based Testing current-run evidence was missing or could not be read.";
  private static final String MUTATION_TASK_INCOMPLETE =
      "Mutation Testing did not complete in this verification run.";
  private static final String MUTATION_EVIDENCE_INCOMPLETE =
      "Mutation Testing current-run evidence was missing or could not be read.";
  private static final String MUTATION_SURVIVOR =
      "Mutation Testing found one or more surviving mutants in this run.";

  @Internal
  public abstract DirectoryProperty getProjectRoot();

  @Internal
  public abstract DirectoryProperty getPublicCaseRoot();

  @org.gradle.api.tasks.InputFile
  public abstract RegularFileProperty getDefinitionFile();

  /** Reviewer-only definition built after hidden source has been restored for this exact run. */
  @Internal
  public abstract RegularFileProperty getReviewerDefinitionFile();

  /** Whether this run scheduled the reviewer definition used by Hidden Tests. */
  @Input
  public abstract Property<Boolean> getReviewerDefinitionRequired();

  @Internal
  public abstract DirectoryProperty getRunDirectory();

  @Internal
  public abstract RegularFileProperty getContractIntegrityResultFile();

  @Internal
  public abstract RegularFileProperty getMutationResultsFile();

  @Input
  public abstract Property<Boolean> getHiddenTestsEnabled();

  @Input
  public abstract Property<String> getHiddenTestsDisabledReason();

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

  @Input
  public abstract Property<Boolean> getPropertyEnabled();

  @Input
  public abstract Property<String> getPropertyDisabledReason();

  @Input
  public abstract ListProperty<String> getSelectedSpecPaths();

  @Input
  public abstract Property<Boolean> getSpecOptionProvided();

  @Input
  public abstract Property<Boolean> getAllHidden();

  @TaskAction
  public void report() {
    Path root = getProjectRoot().get().getAsFile().toPath();
    Path runDirectory = getRunDirectory().get().getAsFile().toPath();
    VerificationRunWorkspace.prepare(runDirectory);
    SpecScopeResolver.ResolvedSpecScope selectedScope =
        SpecScopeResolver.resolve(
            root,
            getSelectedSpecPaths().getOrElse(List.of()),
            getSpecOptionProvided().getOrElse(false));
    boolean allHidden = getAllHidden().getOrElse(false) || !selectedScope.scope().selected();
    VerificationScope verificationScope =
        new VerificationScope(
            VerificationScope.SCHEMA_VERSION,
            selectedScope.scope(),
            allHidden ? VerificationScope.HIDDEN_ALL : VerificationScope.HIDDEN_SELECTED_SPECS,
            VerificationScope.MUTATION_ALL_PUBLIC_ACCEPTANCE_CONTRACTS,
            selectedScope.scope().selected()
                ? VerificationScope.PROPERTY_PUBLIC_SELECTED_SPECS
                : VerificationScope.PROPERTY_PUBLIC_FULL_CONTRACT);
    Path verificationScopeFile = runDirectory.resolve("verification-scope.json");
    write(verificationScopeFile, VerificationScopeJson.write(verificationScope));
    ContractDefinition publicDefinition = definition(getDefinitionFile(), "public");
    GateOutcome integrity = contractIntegrityVerdict();
    boolean integrityPassed = integrity.verdict() == EvidenceVerdict.PASS;
    ContractDefinition reviewerDefinition =
        integrityPassed && getReviewerDefinitionRequired().getOrElse(false)
            ? reviewerDefinition()
            : publicDefinition;
    List<ToppleCaseData> publicCases =
        publicDefinition.acceptanceConditions().stream()
            .flatMap(contract -> contract.cases().stream())
            .map(ToppleCatReportTask::caseData)
            .filter(testCase -> testCase.visibility() == CaseVisibility.PUBLIC)
            .filter(
                testCase ->
                    !selectedScope.scope().selected()
                        || selectedScope.scope().acceptanceConditionIds().contains(testCase.acId()))
            .toList();
    List<ToppleCaseData> hiddenVerificationCases =
        reviewerDefinition.acceptanceConditions().stream()
            .flatMap(contract -> contract.cases().stream())
            .map(ToppleCatReportTask::caseData)
            .filter(
                testCase ->
                    testCase.visibility() == CaseVisibility.HIDDEN
                        && getHiddenTestsEnabled().get()
                        && (allHidden
                            || selectedScope
                                .scope()
                                .acceptanceConditionIds()
                                .contains(testCase.acId())))
            .toList();
    List<ToppleCaseData> verificationCases = new ArrayList<>(publicCases);
    verificationCases.addAll(hiddenVerificationCases);
    Map<String, String> publicTitles =
        publicDefinition.acceptanceConditions().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    AcceptanceContract::acId,
                    AcceptanceContract::title,
                    (left, right) -> left,
                    LinkedHashMap::new));
    Map<String, List<String>> publicScenarios =
        publicDefinition.acceptanceConditions().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    AcceptanceContract::acId,
                    contract ->
                        contract.scenario().steps().stream()
                            .map(io.github.samzhu.topplecat.core.ScenarioTemplateRenderer::template)
                            .toList(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    Map<String, String> reviewerTitles =
        reviewerDefinition.acceptanceConditions().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    AcceptanceContract::acId,
                    AcceptanceContract::title,
                    (left, right) -> left,
                    LinkedHashMap::new));
    Map<String, List<io.github.samzhu.topplecat.core.StepTemplate>> reviewerScenarioTemplates =
        reviewerDefinition.acceptanceConditions().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    AcceptanceContract::acId,
                    contract -> contract.scenario().steps(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    Set<String> definedCaseIds =
        verificationCases.stream()
            .map(ToppleCaseData::caseId)
            .collect(java.util.stream.Collectors.toSet());
    Map<String, String> definitionDigestsByCaseId = new LinkedHashMap<>();
    String publicVerificationDefinitionDigest = publicDefinition.digest();
    publicCases.forEach(
        testCase ->
            definitionDigestsByCaseId.put(testCase.caseId(), publicVerificationDefinitionDigest));
    hiddenVerificationCases.forEach(
        testCase -> definitionDigestsByCaseId.put(testCase.caseId(), reviewerDefinition.digest()));
    ExecutionSummary executionSummary =
        executions(runDirectory, definitionDigestsByCaseId, definedCaseIds);
    Map<String, ReportViews.CaseExecution> executions = executionSummary.executions();
    Set<String> selectedAcIds = Set.copyOf(selectedScope.scope().acceptanceConditionIds());
    Set<String> executedHiddenRowAcIds =
        executedHiddenRowAcceptanceConditions(verificationCases, executions);
    int executedHiddenRows =
        (int)
            verificationCases.stream()
                .filter(testCase -> testCase.visibility() == CaseVisibility.HIDDEN)
                .filter(
                    testCase ->
                        executions
                                .getOrDefault(
                                    testCase.caseId(), ReportViews.CaseExecution.notReported())
                                .status()
                            != CaseResultStatus.NOT_REPORTED)
                .count();
    boolean reviewerRowsExecuted = !executedHiddenRowAcIds.isEmpty();
    String runId = currentRunId(runDirectory);
    PropertyCollection properties =
        collectProperties(
            runDirectory,
            publicDefinition,
            selectedScope.scope(),
            getPropertyEnabled().get(),
            integrityPassed,
            runId);
    GateOutcome property = properties.gate();
    Set<String> selectedReviewerCoverage = new java.util.LinkedHashSet<>(executedHiddenRowAcIds);
    boolean missingSelectedReviewerCoverage =
        selectedScope.scope().selected() && !selectedReviewerCoverage.containsAll(selectedAcIds);
    Instant generatedAt = Instant.now();
    GateOutcome junit =
        integrityPassed
            ? testVerdict(
                runDirectory,
                "the public verification",
                VerificationRunArtifacts.JUNIT,
                executionSummary.junit(),
                executionSummary.narrativeEvidenceUsable())
            : GateOutcome.incomplete(INTEGRITY_PRECONDITION);
    GateOutcome reviewer =
        !integrityPassed
            ? GateOutcome.incomplete(INTEGRITY_PRECONDITION)
            : getHiddenTestsEnabled().get()
                ? reviewerRetestVerdict(
                    runDirectory,
                    missingSelectedReviewerCoverage,
                    reviewerRowsExecuted,
                    executionSummary)
                : GateOutcome.disabled(getHiddenTestsDisabledReason().get());
    GateOutcome expectedConsumption =
        !integrityPassed
            ? GateOutcome.incomplete(INTEGRITY_PRECONDITION)
            : getExpectedConsumptionEnabled().get()
                ? expectedConsumptionVerdict(
                    junit,
                    reviewer,
                    verificationCases,
                    executions,
                    executionSummary.expectedConsumptionEvidenceUsable())
                : GateOutcome.disabled(getExpectedConsumptionDisabledReason().get());
    GateOutcome mutation =
        !integrityPassed
            ? GateOutcome.incomplete(INTEGRITY_PRECONDITION)
            : getMutationEnabled().get()
                ? mutationVerdict(runDirectory)
                : GateOutcome.disabled(getMutationDisabledReason().get());
    List<EvidenceGate> reviewerGates =
        List.of(
            integrity.asEvidenceGate(VerificationRunArtifacts.CONTRACT_INTEGRITY),
            junit.asEvidenceGate(VerificationRunArtifacts.JUNIT),
            reviewer.asEvidenceGate(VerificationRunArtifacts.REVIEWER_JUNIT),
            expectedConsumption.asEvidenceGate(VerificationRunArtifacts.EXPECTED_CONSUMPTION),
            property.asEvidenceGate(VerificationRunArtifacts.PROPERTY),
            mutation.asEvidenceGate(VerificationRunArtifacts.MUTATION));
    SpecView spec =
        ReportViews.withSpecProperties(
            ReportViews.spec(
                publicTitles,
                publicCases,
                selectedScope.parsedSpecs().narratives(),
                publicScenarios,
                generatedAt),
            specProperties(publicDefinition, selectedScope.scope()));
    VerificationView verification =
        ReportViews.verificationFromTemplates(
            reviewerTitles,
            verificationCases,
            executions,
            selectedScope.parsedSpecs().narratives(),
            reviewerScenarioTemplates,
            getExpectedConsumptionEnabled().get(),
            reviewerGates,
            generatedAt,
            DeliveryScope.from(
                selectedScope.scope(),
                verificationScope.hiddenMode(),
                verificationScope.mutationMode(),
                verificationScope.publicPropertyMode(),
                executedHiddenRows,
                properties.publicCount()));
    verification =
        ReportViews.withVerificationProperties(verification, verificationProperties(properties));
    Path reports = runDirectory.resolve("reports");
    if (integrityPassed) {
      HtmlBundleWriter.spec(reports.resolve("public"), spec);
    }
    HtmlBundleWriter.verification(reports.resolve("verification"), verification);
    copyAttachments(verification, runDirectory, reports.resolve("verification"));

    EvidenceVerdict verdict =
        reviewerGates.stream().anyMatch(gate -> gate.verdict() == EvidenceVerdict.FAIL)
                || verification.verdict() == CaseResultStatus.FAIL
            ? EvidenceVerdict.FAIL
            : reviewerGates.stream()
                        .allMatch(
                            gate ->
                                gate.verdict() == EvidenceVerdict.PASS
                                    || gate.verdict() == EvidenceVerdict.DISABLED
                                    || gate.verdict() == EvidenceVerdict.NOT_APPLICABLE)
                    && verification.verdict() == CaseResultStatus.PASS
                ? EvidenceVerdict.PASS
                : EvidenceVerdict.INCOMPLETE;
    Path propertyResults = runDirectory.resolve("property-results.json");
    write(
        propertyResults,
        PropertyResultsJson.write(
            new PropertyResults(PropertyResults.SCHEMA_VERSION, runId, properties.results())));
    VerificationRun executionRun =
        new VerificationRun(
            VerificationRun.SCHEMA_VERSION,
            reviewerDefinition.digest(),
            runId,
            runStartedAt(runDirectory),
            generatedAt,
            verdict,
            verificationCases.stream().map(testCase -> caseRun(testCase, executions)).toList());
    write(runDirectory.resolve("verification-run.json"), VerificationRunJson.write(executionRun));
    Map<String, String> digests =
        artifactDigests(
            root,
            reports,
            getMutationResultsFile().get().getAsFile().toPath(),
            getContractIntegrityResultFile().get().getAsFile().toPath(),
            verificationScopeFile,
            propertyResults);
    ToppleEvidence evidence =
        ToppleEvidenceJson.create(runId, generatedAt.toString(), verdict, reviewerGates, digests);
    Path evidencePath = runDirectory.resolve("evidence.json");
    write(evidencePath, ToppleEvidenceJson.write(evidence));
    Path feedbackPath = runDirectory.resolve("agent-feedback.json");
    write(
        feedbackPath,
        AgentFeedbackJson.write(
            new AgentFeedback(AgentFeedback.SCHEMA_VERSION, verdict, reviewerGates)));
    publishStableArtifacts(
        root,
        reports,
        evidencePath,
        feedbackPath,
        getMutationResultsFile().get().getAsFile().toPath(),
        propertyResults,
        integrityPassed);
    VerificationRunWorkspace.archive(runDirectory, runId);
    getLogger().lifecycle("ToppleCat verification report written: {}", verdict);
    if (verdict != EvidenceVerdict.PASS) {
      throw new GradleException(
          "ToppleCat verification verdict is "
              + verdict
              + "; see "
              + evidencePath
              + " for gate-level detail.");
    }
  }

  private ContractDefinition reviewerDefinition() {
    if (!getReviewerDefinitionFile().isPresent()) {
      throw new IllegalStateException(
          "ToppleCat hidden verification requires a run-scoped reviewer definition.");
    }
    return definition(getReviewerDefinitionFile(), "reviewer");
  }

  private PropertyCollection collectProperties(
      Path runDirectory,
      ContractDefinition publicDefinition,
      io.github.samzhu.topplecat.core.SelectedSpecScope scope,
      boolean enabled,
      boolean integrityPassed,
      String runId) {
    if (!integrityPassed) {
      return new PropertyCollection(
          List.of(), List.of(), GateOutcome.incomplete(INTEGRITY_PRECONDITION));
    }
    if (!enabled) {
      return new PropertyCollection(
          List.of(), List.of(), GateOutcome.disabled(getPropertyDisabledReason().get()));
    }
    List<PropertyDefinition> effective =
        propertyDefinitions(publicDefinition).stream()
            .filter(
                property ->
                    !scope.selected() || scope.acceptanceConditionIds().contains(property.acId()))
            .toList();
    if (effective.isEmpty()) {
      return new PropertyCollection(List.of(), List.of(), GateOutcome.notApplicable());
    }
    Map<String, List<PropertyExecutionEvent>> events = new LinkedHashMap<>();
    Map<String, PropertyJUnitResult> junit = new LinkedHashMap<>();
    try {
      readPropertyEvents(runDirectory.resolve("public-property-events.jsonl"), events);
      readPropertyJUnitResults(
          runDirectory.resolve("junit").resolve(VerificationRunArtifacts.PROPERTY_PUBLIC), junit);
    } catch (RuntimeException exception) {
      return new PropertyCollection(
          effective, List.of(), GateOutcome.incomplete(PROPERTY_EVIDENCE_INCOMPLETE));
    }

    List<PropertyResult> results = new ArrayList<>();
    boolean incomplete =
        !effective.isEmpty()
            && !VerificationRunArtifacts.completed(
                runDirectory, VerificationRunArtifacts.PROPERTY_PUBLIC);
    boolean failed = false;
    for (PropertyDefinition definition : effective) {
      List<PropertyExecutionEvent> matching =
          events.getOrDefault(definition.methodIdentity(), List.of());
      List<PropertyExecutionEvent> started =
          matching.stream()
              .filter(event -> event.state() == PropertyExecutionState.STARTED)
              .toList();
      List<PropertyExecutionEvent> terminal =
          matching.stream().filter(event -> event.state().terminal()).toList();
      if (matching.size() != 2
          || started.size() != 1
          || terminal.size() != 1
          || matching.stream().anyMatch(event -> !runId.equals(event.runId()))
          || !started.getFirst().acId().equals(definition.acId())
          || !started.getFirst().sourceDigest().equals(definition.sourceDigest())
          || !terminal.getFirst().acId().equals(definition.acId())
          || !terminal.getFirst().sourceDigest().equals(definition.sourceDigest())) {
        incomplete = true;
        continue;
      }
      PropertyResult result = terminal.getFirst().result();
      List<PropertyJUnitResult> junitMatches =
          junit.entrySet().stream()
              .filter(entry -> definition.methodIdentity().startsWith(entry.getKey()))
              .map(Map.Entry::getValue)
              .toList();
      PropertyJUnitResult junitResult = junitMatches.size() == 1 ? junitMatches.getFirst() : null;
      if (junitResult == null
          || junitResult.skipped()
          || result.state() == PropertyExecutionState.COMPLETED_PASS && junitResult.failed()
          || result.state() != PropertyExecutionState.COMPLETED_PASS && !junitResult.failed()) {
        incomplete = true;
        continue;
      }
      results.add(result);
      if (result.state() == PropertyExecutionState.COMPLETED_COUNTEREXAMPLE) {
        failed = true;
      } else if (result.state() != PropertyExecutionState.COMPLETED_PASS) {
        incomplete = true;
      }
    }
    // Events must not be allowed to smuggle an unsealed, duplicated, or out-of-scope declaration
    // into evidence.
    Set<String> allowed =
        effective.stream()
            .map(PropertyDefinition::methodIdentity)
            .collect(java.util.stream.Collectors.toSet());
    if (events.keySet().stream().anyMatch(identity -> !allowed.contains(identity))) {
      incomplete = true;
    }
    GateOutcome gate =
        failed
            ? GateOutcome.fail(PROPERTY_COUNTEREXAMPLE)
            : incomplete || results.size() != effective.size()
                ? GateOutcome.incomplete(
                    VerificationRunArtifacts.completed(
                            runDirectory, VerificationRunArtifacts.PROPERTY_PUBLIC)
                        ? PROPERTY_EVIDENCE_INCOMPLETE
                        : PROPERTY_TASK_INCOMPLETE)
                : GateOutcome.pass();
    return new PropertyCollection(effective, results, gate);
  }

  private static Map<String, List<SpecProperty>> specProperties(
      ContractDefinition definition, io.github.samzhu.topplecat.core.SelectedSpecScope scope) {
    Map<String, List<SpecProperty>> result = new LinkedHashMap<>();
    for (AcceptanceContract contract : definition.acceptanceConditions()) {
      if (scope.selected() && !scope.acceptanceConditionIds().contains(contract.acId())) {
        continue;
      }
      List<SpecProperty> properties =
          contract.properties().stream()
              .map(
                  property ->
                      new SpecProperty(
                          property.title(),
                          property.methodIdentity(),
                          property.sourceRef().file(),
                          property.sourceRef().line(),
                          property.tries(),
                          property.maxDiscards(),
                          property.maxShrinks()))
              .toList();
      if (!properties.isEmpty()) {
        result.put(contract.acId(), properties);
      }
    }
    return Map.copyOf(result);
  }

  private static Map<String, List<VerificationProperty>> verificationProperties(
      PropertyCollection collection) {
    Map<String, PropertyResult> byIdentity =
        collection.results().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    PropertyResult::methodIdentity, result -> result, (left, right) -> left));
    Map<String, List<VerificationProperty>> result = new LinkedHashMap<>();
    for (PropertyDefinition definition : collection.definitions()) {
      PropertyResult execution = byIdentity.get(definition.methodIdentity());
      VerificationProperty property =
          execution == null
              ? new VerificationProperty(
                  definition.title(),
                  definition.methodIdentity(),
                  "INCOMPLETE",
                  definition.tries(),
                  0,
                  0,
                  0,
                  0,
                  List.of(),
                  0L,
                  false,
                  null,
                  null,
                  null,
                  0,
                  false,
                  "Property declaration did not produce trustworthy current-run completion.")
              : verificationProperty(definition, execution);
      result.computeIfAbsent(definition.acId(), ignored -> new ArrayList<>()).add(property);
    }
    return Map.copyOf(result);
  }

  private static VerificationProperty verificationProperty(
      PropertyDefinition definition, PropertyResult result) {
    String status =
        switch (result.state()) {
          case COMPLETED_PASS -> "PASS";
          case COMPLETED_COUNTEREXAMPLE -> "FAIL";
          case COMPLETED_INCOMPLETE, STARTED -> "INCOMPLETE";
        };
    List<VerificationClassification> classifications =
        result.classifications().stream()
            .map(
                item ->
                    new VerificationClassification(
                        item.label(), item.count(), item.percent(), item.minimumPercent()))
            .toList();
    return new VerificationProperty(
        definition.title(),
        definition.methodIdentity(),
        status,
        result.requestedTrials(),
        result.completedTrials(),
        result.edgeTrials(),
        result.randomTrials(),
        result.discards(),
        classifications,
        result.seed(),
        result.replayVerified(),
        result.replayToken(),
        result.originalCounterexample() == null
            ? null
            : new VerificationCounterexample(
                result.originalCounterexample().choicesJson(),
                result.originalCounterexample().shrinkPath()),
        result.shrunkCounterexample() == null
            ? null
            : new VerificationCounterexample(
                result.shrunkCounterexample().choicesJson(),
                result.shrunkCounterexample().shrinkPath()),
        result.shrinkAttempts(),
        result.shrinkComplete(),
        result.incompleteReason());
  }

  private static List<PropertyDefinition> propertyDefinitions(ContractDefinition definition) {
    return definition.acceptanceConditions().stream()
        .flatMap(contract -> contract.properties().stream())
        .toList();
  }

  private static void readPropertyEvents(
      Path file, Map<String, List<PropertyExecutionEvent>> result) {
    if (!Files.isRegularFile(file)) {
      return;
    }
    try {
      for (String line : Files.readAllLines(file)) {
        if (!line.isBlank()) {
          PropertyExecutionEvent event = PropertyExecutionEventJson.readLine(line);
          result.computeIfAbsent(event.methodIdentity(), ignored -> new ArrayList<>()).add(event);
        }
      }
    } catch (IOException | RuntimeException exception) {
      throw new IllegalStateException(
          "Cannot read ToppleCat Property event evidence " + file + ".", exception);
    }
  }

  private static void readPropertyJUnitResults(
      Path directory, Map<String, PropertyJUnitResult> result) {
    if (!Files.isDirectory(directory)) {
      return;
    }
    try (Stream<Path> files = Files.list(directory)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
        NodeList cases = parseCompletedTestResults(file);
        for (int index = 0; index < cases.getLength(); index++) {
          Element testCase = (Element) cases.item(index);
          String className = testCase.getAttribute("classname");
          String testName = testCase.getAttribute("name");
          int parameters = testName.indexOf('(');
          String method = parameters < 0 ? testName : testName.substring(0, parameters);
          if (className.isBlank() || method.isBlank()) {
            continue;
          }
          String prefix = className + "#" + method + "(";
          result.put(prefix, new PropertyJUnitResult(failure(testCase) != null, skipped(testCase)));
        }
      }
    } catch (IOException | SAXException exception) {
      throw new IllegalStateException(
          "Cannot read ToppleCat Property JUnit XML from " + directory + ".", exception);
    }
  }

  private GateOutcome contractIntegrityVerdict() {
    Path resultFile = getContractIntegrityResultFile().get().getAsFile().toPath();
    if (!Files.isRegularFile(resultFile)) {
      return GateOutcome.incomplete(APPROVAL_MISSING);
    }
    try {
      ContractIntegrityResult result =
          ContractIntegrityResultJson.read(Files.readString(resultFile));
      return switch (result.verdict()) {
        case PASS -> GateOutcome.pass();
        case FAIL -> GateOutcome.fail(CONTRACT_CHANGED);
        case INCOMPLETE -> GateOutcome.incomplete(APPROVAL_MISSING);
        case DISABLED -> GateOutcome.incomplete(APPROVAL_MISSING);
        case NOT_APPLICABLE -> GateOutcome.incomplete(APPROVAL_MISSING);
      };
    } catch (IOException | RuntimeException exception) {
      return GateOutcome.incomplete(APPROVAL_MISSING);
    }
  }

  private static ContractDefinition definition(
      RegularFileProperty definitionFile, String projection) {
    Path file = definitionFile.get().getAsFile().toPath();
    try {
      return ContractDefinitionJson.read(Files.readString(file));
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot read "
              + projection
              + " ToppleCat contract definition "
              + file
              + ": "
              + exception.getMessage(),
          exception);
    }
  }

  private static ToppleCaseData caseData(CaseDefinition testCase) {
    return new ToppleCaseData(
        testCase.caseId(),
        testCase.acId(),
        testCase.visibility(),
        testCase.inputs(),
        testCase.expected(),
        Path.of("contract-definition.json"));
  }

  private static Set<String> executedHiddenRowAcceptanceConditions(
      List<ToppleCaseData> cases, Map<String, ReportViews.CaseExecution> executions) {
    return cases.stream()
        .filter(testCase -> testCase.visibility() == CaseVisibility.HIDDEN)
        .filter(
            testCase ->
                executions
                        .getOrDefault(testCase.caseId(), ReportViews.CaseExecution.notReported())
                        .status()
                    != CaseResultStatus.NOT_REPORTED)
        .map(ToppleCaseData::acId)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  private static CaseRun caseRun(
      ToppleCaseData testCase, Map<String, ReportViews.CaseExecution> executions) {
    ReportViews.CaseExecution execution =
        executions.getOrDefault(testCase.caseId(), ReportViews.CaseExecution.notReported());
    List<StepRun> steps =
        execution.steps().stream()
            .map(
                step ->
                    new StepRun(
                        step.stepId(),
                        step.status(),
                        Duration.ofNanos(step.durationNanos()),
                        step.actualArguments().stream().map(argument -> (Object) argument).toList(),
                        step.attachments(),
                        step.failureRef()))
            .toList();
    long durationNanos = steps.stream().mapToLong(step -> step.duration().toNanos()).sum();
    return new CaseRun(
        testCase.caseId(),
        switch (execution.status()) {
          case PASS -> io.github.samzhu.topplecat.core.NarrativeStepStatus.PASS;
          case FAIL -> io.github.samzhu.topplecat.core.NarrativeStepStatus.FAIL;
          case NOT_REPORTED -> io.github.samzhu.topplecat.core.NarrativeStepStatus.SKIPPED;
        },
        Duration.ofNanos(durationNanos),
        steps,
        execution.expectedConsumption(),
        execution.failure() == null || execution.failure().isBlank()
            ? ""
            : "case-failure:" + testCase.caseId());
  }

  private static Instant runStartedAt(Path runDirectory) {
    try {
      return Files.getLastModifiedTime(runDirectory.resolve(".active")).toInstant();
    } catch (IOException ignored) {
      return Instant.now();
    }
  }

  private static String currentRunId(Path runDirectory) {
    try {
      String runId = Files.readString(runDirectory.resolve("run-id")).trim();
      if (runId.isBlank()) {
        throw new IOException("empty run identifier");
      }
      return runId;
    } catch (IOException exception) {
      throw new IllegalStateException(
          "ToppleCat verification run has no current run identifier.", exception);
    }
  }

  private ExecutionSummary executions(
      Path runDirectory,
      Map<String, String> definitionDigestsByCaseId,
      Set<String> definedCaseIds) {
    Map<String, ReportViews.CaseExecution> result = new LinkedHashMap<>();
    TestResults junit =
        readTestResults(
            runDirectory.resolve("junit/").resolve(VerificationRunArtifacts.JUNIT), result);
    TestResults reviewer =
        readTestResults(
            runDirectory.resolve("junit/").resolve(VerificationRunArtifacts.REVIEWER_JUNIT),
            result);
    boolean narrativeEvidenceUsable = true;
    boolean expectedConsumptionEvidenceUsable = true;
    try {
      readNarrativeExecutions(
          runDirectory.resolve("narrative-executions.jsonl"), result, definitionDigestsByCaseId);
    } catch (RuntimeException exception) {
      narrativeEvidenceUsable = false;
      expectedConsumptionEvidenceUsable = false;
    }
    try {
      readExpectedConsumptionExecutions(
          runDirectory.resolve("expected-consumption-executions.jsonl"), result);
    } catch (RuntimeException exception) {
      expectedConsumptionEvidenceUsable = false;
    }
    Set<String> unknownCaseIds =
        result.keySet().stream()
            .filter(caseId -> !definedCaseIds.contains(caseId))
            .collect(java.util.stream.Collectors.toSet());
    if (!unknownCaseIds.isEmpty()) {
      expectedConsumptionEvidenceUsable = false;
      unknownCaseIds.forEach(result::remove);
    }
    return new ExecutionSummary(
        result, junit, reviewer, narrativeEvidenceUsable, expectedConsumptionEvidenceUsable);
  }

  private static void readNarrativeExecutions(
      Path file,
      Map<String, ReportViews.CaseExecution> result,
      Map<String, String> definitionDigestsByCaseId) {
    if (!Files.isRegularFile(file)) {
      throw new IllegalStateException("ToppleCat runtime narrative sidecar is missing: " + file);
    }
    try {
      for (String line : Files.readAllLines(file)) {
        if (line.isBlank()) {
          continue;
        }
        NarrativeExecution narrative = JSON.readValue(line, NarrativeExecution.class);
        String expectedDefinitionDigest = definitionDigestsByCaseId.get(narrative.caseId());
        if (!narrative.definitionDigest().equals(expectedDefinitionDigest)) {
          throw new IllegalStateException(
              "ToppleCat runtime narrative for "
                  + narrative.caseId()
                  + " was produced from a different contract definition digest.");
        }
        ReportViews.CaseExecution execution =
            result.getOrDefault(narrative.caseId(), ReportViews.CaseExecution.notReported());
        result.put(
            narrative.caseId(),
            new ReportViews.CaseExecution(
                execution.status(),
                execution.failure(),
                narrative.steps(),
                execution.expectedConsumption()));
      }
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot read ToppleCat narrative sidecar " + file + ": " + exception.getMessage(),
          exception);
    }
  }

  private static void readExpectedConsumptionExecutions(
      Path file, Map<String, ReportViews.CaseExecution> result) {
    if (!Files.isRegularFile(file)) {
      throw new IllegalStateException("ToppleCat expected-consumption sidecar is missing: " + file);
    }
    try {
      for (String line : Files.readAllLines(file)) {
        if (line.isBlank()) {
          continue;
        }
        ExpectedConsumptionExecution consumption =
            JSON.readValue(line, ExpectedConsumptionExecution.class);
        ReportViews.CaseExecution execution =
            result.getOrDefault(consumption.caseId(), ReportViews.CaseExecution.notReported());
        result.put(
            consumption.caseId(),
            new ReportViews.CaseExecution(
                execution.status(),
                execution.failure(),
                execution.steps(),
                consumption.expectedConsumption()));
      }
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot read ToppleCat expected-consumption sidecar "
              + file
              + ": "
              + exception.getMessage(),
          exception);
    }
  }

  private static TestResults readTestResults(
      Path directory, Map<String, ReportViews.CaseExecution> result) {
    if (!Files.exists(directory)) {
      return TestResults.NONE;
    }
    boolean foundXml = false;
    boolean failures = false;
    int testCasesCount = 0;
    int executedTestCasesCount = 0;
    try (Stream<Path> files = Files.list(directory)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
        foundXml = true;
        NodeList testCases = parseCompletedTestResults(file);
        testCasesCount += testCases.getLength();
        for (int index = 0; index < testCases.getLength(); index++) {
          Element testCase = (Element) testCases.item(index);
          Element failure = failure(testCase);
          failures |= failure != null;
          boolean skipped = skipped(testCase);
          if (!skipped) {
            executedTestCasesCount++;
          }
          Matcher matcher = CASE_DISPLAY_NAME.matcher(testCase.getAttribute("name"));
          if (!matcher.find()) {
            continue;
          }
          result.put(
              matcher.group(1),
              skipped
                  ? ReportViews.CaseExecution.notReported()
                  : failure == null
                      ? new ReportViews.CaseExecution(CaseResultStatus.PASS, null, List.of())
                      : new ReportViews.CaseExecution(
                          CaseResultStatus.FAIL, failure.getAttribute("message"), List.of()));
        }
      }
    } catch (IOException | SAXException | RuntimeException exception) {
      return TestResults.unusable();
    }
    return new TestResults(foundXml, failures, testCasesCount, executedTestCasesCount, true);
  }

  /**
   * Gradle can expose a test XML path to a finalizer just before its writer flushes the document.
   */
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
          throw new IllegalStateException(
              "Interrupted while waiting for JUnit XML results: " + file, interrupted);
        }
      } catch (IOException exception) {
        throw new IllegalStateException(
            "Cannot read JUnit XML results from " + file + ": " + exception.getMessage(),
            exception);
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
      if (node instanceof Element element
          && (element.getTagName().equals("failure") || element.getTagName().equals("error"))) {
        return element;
      }
    }
    return null;
  }

  private static boolean skipped(Element testCase) {
    NodeList children = testCase.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
      Node node = children.item(index);
      if (node instanceof Element element && element.getTagName().equals("skipped")) {
        return true;
      }
    }
    return false;
  }

  private static GateOutcome testVerdict(
      Path runDirectory,
      String taskName,
      String gate,
      TestResults results,
      boolean narrativeEvidenceUsable) {
    if (!VerificationRunArtifacts.completed(runDirectory, gate)) {
      return GateOutcome.incomplete(taskName + " did not complete in this verification run.");
    }
    if (!narrativeEvidenceUsable) {
      return GateOutcome.incomplete(taskName + " completed without usable current-run sidecars.");
    }
    if (!results.usable()) {
      return GateOutcome.incomplete(
          taskName + " current-run JUnit XML was missing or could not be read.");
    }
    if (!results.foundXml()) {
      return GateOutcome.incomplete(taskName + " completed without JUnit XML results.");
    }
    if (results.executedTestCases() == 0) {
      return GateOutcome.incomplete(taskName + " completed without executable JUnit tests.");
    }
    return results.failures() ? GateOutcome.fail() : GateOutcome.pass();
  }

  private static GateOutcome reviewerRetestVerdict(
      Path runDirectory,
      boolean missingSelectedReviewerCoverage,
      boolean reviewerRowsExecuted,
      ExecutionSummary executionSummary) {
    if (missingSelectedReviewerCoverage) {
      return GateOutcome.incomplete(
          "the selected executable contract is missing reviewer coverage.");
    }
    if (!reviewerRowsExecuted) {
      return GateOutcome.incomplete("the hidden tests have no executed hidden typed rows.");
    }
    return testVerdict(
        runDirectory,
        "the hidden typed-row retest",
        VerificationRunArtifacts.REVIEWER_JUNIT,
        executionSummary.reviewerJUnit(),
        executionSummary.narrativeEvidenceUsable());
  }

  private GateOutcome mutationVerdict(Path runDirectory) {
    String incompleteReason = getMutationIncompleteReason().getOrElse("").trim();
    if (!incompleteReason.isEmpty()) {
      return GateOutcome.incomplete("Mutation Testing did not run: " + incompleteReason);
    }
    if (!VerificationRunArtifacts.completed(runDirectory, VerificationRunArtifacts.MUTATION)) {
      return GateOutcome.incomplete(MUTATION_TASK_INCOMPLETE);
    }
    Path results = getMutationResultsFile().get().getAsFile().toPath();
    if (!Files.isRegularFile(results)) {
      return GateOutcome.incomplete(MUTATION_EVIDENCE_INCOMPLETE);
    }
    try {
      MutationGateResults mutationResults = MutationGateResults.read(Files.readString(results));
      if (mutationResults.verdict() == EvidenceVerdict.PASS) {
        return GateOutcome.pass();
      }
      // A usable PIT report that contains no mutant covered by a public acceptance
      // acceptance test is a target-selection failure. Keep the remediation generic:
      // reviewer-only names, case values, and raw PIT details never enter feedback.
      if (mutationResults.assessments().stream()
          .anyMatch(
              assessment ->
                  assessment.verdict() == EvidenceVerdict.FAIL
                      && assessment.totalMutations() == 0)) {
        return GateOutcome.fail(MUTATION_COVERAGE_MISSING);
      }
      return GateOutcome.fail(MUTATION_SURVIVOR);
    } catch (IOException | RuntimeException exception) {
      return GateOutcome.incomplete(MUTATION_EVIDENCE_INCOMPLETE);
    }
  }

  private static GateOutcome expectedConsumptionVerdict(
      GateOutcome junit,
      GateOutcome reviewer,
      List<ToppleCaseData> cases,
      Map<String, ReportViews.CaseExecution> executions,
      boolean currentRunSidecarsUsable) {
    if (junit.verdict() == EvidenceVerdict.INCOMPLETE) {
      return GateOutcome.incomplete(
          "the public verification did not complete expected-consumption tracking.");
    }
    if (reviewer.verdict() == EvidenceVerdict.INCOMPLETE) {
      return GateOutcome.incomplete(
          "the hidden tests did not complete expected-consumption tracking.");
    }
    if (!currentRunSidecarsUsable) {
      return GateOutcome.incomplete(
          "Expected-consumption sidecar data was missing or could not be read.");
    }
    boolean unasserted = false;
    boolean unknown = false;
    for (ToppleCaseData testCase : cases) {
      ReportViews.CaseExecution execution =
          executions.getOrDefault(testCase.caseId(), ReportViews.CaseExecution.notReported());
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
    return unknown
        ? GateOutcome.incomplete(
            "Expected-consumption sidecar data was missing for one or more declared values.")
        : GateOutcome.pass();
  }

  private static Map<String, String> artifactDigests(
      Path root,
      Path reports,
      Path mutationResults,
      Path integrityResult,
      Path verificationScope,
      Path propertyResults) {
    Map<String, String> digests = new LinkedHashMap<>();
    if (Files.isRegularFile(reports.resolve("public/data.json"))) {
      digest(digests, "public-data.json", reports.resolve("public/data.json"));
    }
    digest(digests, "verification-data.json", reports.resolve("verification/data.json"));
    digest(digests, "contract-integrity.json", integrityResult);
    digest(digests, "verification-scope.json", verificationScope);
    Path manifest =
        EscrowService.reviewerStatePath(root, EscrowService.defaultReviewerStateRoot())
            .resolve("manifest.json");
    if (Files.exists(manifest)) {
      digest(digests, "escrow-manifest.json", manifest);
    }
    if (Files.isRegularFile(mutationResults)) {
      digest(digests, "mutation-results.json", mutationResults);
    }
    digest(digests, "property-results.json", propertyResults);
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
      Path propertyResults,
      boolean publishPublicSpec) {
    Path stable = root.resolve("build/topplecat");
    if (publishPublicSpec) {
      replaceTree(reports.resolve("public"), stable.resolve("reports/public"));
    } else {
      deleteTree(stable.resolve("reports/public"));
    }
    replaceTree(reports.resolve("verification"), stable.resolve("reports/verification"));
    copy(evidence, stable.resolve("evidence.json"));
    copy(feedback, stable.resolve("agent-feedback.json"));
    if (Files.isRegularFile(mutationResults)) {
      copy(mutationResults, stable.resolve("mutation-results.json"));
    }
    copy(propertyResults, stable.resolve("property-results.json"));
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
      throw new IllegalStateException(
          "Cannot replace ToppleCat report bundle " + target, exception);
    }
  }

  private static void copy(Path source, Path target) {
    try {
      Files.createDirectories(target.getParent());
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot publish ToppleCat artifact " + source + " to " + target, exception);
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
      throw new IllegalStateException(
          "Cannot publish ToppleCat report bundle " + source + " to " + target, exception);
    }
  }

  private static void copyAttachments(VerificationView view, Path runDirectory, Path bundle) {
    view.acceptanceConditions().stream()
        .flatMap(condition -> condition.cases().stream())
        .flatMap(testCase -> testCase.steps().stream())
        .flatMap(step -> step.attachments().stream())
        .forEach(
            ref -> {
              Path source = runDirectory.resolve(ref.relativePath()).normalize();
              Path target = bundle.resolve(ref.relativePath()).normalize();
              if (!source.startsWith(runDirectory) || !target.startsWith(bundle)) {
                throw new IllegalStateException(
                    "Topple attachment reference escaped its report boundary.");
              }
              if (!Files.isRegularFile(source)) {
                throw new IllegalStateException(
                    "Topple attachment asset is missing: " + ref.sha256());
              }
              copy(source, target);
            });
  }

  private record ExecutionSummary(
      Map<String, ReportViews.CaseExecution> executions,
      TestResults junit,
      TestResults reviewerJUnit,
      boolean narrativeEvidenceUsable,
      boolean expectedConsumptionEvidenceUsable) {
    private ExecutionSummary {
      executions = Map.copyOf(executions);
    }
  }

  private record TestResults(
      boolean foundXml, boolean failures, int testCases, int executedTestCases, boolean usable) {
    private static final TestResults NONE = new TestResults(false, false, 0, 0, true);

    private static TestResults unusable() {
      return new TestResults(false, false, 0, 0, false);
    }
  }

  private record PropertyCollection(
      List<PropertyDefinition> definitions, List<PropertyResult> results, GateOutcome gate) {
    private PropertyCollection {
      definitions = List.copyOf(definitions);
      results = List.copyOf(results);
    }

    private int publicCount() {
      return results.size();
    }
  }

  private record PropertyJUnitResult(boolean failed, boolean skipped) {}

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

    private static GateOutcome notApplicable() {
      return new GateOutcome(EvidenceVerdict.NOT_APPLICABLE, null);
    }

    private EvidenceGate asEvidenceGate(String name) {
      return new EvidenceGate(name, verdict, reason);
    }
  }
}
