package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import io.github.samzhu.topplecat.pitest.PitMutationEvidence;
import io.github.samzhu.topplecat.pitest.PitMutatorSummary;
import io.github.samzhu.topplecat.pitest.PitOutcomeCount;
import io.github.samzhu.topplecat.pitest.ToppleCatManagedMutationProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

class MutationGateResultsTest {
  @Test
  void treatsZeroProducerMutantsAsIncompleteRatherThanPassingAnEmptyAssessmentList() {
    MutationGateResults results =
        new MutationGateResults(
            MutationGateResults.SCHEMA_VERSION,
            ToppleCatManagedMutationProfile.PIT_VERSION,
            ToppleCatManagedMutationProfile.PROFILE_ID,
            ToppleCatManagedMutationProfile.operatorIds(),
            0,
            0,
            0,
            List.of(),
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
            ToppleCatManagedMutationProfile.PIT_VERSION,
            ToppleCatManagedMutationProfile.PROFILE_ID,
            ToppleCatManagedMutationProfile.operatorIds(),
            1,
            1,
            0,
            List.of(new PitOutcomeCount("FUTURE_PIT_STATUS", false, 1)),
            List.of(),
            List.of(
                new PitMutatorSummary(
                    "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
                    1,
                    List.of(new PitOutcomeCount("FUTURE_PIT_STATUS", false, 1)))),
            List.of(
                new PitMutationAssessment(
                    "AC-ORDER",
                    List.of("shop.OrderAcceptanceTest#createsOrder()V"),
                    1,
                    1,
                    100,
                    100,
                    List.of(new PitOutcomeCount("FUTURE_PIT_STATUS", false, 1)),
                    false)),
            List.of(
                new PitMutationEvidence(
                    false,
                    "FUTURE_PIT_STATUS",
                    "shop.OrderService",
                    "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
                    "Replaced integer addition with subtraction",
                    List.of("covering"),
                    List.of("killing"),
                    List.of("succeeding"),
                    List.of("AC-ORDER"))));

    assertEquals(EvidenceVerdict.PASS, results.verdict());
    assertEquals("FUTURE_PIT_STATUS", results.producerOutcomeCounts().getFirst().status());
    assertEquals(false, results.mutations().getFirst().detected());
  }

  @Test
  void treatsZeroCoverageAsANonblockingAttributionGapWhenAnotherAcMeetsItsThreshold() {
    MutationGateResults results =
        new MutationGateResults(
            MutationGateResults.SCHEMA_VERSION,
            ToppleCatManagedMutationProfile.PIT_VERSION,
            ToppleCatManagedMutationProfile.PROFILE_ID,
            ToppleCatManagedMutationProfile.operatorIds(),
            2,
            1,
            1,
            List.of(
                new PitOutcomeCount("KILLED", true, 1),
                new PitOutcomeCount("NO_COVERAGE", false, 1)),
            List.of(new PitOutcomeCount("NO_COVERAGE", false, 1)),
            List.of(
                new PitMutatorSummary(
                    "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
                    2,
                    List.of(
                        new PitOutcomeCount("KILLED", true, 1),
                        new PitOutcomeCount("NO_COVERAGE", false, 1)))),
            List.of(
                new PitMutationAssessment(
                    "AC-COVERED",
                    List.of("shop.CoveredAcceptanceTest#covers()V"),
                    1,
                    1,
                    100,
                    100,
                    List.of(new PitOutcomeCount("KILLED", true, 1)),
                    false),
                new PitMutationAssessment(
                    "AC-GAP",
                    List.of("shop.GapAcceptanceTest#gaps()V"),
                    0,
                    0,
                    100,
                    0,
                    List.of(),
                    true)),
            List.of(
                mutation(true, "KILLED", List.of("AC-COVERED")),
                mutation(false, "NO_COVERAGE", List.of())));

    assertEquals(EvidenceVerdict.PASS, results.verdict());
    assertEquals(true, results.assessments().get(1).attributionGap());
  }

  @Test
  void failsUsableEvidenceWhenNoMutationHasExactPublicAttribution() {
    MutationGateResults results =
        new MutationGateResults(
            MutationGateResults.SCHEMA_VERSION,
            ToppleCatManagedMutationProfile.PIT_VERSION,
            ToppleCatManagedMutationProfile.PROFILE_ID,
            ToppleCatManagedMutationProfile.operatorIds(),
            1,
            0,
            1,
            List.of(new PitOutcomeCount("NO_COVERAGE", false, 1)),
            List.of(new PitOutcomeCount("NO_COVERAGE", false, 1)),
            List.of(
                new PitMutatorSummary(
                    "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
                    1,
                    List.of(new PitOutcomeCount("NO_COVERAGE", false, 1)))),
            List.of(
                new PitMutationAssessment(
                    "AC-GAP",
                    List.of("shop.GapAcceptanceTest#gaps()V"),
                    0,
                    0,
                    100,
                    0,
                    List.of(),
                    true)),
            List.of(mutation(false, "NO_COVERAGE", List.of())));

    assertEquals(EvidenceVerdict.FAIL, results.verdict());
  }

  @Test
  void failsWhenAnAttributedAcceptanceMethodDoesNotDetectEveryMutation() {
    MutationGateResults results =
        new MutationGateResults(
            MutationGateResults.SCHEMA_VERSION,
            ToppleCatManagedMutationProfile.PIT_VERSION,
            ToppleCatManagedMutationProfile.PROFILE_ID,
            ToppleCatManagedMutationProfile.operatorIds(),
            2,
            2,
            0,
            List.of(
                new PitOutcomeCount("KILLED", true, 1), new PitOutcomeCount("SURVIVED", false, 1)),
            List.of(),
            List.of(
                new PitMutatorSummary(
                    "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
                    2,
                    List.of(
                        new PitOutcomeCount("KILLED", true, 1),
                        new PitOutcomeCount("SURVIVED", false, 1)))),
            List.of(
                new PitMutationAssessment(
                    "AC-ORDER",
                    List.of("shop.OrderAcceptanceTest#createsOrder()V"),
                    2,
                    1,
                    50,
                    50,
                    List.of(
                        new PitOutcomeCount("KILLED", true, 1),
                        new PitOutcomeCount("SURVIVED", false, 1)),
                    false)),
            List.of(
                mutation(true, "KILLED", List.of("AC-ORDER")),
                mutation(false, "SURVIVED", List.of("AC-ORDER"))));

    assertEquals(EvidenceVerdict.FAIL, results.verdict());
    assertEquals("SURVIVED", results.mutations().get(1).status());
    assertEquals(false, results.mutations().get(1).detected());
  }

  private static PitMutationEvidence mutation(
      boolean detected, String status, List<String> attributedAcceptanceConditions) {
    return new PitMutationEvidence(
        detected,
        status,
        "shop.OrderService",
        "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
        "Replaced integer addition with subtraction",
        List.of(),
        List.of(),
        List.of(),
        attributedAcceptanceConditions);
  }
}
