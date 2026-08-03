package io.github.samzhu.topplecat.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.ExpectedActualComparison;
import io.github.samzhu.topplecat.core.ExpectedActualDifference;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class ReportViewsTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

  @TempDir Path tempDir;

  @Test
  void reviewKeepsTheCompleteSelectedDocumentAheadOfReviewerOnlyExecutableMaterial()
      throws Exception {
    ReviewDocument document =
        new ReviewDocument(
            "specs/checkout.md",
            "a".repeat(64),
            List.of(
                new SpecMarkdownBlock(
                    SpecMarkdownBlock.Kind.HEADING, 1, "Checkout delivery AC-CHECKOUT", List.of()),
                new SpecMarkdownBlock(
                    SpecMarkdownBlock.Kind.CODE_FENCE,
                    0,
                    "{\"coupon\":\"WELCOME\"}",
                    List.of(),
                    "json",
                    "",
                    "",
                    List.of(),
                    List.of(),
                    ""),
                new SpecMarkdownBlock(
                    SpecMarkdownBlock.Kind.MERMAID,
                    0,
                    "flowchart TD\nA[Cart] --> B[Checkout]",
                    List.of(),
                    "mermaid",
                    "",
                    "",
                    List.of(),
                    List.of(),
                    "")),
            List.of());
    ToppleCaseData hidden =
        new ToppleCaseData(
            "reviewer-boundary",
            "AC-CHECKOUT",
            CaseVisibility.HIDDEN,
            JSON.readTree("{\"amount\":800}"),
            JSON.readTree("{\"accepted\":false}"),
            Path.of("hidden.yaml"));
    ReviewView view =
        ReportViews.review(
            Map.of("AC-CHECKOUT", "Reject a disallowed checkout"),
            List.of(hidden),
            List.of(document),
            Map.of("AC-CHECKOUT", new ReviewAcLocation("specs/checkout.md", 1)),
            Map.of("AC-CHECKOUT", new ReviewMethod(List.of("Given a cart"), "void checkout() {}")),
            Map.of(),
            NOW,
            null);

    Path bundle = tempDir.resolve("review");
    HtmlBundleWriter.review(bundle, view);
    String json = ReportJson.writeReview(view);
    String html = java.nio.file.Files.readString(bundle.resolve("index.html"));

    assertEquals(ReviewView.SCHEMA_VERSION, view.schemaVersion());
    assertEquals(List.of(document), view.selectedSpecDocuments());
    assertEquals(
        "specs/checkout.md", view.acceptanceConditions().getFirst().location().documentPath());
    assertTrue(json.contains("reviewer-boundary"));
    assertTrue(json.contains("flowchart TD"));
    assertFalse(json.contains("\"verdict\""));
    assertTrue(html.contains("assets/mermaid.js"));
    assertEquals(
        view, ReportJson.readReview(java.nio.file.Files.readString(bundle.resolve("data.json"))));
  }

  @Test
  void verificationPreservesStructuredMismatchBeforeRawFailureAndCountsCurrentRun()
      throws Exception {
    ExpectedActualComparison comparison =
        new ExpectedActualComparison(
            "receipt",
            List.of(
                new ExpectedActualDifference(
                    "expected.receipt.lines[1].total",
                    ExpectedActualDifference.Kind.CHANGED,
                    JSON.readTree("20"),
                    JSON.readTree("21"))));
    NarrativeStep failedStep =
        new NarrativeStep(
            "CheckoutStage#receipt()V",
            "the receipt matches the approved total",
            NarrativeStepStatus.FAIL,
            1,
            List.of(),
            List.of(),
            "case-failure",
            List.of(comparison));
    ToppleCaseData row =
        new ToppleCaseData(
            "checkout-public",
            "AC-CHECKOUT",
            CaseVisibility.PUBLIC,
            JSON.readTree("{\"amount\":21}"),
            JSON.readTree("{\"receipt\":{\"lines\":[{\"total\":10},{\"total\":20}]}}"),
            Path.of("public.json"));
    VerificationView view =
        ReportViews.withRun(
            ReportViews.withMutationAttribution(
                ReportViews.verification(
                    Map.of("AC-CHECKOUT", "Calculate checkout"),
                    List.of(row),
                    Map.of(
                        row.caseId(),
                        new ReportViews.CaseExecution(
                            CaseResultStatus.FAIL,
                            "expected receipt total did not match",
                            List.of(failedStep),
                            Map.of("receipt", "ASSERTED"))),
                    NOW),
                null),
            "run-123",
            NOW.minusSeconds(5),
            NOW);

    Path bundle = tempDir.resolve("verification");
    HtmlBundleWriter.verification(bundle, view);
    String json = java.nio.file.Files.readString(bundle.resolve("data.json"));

    assertEquals(CaseResultStatus.FAIL, view.verdict());
    VerificationSafeguard expectedResult =
        view.acceptanceConditions().getFirst().safeguards().stream()
            .filter(item -> item.name().equals("EXPECTED_RESULT_CHECK"))
            .findFirst()
            .orElseThrow();
    assertEquals(VerificationSafeguardOutcome.COMPARISON_COMPLETED, expectedResult.outcome());
    assertEquals(
        VerificationSafeguardReason.EXPECTED_COMPARISON_COMPLETED, expectedResult.reason());
    assertEquals(1, view.run().failedCaseCount());
    assertEquals("run-123", view.run().runId());
    assertTrue(json.contains("expected.receipt.lines[1].total"));
    assertEquals(view, ReportJson.readVerification(json));
  }
}
