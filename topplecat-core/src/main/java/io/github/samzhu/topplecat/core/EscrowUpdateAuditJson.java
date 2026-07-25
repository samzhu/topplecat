package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/** JSON codec for reviewer-local escrow update audit metadata. */
final class EscrowUpdateAuditJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private EscrowUpdateAuditJson() {
    }

    static String write(EscrowUpdateAudit audit) {
        Payload payload = new Payload(audit.schemaVersion(), audit.updatedAt().toString(), audit.previousManifestSha256(),
                audit.newManifestSha256(), audit.added(), audit.changed(), audit.removed());
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(payload) + "\n";
    }

    static EscrowUpdateAudit read(String source) {
        Payload payload = JSON.readValue(source, Payload.class);
        return new EscrowUpdateAudit(payload.schemaVersion(), Instant.parse(payload.updatedAt()),
                payload.previousManifestSha256(), payload.newManifestSha256(), payload.added(), payload.changed(),
                payload.removed());
    }

    record Payload(
            String schemaVersion,
            String updatedAt,
            String previousManifestSha256,
            String newManifestSha256,
            int added,
            int changed,
            int removed
    ) {
    }
}
