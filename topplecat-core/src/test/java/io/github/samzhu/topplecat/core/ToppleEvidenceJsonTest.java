package io.github.samzhu.topplecat.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToppleEvidenceJsonTest {
    @Test
    void detectsAnyChangedArtifactDigest() {
        ToppleEvidence evidence = ToppleEvidenceJson.create("run-123", "2026-07-21T00:00:00Z", EvidenceVerdict.PASS,
                List.of(new EvidenceGate("JUNIT", EvidenceVerdict.PASS)),
                Map.of("spec-data.json", "a".repeat(64)));

        assertEquals(evidence, ToppleEvidenceJson.read(ToppleEvidenceJson.write(evidence)));
        ToppleEvidence tampered = new ToppleEvidence(evidence.schemaVersion(), evidence.runId(), evidence.generatedAt(),
                evidence.verdict(), evidence.gates(), Map.of("spec-data.json", "b".repeat(64)), evidence.integrityHash());

        assertThrows(ToppleCatException.class, () -> ToppleEvidenceJson.validate(tampered));
    }

    @Test
    void agentFeedbackHasOnlySafeGateSummaries() {
        AgentFeedback feedback = new AgentFeedback(AgentFeedback.SCHEMA_VERSION, EvidenceVerdict.FAIL,
                List.of(new EvidenceGate("AC-CART-COUPON/JUNIT", EvidenceVerdict.FAIL)));

        assertEquals(feedback, AgentFeedbackJson.read(AgentFeedbackJson.write(feedback)));
    }
}
