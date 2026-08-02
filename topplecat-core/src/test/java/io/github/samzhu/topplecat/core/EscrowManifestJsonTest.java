package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class EscrowManifestJsonTest {
  @Test
  void writesAndReadsTheCurrentManifestWithItsApproval() {
    ReviewerContractApproval approval =
        ReviewerContractApproval.create(
            List.of(
                new PublicContractEntry(
                    "src/test/java/example/AmountAcceptanceTest.java", "b".repeat(64))),
            "c".repeat(64),
            new VerificationPolicy("0.0.13", true, true, true, true, 100));
    EscrowManifest manifest =
        new EscrowManifest(
            EscrowManifest.SCHEMA_VERSION_V2,
            EscrowState.HIDDEN,
            List.of(
                new EscrowEntry(
                    "src/hiddenTest/resources/topplecat/cases/amount.json",
                    "a".repeat(64),
                    EscrowSourceKind.HIDDEN_TEST)),
            approval);

    assertEquals(manifest, EscrowManifestJson.read(EscrowManifestJson.write(manifest)));
  }

  @Test
  void rejectsAManifestWithoutCurrentApproval() {
    String source =
        "{\"schemaVersion\":\"topplecat.escrow.v1\",\"state\":\"HIDDEN\",\"entries\":[]}";

    assertThrows(ToppleCatException.class, () -> EscrowManifestJson.read(source));
  }
}
