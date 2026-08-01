package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JsonContractComparisonTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void distinguishesNestedChangedMissingAndUnexpectedValuesInDeterministicPaths() throws Exception {
    ExpectedActualComparison comparison =
        JsonContractComparison.compare(
            "receipt",
            JSON.readTree(
                """
                {"lines":[{"total":10},{"total":20}],"required":true,"unchanged":200.00}
                """),
            JSON.readTree(
                """
                {"lines":[{"total":10},{"total":21},{"extra":"x"}],"unexpected":false,"unchanged":200}
                """));

    assertEquals(
        List.of(
            "expected.receipt.lines[1].total",
            "expected.receipt.lines[2]",
            "expected.receipt.required",
            "expected.receipt.unexpected"),
        comparison.differences().stream().map(ExpectedActualDifference::path).toList());
    assertEquals(
        List.of(
            ExpectedActualDifference.Kind.CHANGED,
            ExpectedActualDifference.Kind.UNEXPECTED_ACTUAL,
            ExpectedActualDifference.Kind.MISSING_EXPECTED,
            ExpectedActualDifference.Kind.UNEXPECTED_ACTUAL),
        comparison.differences().stream().map(ExpectedActualDifference::kind).toList());
    assertTrue(
        JsonContractComparison.equivalent(JSON.readTree("200.00"), JSON.readTree("200")),
        "mathematical JSON numeric equality remains authoritative");
  }
}
