package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Creates and validates ToppleCat evidence integrity hashes. */
public final class ToppleEvidenceJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ToppleEvidenceJson() {
    }

    public static ToppleEvidence create(
            String runId,
            String generatedAt,
            EvidenceVerdict verdict,
            List<EvidenceGate> gates,
            Map<String, String> artifactDigests
    ) {
        String hash = hash(runId, generatedAt, verdict, gates, artifactDigests);
        return new ToppleEvidence(ToppleEvidence.SCHEMA_VERSION, runId, generatedAt, verdict, gates, artifactDigests, hash);
    }

    public static String write(ToppleEvidence evidence) {
        validate(evidence);
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(evidence) + "\n";
    }

    public static ToppleEvidence read(String source) {
        ToppleEvidence evidence = JSON.readValue(source, ToppleEvidence.class);
        validate(evidence);
        return evidence;
    }

    public static void validate(ToppleEvidence evidence) {
        String expected = hash(evidence.runId(), evidence.generatedAt(), evidence.verdict(), evidence.gates(),
                evidence.artifactDigests());
        if (!expected.equals(evidence.integrityHash())) {
            throw new ToppleCatException("Evidence integrity validation failed.");
        }
    }

    private static String hash(String runId, String generatedAt, EvidenceVerdict verdict, List<EvidenceGate> gates,
                               Map<String, String> artifactDigests) {
        EvidencePayload payload = new EvidencePayload(ToppleEvidence.SCHEMA_VERSION, runId, generatedAt, verdict,
                List.copyOf(gates == null ? List.of() : gates), new TreeMap<>(artifactDigests == null ? Map.of() : artifactDigests));
        return Hashing.sha256(JSON.writeValueAsBytes(payload));
    }

    private record EvidencePayload(String schemaVersion, String runId, String generatedAt, EvidenceVerdict verdict,
                                   List<EvidenceGate> gates, Map<String, String> artifactDigests) {
    }
}
