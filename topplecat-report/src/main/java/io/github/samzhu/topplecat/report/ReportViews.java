package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.EvidenceGate;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.ScenarioTemplateRenderer;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import io.github.samzhu.topplecat.pitest.PitMutationAttribution;
import io.github.samzhu.topplecat.pitest.PitMutationEvidence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Factories that enforce the information boundary before rendering begins. */
public final class ReportViews {
  private ReportViews() {}

  public static VerificationView verification(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      Instant generatedAt) {
    return verification(titles, cases, executions, true, List.of(), generatedAt);
  }

  public static VerificationView verification(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      boolean expectedConsumptionEnforced,
      List<EvidenceGate> gates,
      Instant generatedAt) {
    return verificationWithSources(
        titles,
        cases,
        executions,
        Map.of(),
        Map.of(),
        Map.of(),
        expectedConsumptionEnforced,
        gates,
        generatedAt,
        null);
  }

  /** Builds the reviewer projection from the exact compiler-owned scenario templates. */
  public static VerificationView verificationFromTemplates(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      Map<String, List<StepTemplate>> scenarios,
      boolean expectedConsumptionEnforced,
      List<EvidenceGate> gates,
      Instant generatedAt) {
    return verificationFromTemplates(
        titles,
        cases,
        executions,
        scenarios,
        expectedConsumptionEnforced,
        gates,
        generatedAt,
        null);
  }

  /**
   * Builds the reviewer projection from compiler templates and an explicit delivery-scope
   * projection.
   */
  public static VerificationView verificationFromTemplates(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      Map<String, List<StepTemplate>> scenarios,
      boolean expectedConsumptionEnforced,
      List<EvidenceGate> gates,
      Instant generatedAt,
      DeliveryScope deliveryScope) {
    Map<String, List<String>> templates = new LinkedHashMap<>();
    Map<String, Map<String, SourceRef>> sources = new LinkedHashMap<>();
    Map<String, Map<String, io.github.samzhu.topplecat.core.StepPhase>> phases =
        new LinkedHashMap<>();
    scenarios.forEach(
        (acId, steps) -> {
          templates.put(acId, steps.stream().map(ScenarioTemplateRenderer::template).toList());
          sources.put(
              acId,
              steps.stream()
                  .collect(
                      java.util.stream.Collectors.toMap(
                          StepTemplate::stepId,
                          StepTemplate::sourceRef,
                          (left, right) -> left,
                          LinkedHashMap::new)));
          phases.put(
              acId,
              steps.stream()
                  .collect(
                      java.util.stream.Collectors.toMap(
                          StepTemplate::stepId,
                          StepTemplate::phase,
                          (left, right) -> left,
                          LinkedHashMap::new)));
        });
    return verificationWithSources(
        titles,
        cases,
        executions,
        templates,
        sources,
        phases,
        expectedConsumptionEnforced,
        gates,
        generatedAt,
        deliveryScope);
  }

