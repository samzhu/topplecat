package io.github.samzhu.topplecat.junit.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.PropertyExecutionState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertyEngineTest {
  @Test
  void runsDeterministicEdgesThenRandomTrialsAndRecordsClassificationCoverage() {
    PropertyEngine.Config config = config(12, 10L);

    PropertyEngine.Outcome first =
        PropertyEngine.execute(
            config,
            Generators.integers(-2, 2),
            List.of(new PropertyEngine.ClassificationRule<Integer>("zero", value -> value == 0)),
            Map.of("zero", 5.0),
            value -> {});
    PropertyEngine.Outcome second =
        PropertyEngine.execute(
            config,
            Generators.integers(-2, 2),
            List.of(new PropertyEngine.ClassificationRule<Integer>("zero", value -> value == 0)),
            Map.of("zero", 5.0),
            value -> {});

    assertEquals(PropertyExecutionState.COMPLETED_PASS, first.result().state());
    assertEquals(first.result(), second.result());
    assertTrue(first.result().edgeTrials() > 0);
    assertTrue(first.result().randomTrials() > 0);
    assertEquals(12, first.result().edgeTrials() + first.result().randomTrials());
    assertEquals(12, first.result().completedTrials());
  }

  @Test
  void shrinksAReproducibleCounterexampleWithoutClaimingGlobalMinimality() {
    PropertyEngine.Outcome outcome =
        PropertyEngine.execute(
            config(20, 7L),
            Generators.integers(-20, 20),
            List.of(),
            Map.of(),
            value -> {
              if ((Integer) value != 0) throw new AssertionError("not zero");
            });

    assertEquals(PropertyExecutionState.COMPLETED_COUNTEREXAMPLE, outcome.result().state());
    assertTrue(outcome.result().replayVerified());
    assertFalse(outcome.result().shrunkCounterexample().choicesJson().isBlank());
  }

  @Test
  void generatorExhaustionAndUnmetCoverageAreIncomplete() {
    PropertyEngine.Outcome exhausted =
        PropertyEngine.execute(
            config(5, 1L),
            Generators.integers(1, 1).filter(value -> false),
            List.of(),
            Map.of(),
            value -> {});
    PropertyEngine.Outcome coverage =
        PropertyEngine.execute(
            config(5, 1L),
            Generators.integers(1, 1),
            List.of(new PropertyEngine.ClassificationRule<Integer>("zero", value -> value == 0)),
            Map.of("zero", 1.0),
            value -> {});

    assertEquals(PropertyExecutionState.COMPLETED_INCOMPLETE, exhausted.result().state());
    assertEquals(PropertyExecutionState.COMPLETED_INCOMPLETE, coverage.result().state());
  }

  @Test
  void supportsFullIntegerBoundsAndSchedulesThreePartCombinationEdges() {
    PropertyEngine.Outcome fullRange =
        PropertyEngine.execute(
            config(7, 99L),
            Generators.integers(Integer.MIN_VALUE, Integer.MAX_VALUE),
            List.of(),
            Map.of(),
            value -> {});
    PropertyEngine.Outcome combined =
        PropertyEngine.execute(
            config(8, 3L),
            Generators.combine(
                Generators.booleans(),
                Generators.integers(0, 1),
                Generators.elements(List.of("a", "b")),
                (first, second, third) -> List.of(first, second, third)),
            List.of(),
            Map.of(),
            value -> {});

    assertEquals(PropertyExecutionState.COMPLETED_PASS, fullRange.result().state());
    assertEquals(7, fullRange.result().completedTrials());
    assertEquals(PropertyExecutionState.COMPLETED_PASS, combined.result().state());
    assertTrue(combined.result().edgeTrials() > 0);
  }

  @Test
  void replayTokenReproducesOnlyTheMatchingManagedExecutionContext() {
    PropertyEngine.Config managed =
        new PropertyEngine.Config(
            "AC-PROPERTY",
            "fixture.Property#rule(LPropertyTrials;)V",
            20,
            10,
            20,
            7L,
            "managed-context",
            null);
    PropertyEngine.Outcome original =
        PropertyEngine.execute(
            managed,
            Generators.integers(-2, 2),
            List.of(),
            Map.of(),
            value -> {
              if ((Integer) value != 0) throw new AssertionError("not zero");
            });
    PropertyEngine.Config replay =
        new PropertyEngine.Config(
            "AC-PROPERTY",
            "fixture.Property#rule(LPropertyTrials;)V",
            20,
            10,
            20,
            7L,
            "managed-context",
            original.result().replayToken());
    PropertyEngine.Config wrongContext =
        new PropertyEngine.Config(
            "AC-PROPERTY",
            "fixture.Property#rule(LPropertyTrials;)V",
            20,
            10,
            20,
            7L,
            "other-context",
            original.result().replayToken());

    PropertyEngine.Outcome replayed =
        PropertyEngine.execute(
            replay,
            Generators.integers(-2, 2),
            List.of(),
            Map.of(),
            value -> {
              if ((Integer) value != 0) throw new AssertionError("not zero");
            });
    PropertyEngine.Outcome mismatched =
        PropertyEngine.execute(
            wrongContext,
            Generators.integers(-2, 2),
            List.of(),
            Map.of(),
            value -> {
              if ((Integer) value != 0) throw new AssertionError("not zero");
            });

    assertEquals(PropertyExecutionState.COMPLETED_COUNTEREXAMPLE, replayed.result().state());
    assertEquals(
        original.result().shrunkCounterexample().choicesJson(),
        replayed.result().shrunkCounterexample().choicesJson());
    assertEquals(PropertyExecutionState.COMPLETED_INCOMPLETE, mismatched.result().state());
  }

  @Test
  void rejectedFilterValuesUseTheExactDiscardBudget() {
    PropertyEngine.Outcome outcome =
        PropertyEngine.execute(
            config(5, 1L),
            Generators.integers(1, 1).filter(value -> false),
            List.of(),
            Map.of(),
            value -> {});

    assertEquals(PropertyExecutionState.COMPLETED_INCOMPLETE, outcome.result().state());
    assertEquals(11, outcome.result().discards());
  }

  private static PropertyEngine.Config config(int tries, long seed) {
    return new PropertyEngine.Config(
        "AC-PROPERTY", "fixture.Property#rule(LPropertyTrials;)V", tries, 10, 20, seed);
  }
}
