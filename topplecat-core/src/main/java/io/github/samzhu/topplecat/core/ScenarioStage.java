package io.github.samzhu.topplecat.core;

/** One concrete Stage parameter declared by a compiler-described Scenario. */
public record ScenarioStage(int parameterIndex, String stageBinaryName) {
  public ScenarioStage {
    if (parameterIndex < 2) {
      throw new ToppleCatException(
          "A Scenario Stage parameter must follow ToppleCase and ToppleScenario.");
    }
    if (stageBinaryName == null || stageBinaryName.isBlank()) {
      throw new ToppleCatException("A Scenario Stage binary name is required.");
    }
  }
}
