package io.github.samzhu.topplecat.core;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Builds reviewer diagnostics from the same mathematical JSON equality used by typed case rows.
 *
 * <p>Paths begin with {@code expected.&lt;key&gt;}; object fields use a dot when safe and bracket
 * notation otherwise, while arrays always use indexed paths.
 */
public final class JsonContractComparison {
  private JsonContractComparison() {}

  public static ExpectedActualComparison compare(
      String expectedKey, JsonNode expected, JsonNode actual) {
    List<ExpectedActualDifference> differences = new ArrayList<>();
    compareAt("expected." + fieldPath(expectedKey), expected, actual, differences);
    return new ExpectedActualComparison(expectedKey, differences);
  }

  /** Preserves existing JSON numeric semantics: 200, 200.0, and 200.00 are equal. */
  public static boolean equivalent(JsonNode expected, JsonNode actual) {
    if (expected == actual) {
      return true;
    }
    if (expected == null || actual == null) {
      return false;
    }
    if (expected.isNumber() && actual.isNumber()) {
      return expected.decimalValue().compareTo(actual.decimalValue()) == 0;
    }
    if (expected.isObject() && actual.isObject()) {
      if (expected.size() != actual.size()) {
        return false;
      }
      for (String name : expected.propertyNames()) {
        if (!equivalent(expected.get(name), actual.get(name))) {
          return false;
        }
      }
      return true;
    }
    if (expected.isArray() && actual.isArray()) {
      if (expected.size() != actual.size()) {
        return false;
      }
      for (int index = 0; index < expected.size(); index++) {
        if (!equivalent(expected.get(index), actual.get(index))) {
          return false;
        }
      }
      return true;
    }
    return expected.equals(actual);
  }

  private static void compareAt(
      String path, JsonNode expected, JsonNode actual, List<ExpectedActualDifference> differences) {
    if (expected == null) {
      differences.add(
          new ExpectedActualDifference(
              path, ExpectedActualDifference.Kind.UNEXPECTED_ACTUAL, null, actual));
      return;
    }
    if (actual == null) {
      differences.add(
          new ExpectedActualDifference(
              path, ExpectedActualDifference.Kind.MISSING_EXPECTED, expected, null));
      return;
    }
    if (expected.isObject() && actual.isObject()) {
      expected.propertyNames().stream()
          .sorted()
          .forEach(
              name ->
                  compareAt(
                      path + "." + fieldPath(name),
                      expected.get(name),
                      actual.get(name),
                      differences));
      actual.propertyNames().stream()
          .filter(name -> expected.get(name) == null)
          .sorted()
          .forEach(
              name ->
                  differences.add(
                      new ExpectedActualDifference(
                          path + "." + fieldPath(name),
                          ExpectedActualDifference.Kind.UNEXPECTED_ACTUAL,
                          null,
                          actual.get(name))));
      return;
    }
    if (expected.isArray() && actual.isArray()) {
      int common = Math.min(expected.size(), actual.size());
      for (int index = 0; index < common; index++) {
        compareAt(path + "[" + index + "]", expected.get(index), actual.get(index), differences);
      }
      for (int index = common; index < expected.size(); index++) {
        differences.add(
            new ExpectedActualDifference(
                path + "[" + index + "]",
                ExpectedActualDifference.Kind.MISSING_EXPECTED,
                expected.get(index),
                null));
      }
      for (int index = common; index < actual.size(); index++) {
        differences.add(
            new ExpectedActualDifference(
                path + "[" + index + "]",
                ExpectedActualDifference.Kind.UNEXPECTED_ACTUAL,
                null,
                actual.get(index)));
      }
      return;
    }
    if (!equivalent(expected, actual)) {
      differences.add(
          new ExpectedActualDifference(
              path, ExpectedActualDifference.Kind.CHANGED, expected, actual));
    }
  }

  private static String fieldPath(String field) {
    if (field != null && field.matches("[A-Za-z_$][A-Za-z0-9_$-]*")) {
      return field;
    }
    String escaped = field == null ? "" : field.replace("\\", "\\\\").replace("'", "\\'");
    return "['" + escaped + "']";
  }
}
