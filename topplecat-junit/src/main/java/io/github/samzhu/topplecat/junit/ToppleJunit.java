package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.SelectedSpecScope;
import io.github.samzhu.topplecat.core.SelectedSpecScopeJson;
import io.github.samzhu.topplecat.core.ReviewerJavaExecution;
import io.github.samzhu.topplecat.core.ToppleCatException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Configuration keys understood by the ToppleCat JUnit integration. */
public final class ToppleJunit {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Object REVIEWER_JAVA_EXECUTION_LOCK = new Object();
    /** Fixed JUnit metadata used to include only AC-bound reviewer checks. */
    public static final String CONTRACT_TAG = "topplecat-contract";
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
    /** File written by the hidden test task only after an AC-bound reviewer Java test starts its body. */
    public static final String REVIEWER_JAVA_EXECUTIONS_FILE_PROPERTY = "topplecat.reviewerJavaExecutionsFile";
    /** Checked ContractDefinition used by runtime Stage parity verification. */
    public static final String CONTRACT_DEFINITION_FILE_PROPERTY = "topplecat.contractDefinitionFile";
    /** Directory for content-addressed, reviewer-only verification attachment assets. */
    public static final String ATTACHMENTS_DIRECTORY_PROPERTY = "topplecat.attachmentsDirectory";
    /** Checked selected-Spec scope file; absent means preserve the existing all-hidden behaviour. */
    public static final String SELECTED_HIDDEN_SCOPE_FILE_PROPERTY = "topplecat.selectedHiddenScopeFile";
    /** Enables AC filtering for the reviewer-only Java source set; public JUnit always remains complete. */
    public static final String FILTER_CONTRACT_TESTS_PROPERTY = "topplecat.filterContractTests";
    /** Default public case root for a Gradle consumer project. */
    public static final String DEFAULT_PUBLIC_CASE_ROOT = "src/test/resources/topplecat/cases";

    private ToppleJunit() {
    }

    static boolean hiddenAcceptanceConditionSelected(String acId) {
        String configured = System.getProperty(SELECTED_HIDDEN_SCOPE_FILE_PROPERTY, "").trim();
        if (configured.isEmpty()) {
            return true;
        }
        try {
            SelectedSpecScope scope = SelectedSpecScopeJson.read(Files.readString(Path.of(configured)));
            return scope.acceptanceConditionIds().contains(acId);
        } catch (IOException | RuntimeException exception) {
            throw new ToppleCatException("Cannot read selected ToppleCat hidden scope: " + configured, exception);
        }
    }

    static boolean shouldFilterContractTests() {
        return Boolean.parseBoolean(System.getProperty(FILTER_CONTRACT_TESTS_PROPERTY, "false"));
    }

    static void recordReviewerJavaExecution(String acId) {
        String configured = System.getProperty(REVIEWER_JAVA_EXECUTIONS_FILE_PROPERTY, "").trim();
        if (configured.isEmpty()) {
            return;
        }
        Path output = Path.of(configured);
        try {
            synchronized (REVIEWER_JAVA_EXECUTION_LOCK) {
                Files.createDirectories(output.getParent());
                Files.writeString(output, JSON.writeValueAsString(new ReviewerJavaExecution(acId)) + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot write ToppleCat reviewer Java execution evidence: "
                    + exception.getMessage(), exception);
        }
    }
}
