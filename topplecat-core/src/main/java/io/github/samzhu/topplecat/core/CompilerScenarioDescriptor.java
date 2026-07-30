package io.github.samzhu.topplecat.core;

import java.util.List;
import java.util.Objects;

/**
 * One javac-emitted descriptor. It contains no case values and can therefore be safely produced
 * during compilation before the Gradle plugin joins it with public/reviewer data.
 */
public record CompilerScenarioDescriptor(
    String schemaVersion,
    String acId,
    String title,
    String scenarioId,
    String declaringBinaryName,
    String methodName,
    String methodDescriptor,
    SourceRef sourceRef,
    List<StepTemplate> steps,
    int scenarioParameterIndex,
    List<ScenarioStage> stageParameters) {
  public static final String SCHEMA_VERSION = "topplecat.compiler-scenario.v2";

  public CompilerScenarioDescriptor(
      String schemaVersion,
      String acId,
      String title,
      String scenarioId,
      String declaringBinaryName,
      String methodName,
      String methodDescriptor,
      SourceRef sourceRef,
      List<StepTemplate> steps) {
    this(
        schemaVersion,
        acId,
        title,
        scenarioId,
        declaringBinaryName,
        methodName,
        methodDescriptor,
        sourceRef,
        steps,
        -1,
        List.of());
  }

  public CompilerScenarioDescriptor {
    if (!SCHEMA_VERSION.equals(schemaVersion)) {
      throw new ToppleCatException("Unsupported compiler scenario schema: " + schemaVersion);
    }
    requireText(acId, "acId");
    requireText(title, "title");
    requireText(scenarioId, "scenarioId");
    requireText(declaringBinaryName, "declaringBinaryName");
    requireText(methodName, "methodName");
    requireText(methodDescriptor, "methodDescriptor");
    sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
    steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
    stageParameters = List.copyOf(stageParameters == null ? List.of() : stageParameters);
    if (steps.isEmpty()) {
      throw new ToppleCatException(
          "Compiler scenario " + scenarioId + " requires at least one step.");
    }
    if (scenarioParameterIndex < -1) {
      throw new ToppleCatException("Compiler scenario parameter index is invalid.");
    }
    if (scenarioParameterIndex == -1 && !stageParameters.isEmpty()) {
      throw new ToppleCatException(
          "Legacy compiler scenarios cannot declare Scenario Stage parameters.");
    }
    if (scenarioParameterIndex >= 0 && stageParameters.isEmpty()) {
      throw new ToppleCatException(
          "A new Scenario requires at least one concrete Stage parameter.");
    }
    if (stageParameters.stream().map(ScenarioStage::parameterIndex).distinct().count()
        != stageParameters.size()) {
      throw new ToppleCatException(
          "A Scenario cannot declare duplicate Stage parameter positions.");
    }
    if (stageParameters.stream().map(ScenarioStage::stageBinaryName).distinct().count()
        != stageParameters.size()) {
      throw new ToppleCatException("A Scenario cannot declare the same concrete Stage type twice.");
    }
  }

  public ScenarioTemplate scenario() {
    return new ScenarioTemplate(
        scenarioId,
        declaringBinaryName + "#" + methodName + methodDescriptor,
        sourceRef,
        steps,
        scenarioParameterIndex,
        stageParameters);
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("Compiler scenario " + field + " is required.");
    }
  }
}
