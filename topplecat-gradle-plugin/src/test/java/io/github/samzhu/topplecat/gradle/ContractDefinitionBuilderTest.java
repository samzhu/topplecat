package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.samzhu.topplecat.core.ArgumentBinding;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.CompilerPropertyDescriptor;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ContractDefinitionBuilderTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void rejectsDuplicateAcUnknownCaseAndUnresolvedCompiledBinding() throws Exception {
    CompilerScenarioDescriptor descriptor = descriptor("AC-ONE", "/inputs/cart/id");
    ToppleCaseData known = testCase("case-known", "AC-ONE", "{\"cart\":{\"id\":\"1\"}}");

    assertThrows(
        ToppleCatException.class,
        () -> ContractDefinitionBuilder.build(List.of(descriptor, descriptor), List.of(known)));
    assertThrows(
        ToppleCatException.class,
        () ->
            ContractDefinitionBuilder.build(
                List.of(descriptor), List.of(testCase("case-unknown", "AC-TWO", "{}"))));
    assertThrows(
        ToppleCatException.class,
        () ->
            ContractDefinitionBuilder.build(
                List.of(descriptor("AC-ONE", "/inputs/cart/missing")), List.of(known)));
  }

  @Test
  void attachesPropertiesToTheirExistingCanonicalAcceptanceCondition() throws Exception {
    CompilerScenarioDescriptor scenario = descriptor("AC-ONE", "/inputs/cart/id");
    CompilerPropertyDescriptor property = property("AC-ONE", "keepsTotalStable");

    assertEquals(
        1,
        ContractDefinitionBuilder.build(
                List.of(scenario),
                List.of(property),
                List.of(testCase("case-known", "AC-ONE", "{\"cart\":{\"id\":\"1\"}}")))
            .acceptanceConditions()
            .getFirst()
            .properties()
            .size());
    assertThrows(
        ToppleCatException.class,
        () ->
            ContractDefinitionBuilder.build(
                List.of(scenario),
                List.of(property("AC-TWO", "unknown")),
                List.of(testCase("case-known", "AC-ONE", "{\"cart\":{\"id\":\"1\"}}"))));
  }

  private static CompilerScenarioDescriptor descriptor(String acId, String pointer) {
    StepTemplate step =
        new StepTemplate(
            "fixture.Given#a_cart()V",
            StepPhase.GIVEN,
            List.of(
                new StepToken(StepTokenKind.PHASE, "GIVEN"),
                new StepToken(StepTokenKind.ARGUMENT, "0")),
            List.of(new ArgumentBinding(0, "cart", pointer)),
            new SourceRef("Fixture.java", 1, 1));
    return new CompilerScenarioDescriptor(
        CompilerScenarioDescriptor.SCHEMA_VERSION,
        acId,
        acId,
        acId + "|scenario",
        "fixture.Test",
        "accepts",
        "(LToppleCase;)V",
        new SourceRef("Fixture.java", 1, 1),
        List.of(step));
  }

  private static ToppleCaseData testCase(String caseId, String acId, String inputs)
      throws Exception {
    return new ToppleCaseData(
        caseId,
        acId,
        CaseVisibility.PUBLIC,
        JSON.readTree(inputs),
        JSON.readTree("{\"result\":true}"),
        Path.of(caseId + ".json"));
  }

  private static CompilerPropertyDescriptor property(String acId, String method) {
    return new CompilerPropertyDescriptor(
        CompilerPropertyDescriptor.SCHEMA_VERSION,
        acId,
        method,
        "fixture.PropertyTest",
        method,
        "(Lio/github/samzhu/topplecat/junit/property/PropertyTrials;)V",
        new SourceRef("PropertyTest.java", 10, 1),
        200,
        1_000,
        500,
        "a".repeat(64));
  }
}
