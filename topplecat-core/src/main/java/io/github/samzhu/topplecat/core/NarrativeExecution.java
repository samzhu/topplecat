package io.github.samzhu.topplecat.core;

import java.util.List;

/** Case-scoped, reviewer-only execution sidecar tied to one compiled definition digest. */
public record NarrativeExecution(
    String definitionDigest, String caseId, List<NarrativeStep> steps) {
  public NarrativeExecution {
    definitionDigest = definitionDigest == null ? "" : definitionDigest;
    if (caseId == null || caseId.isBlank()) {
      throw new ToppleCatException("Narrative execution caseId is required.");
    }
    steps = List.copyOf(steps == null ? List.of() : steps);
  }

  public NarrativeExecution(String caseId, List<NarrativeStep> steps) {
    this("", caseId, steps);
  }
}
