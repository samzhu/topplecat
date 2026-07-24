package io.github.samzhu.topplecat.junit;

/** Configuration keys understood by the ToppleCat JUnit integration. */
public final class ToppleJunit {
    /** Path-separator-separated public JSON/YAML case roots. */
    public static final String PUBLIC_CASE_SOURCES_PROPERTY = "topplecat.publicCaseSources";
    /** Path-separator-separated reviewer-only JSON/YAML case roots. */
    public static final String HIDDEN_CASE_SOURCES_PROPERTY = "topplecat.hiddenCaseSources";
    /** Enables restored hidden case rows for reviewer verification. */
    public static final String INCLUDE_HIDDEN_CASES_PROPERTY = "topplecat.includeHiddenCases";
    public static final String EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY = "topplecat.expectedConsumption.enforcement";
    /** Report-entry key for the AC id associated with a JUnit invocation. */
    public static final String AC_ID_ENTRY = "topplecat.acId";
    /** Report-entry prefix for expected-value consumption states. */
    public static final String EXPECTED_CONSUMPTION_ENTRY_PREFIX = "topplecat.expectedConsumption.";
    /** File written only by verification tasks for case-scoped narrative execution records. */
    public static final String NARRATIVE_EVENTS_FILE_PROPERTY = "topplecat.narrativeEventsFile";
    /** File written only by verification tasks for case-scoped expected-value consumption records. */
    public static final String EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY = "topplecat.expectedConsumptionEventsFile";
    /** Checked ContractDefinition used by runtime Stage parity verification. */
    public static final String CONTRACT_DEFINITION_FILE_PROPERTY = "topplecat.contractDefinitionFile";
    /** Directory for content-addressed, reviewer-only verification attachment assets. */
    public static final String ATTACHMENTS_DIRECTORY_PROPERTY = "topplecat.attachmentsDirectory";
    /** Default public case root for a Gradle consumer project. */
    public static final String DEFAULT_PUBLIC_CASE_ROOT = "src/test/resources/topplecat/cases";

    private ToppleJunit() {
    }
}
