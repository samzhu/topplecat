package io.github.samzhu.topplecat.report;

import java.util.List;

/** Safe projection of one acceptance condition. */
public record SpecAcceptanceCondition(String acId, String title, List<String> scenario, List<SpecCase> publicCases,
                                      List<SpecMarkdownBlock> specNarrative) {
    public SpecAcceptanceCondition {
        scenario = List.copyOf(scenario == null ? List.of() : scenario);
        publicCases = List.copyOf(publicCases);
        specNarrative = List.copyOf(specNarrative == null ? List.of() : specNarrative);
    }

    public SpecAcceptanceCondition(String acId, String title, List<SpecCase> publicCases) {
        this(acId, title, List.of(), publicCases, List.of());
    }
}
