package io.github.samzhu.topplecat.report;

import tools.jackson.databind.json.JsonMapper;

/** Stable JSON codecs for the Spec, Review, and Verification projections. */
public final class ReportJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ReportJson() {
    }

    public static String writeSpec(SpecView view) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(view) + "\n";
    }

    public static String writeVerification(VerificationView view) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(view) + "\n";
    }

    public static String writeReview(ReviewView view) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(view) + "\n";
    }

    public static SpecView readSpec(String source) {
        return JSON.readValue(source, SpecView.class);
    }

    public static VerificationView readVerification(String source) {
        return JSON.readValue(source, VerificationView.class);
    }

    public static ReviewView readReview(String source) {
        return JSON.readValue(source, ReviewView.class);
    }
}
