package io.github.samzhu.topplecat.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Mutable-world execution evidence, deliberately separate from {@link ContractDefinition}. */
public record VerificationRun(String schemaVersion, String definitionDigest, String runId, Instant startedAt,
                              Instant finishedAt, EvidenceVerdict verdict, List<CaseRun> caseRuns) {
    public static final String SCHEMA_VERSION = "topplecat.verification-run.v1";

    public VerificationRun {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new ToppleCatException("Unsupported verification-run schema: " + schemaVersion);
        }
        if (definitionDigest == null || definitionDigest.isBlank() || runId == null || runId.isBlank()) {
            throw new ToppleCatException("Verification run requires a definition digest and run id.");
        }
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
        verdict = Objects.requireNonNull(verdict, "verdict");
        caseRuns = List.copyOf(Objects.requireNonNull(caseRuns, "caseRuns"));
    }
}
