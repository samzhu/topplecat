package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** Complete reviewer-only result derived from one PIT full mutation matrix. */
public record PitMutationAttribution(
    int producerMutationCount,
    int uniquelyAttributedMutationCount,
    int unattributedMutationCount,
    List<PitOutcomeCount> producerOutcomeCounts,
    List<PitOutcomeCount> unattributedOutcomeCounts,
    List<PitMutationAssessment> assessments,
    List<PitMutationEvidence> mutations) {
  public PitMutationAttribution {
    if (producerMutationCount < 0
        || uniquelyAttributedMutationCount < 0
        || unattributedMutationCount < 0
        || uniquelyAttributedMutationCount + unattributedMutationCount != producerMutationCount) {
      throw new ToppleCatException("PIT mutation attribution counts are invalid.");
    }
    producerOutcomeCounts =
        List.copyOf(producerOutcomeCounts == null ? List.of() : producerOutcomeCounts);
    unattributedOutcomeCounts =
        List.copyOf(unattributedOutcomeCounts == null ? List.of() : unattributedOutcomeCounts);
    assessments = List.copyOf(assessments == null ? List.of() : assessments);
    mutations = List.copyOf(mutations == null ? List.of() : mutations);
    if (mutations.size() != producerMutationCount) {
      throw new ToppleCatException("PIT mutation attribution must retain every producer mutation.");
    }
  }
}
