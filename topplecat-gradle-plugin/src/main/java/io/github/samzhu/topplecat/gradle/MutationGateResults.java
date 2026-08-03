package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import io.github.samzhu.topplecat.pitest.PitMutationAttribution;
import io.github.samzhu.topplecat.pitest.PitMutationEvidence;
import io.github.samzhu.topplecat.pitest.PitMutatorSummary;
import io.github.samzhu.topplecat.pitest.PitOutcomeCount;
import io.github.samzhu.topplecat.pitest.ToppleCatManagedMutationProfile;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/** Stable reviewer-only mutation-gate artifact written by the Gradle plugin. */
public record MutationGateResults(
    String schemaVersion,
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
  static final String SCHEMA_VERSION = "topplecat.mutation-results.v1";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  public MutationGateResults {
    if (!SCHEMA_VERSION.equals(schemaVersion)) {
      throw new IllegalArgumentException("Unsupported mutation results schema: " + schemaVersion);
    }
    if (!ToppleCatManagedMutationProfile.PIT_VERSION.equals(pitVersion)
        || !ToppleCatManagedMutationProfile.PROFILE_ID.equals(managedProfileId)
        || !ToppleCatManagedMutationProfile.operatorIds().equals(managedOperatorIds)) {
      throw new IllegalArgumentException("Mutation results managed profile metadata is invalid.");
    }
    if (producerMutationCount < 0
        || uniquelyAttributedMutationCount < 0
        || unattributedMutationCount < 0
        || uniquelyAttributedMutationCount + unattributedMutationCount != producerMutationCount) {
      throw new IllegalArgumentException("Mutation results counts are invalid.");
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
      throw new IllegalArgumentException("Mutation results must retain every PIT mutation.");
    }
  }

  static MutationGateResults from(PitMutationAttribution attribution) {
    return new MutationGateResults(
        SCHEMA_VERSION,
        attribution.pitVersion(),
        attribution.managedProfileId(),
        attribution.managedOperatorIds(),
        attribution.producerMutationCount(),
        attribution.uniquelyAttributedMutationCount(),
        attribution.unattributedMutationCount(),
        attribution.producerOutcomeCounts(),
        attribution.unattributedOutcomeCounts(),
        attribution.perMutatorSummaries(),
        attribution.assessments(),
        attribution.mutations());
  }

  static String write(MutationGateResults results) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(results) + "\n";
  }

  static MutationGateResults read(String source) {
    return JSON.readValue(source, MutationGateResults.class);
  }

  EvidenceVerdict verdict() {
    if (producerMutationCount == 0) {
      return EvidenceVerdict.INCOMPLETE;
    }
    if (uniquelyAttributedMutationCount == 0) {
      return EvidenceVerdict.FAIL;
    }
    boolean failingAttributedAssessment =
        assessments.stream()
            .filter(assessment -> !assessment.attributionGap())
            .anyMatch(
                assessment ->
                    assessment.killedByAcceptanceMethodMutantCount()
                        < assessment.coveredMutantCount());
    return failingAttributedAssessment ? EvidenceVerdict.FAIL : EvidenceVerdict.PASS;
  }
}
