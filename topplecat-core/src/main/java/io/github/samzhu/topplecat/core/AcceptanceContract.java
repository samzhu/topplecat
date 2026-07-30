package io.github.samzhu.topplecat.core;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** One acceptance condition and its one acceptance scenario. */
public record AcceptanceContract(
    String acId,
    String title,
    ScenarioTemplate scenario,
    List<CaseDefinition> cases,
    List<PropertyDefinition> properties) {
  public AcceptanceContract(
      String acId, String title, ScenarioTemplate scenario, List<CaseDefinition> cases) {
    this(acId, title, scenario, cases, List.of());
  }

  public AcceptanceContract {
    requireText(acId, "acId");
    requireText(title, "title");
    scenario = Objects.requireNonNull(scenario, "scenario");
    cases =
        cases == null
            ? List.of()
            : cases.stream().sorted(Comparator.comparing(CaseDefinition::caseId)).toList();
    if (cases.stream().anyMatch(testCase -> !acId.equals(testCase.acId()))) {
      throw new ToppleCatException("Every case in " + acId + " must bind to that AC.");
    }
    properties =
        properties == null
            ? List.of()
            : properties.stream()
                .sorted(Comparator.comparing(PropertyDefinition::methodIdentity))
                .toList();
    if (properties.stream().anyMatch(property -> !acId.equals(property.acId()))
        || properties.stream().map(PropertyDefinition::methodIdentity).distinct().count()
            != properties.size()) {
      throw new ToppleCatException(
          "Every Property in " + acId + " must bind to that AC with a unique method identity.");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("Acceptance contract " + field + " is required.");
    }
  }
}
