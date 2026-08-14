package io.github.samzhu.topplecat.report;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable reviewer-only projection persisted by Check and consumed by Review. */
public record SelectedSpecProjection(
    String schemaVersion,
    List<ReviewDocument> selectedSpecDocuments,
    Map<String, ReviewAcLocation> acceptanceLocations,
    Map<String, String> acceptanceMethodSources,
    Map<String, String> propertySources) {
  public static final String SCHEMA_VERSION = "topplecat.selected-spec-projection.v2";

  public SelectedSpecProjection(
      String schemaVersion,
      List<ReviewDocument> selectedSpecDocuments,
      Map<String, ReviewAcLocation> acceptanceLocations) {
    this(schemaVersion, selectedSpecDocuments, acceptanceLocations, Map.of(), Map.of());
  }

  public SelectedSpecProjection {
    if (!SCHEMA_VERSION.equals(schemaVersion)) {
      throw new IllegalArgumentException("Unsupported selected-Spec projection: " + schemaVersion);
    }
    selectedSpecDocuments =
        List.copyOf(selectedSpecDocuments == null ? List.of() : selectedSpecDocuments);
    Map<String, ReviewAcLocation> sorted = new java.util.TreeMap<>();
    if (acceptanceLocations != null) {
      sorted.putAll(acceptanceLocations);
    }
    acceptanceLocations = Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    acceptanceMethodSources = sortedSources(acceptanceMethodSources);
    propertySources = sortedSources(propertySources);
  }

  public List<String> acceptanceConditionIds() {
    return acceptanceLocations.keySet().stream().sorted().toList();
  }

  private static Map<String, String> sortedSources(Map<String, String> values) {
    Map<String, String> sorted = new java.util.TreeMap<>();
    if (values != null) sorted.putAll(values);
    return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
  }
}
