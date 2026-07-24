package io.github.samzhu.topplecat.core;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, versioned contract assembled from compiler descriptors and typed case data.
 *
 * <p>The definition deliberately has no execution status, duration, failure, or attachment
 * fields. Those belong to a {@link VerificationRun} and are projected separately.</p>
 */
public record ContractDefinition(String schemaVersion, String digest,
                                 List<AcceptanceContract> acceptanceConditions) {
    public static final String SCHEMA_VERSION = "topplecat.contract-definition.v1";

    public ContractDefinition {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new ToppleCatException("Unsupported contract-definition schema: " + schemaVersion);
        }
        if (digest == null || digest.isBlank()) {
            throw new ToppleCatException("Contract definition digest is required.");
        }
        acceptanceConditions = sorted(acceptanceConditions);
    }

    public static ContractDefinition withComputedDigest(List<AcceptanceContract> acceptanceConditions) {
        List<AcceptanceContract> sorted = sorted(acceptanceConditions);
        ContractDefinition unsigned = new ContractDefinition(SCHEMA_VERSION, "pending", sorted);
        return new ContractDefinition(SCHEMA_VERSION, ContractDefinitionJson.digest(unsigned), sorted);
    }

    private static List<AcceptanceContract> sorted(List<AcceptanceContract> values) {
        Objects.requireNonNull(values, "acceptanceConditions");
        List<AcceptanceContract> result = values.stream()
                .sorted(Comparator.comparing(AcceptanceContract::acId))
                .toList();
        if (result.stream().map(AcceptanceContract::acId).distinct().count() != result.size()) {
            throw new ToppleCatException("Contract definition contains duplicate AC ids.");
        }
        return List.copyOf(result);
    }
}
