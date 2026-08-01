package io.github.samzhu.topplecat.pitest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.pitest.mutationtest.engine.gregor.config.Mutator;

class ToppleCatManagedMutationProfileTest {
  @Test
  void pinsAllTwelvePit1255OperatorIdsToTheirActualRawMutatorIdentities() {
    List<String> expectedOperatorIds =
        List.of(
            "TRUE_RETURNS",
            "FALSE_RETURNS",
            "PRIMITIVE_RETURNS",
            "EMPTY_RETURNS",
            "NULL_RETURNS",
            "REMOVE_CONDITIONALS_EQUAL_IF",
            "REMOVE_CONDITIONALS_EQUAL_ELSE",
            "REMOVE_CONDITIONALS_ORDER_IF",
            "REMOVE_CONDITIONALS_ORDER_ELSE",
            "CONDITIONALS_BOUNDARY",
            "VOID_METHOD_CALLS",
            "MATH");
    assertEquals(expectedOperatorIds, ToppleCatManagedMutationProfile.operatorIds());

    Map<String, String> actualRawIdentities = new LinkedHashMap<>();
    for (String operatorId : expectedOperatorIds) {
      var factories = Mutator.byName(operatorId);
      assertEquals(
          1, factories.size(), "PIT 1.25.5 must resolve exactly one factory for " + operatorId);
      actualRawIdentities.put(operatorId, factories.iterator().next().getGloballyUniqueId());
    }

    assertEquals(Set.copyOf(expectedOperatorIds), actualRawIdentities.keySet());
    assertEquals(
        actualRawIdentities.size(),
        Set.copyOf(actualRawIdentities.values()).size(),
        "each managed operator must have one distinct PIT raw identity");
    assertEquals(ToppleCatManagedMutationProfile.rawIdentitiesByOperator(), actualRawIdentities);
  }

  @Test
  void rejectsAReportWithARawMutatorOutsideTheManagedProfile() {
    PitMutationReport report =
        new PitMutationReport(
            List.of(
                new PitMutation(
                    true,
                    "KILLED",
                    "shop.OrderService",
                    "foreign.Mutator",
                    "foreign description",
                    List.of("covering"),
                    List.of("killing"),
                    List.of("succeeding"))),
            true);

    assertThrows(ToppleCatException.class, () -> ToppleCatManagedMutationProfile.validate(report));
  }
}
