package io.github.samzhu.topplecat.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reviewer-only runtime record of how one case consumed its declared expected values. */
public record ExpectedConsumptionExecution(String caseId, Map<String, String> expectedConsumption) {
  public ExpectedConsumptionExecution {
    if (caseId == null || caseId.isBlank()) {
      throw new IllegalArgumentException("caseId is required");
    }
    expectedConsumption =
        Map.copyOf(
            new LinkedHashMap<>(expectedConsumption == null ? Map.of() : expectedConsumption));
  }
}
