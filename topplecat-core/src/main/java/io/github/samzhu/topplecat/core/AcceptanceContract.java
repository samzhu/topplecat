package io.github.samzhu.topplecat.core;

import java.util.List;
import java.util.Objects;

/** One acceptance condition and its one canonical scenario. */
public record AcceptanceContract(String acId, String title, ScenarioTemplate scenario, List<CaseDefinition> cases) {
    public AcceptanceContract {
        requireText(acId, "acId");
        requireText(title, "title");
        scenario = Objects.requireNonNull(scenario, "scenario");
        cases = cases == null ? List.of() : cases.stream().sorted(java.util.Comparator.comparing(CaseDefinition::caseId)).toList();
        if (cases.stream().anyMatch(testCase -> !acId.equals(testCase.acId()))) {
            throw new ToppleCatException("Every case in " + acId + " must bind to that AC.");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ToppleCatException("Acceptance contract " + field + " is required.");
        }
    }
}
