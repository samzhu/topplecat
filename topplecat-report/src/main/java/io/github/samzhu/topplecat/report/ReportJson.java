package io.github.samzhu.topplecat.report;

import tools.jackson.databind.json.JsonMapper;

/** Current JSON codecs for the reviewer-only Review and Verification projections. */
public final class ReportJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ReportJson() {}

  public static String writeVerification(VerificationView view) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(view) + "\n";
  }

  public static String writeReview(ReviewView view) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(view) + "\n";
  }

  public static VerificationView readVerification(String source) {
    return JSON.readValue(source, VerificationView.class);
  }

  public static ReviewView readReview(String source) {
    return JSON.readValue(source, ReviewView.class);
  }

  public static String writeSelectedSpecProjection(SelectedSpecProjection projection) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(projection) + "\n";
  }

  public static SelectedSpecProjection readSelectedSpecProjection(String source) {
    return JSON.readValue(source, SelectedSpecProjection.class);
  }
}
