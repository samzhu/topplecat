package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec for the current-run public verification scope artifact. */
public final class VerificationScopeJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private VerificationScopeJson() {}

  public static String write(VerificationScope scope) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(scope) + "\n";
  }

  public static VerificationScope read(String source) {
    return JSON.readValue(source, VerificationScope.class);
  }
}
