package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.JsonContractComparison;
import tools.jackson.databind.JsonNode;

/** Exact recursive equality for JSON values in a ToppleCat expected contract. */
final class JsonContractEquality {
  private JsonContractEquality() {}

  static boolean equivalent(JsonNode expected, JsonNode actual) {
    return JsonContractComparison.equivalent(expected, actual);
  }
}
