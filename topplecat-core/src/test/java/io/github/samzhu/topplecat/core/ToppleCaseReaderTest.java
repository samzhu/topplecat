package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToppleCaseReaderTest {
  @TempDir Path tempDir;

  @Test
  void readsJsonAndYamlWithoutFlatteningNestedValues() throws Exception {
    Path json = tempDir.resolve("public.json");
    Files.writeString(
        json,
        """
        [{
          "caseId":"coupon-public-500",
          "acId":"AC-CART-COUPON",
          "inputs":{"cart":{"items":[{"sku":"book","quantity":2}],"subtotal":500}},
          "expected":{"receipt":{"discount":100,"discountedSubtotal":400}}
        }]
        """);
    Path yaml = tempDir.resolve("hidden.yaml");
    Files.writeString(
        yaml,
        """
        cases:
          - caseId: coupon-hidden-800
            acId: AC-CART-COUPON
            inputs:
              cart:
                subtotal: 800
            expected:
              receipt:
                discount: 100
                discountedSubtotal: 700
        """);

    List<ToppleCaseData> cases =
        ToppleCaseReader.readAll(
            List.of(
                new ToppleCaseSource(json, CaseVisibility.PUBLIC),
                new ToppleCaseSource(yaml, CaseVisibility.HIDDEN)));

    assertEquals(
        List.of("coupon-hidden-800", "coupon-public-500"),
        cases.stream().map(ToppleCaseData::caseId).toList());
    assertEquals(2, cases.get(1).inputs().get("cart").get("items").get(0).get("quantity").asInt());
    assertEquals(CaseVisibility.HIDDEN, cases.getFirst().visibility());
    assertEquals(700, cases.getFirst().expected().get("receipt").get("discountedSubtotal").asInt());
  }

  @Test
  void rejectsAmbiguousOrIncompleteRowsBeforeTheyReachJUnit() throws Exception {
    Path invalid = tempDir.resolve("invalid.json");
    Files.writeString(
        invalid,
        """
        [{"caseId":"bad","acId":"AC-BAD","inputs":{},"expected":{"total":1},"typo":true}]
        """);

    ToppleCatException error =
        assertThrows(
            ToppleCatException.class,
            () -> ToppleCaseReader.read(new ToppleCaseSource(invalid, CaseVisibility.PUBLIC)));

    assertEquals(true, error.getMessage().contains("exactly"));
  }
}
