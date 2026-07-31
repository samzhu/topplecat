package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ContractQualityAdvisorTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void acceptsAnyPublicExpectedShapeVariantRegardlessOfValuesTypesOrObjectFieldOrder()
      throws Exception {
    List<ContractQualityAdvisory> advisories =
        ContractQualityAdvisor.analyze(
            List.of(
                row("public-simple", CaseVisibility.PUBLIC, "{\"receipt\":{\"accepted\":true}}"),
                row(
                    "public-shipping",
                    CaseVisibility.PUBLIC,
                    "{\"receipt\":{\"shipping\":{\"cost\":10},\"accepted\":false}}"),
                row("hidden-simple", CaseVisibility.HIDDEN, "{\"receipt\":{\"accepted\":\"yes\"}}"),
                row(
                    "hidden-shipping",
                    CaseVisibility.HIDDEN,
                    "{\"receipt\":{\"accepted\":1,\"shipping\":{\"cost\":\"10\"}}}")));

    assertFalse(
        advisories.stream()
            .anyMatch(
                advisory ->
                    ContractQualityAdvisory.EXPECTED_SHAPE_VARIANT_MISSING.equals(
                        advisory.ruleCode())));
  }

  @Test
  void warnsOnceAtExpectedRootForHiddenShapeDriftAndCountsVariantsAndRows() throws Exception {
    List<ContractQualityAdvisory> advisories =
        ContractQualityAdvisor.analyze(
            List.of(
                row("public-one", CaseVisibility.PUBLIC, "{\"receipt\":{\"accepted\":true}}"),
                row("public-two", CaseVisibility.PUBLIC, "{\"receipt\":{\"code\":\"OK\"}}"),
                row(
                    "hidden-one",
                    CaseVisibility.HIDDEN,
                    "{\"receipt\":{\"accepted\":true,\"reason\":\"new\"}}"),
                row(
                    "hidden-two",
                    CaseVisibility.HIDDEN,
                    "{\"receipt\":{\"accepted\":false,\"reason\":\"other\"}}")));

    assertEquals(
        List.of(
            new ContractQualityAdvisory(
                ContractQualityAdvisory.EXPECTED_SHAPE_VARIANT_MISSING,
                "AC-ORDER",
                "expected",
                2,
                2)),
        advisories);
  }

  @Test
  void treatsArraysAsTerminalExpectedFieldsForBothShapeAndIdentifierAnalysis() throws Exception {
    List<ContractQualityAdvisory> advisories =
        ContractQualityAdvisor.analyze(
            List.of(
                row(
                    "public-one",
                    CaseVisibility.PUBLIC,
                    "{\"items\":[{\"opaqueId\":\"public-1\",\"shape\":\"A\"}]}"),
                row(
                    "public-two",
                    CaseVisibility.PUBLIC,
                    "{\"items\":[{\"opaqueId\":\"public-2\",\"shape\":\"B\"}]}"),
                row(
                    "hidden-one",
                    CaseVisibility.HIDDEN,
                    "{\"items\":[{\"opaqueId\":\"hidden-1\",\"different\":true}]}"),
                row(
                    "hidden-two",
                    CaseVisibility.HIDDEN,
                    "{\"items\":[{\"opaqueId\":\"hidden-2\",\"different\":false}]}")));

    assertTrue(advisories.isEmpty());
  }

  @Test
  void flagsOnlyDistinctNonblankStringIdentifierLiteralsAtTheSameExpectedPath() throws Exception {
    List<ContractQualityAdvisory> advisories =
        ContractQualityAdvisor.analyze(
            List.of(
                row(
                    "public-one",
                    CaseVisibility.PUBLIC,
                    "{\"receipt\":{\"orderId\":\"public-1\"}}"),
                row(
                    "public-two",
                    CaseVisibility.PUBLIC,
                    "{\"receipt\":{\"orderId\":\"public-2\"}}"),
                row(
                    "hidden-one",
                    CaseVisibility.HIDDEN,
                    "{\"receipt\":{\"orderId\":\"hidden-1\"}}"),
                row(
                    "hidden-two",
                    CaseVisibility.HIDDEN,
                    "{\"receipt\":{\"orderId\":\"hidden-2\"}}")));

    assertEquals(
        List.of(
            new ContractQualityAdvisory(
                ContractQualityAdvisory.EXPECTED_OPAQUE_IDENTIFIER_LITERALS,
                "AC-ORDER",
                "expected.receipt.orderId",
                2,
                2)),
        advisories);
  }

  @Test
  void doesNotFlagDuplicateBlankNonStringOrNonSuffixValues() throws Exception {
    List<ContractQualityAdvisory> advisories =
        ContractQualityAdvisor.analyze(
            List.of(
                row("public-one", CaseVisibility.PUBLIC, "{\"id\":\"a\",\"orderId\":\"same\"}"),
                row("public-two", CaseVisibility.PUBLIC, "{\"id\":\"b\",\"orderId\":\"same\"}"),
                row("hidden-one", CaseVisibility.HIDDEN, "{\"id\":\"c\",\"orderId\":\" \"}"),
                row("hidden-two", CaseVisibility.HIDDEN, "{\"id\":\"d\",\"orderId\":42}")));

    assertFalse(
        advisories.stream()
            .anyMatch(
                advisory ->
                    ContractQualityAdvisory.EXPECTED_OPAQUE_IDENTIFIER_LITERALS.equals(
                        advisory.ruleCode())));
  }

  private static CaseDefinition row(String caseId, CaseVisibility visibility, String expected)
      throws Exception {
    return new CaseDefinition(
        caseId, "AC-ORDER", visibility, JSON.readTree("{}"), JSON.readTree(expected));
  }
}
