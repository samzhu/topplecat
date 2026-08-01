package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.AttachmentRef;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ExpectedActualComparison;
import io.github.samzhu.topplecat.core.ExpectedConsumptionExecution;
import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.NarrativeExecution;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.ScenarioTemplate;
import io.github.samzhu.topplecat.core.ScenarioTemplateRenderer;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.opentest4j.TestAbortedException;
import tools.jackson.databind.json.JsonMapper;

/** Internal per-invocation evidence writer for compiler-described Scenario sessions. */
final class ToppleNarrative {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Object FILE_LOCK = new Object();
  private static final long MAX_ATTACHMENT_REPORT_BYTES = 100L * 1024 * 1024;
  private static final Pattern SENSITIVE_VALUE =
      Pattern.compile(
          "(?i)(authorization|cookie|set-cookie|token|password|secret)"
              + "(\\s*[:=]\\s*)([^,\\s\\\"}]+|\\\"[^\\\"]*\\\")");

  private ToppleNarrative() {}

  static Session startScenario(ToppleCase testCase, ScenarioTemplate scenario) {
    return new Session(new Execution(testCase.caseId(), testCase.visibility(), scenario));
  }

  /** Per-invocation writer owned by the active Scenario session. */
  static final class Session {
    private final Execution execution;

    private Session(Execution execution) {
      this.execution = execution;
    }

    void attach(ToppleAttachment attachment) {
      if (execution.active == null || execution.active.status != null) {
        throw new ToppleCatException(
            "Topple attachment requires a currently active compiled Step.");
      }
      execution.active.attach(writeAttachment(attachment, execution.visibility));
    }

    void beginScenarioStep(String runtimeStepId, Object[] arguments) {
      execution.beginScenarioStep(runtimeStepId, arguments);
    }

    void recordComparison(ExpectedActualComparison comparison) {
      execution.recordComparison(comparison);
    }

    void finishScenarioStep(Throwable failure) {
      execution.completeActive(
          failure == null
              ? NarrativeStepStatus.PASS
              : failure instanceof TestAbortedException
                  ? NarrativeStepStatus.ABORTED
                  : NarrativeStepStatus.FAIL);
    }

    void finishScenario(Throwable failure, Map<String, ExpectedConsumption> consumption) {
      execution.finish(failure);
      writeNarrative(execution.snapshot());
      writeExpectedConsumption(
          new ExpectedConsumptionExecution(
              execution.caseId,
              consumption.entrySet().stream()
                  .collect(
                      java.util.stream.Collectors.toMap(
                          Map.Entry::getKey,
                          entry -> entry.getValue().name(),
                          (left, right) -> right,
                          LinkedHashMap::new))));
    }
  }

