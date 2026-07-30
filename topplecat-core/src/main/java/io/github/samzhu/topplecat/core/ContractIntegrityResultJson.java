package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec for the reviewer-only run-scoped contract-integrity result. */
public final class ContractIntegrityResultJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ContractIntegrityResultJson() {}

  public static String write(ContractIntegrityResult result) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n";
  }

  public static ContractIntegrityResult read(String source) {
    return JSON.readValue(source, ContractIntegrityResult.class);
  }
}
