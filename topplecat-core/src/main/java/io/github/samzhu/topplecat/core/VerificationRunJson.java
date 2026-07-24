package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec for reviewer-only execution evidence. */
public final class VerificationRunJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private VerificationRunJson() {
    }

    public static String write(VerificationRun run) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(run) + "\n";
    }

    public static VerificationRun read(String source) {
        try {
            return JSON.readValue(source, VerificationRun.class);
        } catch (RuntimeException exception) {
            for (Throwable current = exception; current != null; current = current.getCause()) {
                if (current instanceof ToppleCatException domain) {
                    throw domain;
                }
            }
            throw exception;
        }
    }
}
