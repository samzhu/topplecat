package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec for the stable escrow manifest. */
public final class EscrowManifestJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private EscrowManifestJson() {
    }

    public static String write(EscrowManifest manifest) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n";
    }

    public static EscrowManifest read(String source) {
        return JSON.readValue(source, EscrowManifest.class);
    }
}
