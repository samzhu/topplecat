package io.github.samzhu.topplecat.core;

import tools.jackson.databind.JsonNode;

import java.util.List;

/** One compiled Stage identity and its runtime result for a single Topple case. */
public record NarrativeStep(String stepId, String sentence, NarrativeStepStatus status, long durationNanos,
                            List<JsonNode> actualArguments, List<AttachmentRef> attachments, String failureRef) {
    public NarrativeStep {
        stepId = stepId == null ? "" : stepId;
        sentence = sentence == null ? "" : sentence;
        status = status == null ? NarrativeStepStatus.SKIPPED : status;
        durationNanos = Math.max(0, durationNanos);
        actualArguments = List.copyOf(actualArguments == null ? List.of() : actualArguments.stream().map(JsonNode::deepCopy).toList());
        attachments = List.copyOf(attachments == null ? List.of() : attachments);
        failureRef = failureRef == null ? "" : failureRef;
    }
}
