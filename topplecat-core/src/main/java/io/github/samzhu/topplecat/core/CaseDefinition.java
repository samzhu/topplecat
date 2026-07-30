package io.github.samzhu.topplecat.core;

import java.util.Objects;
import tools.jackson.databind.JsonNode;

/** Case data copied into the definition without its reviewer filesystem source path. */
public record CaseDefinition(
    String caseId, String acId, CaseVisibility visibility, JsonNode inputs, JsonNode expected) {
  public CaseDefinition {
    if (caseId == null || caseId.isBlank() || acId == null || acId.isBlank()) {
      throw new ToppleCatException("Case definition requires caseId and acId.");
    }
    visibility = Objects.requireNonNull(visibility, "visibility");
    if (inputs == null || !inputs.isObject() || expected == null || !expected.isObject()) {
      throw new ToppleCatException(
          "Case definition " + caseId + " requires object inputs and expected values.");
    }
    inputs = inputs.deepCopy();
    expected = expected.deepCopy();
  }
}
