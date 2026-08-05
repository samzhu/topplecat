package io.github.samzhu.topplecat.core;

/** Public current-run scope projection; it records selection, not reviewer data. */
public record VerificationScope(
    String schemaVersion,
    SelectedSpecScope selectedSpecScope,
    String hiddenMode,
    String mutationMode,
    String publicPropertyMode) {
  public static final String SCHEMA_VERSION = "topplecat.verification-scope.v4";
  public static final String HIDDEN_SELECTED_ACCEPTANCE_CONDITIONS =
      "SELECTED_ACCEPTANCE_CONDITIONS";
  public static final String HIDDEN_ALL = "ALL";
  public static final String MUTATION_SELECTED_ACCEPTANCE_CONDITIONS =
      "SELECTED_ACCEPTANCE_CONDITIONS";

  /**
   * @deprecated use {@link #MUTATION_SELECTED_ACCEPTANCE_CONDITIONS}.
   */
  @Deprecated
  public static final String MUTATION_ALL_PUBLIC_ACCEPTANCE_CONTRACTS =
      MUTATION_SELECTED_ACCEPTANCE_CONDITIONS;

  public static final String PROPERTY_PUBLIC_FULL_CONTRACT = "FULL_CONTRACT";
  public static final String PROPERTY_PUBLIC_SELECTED_ACCEPTANCE_CONDITIONS =
      "SELECTED_ACCEPTANCE_CONDITIONS";

  public VerificationScope {
    if (!SCHEMA_VERSION.equals(schemaVersion)
        || selectedSpecScope == null
        || !(hiddenMode.equals(HIDDEN_SELECTED_ACCEPTANCE_CONDITIONS)
            || hiddenMode.equals(HIDDEN_ALL))
        || !MUTATION_SELECTED_ACCEPTANCE_CONDITIONS.equals(mutationMode)
        || !(PROPERTY_PUBLIC_FULL_CONTRACT.equals(publicPropertyMode)
            || PROPERTY_PUBLIC_SELECTED_ACCEPTANCE_CONDITIONS.equals(publicPropertyMode))) {
      throw new ToppleCatException("Verification scope is invalid.");
    }
  }
}
