package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertyModelTest {
  @Test
  void propertyDefinitionsParticipateInTheVersionedContractDigest() {
    PropertyDefinition publicProperty = property("PUBLIC", "a");
    PropertyDefinition changedProperty = property("PUBLIC", "b");

    ContractDefinition first =
        ContractDefinition.withComputedDigest(
            List.of(
                new AcceptanceContract(
                    "AC-PROPERTY",
                    "Totals remain stable",
                    scenario(),
                    List.of(),
                    List.of(publicProperty))));
    ContractDefinition changed =
        ContractDefinition.withComputedDigest(
            List.of(
                new AcceptanceContract(
                    "AC-PROPERTY",
                    "Totals remain stable",
                    scenario(),
                    List.of(),
                    List.of(changedProperty))));

    assertEquals("topplecat.contract-definition.v3", first.schemaVersion());
    assertNotEquals(first.digest(), changed.digest());
    assertEquals(first, ContractDefinitionJson.read(ContractDefinitionJson.write(first)));
  }

  @Test
  void aggregateEvidenceRejectsGateOnlyVerdictsWhileGatesCanUseThem() {
    assertThrows(
        RuntimeException.class,
        () ->
            ToppleEvidenceJson.create(
                "run-1",
                "2026-07-29T00:00:00Z",
                EvidenceVerdict.NOT_APPLICABLE,
                List.of(),
                Map.of()));

    ToppleEvidence evidence =
        ToppleEvidenceJson.create(
            "run-1",
            "2026-07-29T00:00:00Z",
            EvidenceVerdict.PASS,
            List.of(new EvidenceGate("PROPERTY", EvidenceVerdict.NOT_APPLICABLE)),
            Map.of());

    assertEquals("topplecat.evidence.v2", evidence.schemaVersion());
    assertEquals(evidence, ToppleEvidenceJson.read(ToppleEvidenceJson.write(evidence)));
  }

  @Test
  void propertyPolicyAndScopeAreExplicitAndTamperEvident() {
    VerificationPolicy enabled =
        new VerificationPolicy(
            "0.0.7", true, true, true, true, 100, MutationProducerKind.DEFAULT, null);
    VerificationPolicy disabled =
        new VerificationPolicy(
            "0.0.7", true, true, false, true, 100, MutationProducerKind.DEFAULT, null);
    VerificationScope scope =
        new VerificationScope(
            VerificationScope.SCHEMA_VERSION,
            SelectedSpecScope.empty(),
            VerificationScope.HIDDEN_SELECTED_SPECS,
            VerificationScope.MUTATION_ALL_PUBLIC_ACCEPTANCE_CONTRACTS,
            VerificationScope.PROPERTY_PUBLIC_SELECTED_SPECS);

    ReviewerContractApproval first =
        ReviewerContractApproval.create(
            List.of(
                new PublicContractEntry(
                    "src/test/java/example/PropertyContractTest.java", "a".repeat(64))),
            "b".repeat(64),
            enabled,
            SelectedSpecScope.empty());
    ReviewerContractApproval changed =
        ReviewerContractApproval.create(
            List.of(
                new PublicContractEntry(
                    "src/test/java/example/PropertyContractTest.java", "a".repeat(64))),
            "b".repeat(64),
            disabled,
            SelectedSpecScope.empty());

    assertEquals("topplecat.contract-approval.v5", first.schemaVersion());
    assertNotEquals(first.approvalDigest(), changed.approvalDigest());
    assertEquals(scope, VerificationScopeJson.read(VerificationScopeJson.write(scope)));
    assertThrows(
        RuntimeException.class,
        () ->
            VerificationScopeJson.read(
                """
                {"schemaVersion":"topplecat.verification-scope.v2","selectedSpecScope":{"schemaVersion":"topplecat.selected-spec-scope.v1","specDocuments":[],"acceptanceConditionIds":[],"acceptanceConditionSetDigest":"%s"},"hiddenMode":"ALL","mutationMode":"ALL_CANONICAL_CONTRACTS","publicPropertyMode":"ALL_PUBLIC","hiddenPropertyMode":"ALL"}
                """
                    .formatted("0".repeat(64))));
  }

  @Test
  void propertyResultsKeepGeneratedChoicesOutOfCaseRunEvidence() {
    PropertyResult result =
        new PropertyResult(
            "AC-PROPERTY",
            "example.PropertyContract#lineOrder(Lio/github/samzhu/topplecat/junit/property/PropertyTrials;)V",
            PropertyExecutionState.COMPLETED_COUNTEREXAMPLE,
            200,
            12,
            4,
            8,
            0,
            List.of(new PropertyClassification("boundary", 4, 33.333, 5.0)),
            42L,
            true,
            "replay-token",
            new PropertyCounterexample("{\"amount\":100}", List.of()),
            new PropertyCounterexample("{\"amount\":0}", List.of(0, 1)),
            2,
            true,
            null);
    PropertyResults results =
        new PropertyResults(PropertyResults.SCHEMA_VERSION, "run-1", List.of(result));

    assertEquals(results, PropertyResultsJson.read(PropertyResultsJson.write(results)));
    assertThrows(
        ToppleCatException.class,
        () ->
            new PropertyResult(
                "AC-PROPERTY",
                result.methodIdentity(),
                PropertyExecutionState.COMPLETED_PASS,
                2,
                1,
                0,
                1,
                0,
                List.of(),
                42L,
                true,
                null,
                null,
                null,
                0,
                false,
                null));
  }

  private static PropertyDefinition property(String title, String sourceDigest) {
    return new PropertyDefinition(
        "AC-PROPERTY",
        "example.PropertyContract#lineOrder(Lio/github/samzhu/topplecat/junit/property/PropertyTrials;)V",
        title,
        200,
        1_000,
        500,
        new SourceRef("PropertyContract.java", 10, 14),
        sourceDigest.repeat(64));
  }

  private static ScenarioTemplate scenario() {
    return new ScenarioTemplate(
        "AC-PROPERTY|example.PropertyContract#examples(LToppleCase;)V",
        "example.PropertyContract#examples(LToppleCase;)V",
        new SourceRef("PropertyContract.java", 1, 8),
        List.of(
            new StepTemplate(
                "example.PropertyContract#given()Lexample/PropertyContract;",
                StepPhase.GIVEN,
                List.of(new StepToken(StepTokenKind.PHASE, "GIVEN")),
                List.of(),
                new SourceRef("PropertyContract.java", 2, 9))));
  }
}
