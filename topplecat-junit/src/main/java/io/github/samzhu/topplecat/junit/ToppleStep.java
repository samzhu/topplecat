package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.Objects;

/** Narrow, framework-provided capability for the currently active compiled Step. */
public final class ToppleStep {
  private AttachmentSink attachments;

  ToppleStep() {}

  /** Adds an allowlisted attachment to the active compiled Step. */
  public void attach(ToppleAttachment attachment) {
    if (attachments == null) {
      throw new ToppleCatException("ToppleStep.attach(...) requires an active compiled Step.");
    }
    attachments.attach(Objects.requireNonNull(attachment, "attachment"));
  }

  void bind(AttachmentSink sink) {
    attachments = Objects.requireNonNull(sink, "sink");
  }

  void clear() {
    attachments = null;
  }

  interface AttachmentSink {
    void attach(ToppleAttachment attachment);
  }
}
