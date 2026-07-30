package io.github.samzhu.topplecat.core;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** JSON codec for the stable escrow manifest. */
public final class EscrowManifestJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private EscrowManifestJson() {}

  public static String write(EscrowManifest manifest) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n";
  }

  public static EscrowManifest read(String source) {
    RawPayload payload = JSON.readValue(source, RawPayload.class);
    ReviewerContractApproval approval =
        payload.approval() == null
            ? null
            : ReviewerContractApprovalJson.read(payload.approval().toString());
    return new EscrowManifest(
        payload.schemaVersion(), payload.state(), payload.entries(), approval);
  }

  private record RawPayload(
      String schemaVersion,
      EscrowState state,
      java.util.List<EscrowEntry> entries,
      JsonNode approval) {}
}
