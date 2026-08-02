package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.PropertyCounterexample;
import io.github.samzhu.topplecat.core.PropertyDefinition;
import io.github.samzhu.topplecat.core.PropertyExecutionEvent;
import io.github.samzhu.topplecat.core.PropertyExecutionEventJson;
import io.github.samzhu.topplecat.core.PropertyExecutionState;
import io.github.samzhu.topplecat.core.PropertyResult;
import io.github.samzhu.topplecat.core.SourceRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertyEvidenceAssessmentTest {
  private static final String RUN_ID = "run-123";

  @TempDir Path tempDir;

  @Test
  void usesSidecarIdentityWhenFiveDisplayedPropertiesPassAndDisplayNamesRepeat() throws Exception {
    List<PropertyDefinition> definitions = definitions(5);
    Path run = completeRun(definitions, List.of(false, false, false, false, false), 0, false);

    PropertyAssessment assessment = PropertyEvidenceAssessment.assess(run, definitions, RUN_ID);

    assertEquals(EvidenceVerdict.PASS, assessment.verdict());
    assertEquals(5, assessment.results().size());
    assertEquals(5, assessment.completedProperties());
  }

  @Test
  void retainsEveryCompletedPropertyWhenTwoCounterexamplesAndThreePass() throws Exception {
    List<PropertyDefinition> definitions = definitions(5);
    Path run = completeRun(definitions, List.of(true, true, false, false, false), 2, false);

    PropertyAssessment assessment = PropertyEvidenceAssessment.assess(run, definitions, RUN_ID);

    assertEquals(EvidenceVerdict.FAIL, assessment.verdict());
    assertEquals(5, assessment.results().size());
    assertEquals(5, assessment.completedProperties());
  }

  @Test
  void countsEveryTrustedTerminalOutcomeWithoutConfusingCompletionWithVerdict() throws Exception {
    for (PropertyExecutionState terminalState :
        List.of(
            PropertyExecutionState.COMPLETED_PASS,
            PropertyExecutionState.COMPLETED_COUNTEREXAMPLE,
            PropertyExecutionState.COMPLETED_INCOMPLETE)) {
      List<PropertyDefinition> definitions = definitions(1);
      Path run =
          completeRunWithStates(
              definitions,
              List.of(terminalState),
              terminalState == PropertyExecutionState.COMPLETED_PASS ? 0 : 1,
              false);

      PropertyAssessment assessment = PropertyEvidenceAssessment.assess(run, definitions, RUN_ID);

      assertEquals(
          switch (terminalState) {
            case COMPLETED_PASS -> EvidenceVerdict.PASS;
            case COMPLETED_COUNTEREXAMPLE -> EvidenceVerdict.FAIL;
            case COMPLETED_INCOMPLETE -> EvidenceVerdict.INCOMPLETE;
            case STARTED -> throw new AssertionError("A terminal state is required.");
          },
          assessment.verdict(),
          terminalState.name());
      assertEquals(1, assessment.completedProperties(), terminalState.name());
    }
  }

  @Test
  void rejectsIncompleteOrMismatchedPropertyLifecyclesFromCompletedCount() throws Exception {
    for (EventAttack attack : EventAttack.values()) {
      List<PropertyDefinition> definitions = definitions(1);
      Path run = completeRun(definitions, List.of(false), 0, false);
      Path events = run.resolve("public-property-events.jsonl");
      List<String> validEvents = Files.readAllLines(events);
      switch (attack) {
        case STARTED_ONLY -> Files.writeString(events, validEvents.getFirst() + "\n");
        case TERMINAL_ONLY -> Files.writeString(events, validEvents.getLast() + "\n");
        case DUPLICATE_STARTED ->
            Files.writeString(
                events,
                validEvents.getFirst()
                    + "\n"
                    + validEvents.getFirst()
                    + "\n"
                    + validEvents.getLast()
                    + "\n");
        case DUPLICATE_TERMINAL ->
            Files.writeString(
                events,
                validEvents.getFirst()
                    + "\n"
                    + validEvents.getLast()
                    + "\n"
                    + validEvents.getLast()
                    + "\n");
        case TERMINAL_BEFORE_STARTED ->
            Files.writeString(events, validEvents.getLast() + "\n" + validEvents.getFirst() + "\n");
        case WRONG_RUN -> writeEvents(events, definitions, List.of(false), "other-run", null, null);
        case WRONG_AC -> writeEvents(events, definitions, List.of(false), RUN_ID, "AC-WRONG", null);
        case WRONG_METHOD ->
            writeEvents(
                events,
                definitions,
                List.of(false),
                RUN_ID,
                null,
                null,
                definitions.getFirst().methodIdentity().replace(")V", "I)V"));
        case WRONG_DIGEST ->
            writeEvents(events, definitions, List.of(false), RUN_ID, null, "f".repeat(64));
      }

      PropertyAssessment assessment = PropertyEvidenceAssessment.assess(run, definitions, RUN_ID);

      assertEquals(EvidenceVerdict.INCOMPLETE, assessment.verdict(), attack.name());
      assertTrue(assessment.results().isEmpty(), attack.name());
      assertEquals(0, assessment.completedProperties(), attack.name());
    }
  }

  @Test
  void rejectsTerminalEventsWhoseEmbeddedResultsDisagreeWithTheEvent() throws Exception {
    for (String resultField : List.of("acId", "methodIdentity", "state")) {
      List<PropertyDefinition> definitions = definitions(1);
      Path run = completeRun(definitions, List.of(false), 0, false);
      Path events = run.resolve("public-property-events.jsonl");
      List<String> validEvents = Files.readAllLines(events);
      String terminal = validEvents.getLast();
      String expected =
          switch (resultField) {
            case "acId" -> "\"acId\":\"" + definitions.getFirst().acId() + "\"";
            case "methodIdentity" ->
                "\"methodIdentity\":\"" + definitions.getFirst().methodIdentity() + "\"";
            case "state" -> "\"state\":\"COMPLETED_PASS\"";
            default -> throw new AssertionError("Unexpected result field.");
          };
      String actual =
          switch (resultField) {
            case "acId" -> "\"acId\":\"AC-WRONG\"";
            case "methodIdentity" -> "\"methodIdentity\":\"example.Properties#wrong()V\"";
            case "state" -> "\"state\":\"COMPLETED_INCOMPLETE\"";
            default -> throw new AssertionError("Unexpected result field.");
          };
      int embeddedResultField = terminal.lastIndexOf(expected);
      assertTrue(embeddedResultField > 0, resultField);
      terminal =
          terminal.substring(0, embeddedResultField)
              + actual
              + terminal.substring(embeddedResultField + expected.length());
      Files.writeString(events, validEvents.getFirst() + "\n" + terminal + "\n");

      PropertyAssessment assessment = PropertyEvidenceAssessment.assess(run, definitions, RUN_ID);

      assertEquals(EvidenceVerdict.INCOMPLETE, assessment.verdict(), resultField);
      assertEquals(0, assessment.completedProperties(), resultField);
    }
  }

  @Test
  void rejectsMissingCompletionMarkerAndBrokenJUnitXml() throws Exception {
    List<PropertyDefinition> definitions = definitions(1);
    Path missingMarker = completeRun(definitions, List.of(false), 0, false);
    Files.delete(missingMarker.resolve("gates/PROPERTY_PUBLIC.completed"));
    assertEquals(
        EvidenceVerdict.INCOMPLETE,
        PropertyEvidenceAssessment.assess(missingMarker, definitions, RUN_ID).verdict());
    assertEquals(
        1,
        PropertyEvidenceAssessment.assess(missingMarker, definitions, RUN_ID)
            .completedProperties());

    Path missingXml = completeRun(definitions, List.of(false), 0, false);
    Files.delete(missingXml.resolve("junit/PROPERTY_PUBLIC/TEST-properties.xml"));
    assertEquals(
        EvidenceVerdict.INCOMPLETE,
        PropertyEvidenceAssessment.assess(missingXml, definitions, RUN_ID).verdict());
    assertEquals(
        1,
        PropertyEvidenceAssessment.assess(missingXml, definitions, RUN_ID).completedProperties());

    Path malformedXml = completeRun(definitions, List.of(false), 0, false);
    Files.writeString(
        malformedXml.resolve("junit/PROPERTY_PUBLIC/TEST-properties.xml"), "<testsuite><testcase>");
    assertEquals(
        EvidenceVerdict.INCOMPLETE,
        PropertyEvidenceAssessment.assess(malformedXml, definitions, RUN_ID).verdict());
    assertEquals(
        1,
        PropertyEvidenceAssessment.assess(malformedXml, definitions, RUN_ID).completedProperties());

    Path skippedXml = completeRun(definitions, List.of(false), 0, true);
    assertEquals(
        EvidenceVerdict.INCOMPLETE,
        PropertyEvidenceAssessment.assess(skippedXml, definitions, RUN_ID).verdict());
    assertEquals(
        1,
        PropertyEvidenceAssessment.assess(skippedXml, definitions, RUN_ID).completedProperties());
  }

  @Test
  void rejectsJUnitAggregateCountsThatDisagreeWithCurrentTerminalEvents() throws Exception {
    List<PropertyDefinition> definitions = definitions(1);
    Path extraExecuted = completeRun(definitions, List.of(false), 0, false);
    writeJunit(extraExecuted, 2, 0, false);
    assertEquals(
        EvidenceVerdict.INCOMPLETE,
        PropertyEvidenceAssessment.assess(extraExecuted, definitions, RUN_ID).verdict());
    assertEquals(
        1,
        PropertyEvidenceAssessment.assess(extraExecuted, definitions, RUN_ID)
            .completedProperties());

    Path mismatchedFailures = completeRun(definitions, List.of(false), 0, false);
    writeJunit(mismatchedFailures, 1, 1, false);
    assertEquals(
        EvidenceVerdict.INCOMPLETE,
        PropertyEvidenceAssessment.assess(mismatchedFailures, definitions, RUN_ID).verdict());
    assertEquals(
        1,
        PropertyEvidenceAssessment.assess(mismatchedFailures, definitions, RUN_ID)
            .completedProperties());
  }

  @Test
  void refusesOutOfScopeMethodIdentityInsteadOfProjectingItAsPropertyEvidence() throws Exception {
    List<PropertyDefinition> definitions = definitions(1);
    Path run = completeRun(definitions, List.of(false), 0, false);
    PropertyDefinition extra = definitions(2).get(1);
    Path events = run.resolve("public-property-events.jsonl");
    Files.writeString(
        events, Files.readString(events) + events(extra, false, RUN_ID, null, null, null));

    PropertyAssessment assessment = PropertyEvidenceAssessment.assess(run, definitions, RUN_ID);

    assertEquals(EvidenceVerdict.INCOMPLETE, assessment.verdict());
    assertTrue(assessment.results().isEmpty());
    assertEquals(1, assessment.completedProperties());
  }

  @Test
  void preservesOneTrustedCompletionWhenAnotherSelectedPropertyIsMalformed() throws Exception {
    List<PropertyDefinition> definitions = definitions(2);
    Path run = completeRun(definitions, List.of(false, false), 0, false);
    Path events = run.resolve("public-property-events.jsonl");
    List<String> validEvents = Files.readAllLines(events);
    Files.writeString(
        events,
        String.join(
                "\n",
                validEvents.get(0),
                validEvents.get(1),
                validEvents.get(2),
                validEvents.get(3),
                validEvents.get(3))
            + "\n");

    PropertyAssessment assessment = PropertyEvidenceAssessment.assess(run, definitions, RUN_ID);

    assertEquals(EvidenceVerdict.INCOMPLETE, assessment.verdict());
    assertEquals(1, assessment.completedProperties());
  }

  private Path completeRun(
      List<PropertyDefinition> definitions,
      List<Boolean> counterexamples,
      int failureCount,
      boolean skipped)
      throws Exception {
    Path run = tempDir.resolve("run-" + System.nanoTime());
    Files.createDirectories(run.resolve("gates"));
    Files.writeString(run.resolve("gates/PROPERTY_PUBLIC.completed"), "completed\n");
    writeEvents(
        run.resolve("public-property-events.jsonl"),
        definitions,
        counterexamples,
        RUN_ID,
        null,
        null);
    writeJunit(run, definitions.size(), failureCount, skipped);
    return run;
  }

  private Path completeRunWithStates(
      List<PropertyDefinition> definitions,
      List<PropertyExecutionState> terminalStates,
      int failureCount,
      boolean skipped)
      throws Exception {
    Path run = tempDir.resolve("run-" + System.nanoTime());
    Files.createDirectories(run.resolve("gates"));
    Files.writeString(run.resolve("gates/PROPERTY_PUBLIC.completed"), "completed\n");
    writeEventsWithStates(
        run.resolve("public-property-events.jsonl"), definitions, terminalStates, RUN_ID);
    writeJunit(run, definitions.size(), failureCount, skipped);
    return run;
  }

  private static void writeEvents(
      Path file,
      List<PropertyDefinition> definitions,
      List<Boolean> counterexamples,
      String runId,
      String acOverride,
      String digestOverride)
      throws Exception {
    writeEvents(file, definitions, counterexamples, runId, acOverride, digestOverride, null);
  }

  private static void writeEvents(
      Path file,
      List<PropertyDefinition> definitions,
      List<Boolean> counterexamples,
      String runId,
      String acOverride,
      String digestOverride,
      String methodIdentityOverride)
      throws Exception {
    StringBuilder source = new StringBuilder();
    for (int index = 0; index < definitions.size(); index++) {
      source.append(
          events(
              definitions.get(index),
              counterexamples.get(index),
              runId,
              acOverride,
              digestOverride,
              methodIdentityOverride));
    }
    Files.writeString(file, source.toString());
  }

  private static void writeEventsWithStates(
      Path file,
      List<PropertyDefinition> definitions,
      List<PropertyExecutionState> terminalStates,
      String runId)
      throws Exception {
    StringBuilder source = new StringBuilder();
    for (int index = 0; index < definitions.size(); index++) {
      source.append(
          events(definitions.get(index), terminalStates.get(index), runId, null, null, null));
    }
    Files.writeString(file, source.toString());
  }

  private static String events(
      PropertyDefinition definition,
      boolean counterexample,
      String runId,
      String acOverride,
      String digestOverride,
      String methodIdentityOverride) {
    return events(
        definition,
        counterexample
            ? PropertyExecutionState.COMPLETED_COUNTEREXAMPLE
            : PropertyExecutionState.COMPLETED_PASS,
        runId,
        acOverride,
        digestOverride,
        methodIdentityOverride);
  }

  private static String events(
      PropertyDefinition definition,
      PropertyExecutionState terminalState,
      String runId,
      String acOverride,
      String digestOverride,
      String methodIdentityOverride) {
    String acId = acOverride == null ? definition.acId() : acOverride;
    String digest = digestOverride == null ? definition.sourceDigest() : digestOverride;
    String methodIdentity =
        methodIdentityOverride == null ? definition.methodIdentity() : methodIdentityOverride;
    PropertyExecutionEvent started =
        new PropertyExecutionEvent(
            PropertyExecutionEvent.SCHEMA_VERSION,
            runId,
            acId,
            methodIdentity,
            digest,
            PropertyExecutionState.STARTED,
            null);
    PropertyResult result = result(acId, methodIdentity, terminalState);
    PropertyExecutionEvent terminal =
        new PropertyExecutionEvent(
            PropertyExecutionEvent.SCHEMA_VERSION,
            runId,
            acId,
            methodIdentity,
            digest,
            result.state(),
            result);
    return PropertyExecutionEventJson.writeLine(started)
        + PropertyExecutionEventJson.writeLine(terminal);
  }

  private static PropertyResult result(String acId, String methodIdentity, boolean counterexample) {
    return result(
        acId,
        methodIdentity,
        counterexample
            ? PropertyExecutionState.COMPLETED_COUNTEREXAMPLE
            : PropertyExecutionState.COMPLETED_PASS);
  }

  private static PropertyResult result(
      String acId, String methodIdentity, PropertyExecutionState terminalState) {
    if (terminalState == PropertyExecutionState.COMPLETED_INCOMPLETE) {
      return new PropertyResult(
          acId,
          methodIdentity,
          terminalState,
          10,
          5,
          1,
          4,
          1,
          List.of(),
          1L,
          false,
          null,
          null,
          null,
          0,
          false,
          "Coverage target was not reached.");
    }
    if (terminalState == PropertyExecutionState.COMPLETED_COUNTEREXAMPLE) {
      return new PropertyResult(
          acId,
          methodIdentity,
          PropertyExecutionState.COMPLETED_COUNTEREXAMPLE,
          10,
          3,
          1,
          2,
          0,
          List.of(),
          1L,
          true,
          "replay",
          new PropertyCounterexample("{\"amount\":10}", List.of()),
          new PropertyCounterexample("{\"amount\":0}", List.of(0)),
          1,
          true,
          null);
    }
    return new PropertyResult(
        acId,
        methodIdentity,
        PropertyExecutionState.COMPLETED_PASS,
        10,
        10,
        1,
        9,
        0,
        List.of(),
        1L,
        false,
        null,
        null,
        null,
        0,
        false,
        null);
  }

  private static void writeJunit(Path run, int testCount, int failureCount, boolean skipped)
      throws Exception {
    Path xml = run.resolve("junit/PROPERTY_PUBLIC/TEST-properties.xml");
    Files.createDirectories(xml.getParent());
    List<String> cases = new ArrayList<>();
    for (int index = 0; index < testCount; index++) {
      String child = skipped ? "<skipped/>" : index < failureCount ? "<failure/>" : "";
      // These are intentionally duplicated arbitrary display names, never Java identities.
      cases.add(
          "<testcase classname=\"example.Properties\" name=\"The same human title\">"
              + child
              + "</testcase>");
    }
    Files.writeString(xml, "<testsuite>" + String.join("", cases) + "</testsuite>");
  }

  private static List<PropertyDefinition> definitions(int count) {
    List<PropertyDefinition> definitions = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      definitions.add(
          new PropertyDefinition(
              "AC-" + index,
              "example.Properties#property"
                  + index
                  + "(Lio/github/samzhu/topplecat/junit/property/PropertyTrials;)V",
              "Property " + index,
              10,
              10,
              10,
              new SourceRef("Properties.java", index + 1, 1),
              Integer.toHexString(index + 10).repeat(64).substring(0, 64)));
    }
    return definitions;
  }

  private enum EventAttack {
    STARTED_ONLY,
    TERMINAL_ONLY,
    DUPLICATE_STARTED,
    DUPLICATE_TERMINAL,
    TERMINAL_BEFORE_STARTED,
    WRONG_RUN,
    WRONG_AC,
    WRONG_METHOD,
    WRONG_DIGEST
  }
}
