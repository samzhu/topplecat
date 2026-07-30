package io.github.samzhu.topplecat.junit;

import tools.jackson.databind.JsonNode;

/** Exact recursive equality for JSON values in a ToppleCat expected contract. */
final class JsonContractEquality {
  private JsonContractEquality() {}

  static boolean equivalent(JsonNode expected, JsonNode actual) {
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
}
