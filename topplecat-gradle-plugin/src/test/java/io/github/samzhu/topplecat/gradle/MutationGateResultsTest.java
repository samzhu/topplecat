package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import io.github.samzhu.topplecat.pitest.PitMutationEvidence;
import io.github.samzhu.topplecat.pitest.PitOutcomeCount;
import java.util.List;
import org.junit.jupiter.api.Test;

class MutationGateResultsTest {
  @Test
  void treatsZeroProducerMutantsAsIncompleteRatherThanPassingAnEmptyAssessmentList() {
    MutationGateResults results =
        new MutationGateResults(
            MutationGateResults.SCHEMA_VERSION,
            0,
            0,
            0,
            List.of(),
            List.of(),
            List.of(),
            List.of());

    assertEquals(EvidenceVerdict.INCOMPLETE, results.verdict());
    assertEquals(results, MutationGateResults.read(MutationGateResults.write(results)));
  }

  @Test
  void preservesUnknownPITOutcomesAndDoesNotUseDetectedToComputeTheMethodRate() {
    MutationGateResults results =
        new MutationGateResults(
            MutationGateResults.SCHEMA_VERSION,
            1,
            1,
            0,
            List.of(new PitOutcomeCount("FUTURE_PIT_STATUS", false, 1)),
            List.of(),
            List.of(
                new PitMutationAssessment(
                    "AC-ORDER",
                    List.of("shop.OrderAcceptanceTest#createsOrder()V"),
                    1,
                    1,
                    100,
                    100,
                    List.of(new PitOutcomeCount("FUTURE_PIT_STATUS", false, 1)),
                    EvidenceVerdict.PASS)),
            List.of(
                new PitMutationEvidence(
                    false,
                    "FUTURE_PIT_STATUS",
                    "shop.OrderService",
                    List.of("covering"),
                    List.of("killing"),
                    List.of("succeeding"),
                    List.of("AC-ORDER"))));

    assertEquals(EvidenceVerdict.PASS, results.verdict());
    assertEquals("FUTURE_PIT_STATUS", results.producerOutcomeCounts().getFirst().status());
    assertEquals(false, results.mutations().getFirst().detected());
  }
}
