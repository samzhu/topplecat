package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.EvidenceGate;
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
    DeliveryScope deliveryScope) {
  public static final String SCHEMA_VERSION = "topplecat.verification-view.v7";

  public VerificationView {
    if (!SCHEMA_VERSION.equals(schemaVersion)) {
      throw new IllegalArgumentException("Unsupported verification-view schema: " + schemaVersion);
    }
    gates = List.copyOf(gates);
    acceptanceConditions = List.copyOf(acceptanceConditions);
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
        null);
  }
}
