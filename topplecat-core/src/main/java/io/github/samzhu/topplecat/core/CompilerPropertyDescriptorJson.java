package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec used by the compiler emitter and Gradle-side descriptor reader. */
public final class CompilerPropertyDescriptorJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private CompilerPropertyDescriptorJson() {}

  public static String write(CompilerPropertyDescriptor descriptor) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(descriptor) + "\n";
  }

  public static CompilerPropertyDescriptor read(String source) {
    return JSON.readValue(source, CompilerPropertyDescriptor.class);
  }
}
