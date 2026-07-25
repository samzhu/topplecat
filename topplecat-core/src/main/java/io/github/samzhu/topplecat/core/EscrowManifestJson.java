package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec for the stable escrow manifest. */
public final class EscrowManifestJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private EscrowManifestJson() {
    }

    public static String write(EscrowManifest manifest) {
        Object payload = manifest.isLegacyVersionOne()
                ? new VersionOnePayload(manifest.schemaVersion(), manifest.state(), manifest.entries())
                : new VersionTwoPayload(manifest.schemaVersion(), manifest.state(), manifest.entries(), manifest.approval());
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(payload) + "\n";
    }

    public static EscrowManifest read(String source) {
        return JSON.readValue(source, EscrowManifest.class);
    }

    private record VersionOnePayload(String schemaVersion, EscrowState state, java.util.List<EscrowEntry> entries) {
    }

    private record VersionTwoPayload(
            String schemaVersion,
            EscrowState state,
            java.util.List<EscrowEntry> entries,
            ReviewerContractApproval approval
    ) {
    }
}
