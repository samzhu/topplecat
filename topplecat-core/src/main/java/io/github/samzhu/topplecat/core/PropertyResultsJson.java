package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec for current-run Property result artifacts. */
public final class PropertyResultsJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private PropertyResultsJson() {}

  public static String write(PropertyResults results) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(results) + "\n";
  }

  public static PropertyResults read(String source) {
    return JSON.readValue(source, PropertyResults.class);
  }
}
