package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.PropertyDefinition;
import io.github.samzhu.topplecat.core.PropertyExecutionEvent;
import io.github.samzhu.topplecat.core.PropertyExecutionEventJson;
import io.github.samzhu.topplecat.core.PropertyExecutionState;
import io.github.samzhu.topplecat.core.PropertyResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Assesses the complete current-run Property evidence contract behind one narrow interface.
 *
 * <p>Property sidecars carry stable Java method identities. JUnit XML deliberately supplies only
 * aggregate execution facts because testcase names are display text and may be arbitrary.
 */
final class PropertyEvidenceAssessment {
  static final String COUNTEREXAMPLE_REASON =
      "Property-Based Testing found a counterexample in this run. Review the approved public "
          + "contract and implement the rule generally.";
  static final String TASK_INCOMPLETE_REASON =
      "Property-Based Testing did not complete in this verification run.";
  static final String EVIDENCE_INCOMPLETE_REASON =
      "Property-Based Testing current-run evidence was missing or could not be read.";

  private PropertyEvidenceAssessment() {}

  /**
   * Assesses one sealed Property scope for one run. The returned count is the number of sealed
   * current-run Properties with one trusted complete lifecycle, never the number of projected
   * results.
   */
  static PropertyAssessment assess(
      Path runDirectory, List<PropertyDefinition> sealedDefinitions, String runId) {
    List<PropertyDefinition> definitions = List.copyOf(sealedDefinitions);
    if (definitions.isEmpty()) {
      throw new IllegalArgumentException(
          "Property assessment requires at least one sealed definition.");
    }
    Set<String> identities =
        definitions.stream()
            .map(PropertyDefinition::methodIdentity)
            .collect(java.util.stream.Collectors.toSet());
    if (identities.size() != definitions.size()) {
      throw new IllegalArgumentException(
          "Property assessment received duplicate sealed method identities.");
    }

    List<PropertyExecutionEvent> events;
    try {
      events = readEvents(runDirectory.resolve("public-property-events.jsonl"));
    } catch (IOException | RuntimeException exception) {
      return incomplete(0);
    }
    PropertyLifecycleAssessment lifecycle =
        assessLifecycles(events, definitions, identities, runId);
    int completedProperties = lifecycle.completedProperties();
    if (!VerificationRunArtifacts.completed(
        runDirectory, VerificationRunArtifacts.PROPERTY_PUBLIC)) {
      return new PropertyAssessment(
          List.of(), EvidenceVerdict.INCOMPLETE, TASK_INCOMPLETE_REASON, completedProperties);
    }

    JunitSummary junit;
    try {
      junit =
          readJunitSummary(
              runDirectory.resolve("junit").resolve(VerificationRunArtifacts.PROPERTY_PUBLIC));
    } catch (IOException | RuntimeException exception) {
      return incomplete(completedProperties);
    }

    int terminalEventCount =
        Math.toIntExact(
            events.stream()
                .filter(event -> runId.equals(event.runId()))
                .filter(event -> event.state().terminal())
                .count());
    int failedTerminalEventCount =
        Math.toIntExact(
            events.stream()
                .filter(event -> runId.equals(event.runId()))
                .filter(event -> event.state().terminal())
                .filter(event -> event.state() != PropertyExecutionState.COMPLETED_PASS)
                .count());
    boolean evidenceConsistent =
        lifecycle.evidenceConsistent()
            && junit.testCount() == definitions.size()
            && junit.skippedCount() == 0
            && junit.executedCount() == terminalEventCount
            && junit.failureCount() == failedTerminalEventCount;
    if (!evidenceConsistent) {
      return incomplete(completedProperties);
    }

    boolean counterexample =
        lifecycle.results().stream()
            .anyMatch(result -> result.state() == PropertyExecutionState.COMPLETED_COUNTEREXAMPLE);
    boolean incomplete =
        lifecycle.results().stream()
            .anyMatch(result -> result.state() == PropertyExecutionState.COMPLETED_INCOMPLETE);
    if (incomplete) {
      return incomplete(completedProperties);
    }
    return new PropertyAssessment(
        lifecycle.results(),
        counterexample ? EvidenceVerdict.FAIL : EvidenceVerdict.PASS,
        counterexample ? COUNTEREXAMPLE_REASON : null,
        completedProperties);
  }

  private static PropertyAssessment incomplete(int completedProperties) {
    return new PropertyAssessment(
        List.of(), EvidenceVerdict.INCOMPLETE, EVIDENCE_INCOMPLETE_REASON, completedProperties);
  }

  private static PropertyLifecycleAssessment assessLifecycles(
      List<PropertyExecutionEvent> events,
      List<PropertyDefinition> definitions,
      Set<String> identities,
      String runId) {
    Map<String, List<PropertyExecutionEvent>> byIdentity = new LinkedHashMap<>();
    for (PropertyExecutionEvent event : events) {
      byIdentity.computeIfAbsent(event.methodIdentity(), ignored -> new ArrayList<>()).add(event);
    }
    boolean evidenceConsistent = byIdentity.keySet().equals(identities);
    List<PropertyResult> results = new ArrayList<>();
    for (PropertyDefinition definition : definitions) {
      PropertyResult result =
          trustedResult(
              byIdentity.getOrDefault(definition.methodIdentity(), List.of()), definition, runId);
      if (result == null) {
        evidenceConsistent = false;
      } else {
        results.add(result);
      }
    }
    return new PropertyLifecycleAssessment(results, evidenceConsistent);
  }

