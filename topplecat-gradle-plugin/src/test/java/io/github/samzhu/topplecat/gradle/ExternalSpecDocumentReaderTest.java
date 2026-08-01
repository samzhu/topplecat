package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.report.SpecMarkdownBlock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        # Checkout AC-CHECKOUT

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
                SpecMarkdownBlock.Kind.FALLBACK)));
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
}
