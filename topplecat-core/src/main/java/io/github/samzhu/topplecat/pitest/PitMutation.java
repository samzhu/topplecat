package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;

import java.util.LinkedHashSet;
import java.util.List;

/** One mutant from a PIT {@code mutations.xml} report. */
public record PitMutation(boolean detected, String status, String mutatedClass, List<String> coveringTests) {
    public PitMutation {
        requireText(status, "status");
        requireText(mutatedClass, "mutatedClass");
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (coveringTests != null) {
            for (String test : coveringTests) {
                if (test != null && !test.isBlank()) {
                    normalized.add(test.trim());
                }
            }
        }
        coveringTests = List.copyOf(normalized);
    }

    /** Returns whether PIT reports this mutant as detected by a test. */
    public boolean killed() {
        return detected || "KILLED".equalsIgnoreCase(status);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ToppleCatException("PIT mutation " + field + " is required.");
        }
    }
}
