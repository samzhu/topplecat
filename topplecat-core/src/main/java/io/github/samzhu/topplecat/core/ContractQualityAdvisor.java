package io.github.samzhu.topplecat.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Finds reviewer-only expected-output patterns without inferring a business rule or changing the
 * executable contract.
 */
public final class ContractQualityAdvisor {
  private static final String EXPECTED_ROOT = "expected";

  private ContractQualityAdvisor() {}

  /** Returns fixed-order advisories with no values, case identifiers, paths, or failures. */
  public static List<ContractQualityAdvisory> analyze(Collection<CaseDefinition> cases) {
    Map<String, List<CaseDefinition>> byAc = new HashMap<>();
    for (CaseDefinition row : cases == null ? List.<CaseDefinition>of() : cases) {
      if (row != null) {
        byAc.computeIfAbsent(row.acId(), ignored -> new ArrayList<>()).add(row);
      }
    }
    List<ContractQualityAdvisory> advisories = new ArrayList<>();
    for (String acId : byAc.keySet().stream().sorted().toList()) {
      List<CaseDefinition> publicRows =
          byAc.get(acId).stream()
              .filter(row -> row.visibility() == CaseVisibility.PUBLIC)
              .sorted(Comparator.comparing(CaseDefinition::caseId))
              .toList();
      List<CaseDefinition> hiddenRows =
          byAc.get(acId).stream()
              .filter(row -> row.visibility() == CaseVisibility.HIDDEN)
              .sorted(Comparator.comparing(CaseDefinition::caseId))
              .toList();
      addShapeAdvisory(advisories, acId, publicRows, hiddenRows);
      addIdentifierAdvisories(advisories, acId, publicRows, hiddenRows);
    }
    return advisories.stream().sorted().toList();
  }

  private static void addShapeAdvisory(
      List<ContractQualityAdvisory> advisories,
      String acId,
      List<CaseDefinition> publicRows,
      List<CaseDefinition> hiddenRows) {
    Set<Set<String>> publicVariants = new HashSet<>();
    for (CaseDefinition row : publicRows) {
      publicVariants.add(shape(row.expected()));
    }
    int unmatched =
        (int)
            hiddenRows.stream()
                .map(CaseDefinition::expected)
                .map(ContractQualityAdvisor::shape)
                .filter(hiddenShape -> !publicVariants.contains(hiddenShape))
                .count();
    if (unmatched > 0) {
      advisories.add(
          new ContractQualityAdvisory(
              ContractQualityAdvisory.EXPECTED_SHAPE_VARIANT_MISSING,
              acId,
              EXPECTED_ROOT,
              publicVariants.size(),
              unmatched));
    }
  }

  private static void addIdentifierAdvisories(
      List<ContractQualityAdvisory> advisories,
      String acId,
      List<CaseDefinition> publicRows,
      List<CaseDefinition> hiddenRows) {
    Map<String, Values> byPath = new HashMap<>();
    publicRows.forEach(
        row -> collectOpaqueIdentifierValues(row.expected(), EXPECTED_ROOT, true, byPath));
    hiddenRows.forEach(
        row -> collectOpaqueIdentifierValues(row.expected(), EXPECTED_ROOT, false, byPath));
    byPath.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .filter(values -> values.publicValues.size() >= 2 && values.hiddenValues.size() >= 2)
        .filter(Values::allDistinct)
        .forEach(
            values ->
                advisories.add(
                    new ContractQualityAdvisory(
                        ContractQualityAdvisory.EXPECTED_OPAQUE_IDENTIFIER_LITERALS,
                        acId,
                        values.path,
                        values.publicValues.size(),
                        values.hiddenValues.size())));
  }

  private static Set<String> shape(JsonNode expected) {
    Set<String> fields = new HashSet<>();
    collectShape(expected, EXPECTED_ROOT, fields);
    return Set.copyOf(fields);
  }

  private static void collectShape(JsonNode node, String path, Set<String> fields) {
    if (node == null || !node.isObject() || node.size() == 0) {
      fields.add(path);
      return;
    }
    node.properties().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> collectShape(entry.getValue(), path + "." + entry.getKey(), fields));
  }

  private static void collectOpaqueIdentifierValues(
      JsonNode node, String path, boolean publicRow, Map<String, Values> byPath) {
    if (node == null || !node.isObject()) {
      return;
    }
    node.properties().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              String fieldPath = path + "." + entry.getKey();
              JsonNode value = entry.getValue();
              if (value.isObject()) {
                collectOpaqueIdentifierValues(value, fieldPath, publicRow, byPath);
              } else if (value.isString()
                  && opaqueIdentifierField(entry.getKey())
                  && !value.asString().isBlank()) {
                Values values = byPath.computeIfAbsent(fieldPath, Values::new);
                (publicRow ? values.publicValues : values.hiddenValues).add(value.asString());
              }
              // Arrays are terminal. Their nested fields are intentionally not analyzed.
            });
  }

  private static boolean opaqueIdentifierField(String field) {
    return field.endsWith("Id") || field.endsWith("Key") || field.endsWith("Token");
  }

  private static final class Values {
    private final String path;
    private final List<String> publicValues = new ArrayList<>();
    private final List<String> hiddenValues = new ArrayList<>();

    private Values(String path) {
      this.path = path;
    }

    private boolean allDistinct() {
      List<String> values = new ArrayList<>(publicValues);
      values.addAll(hiddenValues);
      return new HashSet<>(values).size() == values.size();
    }
  }
}
