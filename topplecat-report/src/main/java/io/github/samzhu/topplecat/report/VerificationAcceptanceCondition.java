package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.SourceRef;

import java.util.List;
import java.util.Map;

/** Reviewer-only outcome summary for an acceptance condition. */
public record VerificationAcceptanceCondition(String acId, String title, List<String> scenario, CaseResultStatus status,
                                               List<VerificationCase> cases,
                                               List<SpecMarkdownBlock> specNarrative,
                                               Map<String, SourceRef> stepSources) {
    public VerificationAcceptanceCondition {
        scenario = List.copyOf(scenario == null ? List.of() : scenario);
        cases = List.copyOf(cases);
        specNarrative = List.copyOf(specNarrative == null ? List.of() : specNarrative);
        stepSources = Map.copyOf(stepSources == null ? Map.of() : stepSources);
    }

    public VerificationAcceptanceCondition(String acId, String title, List<String> scenario, CaseResultStatus status,
                                           List<VerificationCase> cases, List<SpecMarkdownBlock> specNarrative) {
        this(acId, title, scenario, status, cases, specNarrative, Map.of());
    }

    public VerificationAcceptanceCondition(String acId, String title, CaseResultStatus status,
                                           List<VerificationCase> cases) {
        this(acId, title, List.of(), status, cases, List.of(), Map.of());
    }
}
