package io.github.samzhu.topplecat.report;

import java.util.List;

/** Safe projection of one acceptance condition. */
public record SpecAcceptanceCondition(
    String acId,
    String title,
    List<String> scenario,
    List<SpecCase> publicCases,
    List<SpecMarkdownBlock> specNarrative,
    List<SpecProperty> properties) {
  public SpecAcceptanceCondition {
    scenario = List.copyOf(scenario == null ? List.of() : scenario);
    publicCases = List.copyOf(publicCases);
    specNarrative = List.copyOf(specNarrative == null ? List.of() : specNarrative);
    properties = List.copyOf(properties == null ? List.of() : properties);
  }

  public SpecAcceptanceCondition(
      String acId,
      String title,
      List<String> scenario,
      List<SpecCase> publicCases,
      List<SpecMarkdownBlock> specNarrative) {
    this(acId, title, scenario, publicCases, specNarrative, List.of());
  }

  public SpecAcceptanceCondition(String acId, String title, List<SpecCase> publicCases) {
    this(acId, title, List.of(), publicCases, List.of());
  }
}
