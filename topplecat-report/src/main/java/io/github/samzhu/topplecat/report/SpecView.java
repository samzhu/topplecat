package io.github.samzhu.topplecat.report;

import java.time.Instant;
import java.util.List;

/** Agent-safe report model. It has no reviewer-only fields by design. */
public record SpecView(String schemaVersion, Instant generatedAt, List<SpecAcceptanceCondition> acceptanceConditions) {
    public static final String SCHEMA_VERSION = "topplecat.spec-view.v3";

    public SpecView {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported spec-view schema: " + schemaVersion);
        }
        acceptanceConditions = List.copyOf(acceptanceConditions);
    }
}
