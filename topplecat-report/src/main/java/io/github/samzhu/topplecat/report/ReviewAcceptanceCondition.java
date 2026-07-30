package io.github.samzhu.topplecat.report;

import java.util.List;

/** Reviewer-only, non-execution projection of one acceptance condition. */
public record ReviewAcceptanceCondition(
    String acId,
    String title,
    List<ReviewCase> cases,
    List<SpecMarkdownBlock> specNarrative,
    ReviewMethod method,
    List<ReviewProperty> properties) {
  public ReviewAcceptanceCondition {
    cases = List.copyOf(cases);
    specNarrative = List.copyOf(specNarrative == null ? List.of() : specNarrative);
    method = method == null ? new ReviewMethod(List.of(), "") : method;
    properties = List.copyOf(properties == null ? List.of() : properties);
  }

  public ReviewAcceptanceCondition(
      String acId,
      String title,
      List<ReviewCase> cases,
      List<SpecMarkdownBlock> specNarrative,
      ReviewMethod method) {
    this(acId, title, cases, specNarrative, method, List.of());
  }
}
