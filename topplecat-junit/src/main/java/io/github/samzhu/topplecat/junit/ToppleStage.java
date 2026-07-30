package io.github.samzhu.topplecat.junit;

/**
 * Base class for Java acceptance Stages.
 *
 * <p>With {@link ToppleScenario}, a direct, concrete Stage method is a compiler-described Step:
 * author the method as ordinary Java and call {@link #step()} only for scoped evidence attachments.
 */
public abstract class ToppleStage {
  private final ToppleStep step = new ToppleStep();

  /** Returns the narrowly scoped handle for the currently executing compiled Step. */
  protected final ToppleStep step() {
    return step;
  }

  final void bindStep(ToppleStep.AttachmentSink attachments) {
    step.bind(attachments);
  }

  final void clearStep() {
    step.clear();
  }
}
