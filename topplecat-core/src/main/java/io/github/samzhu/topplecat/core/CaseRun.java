package io.github.samzhu.topplecat.core;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Runtime outcome for one explicit case id. */
public record CaseRun(
    String caseId,
    NarrativeStepStatus status,
    Duration duration,
    List<StepRun> stepRuns,
    Map<String, String> expectedConsumption,
    String failureRef) {
  public CaseRun {
    if (caseId == null || caseId.isBlank()) {
      throw new ToppleCatException("Case run id is required.");
    }
    status = status == null ? NarrativeStepStatus.SKIPPED : status;
    duration = duration == null ? Duration.ZERO : duration;
    stepRuns = List.copyOf(stepRuns == null ? List.of() : stepRuns);
    expectedConsumption = Map.copyOf(expectedConsumption == null ? Map.of() : expectedConsumption);
    failureRef = failureRef == null ? "" : failureRef;
  }
}
