package io.github.samzhu.topplecat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SelectedSpecScopeTest {
  @Test
  void canonicalizesDocumentsAndAcceptanceConditionsIntoATamperEvidentScope() {
    SelectedSpecScope scope =
        SelectedSpecScope.create(
            List.of(
                new SelectedSpecDocument("specs/b.md", "b".repeat(64)),
                new SelectedSpecDocument("specs/a.md", "a".repeat(64))),
            List.of("AC-B", "AC-A"));

    assertEquals(
        List.of("specs/a.md", "specs/b.md"),
        scope.specDocuments().stream().map(SelectedSpecDocument::path).toList());
    assertEquals(List.of("AC-A", "AC-B"), scope.acceptanceConditionIds());
    assertEquals(
        scope,
        VerificationScopeJson.read(
                VerificationScopeJson.write(
                    new VerificationScope(
                        VerificationScope.SCHEMA_VERSION,
                        scope,
                        VerificationScope.HIDDEN_SELECTED_ACCEPTANCE_CONDITIONS,
                        VerificationScope.MUTATION_ALL_PUBLIC_ACCEPTANCE_CONTRACTS,
                        VerificationScope.PROPERTY_PUBLIC_SELECTED_ACCEPTANCE_CONDITIONS)))
            .selectedSpecScope());
  }

  @Test
  void rejectsADigestThatDoesNotMatchTheSelectedAcceptanceConditions() {
    assertThrows(
        ToppleCatException.class,
        () ->
            new SelectedSpecScope(
                SelectedSpecScope.SCHEMA_VERSION,
                List.of(new SelectedSpecDocument("specs/a.md", "a".repeat(64))),
                List.of("AC-A"),
                "b".repeat(64)));
  }
}