  private static PropertyResult trustedResult(
      List<PropertyExecutionEvent> propertyEvents, PropertyDefinition definition, String runId) {
    if (propertyEvents.size() != 2) {
      return null;
    }
    PropertyExecutionEvent started = propertyEvents.getFirst();
    PropertyExecutionEvent terminal = propertyEvents.getLast();
    if (started.state() != PropertyExecutionState.STARTED
        || !terminal.state().terminal()
        || propertyEvents.stream().anyMatch(event -> !runId.equals(event.runId()))
        || propertyEvents.stream().anyMatch(event -> !definition.acId().equals(event.acId()))
        || propertyEvents.stream()
            .anyMatch(event -> !definition.methodIdentity().equals(event.methodIdentity()))
        || propertyEvents.stream()
            .anyMatch(event -> !definition.sourceDigest().equals(event.sourceDigest()))) {
      return null;
    }
    PropertyResult result = terminal.result();
    return result != null
            && definition.acId().equals(result.acId())
            && definition.methodIdentity().equals(result.methodIdentity())
            && terminal.state() == result.state()
        ? result
        : null;
  }

  private static List<PropertyExecutionEvent> readEvents(Path file) throws IOException {
    if (!Files.isRegularFile(file)) {
      throw new IOException("Property event sidecar is missing.");
    }
    List<PropertyExecutionEvent> events = new ArrayList<>();
    for (String line : Files.readAllLines(file)) {
      if (!line.isBlank()) {
        events.add(PropertyExecutionEventJson.readLine(line));
      }
    }
    return events;
  }

  private static JunitSummary readJunitSummary(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      throw new IOException("Property JUnit XML directory is missing.");
    }
    int tests = 0;
    int skipped = 0;
    int failures = 0;
    boolean foundXml = false;
    try (Stream<Path> files = Files.list(directory)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
        foundXml = true;
        NodeList testCases = parseCompletedTestResults(file);
        tests += testCases.getLength();
        for (int index = 0; index < testCases.getLength(); index++) {
          Element testCase = (Element) testCases.item(index);
          if (skipped(testCase)) {
            skipped++;
          }
          if (failure(testCase) != null) {
            failures++;
          }
        }
      }
    } catch (SAXException exception) {
      throw new IOException("Property JUnit XML could not be parsed.", exception);
    }
    if (!foundXml) {
      throw new IOException("Property JUnit XML is missing.");
    }
    return new JunitSummary(tests, tests - skipped, skipped, failures);
  }

  private static NodeList parseCompletedTestResults(Path file) throws SAXException, IOException {
    SAXException lastFailure = null;
    for (int attempt = 0; attempt < 5; attempt++) {
      try {
        return builder().parse(file.toFile()).getElementsByTagName("testcase");
      } catch (SAXException exception) {
        lastFailure = exception;
        if (attempt == 4) {
          break;
        }
        try {
          Thread.sleep(25L);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while waiting for Property JUnit XML.", interrupted);
        }
      }
    }
    throw lastFailure;
  }

  private static javax.xml.parsers.DocumentBuilder builder() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder();
    } catch (ParserConfigurationException exception) {
      throw new IllegalStateException(
          "Cannot configure secure Property JUnit XML parser", exception);
    }
  }

  private static Element failure(Element testCase) {
    NodeList children = testCase.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
      Node node = children.item(index);
      if (node instanceof Element element
          && (element.getTagName().equals("failure") || element.getTagName().equals("error"))) {
        return element;
      }
    }
    return null;
  }

  private static boolean skipped(Element testCase) {
    NodeList children = testCase.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
      Node node = children.item(index);
      if (node instanceof Element element && element.getTagName().equals("skipped")) {
        return true;
      }
    }
    return false;
  }

  private record JunitSummary(
      int testCount, int executedCount, int skippedCount, int failureCount) {}

  private record PropertyLifecycleAssessment(
      List<PropertyResult> results, boolean evidenceConsistent) {
    private PropertyLifecycleAssessment {
      results = List.copyOf(results);
    }

    private int completedProperties() {
      return results.size();
    }
  }
}

/** The compact result returned by {@link PropertyEvidenceAssessment}'s one assessment interface. */
record PropertyAssessment(
    List<PropertyResult> results, EvidenceVerdict verdict, String reason, int completedProperties) {
  PropertyAssessment {
    results = List.copyOf(results == null ? List.of() : results);
    if (verdict != EvidenceVerdict.PASS
            && verdict != EvidenceVerdict.FAIL
            && verdict != EvidenceVerdict.INCOMPLETE
        || completedProperties < 0) {
      throw new IllegalArgumentException("Property assessment is invalid.");
    }
  }
}
