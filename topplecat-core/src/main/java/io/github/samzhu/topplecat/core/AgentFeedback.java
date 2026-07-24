package io.github.samzhu.topplecat.core;

import java.util.List;

/** Safe remediation feedback for an implementation agent. Never carries case-level data. */
public record AgentFeedback(String schemaVersion, EvidenceVerdict verdict, List<EvidenceGate> gates) {
    public static final String SCHEMA_VERSION = "topplecat.agent-feedback.v1";

    public AgentFeedback {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new ToppleCatException("Unsupported agent feedback schema: " + schemaVersion);
        }
        gates = List.copyOf(gates == null ? List.of() : gates);
    }
}