  private static VerificationView verificationWithSources(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      Map<String, List<String>> scenarios,
      Map<String, Map<String, SourceRef>> stepSources,
      Map<String, Map<String, io.github.samzhu.topplecat.core.StepPhase>> stepPhases,
      boolean expectedConsumptionEnforced,
      List<EvidenceGate> gates,
      Instant generatedAt,
      DeliveryScope deliveryScope) {
    Map<String, List<ToppleCaseData>> byAc = group(cases);
    List<VerificationAcceptanceCondition> acs = new ArrayList<>();
    for (Map.Entry<String, List<ToppleCaseData>> entry : byAc.entrySet()) {
      List<VerificationCase> rows =
          entry.getValue().stream()
              .map(
                  row -> {
                    CaseExecution execution =
                        executions.getOrDefault(row.caseId(), CaseExecution.notReported());
                    Map<String, String> consumption = new LinkedHashMap<>();
                    row.expected()
                        .propertyNames()
                        .forEach(
                            key ->
                                consumption.put(
                                    key,
                                    execution.expectedConsumption().getOrDefault(key, "UNKNOWN")));
                    return new VerificationCase(
                        row.caseId(),
                        row.visibility(),
                        row.inputs(),
                        row.expected(),
                        execution.status(),
                        consumption,
                        execution.steps(),
                        execution.failure());
                  })
              .sorted(
                  Comparator.comparing(
                          (VerificationCase row) -> row.status() != CaseResultStatus.FAIL)
                      .thenComparing(VerificationCase::caseId))
              .toList();
      CaseResultStatus status =
          rows.stream().map(VerificationCase::status).anyMatch(CaseResultStatus.FAIL::equals)
              ? CaseResultStatus.FAIL
              : rows.stream().map(VerificationCase::status).allMatch(CaseResultStatus.PASS::equals)
                  ? CaseResultStatus.PASS
                  : CaseResultStatus.NOT_REPORTED;
      acs.add(
          new VerificationAcceptanceCondition(
              entry.getKey(),
              title(titles, entry.getKey()),
              scenarios.getOrDefault(entry.getKey(), List.of()),
              status,
              rows,
              stepSources.getOrDefault(entry.getKey(), Map.of()),
              stepPhases.getOrDefault(entry.getKey(), Map.of())));
    }
    acs.sort(
        Comparator.comparing(
                (VerificationAcceptanceCondition ac) -> ac.status() != CaseResultStatus.FAIL)
            .thenComparing(VerificationAcceptanceCondition::acId));
    CaseResultStatus verdict = suiteVerdict(acs, gates);
    return new VerificationView(
        VerificationView.SCHEMA_VERSION,
        generatedAt,
        verdict,
        expectedConsumptionEnforced,
        gates,
        acs,
        deliveryScope,
        null,
        null);
  }

  /**
   * Joins independently collected Property execution evidence without placing it in case-row
   * models.
   */
  public static VerificationView withVerificationProperties(
      VerificationView view, Map<String, List<VerificationProperty>> properties) {
    List<VerificationAcceptanceCondition> acceptanceConditions =
        view.acceptanceConditions().stream()
            .map(
                ac -> {
                  List<VerificationProperty> attached =
                      properties.getOrDefault(ac.acId(), List.of());
                  CaseResultStatus status =
                      attached.stream().anyMatch(property -> property.status().equals("FAIL"))
                          ? CaseResultStatus.FAIL
                          : ac.status() == CaseResultStatus.FAIL
                              ? CaseResultStatus.FAIL
                              : attached.stream()
                                          .anyMatch(
                                              property -> property.status().equals("INCOMPLETE"))
                                      || ac.status() == CaseResultStatus.NOT_REPORTED
                                  ? CaseResultStatus.NOT_REPORTED
                                  : CaseResultStatus.PASS;
                  return new VerificationAcceptanceCondition(
                      ac.acId(),
                      ac.title(),
                      ac.scenario(),
                      status,
                      ac.cases(),
                      ac.stepSources(),
                      ac.stepPhases(),
                      attached);
                })
            .sorted(
                Comparator.comparing(
                        (VerificationAcceptanceCondition ac) ->
                            ac.status() != CaseResultStatus.FAIL)
                    .thenComparing(VerificationAcceptanceCondition::acId))
            .toList();
    CaseResultStatus verdict = suiteVerdict(acceptanceConditions, view.gates());
    return new VerificationView(
        VerificationView.SCHEMA_VERSION,
        view.generatedAt(),
        verdict,
        view.expectedConsumptionEnforced(),
        view.gates(),
        acceptanceConditions,
        view.deliveryScope(),
        view.mutationAttribution(),
        view.run());
  }

