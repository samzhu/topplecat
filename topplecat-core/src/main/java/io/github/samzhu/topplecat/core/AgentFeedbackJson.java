package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec for safe agent feedback. */
public final class AgentFeedbackJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private AgentFeedbackJson() {}

  public static String write(AgentFeedback feedback) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(feedback) + "\n";
  }

  public static AgentFeedback read(String source) {
    return JSON.readValue(source, AgentFeedback.class);
  }
}
