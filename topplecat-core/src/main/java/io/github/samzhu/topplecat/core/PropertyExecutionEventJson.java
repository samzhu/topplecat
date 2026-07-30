package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON-lines codec for independent current-run Property lifecycle evidence. */
public final class PropertyExecutionEventJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private PropertyExecutionEventJson() {}

  public static String writeLine(PropertyExecutionEvent event) {
    return JSON.writeValueAsString(event) + "\n";
  }

  public static PropertyExecutionEvent readLine(String source) {
    return JSON.readValue(source, PropertyExecutionEvent.class);
  }
}
