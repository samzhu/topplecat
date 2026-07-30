package io.github.samzhu.topplecat.core;

import java.time.Duration;
import java.util.List;

/** Runtime outcome for one compiled step identity. */
public record StepRun(
    String stepId,
    NarrativeStepStatus status,
    Duration duration,
    List<Object> actualArguments,
    List<AttachmentRef> attachments,
    String failureRef) {
  public StepRun {
    if (stepId == null || stepId.isBlank()) {
      throw new ToppleCatException("Step run id is required.");
    }
    status = status == null ? NarrativeStepStatus.SKIPPED : status;
    duration = duration == null ? Duration.ZERO : duration;
    actualArguments = List.copyOf(actualArguments == null ? List.of() : actualArguments);
    attachments = List.copyOf(attachments == null ? List.of() : attachments);
    failureRef = failureRef == null ? "" : failureRef;
  }
}
