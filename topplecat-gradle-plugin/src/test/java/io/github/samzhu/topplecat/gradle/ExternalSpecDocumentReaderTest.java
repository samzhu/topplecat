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
  @TempDir Path project;

  @Test
  void anchorsHeadingsAndParagraphsUntilTheirSectionBoundary() throws Exception {
    Path specs = project.resolve("specs");
    Files.createDirectories(specs);
    Files.writeString(
        specs.resolve("cart.md"),
        """
        # Cart orders

        ## AC-CART-COUPON Apply a coupon
        The order applies `SAVE100` exactly once.
        - Keep the receipt total deterministic.
        - Do not expose reviewer values.

        This paragraph also anchors AC-CART-NO-COUPON.

        ## Delivery
        A later section must not be included.
        """);

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(project, List.of(specs));

    assertTrue(parsed.configured());
    assertEquals(
        List.of("AC-CART-COUPON", "AC-CART-NO-COUPON"),
        parsed.narratives().keySet().stream().sorted().toList());
    List<SpecMarkdownBlock> coupon = parsed.narratives().get("AC-CART-COUPON");
    assertEquals(SpecMarkdownBlock.Kind.HEADING, coupon.getFirst().kind());
    assertTrue(coupon.stream().anyMatch(block -> block.text().contains("SAVE100")));
    assertTrue(
        coupon.stream()
            .anyMatch(
                block ->
                    block.kind() == SpecMarkdownBlock.Kind.LIST
                        && block.items().contains("Keep the receipt total deterministic.")));
    assertFalse(coupon.stream().anyMatch(block -> block.text().contains("later section")));
    List<SpecMarkdownBlock> noCoupon = parsed.narratives().get("AC-CART-NO-COUPON");
    assertTrue(
        noCoupon.stream()
            .anyMatch(block -> block.text().contains("AC-CART-COUPON Apply a coupon")));
    assertFalse(noCoupon.stream().anyMatch(block -> block.text().contains("later section")));
    assertEquals(List.of("specs/cart.md"), parsed.sources().get("AC-CART-COUPON"));
  }

  @Test
  void ignoresAcLookingTextInFencedCodeAndRemainsSilentWhenUnconfigured() throws Exception {
    Path spec = project.resolve("spec.md");
    Files.writeString(
        spec,
        """
        ```java
        // AC-FAKE-CODE
        ```

        AC-REAL appears in normal prose.
        """);

    ExternalSpecDocumentReader.ParsedSpecs parsed =
        ExternalSpecDocumentReader.read(project, List.of(spec));

    assertEquals(List.of("AC-REAL"), parsed.narratives().keySet().stream().toList());
    assertFalse(parsed.narratives().containsKey("AC-FAKE-CODE"));
    assertFalse(ExternalSpecDocumentReader.read(project, List.of()).configured());
  }
}
