package io.github.samzhu.topplecat.core;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Tamper-evident machine verdict with digests of every referenced artifact. */
public record ToppleEvidence(
        String schemaVersion,
        String runId,
        String generatedAt,
        EvidenceVerdict verdict,
        List<EvidenceGate> gates,
        Map<String, String> artifactDigests,
        String integrityHash
) {
    public static final String SCHEMA_VERSION = "topplecat.evidence.v1";

    public ToppleEvidence {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new ToppleCatException("Unsupported evidence schema: " + schemaVersion);
        }
        if (runId == null || runId.isBlank() || generatedAt == null || generatedAt.isBlank() || verdict == null) {
            throw new ToppleCatException("Evidence runId, generatedAt, and verdict are required.");
        }
        gates = List.copyOf(gates == null ? List.of() : gates);
        artifactDigests = Map.copyOf(new TreeMap<>(artifactDigests == null ? Map.of() : artifactDigests));
        if (integrityHash == null || integrityHash.isBlank()) {
            throw new ToppleCatException("Evidence integrityHash is required.");
        }
    }
}
