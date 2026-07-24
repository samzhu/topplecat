package io.github.samzhu.topplecat.pitest;

import java.util.List;

/** Parsed PIT report and whether it supports automatic per-test attribution. */
public record PitMutationReport(List<PitMutation> mutations, boolean coverageMatrix) {
    public PitMutationReport {
        mutations = List.copyOf(mutations == null ? List.of() : mutations);
    }
}
