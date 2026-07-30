package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.Objects;

/**
 * The one compiler-described Given/When/Then narrative for an acceptance invocation.
 *
 * <p>ToppleCat creates and binds this type for a supported acceptance method. Application code
 * selects a phase by passing the concrete Stage parameter that belongs to the same invocation.
 */
public final class ToppleScenario {
  private Selector selector;

  ToppleScenario() {}

  /** Arms the next compiled Step as Given and returns the supplied Stage proxy. */
  public <STAGE extends ToppleStage> STAGE given(STAGE stage) {
    return select(StepPhase.GIVEN, stage);
  }

  /** Arms the next compiled Step as When and returns the supplied Stage proxy. */
  public <STAGE extends ToppleStage> STAGE when(STAGE stage) {
    return select(StepPhase.WHEN, stage);
  }

  /** Arms the next compiled Step as Then and returns the supplied Stage proxy. */
  public <STAGE extends ToppleStage> STAGE then(STAGE stage) {
    return select(StepPhase.THEN, stage);
  }

  /** Arms the next compiled Step as And and returns the supplied Stage proxy. */
  public <STAGE extends ToppleStage> STAGE and(STAGE stage) {
    return select(StepPhase.AND, stage);
  }

  private <STAGE extends ToppleStage> STAGE select(StepPhase phase, STAGE stage) {
    STAGE selected = Objects.requireNonNull(stage, "stage");
    if (selector == null) {
      throw new ToppleCatException(
          "ToppleScenario is available only for a compiler-approved ToppleCat acceptance"
              + " invocation.");
    }
    return selector.select(phase, selected);
  }

  void bind(Selector binding) {
    if (selector != null) {
      throw new ToppleCatException("ToppleScenario is already bound to an acceptance invocation.");
    }
    selector = Objects.requireNonNull(binding, "binding");
  }

  interface Selector {
    <STAGE extends ToppleStage> STAGE select(StepPhase phase, STAGE stage);
  }
}
