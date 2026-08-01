package io.github.samzhu.topplecat.report;

import java.util.List;

/** Reviewer-only, non-execution projection of one acceptance condition. */
public record ReviewAcceptanceCondition(
    String acId,
    String title,
    ReviewAcLocation location,
    List<ReviewCase> cases,
    ReviewMethod method,
    List<ReviewProperty> properties) {
  public ReviewAcceptanceCondition {
    cases = List.copyOf(cases);
    location = location == null ? ReviewAcLocation.unavailable() : location;
    method = method == null ? new ReviewMethod(List.of(), "") : method;
    properties = List.copyOf(properties == null ? List.of() : properties);
  }

  public ReviewAcceptanceCondition(
      String acId,
      String title,
      ReviewAcLocation location,
      List<ReviewCase> cases,
      ReviewMethod method) {
    this(acId, title, location, cases, method, List.of());
  }

  public ReviewAcceptanceCondition(
      String acId, String title, List<ReviewCase> cases, ReviewMethod method) {
    this(acId, title, ReviewAcLocation.unavailable(), cases, method, List.of());
  }
}
