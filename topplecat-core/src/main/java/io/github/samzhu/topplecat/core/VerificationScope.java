package io.github.samzhu.topplecat.core;

/** Public current-run scope projection; it records selection, not reviewer data. */
public record VerificationScope(
        String schemaVersion,
        SelectedSpecScope selectedSpecScope,
        String hiddenMode,
        String mutationMode
) {
    public static final String SCHEMA_VERSION = "topplecat.verification-scope.v1";
    public static final String HIDDEN_SELECTED_SPECS = "SELECTED_SPECS";
    public static final String HIDDEN_ALL = "ALL";
    public static final String MUTATION_ALL_CANONICAL_CONTRACTS = "ALL_CANONICAL_CONTRACTS";

    public VerificationScope {
        if (!SCHEMA_VERSION.equals(schemaVersion) || selectedSpecScope == null
                || !(hiddenMode.equals(HIDDEN_SELECTED_SPECS) || hiddenMode.equals(HIDDEN_ALL))
                || !MUTATION_ALL_CANONICAL_CONTRACTS.equals(mutationMode)) {
            throw new ToppleCatException("Verification scope is invalid.");
        }
    }
}
