package io.github.samzhu.topplecat.core;

import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/** Canonical generator-choice presentation and deterministic shrink path for a failing trial. */
public record PropertyCounterexample(String choicesJson, List<Integer> shrinkPath) {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  public PropertyCounterexample {
    if (choicesJson == null || choicesJson.isBlank() || !validJson(choicesJson)) {
      throw new ToppleCatException("Property counterexample requires canonical JSON choices.");
    }
    shrinkPath = List.copyOf(shrinkPath == null ? List.of() : shrinkPath);
    if (shrinkPath.stream().anyMatch(index -> index == null || index < 0)) {
      throw new ToppleCatException("Property counterexample shrink path is invalid.");
    }
  }

  private static boolean validJson(String source) {
    try {
      JSON.readTree(source);
      return true;
    } catch (RuntimeException ignored) {
      return false;
    }
  }
}
