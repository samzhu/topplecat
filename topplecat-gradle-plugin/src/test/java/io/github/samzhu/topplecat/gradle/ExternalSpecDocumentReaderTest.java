package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.report.SpecMarkdownBlock;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalSpecDocumentReaderTest {
  @TempDir Path root;

  @Test
  void preservesCompleteSelectedMarkdownInDocumentOrderAndKeepsOnlyRealAcAnchors()
      throws Exception {
    Path specs = Files.createDirectories(root.resolve("specs"));
    Files.write(specs.resolve("receipt.png"), new byte[] {1, 2, 3});
    Path document = specs.resolve("checkout.md");
    Files.writeString(
        document,
        """
        # Checkout

        Authored checkout rule.

        <!-- topplecat:acceptance:AC-CHECKOUT -->

        A [safe link](https://example.test/spec) with `inline` code and **emphasis**.

        - item
        1. ordered item
        - [x] completed task

        > quoted context

        | Amount | Result |
        | --- | --- |
        | 500 | accepted |

        ![Receipt screenshot](receipt.png "Receipt")
        ![Remote screenshot](https://example.test/receipt.png)
        ![Traversal](../outside.png)

        ```json
        {"AC-FAKE-CODE": "does not select scope"}
        ```

        ```mermaid
        flowchart TD
        A[Cart] --> B[Receipt]
        ```

        <script>window.injected = true</script>
        """);

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(root, List.of(document));

    assertEquals(List.of("AC-CHECKOUT"), parsed.acceptanceConditionIds());
    assertFalse(parsed.locations().containsKey("AC-FAKE-CODE"));
    assertEquals("specs/checkout.md", parsed.documents().getFirst().path());
    List<SpecMarkdownBlock.Kind> kinds =
        parsed.documents().getFirst().blocks().stream().map(SpecMarkdownBlock::kind).toList();
    assertTrue(
        kinds.containsAll(
            List.of(
                SpecMarkdownBlock.Kind.HEADING,
                SpecMarkdownBlock.Kind.PARAGRAPH,
                SpecMarkdownBlock.Kind.LIST,
                SpecMarkdownBlock.Kind.ORDERED_LIST,
                SpecMarkdownBlock.Kind.TASK_LIST,
                SpecMarkdownBlock.Kind.BLOCK_QUOTE,
                SpecMarkdownBlock.Kind.TABLE,
                SpecMarkdownBlock.Kind.IMAGE,
                SpecMarkdownBlock.Kind.CODE_FENCE,
                SpecMarkdownBlock.Kind.MERMAID,
                SpecMarkdownBlock.Kind.ACCEPTANCE_MARKER,
                SpecMarkdownBlock.Kind.FALLBACK)));
    SpecMarkdownBlock quote =
        parsed.documents().getFirst().blocks().stream()
            .filter(block -> block.kind() == SpecMarkdownBlock.Kind.BLOCK_QUOTE)
            .findFirst()
            .orElseThrow();
    assertTrue(
        quote.children().stream().anyMatch(block -> block.text().contains("quoted context")));
    assertFalse(quote.children().stream().anyMatch(block -> block.text().contains("> quoted")));
    SpecMarkdownBlock list =
        parsed.documents().getFirst().blocks().stream()
            .filter(block -> block.kind() == SpecMarkdownBlock.Kind.LIST)
            .findFirst()
            .orElseThrow();
    assertTrue(
        list.children().stream()
            .allMatch(block -> block.kind() == SpecMarkdownBlock.Kind.LIST_ITEM));
    assertEquals(1, parsed.documents().getFirst().assets().size());
    assertTrue(
        parsed.documents().getFirst().assets().getFirst().bundlePath().startsWith("assets/spec/"));
    List<SpecMarkdownBlock> images =
        parsed.documents().getFirst().blocks().stream()
            .filter(block -> block.kind() == SpecMarkdownBlock.Kind.IMAGE)
            .toList();
    assertTrue(images.getFirst().destination().startsWith("assets/spec/"));
    assertEquals("", images.getLast().destination());
  }

  @Test
  void idBearingMarkersIgnoreHeadingsAndOrdinaryReferences() throws Exception {
    Path document = root.resolve("checkout.md");
    Files.writeString(
        document,
        """
        Background mentions AC-CHECKOUT-001 and [AC-CHECKOUT-002](https://example.test).
        - AC-CHECKOUT-001 is only a task reference
        | AC-CHECKOUT-002 | text |
        | --- | --- |
        | AC-CHECKOUT-001 | text |
        `AC-CHECKOUT-002` remains ordinary text.
        <!-- AC-CHECKOUT-001 is ordinary text -->

        #### First rule

        First authored rule.

        <!-- topplecat:acceptance:AC-CHECKOUT-001 -->

        Second rule
        ------------

        Second authored rule.

        <!-- topplecat:acceptance:AC-CHECKOUT-002 -->

        ```markdown
        # AC-CODE: not a declaration
        <!-- topplecat:acceptance -->
        ```
        """);

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(root, List.of(document));

    assertTrue(parsed.diagnostics().isEmpty(), parsed.diagnosticMessage());
    assertEquals(List.of("AC-CHECKOUT-001", "AC-CHECKOUT-002"), parsed.acceptanceConditionIds());
    assertEquals(2, parsed.locations().size());
    assertEquals(
        2,
        parsed.documents().getFirst().blocks().stream()
            .filter(block -> block.kind() == SpecMarkdownBlock.Kind.ACCEPTANCE_MARKER)
            .count());
    assertFalse(parsed.acceptanceConditionIds().contains("AC-CODE"));
  }

  @Test
  void aggregatesStructuralDiagnosticsAndListsDuplicateLocations() throws Exception {
    Path first = root.resolve("a.md");
    Path second = root.resolve("b.md");
    Files.writeString(
        first,
        """
        # First

        <!-- topplecat:acceptance:AC-DUP -->

        # Broken marker

        <!-- topplecat:acceptance:AC-DUP -->
        <!-- topplecat:acceptance:AC-BROKEN -->
        """);
    Files.writeString(
        second,
        """
        AC-DUP: a legacy reference

        # Second

        <!-- topplecat:acceptance:AC-DUP -->
        """);

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(root, List.of(second, first));
    String diagnostics = parsed.diagnosticMessage();
    assertTrue(diagnostics.contains("TC-SPEC-AC-MARKER-DUPLICATE"), diagnostics);
    assertTrue(diagnostics.contains("a.md:3:1"), diagnostics);
    assertTrue(diagnostics.contains("b.md:5:1"), diagnostics);
    assertFalse(diagnostics.contains("TC-SPEC-AC-MARKER-LEGACY"), diagnostics);
    assertTrue(diagnostics.contains("TC-SPEC-AC-MARKER-DUPLICATE"), diagnostics);
  }

  @Test
  void ordinaryReferenceMayBeInAnotherSelectedDocumentThanItsDeclaration() throws Exception {
    Path first = root.resolve("a.md");
    Path second = root.resolve("b.md");
    Files.writeString(first, "See AC-CROSS for the shared checkout rule.\n");
    Files.writeString(
        second,
        "# Shared checkout rule\n\n"
            + "The canonical rule is authored here.\n\n"
            + "<!-- topplecat:acceptance:AC-CROSS -->\n");

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(root, List.of(first, second));

    assertTrue(parsed.diagnostics().isEmpty(), parsed.diagnosticMessage());
    assertEquals(List.of("AC-CROSS"), parsed.acceptanceConditionIds());
    assertEquals("b.md", parsed.locations().get("AC-CROSS").documentPath());
  }

  @Test
  void ordinaryReferencesNeverDeclareOrRequireAnAcMarker() throws Exception {
    Path first = root.resolve("a.md");
    Path second = root.resolve("b.md");
    Files.writeString(first, "See AC-MISSING for the first rule.\n");
    Files.writeString(second, "Another ordinary mention of AC-MISSING.\n");

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(root, List.of(first, second));

    assertTrue(parsed.diagnostics().isEmpty(), parsed.diagnosticMessage());
    assertTrue(parsed.acceptanceConditionIds().isEmpty());
  }

  @Test
  void sharedCommonMarkFixtureKeepsContainerMarkersOutOfDocumentScope() throws Exception {
    Path document = root.resolve("valid.md");
    try (var source = getClass().getResourceAsStream("/selected-spec-fixtures/valid.md")) {
      assertTrue(source != null);
      Files.copy(source, document);
    }

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(root, List.of(document));

    assertTrue(parsed.diagnostics().isEmpty(), parsed.diagnosticMessage());
    assertEquals(List.of("AC-CHECKOUT-001", "AC-CHECKOUT-002"), parsed.acceptanceConditionIds());
    assertEquals(
        2,
        parsed.documents().getFirst().blocks().stream()
            .filter(block -> block.kind() == SpecMarkdownBlock.Kind.ACCEPTANCE_MARKER)
            .count());
  }

  @Test
  void sharedInvalidFixturesRetainDistinctStructuralDiagnostics() throws Exception {
    Map<String, String> expectedCodes =
        Map.of(
            "invalid-duplicate.md", "TC-SPEC-AC-MARKER-DUPLICATE",
            "invalid-overlap.md", "TC-SPEC-AC-MARKER-MALFORMED",
            "invalid-orphan.md", "TC-SPEC-AC-MARKER-LEGACY");
    for (String fixture :
        List.of("invalid-duplicate.md", "invalid-overlap.md", "invalid-orphan.md")) {
      Path document = root.resolve(fixture);
      try (var source = getClass().getResourceAsStream("/selected-spec-fixtures/" + fixture)) {
        assertTrue(source != null);
        Files.copy(source, document);
      }
      ExternalSpecDocumentReader.ParsedSpecs parsed =
          ExternalSpecDocumentReader.read(root, List.of(document));
      String diagnostics = parsed.diagnosticMessage();
      assertFalse(parsed.diagnostics().isEmpty(), fixture);
      assertTrue(diagnostics.contains(expectedCodes.get(fixture)), diagnostics);
    }
  }

  @Test
  void duplicateMarkerDiagnosticNamesTheFirstAndExtraMarkerLocations() throws Exception {
    Path document = root.resolve("duplicate-marker.md");
    Files.writeString(
        document,
        "# AC-DUP-MARKER: Rule\n\n"
            + "<!-- topplecat:acceptance:AC-DUP-MARKER -->\n\n"
            + "<!-- topplecat:acceptance:AC-DUP-MARKER -->\n");

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(root, List.of(document));
    String diagnostics = parsed.diagnosticMessage();
    assertTrue(diagnostics.contains("TC-SPEC-AC-MARKER-DUPLICATE"), diagnostics);
    assertTrue(diagnostics.contains("duplicate-marker.md:3:1"), diagnostics);
    assertTrue(diagnostics.contains("duplicate-marker.md:5:1"), diagnostics);
  }

  @Test
  void commonMarkCorpusUsesTheSameSourcesAndExpectedDocumentEvents() throws Exception {
    try (var source =
        getClass().getResourceAsStream("/selected-spec-fixtures/commonmark-corpus.tsv")) {
      assertTrue(source != null);
      try (BufferedReader lines =
          new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
        for (String line; (line = lines.readLine()) != null; ) {
          if (line.isBlank() || line.startsWith("#")) {
            continue;
          }
          String[] fields = line.split("\\|", -1);
          String sourceName = fields[0];
          List<String> expectedEvents =
              fields[1].isBlank() ? List.of() : List.of(fields[1].split(";"));
          Path document = root.resolve(sourceName);
          try (var fixture =
              getClass().getResourceAsStream("/selected-spec-fixtures/" + sourceName)) {
            assertTrue(fixture != null, sourceName);
            Files.copy(fixture, document);
          }
          CanonicalMarkdownStructure.Parsed structure =
              CanonicalMarkdownStructure.parse(Files.readString(document));
          List<String> actualEvents =
              structure.events().stream()
                  .map(
                      event ->
                          event.kind().name()
                              + ","
                              + event.line()
                              + ","
                              + event.column()
                              + ","
                              + event.text())
                  .toList();
          assertEquals(expectedEvents, actualEvents, sourceName);
          if (!fields[2].isBlank()) {
            ExternalSpecDocumentReader.ParsedSpecs parsed =
                ExternalSpecDocumentReader.read(root, List.of(document));
            assertTrue(
                parsed.diagnostics().stream().anyMatch(item -> item.ruleCode().equals(fields[2])),
                parsed.diagnosticMessage());
          }
        }
      }
    }
  }
}
