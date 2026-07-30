package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/**
 * JSON codec for the selected executable Spec scope shared between Gradle and JUnit runtime tasks.
 */
public final class SelectedSpecScopeJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private SelectedSpecScopeJson() {}

  public static String write(SelectedSpecScope scope) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(scope) + "\n";
  }

  public static SelectedSpecScope read(String source) {
    return JSON.readValue(source, SelectedSpecScope.class);
  }
}
