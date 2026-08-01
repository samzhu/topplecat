package io.github.samzhu.topplecat.core;

import java.util.List;
import tools.jackson.databind.JsonNode;

/** One compiled Stage identity and its runtime result for a single Topple case. */
public record NarrativeStep(
    String stepId,
    String sentence,
    NarrativeStepStatus status,
    long durationNanos,
    List<JsonNode> actualArguments,
    List<AttachmentRef> attachments,
    String failureRef,
    List<ExpectedActualComparison> comparisons) {
  public NarrativeStep {
    stepId = stepId == null ? "" : stepId;
    sentence = sentence == null ? "" : sentence;
    status = status == null ? NarrativeStepStatus.SKIPPED : status;
    durationNanos = Math.max(0, durationNanos);
    actualArguments =
        List.copyOf(
            actualArguments == null
                ? List.of()
                : actualArguments.stream().map(JsonNode::deepCopy).toList());
    attachments = List.copyOf(attachments == null ? List.of() : attachments);
    failureRef = failureRef == null ? "" : failureRef;
    comparisons = List.copyOf(comparisons == null ? List.of() : comparisons);
  }

  public NarrativeStep(
      String stepId,
      String sentence,
      NarrativeStepStatus status,
      long durationNanos,
      List<JsonNode> actualArguments,
      List<AttachmentRef> attachments,
      String failureRef) {
    this(
        stepId,
        sentence,
        status,
        durationNanos,
        actualArguments,
        attachments,
        failureRef,
        List.of());
  }
}
