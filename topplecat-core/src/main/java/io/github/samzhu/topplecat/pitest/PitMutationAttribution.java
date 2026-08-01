package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;

/** Complete reviewer-only result derived from one PIT full mutation matrix. */
public record PitMutationAttribution(
    String pitVersion,
    String managedProfileId,
    List<String> managedOperatorIds,
    int producerMutationCount,
    int uniquelyAttributedMutationCount,
    int unattributedMutationCount,
    List<PitOutcomeCount> producerOutcomeCounts,
    List<PitOutcomeCount> unattributedOutcomeCounts,
    List<PitMutatorSummary> perMutatorSummaries,
    List<PitMutationAssessment> assessments,
    List<PitMutationEvidence> mutations) {
  public PitMutationAttribution {
    if (!ToppleCatManagedMutationProfile.PIT_VERSION.equals(pitVersion)
        || !ToppleCatManagedMutationProfile.PROFILE_ID.equals(managedProfileId)
        || !ToppleCatManagedMutationProfile.operatorIds().equals(managedOperatorIds)) {
      throw new ToppleCatException("PIT managed mutation profile metadata is invalid.");
    }
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
    perMutatorSummaries =
        List.copyOf(perMutatorSummaries == null ? List.of() : perMutatorSummaries);
    assessments = List.copyOf(assessments == null ? List.of() : assessments);
    mutations = List.copyOf(mutations == null ? List.of() : mutations);
    if (mutations.size() != producerMutationCount) {
      throw new ToppleCatException("PIT mutation attribution must retain every producer mutation.");
    }
  }
}
