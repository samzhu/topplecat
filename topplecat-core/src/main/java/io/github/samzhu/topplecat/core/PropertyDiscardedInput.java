package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** One generator choice rejected before it became a completed Property trial. */
public record PropertyDiscardedInput(String choicesJson) {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  public PropertyDiscardedInput {
    if (choicesJson == null || choicesJson.isBlank()) {
      throw new ToppleCatException("Discarded Property input requires canonical JSON choices.");
    }
    try {
      JSON.readTree(choicesJson);
    } catch (RuntimeException exception) {
      throw new ToppleCatException("Discarded Property input requires canonical JSON choices.");
    }
  }
}
