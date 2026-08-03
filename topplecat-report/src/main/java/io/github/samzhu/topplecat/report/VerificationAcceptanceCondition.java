package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import java.util.List;
import java.util.Map;

/** Reviewer-only outcome summary for an acceptance condition. */
public record VerificationAcceptanceCondition(
    String acId,
    String title,
    List<String> scenario,
    CaseResultStatus status,
    List<VerificationCase> cases,
    Map<String, SourceRef> stepSources,
    Map<String, StepPhase> stepPhases,
    List<VerificationProperty> properties,
    List<VerificationSafeguard> safeguards,
    List<VerificationMutationDetail> undetectedMutations) {
  public VerificationAcceptanceCondition(
      String acId,
      String title,
      List<String> scenario,
      CaseResultStatus status,
      List<VerificationCase> cases,
      Map<String, SourceRef> stepSources,
      Map<String, StepPhase> stepPhases,
      List<VerificationProperty> properties) {
    this(
        acId,
        title,
        scenario,
        status,
        cases,
        stepSources,
        stepPhases,
        properties,
        List.of(),
        List.of());
  }

  public VerificationAcceptanceCondition {
    scenario = List.copyOf(scenario == null ? List.of() : scenario);
    cases = List.copyOf(cases);
    stepSources = Map.copyOf(stepSources == null ? Map.of() : stepSources);
    stepPhases = Map.copyOf(stepPhases == null ? Map.of() : stepPhases);
    properties = List.copyOf(properties == null ? List.of() : properties);
    safeguards = List.copyOf(safeguards == null ? List.of() : safeguards);
    undetectedMutations =
        List.copyOf(undetectedMutations == null ? List.of() : undetectedMutations);
  }

  public VerificationAcceptanceCondition(
      String acId,
      String title,
      List<String> scenario,
      CaseResultStatus status,
      List<VerificationCase> cases,
      Map<String, SourceRef> stepSources,
      Map<String, StepPhase> stepPhases) {
    this(
        acId,
        title,
        scenario,
        status,
        cases,
        stepSources,
        stepPhases,
        List.of(),
        List.of(),
        List.of());
  }

  public VerificationAcceptanceCondition(
      String acId,
      String title,
      List<String> scenario,
      CaseResultStatus status,
      List<VerificationCase> cases) {
    this(acId, title, scenario, status, cases, Map.of(), Map.of(), List.of(), List.of(), List.of());
  }

  public VerificationAcceptanceCondition(
      String acId, String title, CaseResultStatus status, List<VerificationCase> cases) {
    this(
        acId, title, List.of(), status, cases, Map.of(), Map.of(), List.of(), List.of(), List.of());
  }

  public VerificationAcceptanceCondition(
      String acId,
      String title,
      List<String> scenario,
      CaseResultStatus status,
      List<VerificationCase> cases,
      Map<String, SourceRef> stepSources) {
    this(
        acId,
        title,
        scenario,
        status,
        cases,
        stepSources,
        Map.of(),
        List.of(),
        List.of(),
        List.of());
  }
}
