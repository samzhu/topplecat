package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContractIntegrityResultJsonTest {
  @Test
  void roundTripsReviewerOnlyMismatchDiagnostics() {
    ContractIntegrityResult result =
        new ContractIntegrityResult(
            ContractIntegrityResult.SCHEMA_VERSION,
            EvidenceVerdict.FAIL,
            "a".repeat(64),
            "b".repeat(64),
            List.of("src/test/resources/topplecat/cases/amount.json"),
            List.of(),
            List.of(),
            false,
            List.of("mutationThreshold"));

    assertEquals(
        result, ContractIntegrityResultJson.read(ContractIntegrityResultJson.write(result)));
  }

  @Test
  void rejectsDisabledIntegrityResults() {
    assertThrows(
        ToppleCatException.class,
        () ->
            new ContractIntegrityResult(
                ContractIntegrityResult.SCHEMA_VERSION,
                EvidenceVerdict.DISABLED,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                false,
                List.of()));
  }
}
