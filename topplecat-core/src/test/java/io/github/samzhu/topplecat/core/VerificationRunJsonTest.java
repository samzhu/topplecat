package io.github.samzhu.topplecat.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerificationRunJsonTest {
    @Test
    void roundTripsReviewerOnlyExecutionEvidence() {
        VerificationRun run = new VerificationRun(VerificationRun.SCHEMA_VERSION, "a".repeat(64), "run-1",
                Instant.parse("2026-07-24T00:00:00Z"), Instant.parse("2026-07-24T00:00:01Z"), EvidenceVerdict.PASS,
                List.of(new CaseRun("case-1", NarrativeStepStatus.PASS, java.time.Duration.ofMillis(1), List.of(),
                        Map.of("result", "ASSERTED"), "")));

        assertEquals(run, VerificationRunJson.read(VerificationRunJson.write(run)));
    }
}
