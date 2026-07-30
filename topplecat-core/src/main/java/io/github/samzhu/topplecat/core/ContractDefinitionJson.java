package io.github.samzhu.topplecat.core;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Canonical JSON and digest support for schema-versioned definitions. */
public final class ContractDefinitionJson {
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
          .build();

  private ContractDefinitionJson() {}

  public static String write(ContractDefinition definition) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(definition) + "\n";
  }

  public static ContractDefinition read(String source) {
    try {
      return JSON.readValue(source, ContractDefinition.class);
    } catch (RuntimeException exception) {
      throw domainFailure(exception);
    }
  }

  private static RuntimeException domainFailure(RuntimeException exception) {
    for (Throwable current = exception; current != null; current = current.getCause()) {
      if (current instanceof ToppleCatException domain) {
        return domain;
      }
    }
    return exception;
  }

  static String digest(ContractDefinition definition) {
    List<Map<String, Object>> contracts =
        definition.acceptanceConditions().stream()
            .map(
                contract -> {
                  Map<String, Object> result = new TreeMap<>();
                  result.put("acId", contract.acId());
                  result.put("title", contract.title());
                  result.put("scenarioId", contract.scenario().scenarioId());
                  result.put(
                      "acceptanceTestMethodIdentity",
                      contract.scenario().acceptanceTestMethodIdentity());
                  result.put(
                      "scenarioParameterIndex", contract.scenario().scenarioParameterIndex());
                  result.put("stageParameters", contract.scenario().stageParameters());
                  result.put(
                      "steps",
                      contract.scenario().steps().stream()
                          .map(
                              step ->
                                  Map.of(
                                      "stepId",
                                      step.stepId(),
                                      "stageBinaryName",
                                      step.stageBinaryName(),
                                      "phase",
                                      step.phase().name(),
                                      "tokens",
                                      step.tokens(),
                                      "argumentBindings",
                                      step.argumentBindings()))
                          .toList());
                  result.put(
                      "cases",
                      contract.cases().stream()
                          .map(
                              testCase ->
                                  Map.of(
                                      "caseId",
                                      testCase.caseId(),
                                      "acId",
                                      testCase.acId(),
                                      "visibility",
                                      testCase.visibility().name(),
                                      "inputs",
                                      sorted(testCase.inputs()),
                                      "expected",
                                      sorted(testCase.expected())))
                          .toList());
                  result.put(
                      "properties",
                      contract.properties().stream()
                          .map(
                              property ->
                                  Map.of(
                                      "acId",
                                      property.acId(),
                                      "methodIdentity",
                                      property.methodIdentity(),
                                      "title",
                                      property.title(),
                                      "tries",
                                      property.tries(),
                                      "maxDiscards",
                                      property.maxDiscards(),
                                      "maxShrinks",
                                      property.maxShrinks(),
                                      "sourceDigest",
                                      property.sourceDigest()))
                          .toList());
                  return result;
                })
            .toList();
    return Hashing.sha256(
        JSON.writeValueAsString(
                Map.of(
                    "schemaVersion",
                    ContractDefinition.SCHEMA_VERSION,
                    "acceptanceConditions",
                    contracts))
            .getBytes(StandardCharsets.UTF_8));
  }

  private static JsonNode sorted(JsonNode value) {
    if (value.isObject()) {
      ObjectNode result = JSON.createObjectNode();
      value.propertyNames().stream()
          .sorted()
          .forEach(name -> result.set(name, sorted(value.get(name))));
      return result;
    }
    if (value.isArray()) {
      ArrayNode result = JSON.createArrayNode();
      value.valueStream().forEach(item -> result.add(sorted(item)));
      return result;
    }
    return value.deepCopy();
  }
}
