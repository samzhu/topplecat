package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.EvidenceGate;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.ScenarioTemplateRenderer;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCatException;
import io.github.samzhu.topplecat.pitest.PitMutationAttribution;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Factories that enforce the information boundary before rendering begins. */
public final class ReportViews {
  private ReportViews() {}

  public static SpecView spec(
      Map<String, String> titles, List<ToppleCaseData> cases, Instant generatedAt) {
    return spec(titles, cases, Map.of(), Map.of(), generatedAt);
  }

  public static SpecView spec(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      Instant generatedAt) {
    return spec(titles, cases, specNarratives, Map.of(), generatedAt);
  }

  public static SpecView spec(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      Map<String, List<String>> scenarios,
      Instant generatedAt) {
    if (cases.stream().anyMatch(testCase -> testCase.visibility() != CaseVisibility.PUBLIC)) {
      throw new ToppleCatException(
          "Spec view accepts public cases only. Hidden case data must never enter this model.");
    }
    Map<String, List<ToppleCaseData>> byAc = group(cases);
    List<SpecAcceptanceCondition> acs = new ArrayList<>();
    byAc.forEach(
        (acId, rows) ->
            acs.add(
                new SpecAcceptanceCondition(
                    acId,
                    title(titles, acId),
                    scenarios.getOrDefault(acId, List.of()),
                    rows.stream()
                        .map(row -> new SpecCase(row.caseId(), row.inputs(), row.expected()))
                        .toList(),
                    specNarratives.getOrDefault(acId, List.of()))));
    return new SpecView(SpecView.SCHEMA_VERSION, generatedAt, acs);
  }

  /** Adds only public static Property metadata to the agent-safe Spec projection. */
  public static SpecView withSpecProperties(
      SpecView view, Map<String, List<SpecProperty>> properties) {
    List<SpecAcceptanceCondition> acceptanceConditions =
        view.acceptanceConditions().stream()
            .map(
                ac ->
                    new SpecAcceptanceCondition(
                        ac.acId(),
                        ac.title(),
                        ac.scenario(),
                        ac.publicCases(),
                        ac.specNarrative(),
                        properties.getOrDefault(ac.acId(), List.of())))
            .toList();
    return new SpecView(SpecView.SCHEMA_VERSION, view.generatedAt(), acceptanceConditions);
  }

  public static VerificationView verification(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      Instant generatedAt) {
    return verification(
        titles, cases, executions, Map.of(), Map.of(), true, List.of(), generatedAt);
  }

  public static VerificationView verification(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      Instant generatedAt) {
    return verification(
        titles, cases, executions, specNarratives, Map.of(), true, List.of(), generatedAt);
  }

  public static VerificationView verification(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      boolean expectedConsumptionEnforced,
      List<EvidenceGate> gates,
      Instant generatedAt) {
    return verification(
        titles,
        cases,
        executions,
        specNarratives,
        Map.of(),
        expectedConsumptionEnforced,
        gates,
        generatedAt);
  }

  public static VerificationView verification(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, CaseExecution> executions,
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      Map<String, List<String>> scenarios,
      boolean expectedConsumptionEnforced,
      List<EvidenceGate> gates,
      Instant generatedAt) {
    return verificationWithSources(
        titles,
        cases,
        executions,
        specNarratives,
        scenarios,
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
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      Map<String, List<StepTemplate>> scenarios,
      boolean expectedConsumptionEnforced,
      List<EvidenceGate> gates,
      Instant generatedAt) {
    return verificationFromTemplates(
        titles,
        cases,
        executions,
        specNarratives,
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
      Map<String, List<SpecMarkdownBlock>> specNarratives,
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
        specNarratives,
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
      Map<String, List<SpecMarkdownBlock>> specNarratives,
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
              specNarratives.getOrDefault(entry.getKey(), List.of()),
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
                      ac.specNarrative(),
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
        view.mutationAttribution());
  }

  /** Adds reviewer-only raw PIT attribution after the mutation producer result is available. */
  public static VerificationView withMutationAttribution(
      VerificationView view, PitMutationAttribution mutationAttribution) {
    return new VerificationView(
        VerificationView.SCHEMA_VERSION,
        view.generatedAt(),
        view.verdict(),
        view.expectedConsumptionEnforced(),
        view.gates(),
        view.acceptanceConditions(),
        view.deliveryScope(),
        mutationAttribution);
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

  /** Builds a reviewer-only source projection without case outcomes or other execution state. */
  public static ReviewView review(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      Map<String, ReviewMethod> methods,
      Instant generatedAt) {
    return review(titles, cases, specNarratives, methods, Map.of(), generatedAt);
  }

  /** Builds reviewer-only per-case scenarios from compiler templates and typed row values. */
  public static ReviewView review(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      Map<String, ReviewMethod> methods,
      Map<String, List<StepTemplate>> templates,
      Instant generatedAt) {
    return review(titles, cases, specNarratives, methods, templates, generatedAt, null);
  }

  /** Builds reviewer-only per-case scenarios and the selected delivery-scope projection. */
  public static ReviewView review(
      Map<String, String> titles,
      List<ToppleCaseData> cases,
      Map<String, List<SpecMarkdownBlock>> specNarratives,
      Map<String, ReviewMethod> methods,
      Map<String, List<StepTemplate>> templates,
      Instant generatedAt,
      DeliveryScope deliveryScope) {
    Map<String, List<ToppleCaseData>> byAc = group(cases);
    java.util.TreeSet<String> acIds = new java.util.TreeSet<>();
    acIds.addAll(titles.keySet());
    acIds.addAll(byAc.keySet());
    acIds.addAll(specNarratives.keySet());
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
              rows,
              specNarratives.getOrDefault(acId, List.of()),
              methods.getOrDefault(acId, new ReviewMethod(List.of(), ""))));
    }
    return new ReviewView(
        ReviewView.SCHEMA_VERSION, generatedAt, acceptanceConditions, deliveryScope, List.of());
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
                        ac.cases(),
                        ac.specNarrative(),
                        ac.method(),
                        properties.getOrDefault(ac.acId(), List.of())))
            .toList();
    return new ReviewView(
        ReviewView.SCHEMA_VERSION,
        view.generatedAt(),
        acceptanceConditions,
        view.deliveryScope(),
        view.contractQualityAdvisories());
  }

  /** Adds reviewer-only, non-blocking expected-output observations to Contract Review. */
  public static ReviewView withContractQualityAdvisories(
      ReviewView view, List<io.github.samzhu.topplecat.core.ContractQualityAdvisory> advisories) {
    return new ReviewView(
        ReviewView.SCHEMA_VERSION,
        view.generatedAt(),
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
