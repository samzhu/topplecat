package io.github.samzhu.topplecat.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.EvidenceGate;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ExpectedActualComparison;
import io.github.samzhu.topplecat.core.ExpectedActualDifference;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlDetails;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Exercises the CSP-safe offline bundle after its local JavaScript has rendered the DOM. */
class ReportBundleDomTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

  @TempDir Path tempDir;

  @Test
  void specReviewRendersCompleteDocumentSemanticSyntaxAndNoExecutionConclusion() throws Exception {
    ReviewDocument document =
        new ReviewDocument(
            "specs/checkout.md",
            "a".repeat(64),
            List.of(
                new SpecMarkdownBlock(
                    SpecMarkdownBlock.Kind.HEADING, 1, "Checkout AC-CHECKOUT", List.of()),
                new SpecMarkdownBlock(
                    SpecMarkdownBlock.Kind.TASK_LIST,
                    0,
                    "",
                    List.of("[x] choose a coupon", "[ ] calculate the total"),
                    "",
                    "",
                    "",
                    List.of(),
                    List.of(),
                    ""),
                new SpecMarkdownBlock(
                    SpecMarkdownBlock.Kind.CODE_FENCE,
                    0,
                    "<script>window.injected = true</script>",
                    List.of(),
                    "java",
                    "",
                    "",
                    List.of(),
                    List.of(),
                    ""),
                new SpecMarkdownBlock(
                    SpecMarkdownBlock.Kind.MERMAID,
                    0,
                    "flowchart TD\nA[Cart] --> B[Receipt]",
                    List.of(),
                    "mermaid",
                    "",
                    "",
                    List.of(),
                    List.of(),
                    "")),
            List.of());
    ReviewView view =
        new ReviewView(
            ReviewView.SCHEMA_VERSION,
            NOW,
            List.of(document),
            List.of(
                new ReviewAcceptanceCondition(
                    "AC-CHECKOUT",
                    "Checkout total",
                    new ReviewAcLocation("specs/checkout.md", 1),
                    List.of(
                        new ReviewCase(
                            CaseVisibility.HIDDEN,
                            "reviewer-case",
                            JSON.readTree("{\"amount\":800}"),
                            JSON.readTree("{\"total\":700}"),
                            List.of(
                                new ReviewScenarioStep(
                                    io.github.samzhu.topplecat.core.StepPhase.GIVEN, "a cart")))),
                    new ReviewMethod(List.of(), "void checkout() {}"))),
            null,
            List.of());
    Path bundle = tempDir.resolve("review");
    HtmlBundleWriter.review(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertEquals("Spec Review", page.getTitleText());
      assertTrue(page.asNormalizedText().contains("Specification prepared — not executed"));
      assertTrue(page.asNormalizedText().contains("choose a coupon"));
      assertTrue(page.querySelectorAll(".mermaid-diagram svg").getLength() == 1);
      assertNotNull(page.querySelector("a.skip-link"));
      assertNotNull(page.querySelector(".bdd-keyword"));
      assertFalse(page.asXml().contains("<script>window.injected = true</script>"));
      assertFalse(page.asNormalizedText().contains("Delivery accepted"));
      assertFalse(page.asNormalizedText().contains("Delivery rejected"));
    }
  }

  @Test
  void verificationLeadsWithFailureAndKeepsFiveIndependentSectionsSeparate() throws Exception {
    NarrativeStep step =
        new NarrativeStep(
            "CheckoutStage#then()V",
            "the receipt has the approved amount",
            NarrativeStepStatus.FAIL,
            1,
            List.of(),
            List.of(),
            "raw assertion",
            List.of(
                new ExpectedActualComparison(
                    "receipt",
                    List.of(
                        new ExpectedActualDifference(
                            "expected.receipt.total",
                            ExpectedActualDifference.Kind.CHANGED,
                            JSON.readTree("20"),
                            JSON.readTree("21"))))));
    VerificationCase failed =
        new VerificationCase(
            "case-fail",
            CaseVisibility.PUBLIC,
            JSON.readTree("{\"amount\":21}"),
            JSON.readTree("{\"receipt\":{\"total\":20}}"),
            CaseResultStatus.FAIL,
            Map.of("receipt", "ASSERTED"),
            List.of(step),
            "expected receipt did not match");
    VerificationAcceptanceCondition ac =
        new VerificationAcceptanceCondition(
            "AC-CHECKOUT",
            "Checkout total",
            List.of("Then the receipt has the approved amount"),
            CaseResultStatus.FAIL,
            List.of(failed),
            Map.of(),
            Map.of());
    VerificationView view =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            NOW,
            CaseResultStatus.FAIL,
            true,
            List.of(
                new EvidenceGate("CONTRACT_INTEGRITY", EvidenceVerdict.PASS, null),
                new EvidenceGate("JUNIT", EvidenceVerdict.FAIL, "The public typed row failed."),
                new EvidenceGate("REVIEWER_JUNIT", EvidenceVerdict.PASS, null),
                new EvidenceGate("EXPECTED_CONSUMPTION", EvidenceVerdict.PASS, null),
                new EvidenceGate("PROPERTY", EvidenceVerdict.NOT_APPLICABLE, null),
                new EvidenceGate(
                    "MUTATION", EvidenceVerdict.DISABLED, "Sealed policy disabled mutation.")),
            List.of(ac),
            null,
            null,
            new VerificationRunSummary("run-1", NOW.minusSeconds(3), NOW, 1, 1, 1, 1));
    Path bundle = tempDir.resolve("verification");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertEquals("Verification Report", page.getTitleText());
      assertTrue(page.asNormalizedText().contains("Delivery rejected — verification failed"));
      assertNotNull(page.querySelector("#problems"));
      assertNotNull(page.querySelector("#contract-integrity"));
      assertNotNull(page.querySelector("#public-acceptance"));
      assertNotNull(page.querySelector("#hidden-tests"));
      assertNotNull(page.querySelector("#property-testing"));
      assertNotNull(page.querySelector("#mutation-testing"));
      assertTrue(page.asNormalizedText().contains("Field-level expected and actual comparison"));
      HtmlDetails failedCase = (HtmlDetails) page.querySelector("#case-case-fail");
      assertTrue(failedCase.isOpen(), "the first real failure is open by default");
    }
  }
}
