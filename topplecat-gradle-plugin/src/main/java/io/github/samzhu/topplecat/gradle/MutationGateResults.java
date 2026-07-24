package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Stable reviewer-only mutation-gate artifact written by the Gradle plugin. */
public record MutationGateResults(String schemaVersion, List<PitMutationAssessment> assessments) {
    static final String SCHEMA_VERSION = "topplecat.mutation-results.v1";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public MutationGateResults {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported mutation results schema: " + schemaVersion);
        }
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
    }

    static String write(MutationGateResults results) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(results) + "\n";
    }

    static MutationGateResults read(String source) {
        return JSON.readValue(source, MutationGateResults.class);
    }

    EvidenceVerdict verdict() {
        return assessments.stream().anyMatch(result -> result.verdict() == EvidenceVerdict.FAIL) ? EvidenceVerdict.FAIL
                : assessments.isEmpty() || assessments.stream().anyMatch(result -> result.verdict() == EvidenceVerdict.INCOMPLETE)
                ? EvidenceVerdict.INCOMPLETE : EvidenceVerdict.PASS;
    }
}
