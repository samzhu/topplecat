package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.EvidenceGate;
import io.github.samzhu.topplecat.pitest.PitMutationAttribution;
import java.time.Instant;
import java.util.List;

/** Reviewer-only report model that may contain hidden case data. */
public record VerificationView(
    String schemaVersion,
    Instant generatedAt,
    CaseResultStatus verdict,
    boolean expectedConsumptionEnforced,
    List<EvidenceGate> gates,
    List<VerificationAcceptanceCondition> acceptanceConditions,
    DeliveryScope deliveryScope,
    PitMutationAttribution mutationAttribution,
    VerificationRunSummary run) {
  public static final String SCHEMA_VERSION = "topplecat.verification-view.v10";

  public VerificationView {
    if (!SCHEMA_VERSION.equals(schemaVersion)) {
      throw new IllegalArgumentException("Unsupported verification-view schema: " + schemaVersion);
    }
    gates = List.copyOf(gates);
    acceptanceConditions = List.copyOf(acceptanceConditions);
    run = run == null ? VerificationRunSummary.unavailable() : run;
  }

  public VerificationView(
      String schemaVersion,
      Instant generatedAt,
      CaseResultStatus verdict,
      boolean expectedConsumptionEnforced,
      List<EvidenceGate> gates,
      List<VerificationAcceptanceCondition> acceptanceConditions) {
    this(
        schemaVersion,
        generatedAt,
        verdict,
        expectedConsumptionEnforced,
        gates,
        acceptanceConditions,
        null,
        null,
        null);
  }
}
