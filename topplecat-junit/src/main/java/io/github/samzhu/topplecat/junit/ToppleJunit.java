package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.PropertyExecutionEvent;
import io.github.samzhu.topplecat.core.PropertyExecutionEventJson;
import io.github.samzhu.topplecat.core.SelectedSpecScope;
import io.github.samzhu.topplecat.core.SelectedSpecScopeJson;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Configuration keys understood by the ToppleCat JUnit integration. */
public final class ToppleJunit {
  private static final Object EVIDENCE_WRITE_LOCK = new Object();

  /** Fixed JUnit metadata used to include only AC-bound reviewer checks. */
  public static final String CONTRACT_TAG = "topplecat-contract";

  /** Fixed JUnit tag used by the dedicated Property tasks. */
  public static final String PROPERTY_TAG = "topplecat-property";

  /** Path-separator-separated public JSON/YAML case roots. */
  public static final String PUBLIC_CASE_SOURCES_PROPERTY = "topplecat.publicCaseSources";

  /** Path-separator-separated reviewer-only JSON/YAML case roots. */
  public static final String HIDDEN_CASE_SOURCES_PROPERTY = "topplecat.hiddenCaseSources";

  /** Selects the mutually exclusive typed-row execution scope for an acceptance task. */
  public static final String CASE_EXECUTION_SCOPE_PROPERTY = "topplecat.caseExecutionScope";

  public static final String EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY =
      "topplecat.expectedConsumption.enforcement";

  /** Report-entry key for the AC id associated with a JUnit invocation. */
  public static final String AC_ID_ENTRY = "topplecat.acId";

  /** Report-entry prefix for expected-value consumption states. */
  public static final String EXPECTED_CONSUMPTION_ENTRY_PREFIX = "topplecat.expectedConsumption.";

  /** File written only by verification tasks for case-scoped narrative execution records. */
  public static final String NARRATIVE_EVENTS_FILE_PROPERTY = "topplecat.narrativeEventsFile";

  /** File written only by verification tasks for case-scoped expected-value consumption records. */
  public static final String EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY =
      "topplecat.expectedConsumptionEventsFile";

  /** Checked ContractDefinition used by runtime Stage parity verification. */
  public static final String CONTRACT_DEFINITION_FILE_PROPERTY = "topplecat.contractDefinitionFile";

  /** Directory for content-addressed, reviewer-only verification attachment assets. */
  public static final String ATTACHMENTS_DIRECTORY_PROPERTY = "topplecat.attachmentsDirectory";

  /** Checked selected-Spec scope file used by formal acceptance and Property verification tasks. */
  public static final String SELECTED_SCOPE_FILE_PROPERTY = "topplecat.selectedScopeFile";

  /** Enables AC filtering for formal acceptance and Property verification tasks. */
  public static final String FILTER_ACCEPTANCE_TESTS_PROPERTY = "topplecat.filterAcceptanceTests";

  /** Current-run JSONL path for terminal Property events. */
  public static final String PROPERTY_EVENTS_FILE_PROPERTY = "topplecat.propertyEventsFile";

  /** Default public case root for a Gradle consumer project. */
  public static final String DEFAULT_PUBLIC_CASE_ROOT = "src/test/resources/topplecat/cases";

  private ToppleJunit() {}

  public static boolean acceptanceConditionSelected(String acId) {
    String configured = System.getProperty(SELECTED_SCOPE_FILE_PROPERTY, "").trim();
    if (configured.isEmpty()) {
      return true;
    }
    try {
      SelectedSpecScope scope = SelectedSpecScopeJson.read(Files.readString(Path.of(configured)));
      return scope.acceptanceConditionIds().contains(acId);
    } catch (IOException | RuntimeException exception) {
      throw new ToppleCatException(
          "Cannot read selected ToppleCat formal acceptance scope: " + configured, exception);
    }
  }

  public static boolean shouldFilterAcceptanceTests() {
    return Boolean.parseBoolean(System.getProperty(FILTER_ACCEPTANCE_TESTS_PROPERTY, "false"));
  }

  /**
   * Appends one Property lifecycle event only when a dedicated verification task configured an
   * output path.
   */
  public static void recordPropertyEvent(PropertyExecutionEvent event) {
    String configured = System.getProperty(PROPERTY_EVENTS_FILE_PROPERTY, "").trim();
    if (configured.isEmpty()) {
      return;
    }
    Path output = Path.of(configured);
    try {
      synchronized (EVIDENCE_WRITE_LOCK) {
        Files.createDirectories(output.getParent());
        Files.writeString(
            output,
            PropertyExecutionEventJson.writeLine(event),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
      }
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot write ToppleCat Property execution evidence: " + exception.getMessage(),
          exception);
    }
  }
}
