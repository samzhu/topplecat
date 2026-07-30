package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.NarrativeStep;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** Reviewer-only case projection, including inputs and execution evidence. */
public record VerificationCase(
    String caseId,
    CaseVisibility visibility,
    JsonNode inputs,
    JsonNode expected,
    CaseResultStatus status,
    Map<String, String> expectedConsumption,
    List<NarrativeStep> steps,
    String failure) {
  public VerificationCase {
    inputs = inputs.deepCopy();
    expected = expected.deepCopy();
    expectedConsumption = Map.copyOf(expectedConsumption);
    steps = List.copyOf(steps == null ? List.of() : steps);
  }
}
