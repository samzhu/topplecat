package io.github.samzhu.topplecat.report;

import java.time.Instant;
import java.util.List;

/** Reviewer-only authoring review model. It deliberately contains no execution state. */
public record ReviewView(String schemaVersion, Instant generatedAt, List<ReviewAcceptanceCondition> acceptanceConditions) {
    public static final String SCHEMA_VERSION = "topplecat.review-view.v1";

    public ReviewView {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported review-view schema: " + schemaVersion);
        }
        acceptanceConditions = List.copyOf(acceptanceConditions);
    }
}
