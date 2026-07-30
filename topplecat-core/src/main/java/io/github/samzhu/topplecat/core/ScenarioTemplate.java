package io.github.samzhu.topplecat.core;

import java.util.List;
import java.util.Objects;

/** Compiler-resolved shape of one {@code @ToppleAcceptanceTest} method. */
public record ScenarioTemplate(
    String scenarioId,
    String acceptanceTestMethodIdentity,
    SourceRef sourceRef,
    List<StepTemplate> steps,
    int scenarioParameterIndex,
    List<ScenarioStage> stageParameters) {
  public ScenarioTemplate(
      String scenarioId,
      String acceptanceTestMethodIdentity,
      SourceRef sourceRef,
      List<StepTemplate> steps) {
    this(scenarioId, acceptanceTestMethodIdentity, sourceRef, steps, -1, List.of());
  }

  public ScenarioTemplate {
    requireText(scenarioId, "scenarioId");
    requireText(acceptanceTestMethodIdentity, "acceptanceTestMethodIdentity");
    sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
    steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
    stageParameters = List.copyOf(stageParameters == null ? List.of() : stageParameters);
    if (steps.isEmpty()) {
      throw new ToppleCatException("Scenario " + scenarioId + " requires at least one Stage step.");
    }
    if (scenarioParameterIndex < -1) {
      throw new ToppleCatException("Scenario parameter index is invalid.");
    }
    if (scenarioParameterIndex == -1 && !stageParameters.isEmpty()) {
      throw new ToppleCatException("Legacy Scenario templates cannot declare Stage parameters.");
    }
    if (scenarioParameterIndex >= 0 && stageParameters.isEmpty()) {
      throw new ToppleCatException("A new Scenario template requires concrete Stage parameters.");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("Scenario " + field + " is required.");
    }
  }
}
