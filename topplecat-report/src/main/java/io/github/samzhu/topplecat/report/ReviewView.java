package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.ContractQualityAdvisory;
import java.time.Instant;
import java.util.List;

/** Reviewer-only authoring review model. It deliberately contains no execution state. */
public record ReviewView(
    String schemaVersion,
    Instant generatedAt,
    List<ReviewDocument> selectedSpecDocuments,
    List<ReviewAcceptanceCondition> acceptanceConditions,
    DeliveryScope deliveryScope,
    List<ContractQualityAdvisory> contractQualityAdvisories) {
  public static final String SCHEMA_VERSION = "topplecat.review-view.v8";

  public ReviewView {
    if (!SCHEMA_VERSION.equals(schemaVersion)) {
      throw new IllegalArgumentException("Unsupported review-view schema: " + schemaVersion);
    }
    selectedSpecDocuments =
        List.copyOf(selectedSpecDocuments == null ? List.of() : selectedSpecDocuments);
    acceptanceConditions = List.copyOf(acceptanceConditions);
    contractQualityAdvisories =
        List.copyOf(contractQualityAdvisories == null ? List.of() : contractQualityAdvisories);
  }

  public ReviewView(
      String schemaVersion,
      Instant generatedAt,
      List<ReviewAcceptanceCondition> acceptanceConditions) {
    this(schemaVersion, generatedAt, List.of(), acceptanceConditions, null, List.of());
  }
}