  /** Adds reviewer-only raw PIT attribution after the mutation producer result is available. */
  public static VerificationView withMutationAttribution(
      VerificationView view, PitMutationAttribution mutationAttribution) {
    VerificationView attached =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            view.generatedAt(),
            view.verdict(),
            view.expectedConsumptionEnforced(),
            view.gates(),
            view.acceptanceConditions(),
            view.deliveryScope(),
            mutationAttribution,
            view.run());
    return withAcSafeguards(attached);
  }

  /** Projects already-assessed safeguard evidence into the fixed order of every AC card. */
  private static VerificationView withAcSafeguards(VerificationView view) {
    Map<String, PitMutationAssessment> mutations =
        view.mutationAttribution() == null
            ? Map.of()
            : view.mutationAttribution().assessments().stream()
                .collect(
                    java.util.stream.Collectors.toMap(
                        PitMutationAssessment::acId, item -> item, (left, right) -> left));
    List<VerificationAcceptanceCondition> assessed =
        view.acceptanceConditions().stream()
            .map(
                ac ->
                    assessAc(
                        ac, view.gates(), mutations.get(ac.acId()), view.mutationAttribution()))
            .sorted(
                Comparator.comparing(
                        (VerificationAcceptanceCondition ac) ->
                            ac.status() == CaseResultStatus.PASS)
                    .thenComparing(ac -> ac.status() == CaseResultStatus.NOT_REPORTED)
                    .thenComparing(VerificationAcceptanceCondition::acId))
            .toList();
    return new VerificationView(
        VerificationView.SCHEMA_VERSION,
        view.generatedAt(),
        suiteVerdict(assessed, view.gates()),
        view.expectedConsumptionEnforced(),
        view.gates(),
        assessed,
        view.deliveryScope(),
        view.mutationAttribution(),
        view.run());
  }

  private static VerificationAcceptanceCondition assessAc(
      VerificationAcceptanceCondition ac,
      List<EvidenceGate> gates,
      PitMutationAssessment mutation,
      PitMutationAttribution mutationAttribution) {
    List<VerificationMutationDetail> undetectedMutations =
        undetectedMutations(ac.acId(), mutationAttribution);
    List<VerificationSafeguard> safeguards =
        List.of(
            caseSafeguard(
                "PUBLIC_ACCEPTANCE",
                "JUNIT",
                ac.cases().stream()
                    .filter(testCase -> testCase.visibility() == CaseVisibility.PUBLIC)
                    .toList(),
                gates),
            caseSafeguard(
                "HIDDEN_TESTS",
                "REVIEWER_JUNIT",
                ac.cases().stream()
                    .filter(testCase -> testCase.visibility() == CaseVisibility.HIDDEN)
                    .toList(),
                gates),
            expectedSafeguard(ac, gates),
            propertySafeguard(ac, gates),
            mutationSafeguard(mutation, gate(gates, "MUTATION"), gate(gates, "JUNIT")));
    CaseResultStatus status =
        safeguards.stream().anyMatch(item -> item.verdict() == EvidenceVerdict.FAIL)
            ? CaseResultStatus.FAIL
            : safeguards.stream().anyMatch(item -> item.verdict() == EvidenceVerdict.INCOMPLETE)
                ? CaseResultStatus.NOT_REPORTED
                : CaseResultStatus.PASS;
    return new VerificationAcceptanceCondition(
        ac.acId(),
        ac.title(),
        ac.scenario(),
        status,
        ac.cases(),
        ac.stepSources(),
        ac.stepPhases(),
        ac.properties(),
        safeguards,
        undetectedMutations);
  }

  private static VerificationSafeguard caseSafeguard(
      String name, String gateName, List<VerificationCase> cases, List<EvidenceGate> gates) {
    EvidenceGate gate = gate(gates, gateName);
    if (cases.stream().anyMatch(item -> item.status() == CaseResultStatus.FAIL)) {
      return safeguard(
          name,
          EvidenceVerdict.FAIL,
          VerificationSafeguardOutcome.PROBLEM_FOUND,
          VerificationSafeguardReason.CASE_FAILED,
          "A case in this acceptance run failed.",
          gateName);
    }
    if (!cases.isEmpty()
        && cases.stream().allMatch(item -> item.status() == CaseResultStatus.PASS)) {
      return safeguard(
          name,
          EvidenceVerdict.PASS,
          VerificationSafeguardOutcome.PASSED,
          VerificationSafeguardReason.ALL_CASES_PASSED,
          "All recorded cases passed.",
          gateName);
    }
    return absentSafeguard(
        name,
        gate,
        gateName,
        VerificationSafeguardReason.NO_CASE_EVIDENCE,
        "No current case evidence was recorded for this AC.");
  }

  private static VerificationSafeguard expectedSafeguard(
      VerificationAcceptanceCondition ac, List<EvidenceGate> gates) {
    EvidenceGate gate = gate(gates, "EXPECTED_CONSUMPTION");
    boolean hasExpected = ac.cases().stream().anyMatch(testCase -> !testCase.expected().isEmpty());
    boolean unverified =
        ac.cases().stream()
            .flatMap(testCase -> testCase.expectedConsumption().values().stream())
            .anyMatch(state -> !"ASSERTED".equals(state));
    boolean missing =
        ac.cases().stream()
            .flatMap(testCase -> testCase.expectedConsumption().values().stream())
            .anyMatch("UNKNOWN"::equals);
    if (missing) {
      return safeguard(
          "EXPECTED_RESULT_CHECK",
          EvidenceVerdict.INCOMPLETE,
          VerificationSafeguardOutcome.UNABLE_TO_ASSESS,
          VerificationSafeguardReason.EXPECTED_COMPARISON_MISSING,
          "The current run did not record an expected-result comparison for this AC.",
          "EXPECTED_CONSUMPTION");
    }
    if (unverified) {
      return safeguard(
          "EXPECTED_RESULT_CHECK",
          EvidenceVerdict.FAIL,
          VerificationSafeguardOutcome.PROBLEM_FOUND,
          VerificationSafeguardReason.EXPECTED_NOT_COMPARED,
          "An authored expected result was read or declared but not compared with the actual"
              + " result.",
          "EXPECTED_CONSUMPTION");
    }
    if (hasExpected && !ac.cases().isEmpty()) {
      return safeguard(
          "EXPECTED_RESULT_CHECK",
          EvidenceVerdict.PASS,
          VerificationSafeguardOutcome.COMPARISON_COMPLETED,
          VerificationSafeguardReason.EXPECTED_COMPARISON_COMPLETED,
          "Every authored expected result was compared with the program's actual result.",
          "EXPECTED_CONSUMPTION");
    }
    return absentSafeguard(
        "EXPECTED_RESULT_CHECK",
        gate,
        "EXPECTED_CONSUMPTION",
        VerificationSafeguardReason.NO_EXPECTED_RESULT,
        "This AC has no authored expected result to check.");
  }

  private static VerificationSafeguard propertySafeguard(
      VerificationAcceptanceCondition ac, List<EvidenceGate> gates) {
    EvidenceGate gate = gate(gates, "PROPERTY");
    if (ac.properties().stream().anyMatch(property -> "FAIL".equals(property.status()))) {
      return safeguard(
          "PROPERTY_BASED_TESTING",
          EvidenceVerdict.FAIL,
          VerificationSafeguardOutcome.PROBLEM_FOUND,
          VerificationSafeguardReason.PROPERTY_COUNTEREXAMPLE,
          "A generated input violated the authored Property rule.",
          "PROPERTY");
    }
    if (ac.properties().stream().anyMatch(property -> "INCOMPLETE".equals(property.status()))) {
      return safeguard(
          "PROPERTY_BASED_TESTING",
          EvidenceVerdict.INCOMPLETE,
          VerificationSafeguardOutcome.UNABLE_TO_ASSESS,
          VerificationSafeguardReason.PROPERTY_EVIDENCE_INCOMPLETE,
          "Property-Based Testing did not produce complete current-run evidence.",
          "PROPERTY");
    }
    if (!ac.properties().isEmpty()) {
      return safeguard(
          "PROPERTY_BASED_TESTING",
          EvidenceVerdict.PASS,
          VerificationSafeguardOutcome.PASSED,
          VerificationSafeguardReason.PROPERTY_COMPLETED,
          "All completed generated inputs satisfied the authored Property rule.",
          "PROPERTY");
    }
    return absentSafeguard(
        "PROPERTY_BASED_TESTING",
        gate,
        "PROPERTY",
        VerificationSafeguardReason.NO_PROPERTY,
        "This AC has no Property declaration in the current scope.");
  }

  private static VerificationSafeguard mutationSafeguard(
      PitMutationAssessment assessment, EvidenceGate gate, EvidenceGate publicAcceptanceGate) {
    if (gate.verdict() == EvidenceVerdict.DISABLED
        || gate.verdict() == EvidenceVerdict.INCOMPLETE
        || gate.verdict() == EvidenceVerdict.NOT_APPLICABLE) {
      VerificationSafeguardReason unavailableReason =
          gate.verdict() == EvidenceVerdict.INCOMPLETE
                  && publicAcceptanceGate.verdict() == EvidenceVerdict.FAIL
              ? VerificationSafeguardReason.MUTATION_BASELINE_FAILED
              : VerificationSafeguardReason.MUTATION_EVIDENCE_UNAVAILABLE;
      return absentSafeguard(
          "MUTATION_TESTING",
          gate,
          "MUTATION",
          unavailableReason,
          "Mutation Testing has no applicable current-run evidence for this AC.");
    }
    if (assessment == null) {
      return absentSafeguard(
          "MUTATION_TESTING",
          gate,
          "MUTATION",
          VerificationSafeguardReason.NO_MUTATION_ATTRIBUTED,
          "No mutation was exactly attributed to this AC in the current run.");
    }
    if (assessment.attributionGap()) {
      return safeguard(
          "MUTATION_TESTING",
          EvidenceVerdict.NOT_APPLICABLE,
          VerificationSafeguardOutcome.NOT_APPLICABLE,
          VerificationSafeguardReason.MUTATION_ATTRIBUTION_GAP,
          "No mutation was exactly attributed to this Acceptance Method in the current run.",
          "MUTATION");
    }
    if (assessment.killedByAcceptanceMethodMutantCount() < assessment.coveredMutantCount()) {
      return safeguard(
          "MUTATION_TESTING",
          EvidenceVerdict.FAIL,
          VerificationSafeguardOutcome.PROBLEM_FOUND,
          VerificationSafeguardReason.MUTATION_SURVIVED,
          "An altered program still passed this AC's public acceptance, so the current acceptance"
              + " may not reveal a problem in this function.",
          "MUTATION");
    }
    return safeguard(
        "MUTATION_TESTING",
        EvidenceVerdict.PASS,
        VerificationSafeguardOutcome.PASSED,
        VerificationSafeguardReason.MUTATION_DETECTED,
        "Every attributed altered program made this AC's public acceptance fail as expected.",
        "MUTATION");
  }

  private static List<VerificationMutationDetail> undetectedMutations(
      String acId, PitMutationAttribution attribution) {
    if (attribution == null) {
      return List.of();
    }
    List<VerificationMutationDetail> result = new ArrayList<>();
    List<PitMutationEvidence> mutations = attribution.mutations();
    for (int index = 0; index < mutations.size(); index++) {
      PitMutationEvidence mutation = mutations.get(index);
      if (!mutation.attributedAcceptanceConditionIds().contains(acId)
          || mutation.detectedAcceptanceConditionIds().contains(acId)) {
        continue;
      }
      Replacement replacement = replacement(mutation);
      result.add(
          new VerificationMutationDetail(
              index + 1,
              mutation.status(),
              mutation.detected(),
              mutation.mutatedClass(),
              mutation.sourceFile(),
              mutation.mutatedMethod(),
              mutation.methodDescription(),
              mutation.lineNumber(),
              mutation.block(),
              mutation.index(),
              mutation.description(),
              mutation.originalSourceLine(),
              replacement.before(),
              replacement.after()));
    }
    return List.copyOf(result);
  }

  /** Identifies only replacements supported by both the PIT description and the original line. */
  private static Replacement replacement(PitMutationEvidence mutation) {
    String sourceLine = mutation.originalSourceLine();
    if (sourceLine == null || sourceLine.isBlank()) {
      return Replacement.NONE;
    }
    String description = mutation.description().toLowerCase(Locale.ROOT);
    List<Replacement> candidates =
        List.of(
            new Replacement("+", "-", "addition with subtraction"),
            new Replacement("-", "+", "subtraction with addition"),
            new Replacement("*", "/", "multiplication with division"),
            new Replacement("/", "*", "division with multiplication"),
            new Replacement("%", "*", "modulus with multiplication"),
            new Replacement("<", ">", "less than with greater than"),
            new Replacement(">", "<", "greater than with less than"),
            new Replacement("<=", ">=", "less than or equal with greater than or equal"),
            new Replacement(">=", "<=", "greater than or equal with less than or equal"),
            new Replacement("==", "!=", "equality with inequality"),
            new Replacement("!=", "==", "inequality with equality"));
    return candidates.stream()
        .filter(candidate -> description.contains(candidate.descriptionPhrase()))
        .filter(candidate -> sourceLine.contains(candidate.before()))
        .findFirst()
        .orElseGet(
            () -> {
              String marker = "replaced return value with";
              int markerIndex = description.indexOf(marker);
              if (markerIndex >= 0 && sourceLine.stripLeading().startsWith("return")) {
                String after =
                    mutation.description().substring(markerIndex + marker.length()).trim();
                return after.isBlank()
                    ? Replacement.NONE
                    : new Replacement("the original return value", after);
              }
              return Replacement.NONE;
            });
  }

  private record Replacement(String before, String after, String descriptionPhrase) {
    private static final Replacement NONE = new Replacement(null, null, "");

    private Replacement(String before, String after) {
      this(before, after, "");
    }
  }

  private static VerificationSafeguard safeguard(
      String name,
      EvidenceVerdict verdict,
      VerificationSafeguardOutcome outcome,
      VerificationSafeguardReason reason,
      String explanation,
      String technicalGate) {
    return new VerificationSafeguard(name, verdict, outcome, reason, explanation, technicalGate);
  }

  private static VerificationSafeguard absentSafeguard(
      String name,
      EvidenceGate gate,
      String technicalGate,
      VerificationSafeguardReason absentReason,
      String availableExplanation) {
    if (gate.verdict() == EvidenceVerdict.DISABLED
        || gate.verdict() == EvidenceVerdict.INCOMPLETE
        || gate.verdict() == EvidenceVerdict.NOT_APPLICABLE) {
      VerificationSafeguardOutcome outcome =
          gate.verdict() == EvidenceVerdict.DISABLED
              ? VerificationSafeguardOutcome.DISABLED
              : gate.verdict() == EvidenceVerdict.NOT_APPLICABLE
                  ? VerificationSafeguardOutcome.NOT_APPLICABLE
                  : VerificationSafeguardOutcome.UNABLE_TO_ASSESS;
      VerificationSafeguardReason presentationReason =
          absentReason == VerificationSafeguardReason.MUTATION_BASELINE_FAILED
                  || absentReason == VerificationSafeguardReason.MUTATION_EVIDENCE_UNAVAILABLE
              ? absentReason
              : VerificationSafeguardReason.GATE_RECORDED;
      return safeguard(
          name, gate.verdict(), outcome, presentationReason, reason(gate), technicalGate);
    }
    return safeguard(
        name,
        EvidenceVerdict.NOT_APPLICABLE,
        VerificationSafeguardOutcome.NOT_APPLICABLE,
        absentReason,
        availableExplanation,
        technicalGate);
  }

  private static EvidenceGate gate(List<EvidenceGate> gates, String name) {
    return gates.stream()
        .filter(item -> name.equals(item.name()))
        .findFirst()
        .orElse(
            new EvidenceGate(
                name, EvidenceVerdict.INCOMPLETE, "Current-run evidence is unavailable."));
  }

  private static String reason(EvidenceGate gate) {
    return gate.reason() == null || gate.reason().isBlank()
        ? "Current-run evidence did not establish this safeguard."
        : gate.reason();
  }

  /** Adds run identity and counts after all case and Gate evidence is available. */
  public static VerificationView withRun(
      VerificationView view, String runId, Instant startedAt, Instant finishedAt) {
    int failedGates =
        (int) view.gates().stream().filter(gate -> gate.verdict() == EvidenceVerdict.FAIL).count();
    int incompleteGates =
        (int)
            view.gates().stream()
                .filter(gate -> gate.verdict() == EvidenceVerdict.INCOMPLETE)
                .count();
    int failedAcs =
        (int)
            view.acceptanceConditions().stream()
                .filter(ac -> ac.status() == CaseResultStatus.FAIL)
                .count();
    int failedCases =
        (int)
            view.acceptanceConditions().stream()
                .flatMap(ac -> ac.cases().stream())
                .filter(testCase -> testCase.status() == CaseResultStatus.FAIL)
                .count();
    int passedAcs =
        (int)
            view.acceptanceConditions().stream()
                .filter(ac -> ac.status() == CaseResultStatus.PASS)
                .count();
    int incompleteAcs =
        (int)
            view.acceptanceConditions().stream()
                .filter(ac -> ac.status() == CaseResultStatus.NOT_REPORTED)
                .count();
    return new VerificationView(
        VerificationView.SCHEMA_VERSION,
        view.generatedAt(),
        view.verdict(),
        view.expectedConsumptionEnforced(),
        view.gates(),
        view.acceptanceConditions(),
        view.deliveryScope(),
        view.mutationAttribution(),
        new VerificationRunSummary(
            runId,
            startedAt,
            finishedAt,
            failedGates,
            incompleteGates,
            failedAcs,
            failedCases,
            passedAcs,
            incompleteAcs));
  }

  private static CaseResultStatus suiteVerdict(
      List<VerificationAcceptanceCondition> acs, List<EvidenceGate> gates) {
    boolean failedGate = gates.stream().anyMatch(gate -> gate.verdict() == EvidenceVerdict.FAIL);
    boolean failedCase = acs.stream().anyMatch(ac -> ac.status() == CaseResultStatus.FAIL);
    if (failedGate || failedCase) {
      return CaseResultStatus.FAIL;
    }
    boolean incompleteGate =
        gates.stream()
            .anyMatch(
                gate ->
                    gate.verdict() != EvidenceVerdict.PASS
                        && gate.verdict() != EvidenceVerdict.DISABLED
                        && gate.verdict() != EvidenceVerdict.NOT_APPLICABLE);
    boolean completeCases =
        !acs.isEmpty() && acs.stream().allMatch(ac -> ac.status() == CaseResultStatus.PASS);
    return !incompleteGate && completeCases ? CaseResultStatus.PASS : CaseResultStatus.NOT_REPORTED;
  }

  /**
   * Builds the document-first reviewer projection from selected Markdown and executable material.
   */
  public static ReviewView review(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      List<ReviewDocument> documents,
      Map<String, ReviewAcLocation> locations,
      Map<String, ReviewMethod> methods,
      Map<String, List<StepTemplate>> templates,
      Instant generatedAt,
      DeliveryScope deliveryScope) {
    Map<String, List<ToppleCaseData>> byAc = group(cases);
    java.util.TreeSet<String> acIds = new java.util.TreeSet<>();
    acIds.addAll(titles.keySet());
    acIds.addAll(byAc.keySet());
    acIds.addAll(locations.keySet());
    acIds.addAll(methods.keySet());
    List<ReviewAcceptanceCondition> acceptanceConditions = new ArrayList<>();
    for (String acId : acIds) {
      List<ReviewCase> rows =
          byAc.getOrDefault(acId, List.of()).stream()
              .map(
                  row ->
                      new ReviewCase(
                          row.visibility(),
                          row.caseId(),
                          row.inputs(),
                          row.expected(),
                          ReviewScenarioResolver.resolve(
                              templates.getOrDefault(acId, List.of()),
                              row.inputs(),
                              row.expected())))
              .sorted(
                  Comparator.comparing(
                          (ReviewCase row) -> row.visibility() != CaseVisibility.PUBLIC)
                      .thenComparing(ReviewCase::caseId))
              .toList();
      acceptanceConditions.add(
          new ReviewAcceptanceCondition(
              acId,
              title(titles, acId),
              locations.getOrDefault(acId, ReviewAcLocation.unavailable()),
              rows,
              methods.getOrDefault(acId, new ReviewMethod(List.of(), ""))));
    }
    return new ReviewView(
        ReviewView.SCHEMA_VERSION,
        generatedAt,
        documents,
        acceptanceConditions,
        deliveryScope,
        List.of());
  }

  /** Adds static Property source cards to a reviewer-only review projection. */
  public static ReviewView withReviewProperties(
      ReviewView view, Map<String, List<ReviewProperty>> properties) {
    List<ReviewAcceptanceCondition> acceptanceConditions =
        view.acceptanceConditions().stream()
            .map(
                ac ->
                    new ReviewAcceptanceCondition(
                        ac.acId(),
                        ac.title(),
                        ac.location(),
                        ac.cases(),
                        ac.method(),
                        properties.getOrDefault(ac.acId(), List.of())))
            .toList();
    return new ReviewView(
        ReviewView.SCHEMA_VERSION,
        view.generatedAt(),
        view.selectedSpecDocuments(),
        acceptanceConditions,
        view.deliveryScope(),
        view.contractQualityAdvisories());
  }

  /** Adds reviewer-only, non-blocking expected-output observations to Spec Review. */
  public static ReviewView withContractQualityAdvisories(
      ReviewView view, List<io.github.samzhu.topplecat.core.ContractQualityAdvisory> advisories) {
    return new ReviewView(
        ReviewView.SCHEMA_VERSION,
        view.generatedAt(),
        view.selectedSpecDocuments(),
        view.acceptanceConditions(),
        view.deliveryScope(),
        advisories);
  }

  private static Map<String, List<ToppleCaseData>> group(List<ToppleCaseData> cases) {
    Map<String, List<ToppleCaseData>> groups = new java.util.TreeMap<>();
    for (ToppleCaseData testCase :
        cases.stream().sorted(Comparator.comparing(ToppleCaseData::caseId)).toList()) {
      groups.computeIfAbsent(testCase.acId(), ignored -> new ArrayList<>()).add(testCase);
    }
    return groups;
  }

  private static String title(Map<String, String> titles, String acId) {
    return titles.getOrDefault(acId, acId);
  }

  /** Per-case test execution data available only to the reviewer projection. */
  public record CaseExecution(
      CaseResultStatus status,
      String failure,
      List<NarrativeStep> steps,
      Map<String, String> expectedConsumption) {
    public CaseExecution {
      steps = List.copyOf(steps == null ? List.of() : steps);
      expectedConsumption =
          Map.copyOf(expectedConsumption == null ? Map.of() : expectedConsumption);
    }

    public CaseExecution(CaseResultStatus status, String failure, List<NarrativeStep> steps) {
      this(status, failure, steps, Map.of());
    }

    public static CaseExecution notReported() {
      return new CaseExecution(CaseResultStatus.NOT_REPORTED, null, List.of(), Map.of());
    }
  }
}
