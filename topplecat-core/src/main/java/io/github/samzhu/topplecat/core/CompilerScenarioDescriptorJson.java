package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec used by the Gradle-side descriptor reader. */
public final class CompilerScenarioDescriptorJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private CompilerScenarioDescriptorJson() {
    }

    public static CompilerScenarioDescriptor read(String source) {
        return JSON.readValue(source, CompilerScenarioDescriptor.class);
    }
}