  private static void writeNarrative(NarrativeExecution execution) {
    String configured = System.getProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY);
    if (configured == null || configured.isBlank() || execution.steps().isEmpty()) {
      return;
    }
    append(Path.of(configured), execution, "narrative");
  }

  private static void writeExpectedConsumption(ExpectedConsumptionExecution execution) {
    String configured = System.getProperty(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY);
    if (configured == null || configured.isBlank()) {
      return;
    }
    append(Path.of(configured), execution, "expected-consumption");
  }

  private static void append(Path file, Object execution, String name) {
    try {
      synchronized (FILE_LOCK) {
        Files.createDirectories(file.getParent());
        Files.writeString(
            file,
            JSON.writeValueAsString(execution) + System.lineSeparator(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
      }
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot write ToppleCat " + name + " sidecar " + file + ": " + exception.getMessage(),
          exception);
    }
  }

  private static AttachmentRef writeAttachment(
      ToppleAttachment attachment, CaseVisibility visibility) {
    String configured = System.getProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY);
    if (configured == null || configured.isBlank()) {
      throw new ToppleCatException(
          "Topple attachments are available only during a configured verification run.");
    }
    byte[] bytes = attachment.content();
    if (attachment.mediaType().startsWith("text/")
        || attachment.mediaType().equals("application/json")) {
      bytes = redact(new String(bytes, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
    }
    String digest = Hashing.sha256(bytes);
    String filename = digest + "." + attachment.extension();
    Path directory = Path.of(configured).toAbsolutePath().normalize();
    Path target = directory.resolve(filename).normalize();
    if (!target.getParent().equals(directory)) {
      throw new ToppleCatException("Topple attachment path escaped its configured directory.");
    }
    synchronized (FILE_LOCK) {
      try {
        Files.createDirectories(directory);
        if (!Files.exists(target)) {
          long current = attachmentBytes(directory);
          if (!attachmentReportCapacityAllows(current, bytes.length)) {
            throw new ToppleCatException(
                "Topple verification attachments exceed the 100 MiB report limit.");
          }
          Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        }
      } catch (IOException exception) {
        throw new ToppleCatException(
            "Cannot write Topple attachment " + filename + ": " + exception.getMessage(),
            exception);
      }
    }
    return new AttachmentRef(
        digest,
        attachment.title(),
        attachment.mediaType(),
        bytes.length,
        visibility,
        "attachments/" + filename);
  }

  static boolean attachmentReportCapacityAllows(long currentBytes, long candidateBytes) {
    return currentBytes >= 0
        && candidateBytes >= 0
        && candidateBytes <= MAX_ATTACHMENT_REPORT_BYTES - currentBytes;
  }

  private static long attachmentBytes(Path directory) throws IOException {
    try (var files = Files.walk(directory)) {
      return files
          .filter(Files::isRegularFile)
          .mapToLong(
              path -> {
                try {
                  return Files.size(path);
                } catch (IOException exception) {
                  throw new AttachmentSizeException(exception);
                }
              })
          .sum();
    } catch (AttachmentSizeException exception) {
      throw exception.cause;
    }
  }

  private static String redact(String value) {
    return SENSITIVE_VALUE.matcher(value).replaceAll("$1$2***REDACTED***");
  }

  private static final class Execution {
    private final String caseId;
    private final CaseVisibility visibility;
    private final ScenarioTemplate scenario;
    private final List<MutableStep> steps = new ArrayList<>();
    private int nextStep;
    private MutableStep active;

    private Execution(String caseId, CaseVisibility visibility, ScenarioTemplate scenario) {
      this.caseId = caseId;
      this.visibility = visibility;
      this.scenario = scenario;
    }

    private void beginScenarioStep(String runtimeStepId, Object[] arguments) {
      StepTemplate expected = expected(runtimeStepId);
      active =
          new MutableStep(
              runtimeStepId, ScenarioTemplateRenderer.template(expected), arguments, null);
      steps.add(active);
    }

    private void finish(Throwable failure) {
      if (active != null && active.status == null) {
        completeActive(
            failure == null
                ? NarrativeStepStatus.PASS
                : failure instanceof TestAbortedException
                    ? NarrativeStepStatus.ABORTED
                    : NarrativeStepStatus.FAIL);
      }
      if (failure != null) {
        while (nextStep < scenario.steps().size()) {
          StepTemplate skipped = scenario.steps().get(nextStep++);
          steps.add(
              new MutableStep(
                  skipped.stepId(),
                  ScenarioTemplateRenderer.template(skipped),
                  new Object[0],
                  NarrativeStepStatus.SKIPPED));
        }
      }
    }

    private void completeActive(NarrativeStepStatus status) {
      if (active != null && active.status == null) {
        active.status = status;
        active.finish();
      }
    }

    private void recordComparison(ExpectedActualComparison comparison) {
      if (active == null || active.status != null) {
        throw new ToppleCatException(
            "ToppleCase.verify(...) mismatch must occur inside an active compiler-described Step.");
      }
      active.comparisons.add(comparison);
    }

    private StepTemplate expected(String runtimeStepId) {
      if (nextStep >= scenario.steps().size()) {
        throw new ToppleCatException(
            "ToppleCat scenario "
                + scenario.scenarioId()
                + " recorded unexpected extra Step "
                + runtimeStepId
                + ".");
      }
      StepTemplate expected = scenario.steps().get(nextStep++);
      if (!expected.stepId().equals(runtimeStepId)) {
        throw new ToppleCatException(
            "ToppleCat scenario "
                + scenario.scenarioId()
                + " expected Step "
                + expected.stepId()
                + " but runtime selected "
                + runtimeStepId
                + ".");
      }
      return expected;
    }

    private NarrativeExecution snapshot() {
      return new NarrativeExecution(
          definitionDigest(), caseId, steps.stream().map(MutableStep::snapshot).toList());
    }

    private String definitionDigest() {
      String configured = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
      if (configured == null || configured.isBlank()) {
        return "";
      }
      try {
        return ContractDefinitionJson.read(Files.readString(Path.of(configured))).digest();
      } catch (IOException exception) {
        throw new ToppleCatException(
            "Cannot read ToppleCat contract definition digest.", exception);
      }
    }
  }

  private static final class MutableStep {
    private final String stepId;
    private final String sentence;
    private final List<tools.jackson.databind.JsonNode> arguments;
    private final List<AttachmentRef> attachments = new ArrayList<>();
    private final List<ExpectedActualComparison> comparisons = new ArrayList<>();
    private final long startedAt = System.nanoTime();
    private long durationNanos;
    private NarrativeStepStatus status;

    private MutableStep(
        String stepId, String sentence, Object[] arguments, NarrativeStepStatus status) {
      this.stepId = stepId;
      this.sentence = sentence;
      this.arguments =
          java.util.Arrays.stream(arguments)
              .map(value -> (tools.jackson.databind.JsonNode) JSON.valueToTree(value))
              .toList();
      this.status = status;
      if (status != null) {
        finish();
      }
    }

    private void finish() {
      if (durationNanos == 0) {
        durationNanos = Math.max(0, System.nanoTime() - startedAt);
      }
    }

    private void attach(AttachmentRef attachment) {
      attachments.add(attachment);
    }

    private NarrativeStep snapshot() {
      return new NarrativeStep(
          stepId,
          sentence,
          status == null ? NarrativeStepStatus.SKIPPED : status,
          durationNanos,
          arguments,
          attachments,
          "",
          comparisons);
    }
  }

  private static final class AttachmentSizeException extends RuntimeException {
    private final IOException cause;

    private AttachmentSizeException(IOException cause) {
      this.cause = cause;
    }
  }
}
