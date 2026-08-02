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
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import io.github.samzhu.topplecat.pitest.PitMutationAttribution;
import io.github.samzhu.topplecat.pitest.PitMutationEvidence;
import io.github.samzhu.topplecat.pitest.PitOutcomeCount;
import io.github.samzhu.topplecat.pitest.ToppleCatManagedMutationProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlDetails;
import org.htmlunit.html.HtmlElement;
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
    String javaSource =
        """
        @DisplayName("套用 SAVE100 折抵訂單小計 <script> & \\"quoted\\"")
        record Receipt(String total) {}
        // Preserve this comment: <script> &
        class Checkout {
          char newline = '\\n';
          String receipt() { return "total\\\\value"; }
        }
        """;
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
                    javaSource,
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
                    "套用 SAVE100 折抵訂單小計",
                    new ReviewAcLocation("specs/checkout.md", 1),
                    List.of(
                        new ReviewCase(
                            CaseVisibility.HIDDEN,
                            "reviewer-case",
                            JSON.readTree("{\"amount\":800}"),
                            JSON.readTree("{\"total\":700}"),
                            List.of(
                                new ReviewScenarioStep(
                                    io.github.samzhu.topplecat.core.StepPhase.GIVEN, "準備可結帳的購物車")))),
                    new ReviewMethod(List.of(), javaSource))),
            null,
            List.of());
    Path bundle = tempDir.resolve("review");
    HtmlBundleWriter.review(bundle, view);

    Path explicitEnglishBundle = tempDir.resolve("review-en");
    HtmlBundleWriter.review(explicitEnglishBundle, view, ReportLanguage.EN);
    assertEquals(
        Files.readString(bundle.resolve("index.html")),
        Files.readString(explicitEnglishBundle.resolve("index.html")));

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
      assertEquals(
          javaSource, ((HtmlElement) page.querySelectorAll("pre code").item(0)).getTextContent());
      assertEquals(
          javaSource, ((HtmlElement) page.querySelectorAll("pre code").item(2)).getTextContent());
      assertNotNull(page.querySelector(".tok-annotation"));
      assertNotNull(page.querySelector(".tok-escape"));
      assertFalse(page.asNormalizedText().contains("class=\"tok-annotation\">"));
      assertFalse(page.asXml().contains("<span <span="));
      assertFalse(page.asXml().contains("<script>"));
      assertFalse(page.asNormalizedText().contains("Delivery accepted"));
      assertFalse(page.asNormalizedText().contains("Delivery rejected"));
    }

    Path traditionalChineseBundle = tempDir.resolve("review-zh-TW");
    HtmlBundleWriter.review(traditionalChineseBundle, view, ReportLanguage.ZH_TW);
    assertEquals(
        Files.readString(bundle.resolve("data.json")),
        Files.readString(traditionalChineseBundle.resolve("data.json")));

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page =
          client.getPage(traditionalChineseBundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertEquals("zh-TW", ((HtmlElement) page.querySelector("html")).getAttribute("lang"));
      assertEquals("規格審閱", page.getTitleText());
      assertTrue(page.asNormalizedText().contains("規格已備妥，尚未執行"));
      assertTrue(page.asNormalizedText().contains("跳至報告內容"));
      assertTrue(page.asNormalizedText().contains("套用 SAVE100 折抵訂單小計"));
      assertTrue(page.asNormalizedText().contains("準備可結帳的購物車"));
      assertFalse(page.asNormalizedText().contains("Specification prepared — not executed"));
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
            List.of(
                JSON.readTree(
                    "{\"request\":{\"coupon\":\"visible-value\"},\"items\":[1,2],\"nullValue\":null}")),
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
      assertTrue(page.asNormalizedText().contains("Step data"));
      HtmlDetails stepData = (HtmlDetails) page.querySelector(".step-data details");
      assertFalse(stepData.isOpen(), "Step data stays collapsed by default");
      assertTrue(stepData.getTextContent().contains("visible-value"));
      HtmlDetails failedCase = (HtmlDetails) page.querySelector("#case-case-fail");
      assertTrue(failedCase.isOpen(), "the first real failure is open by default");
    }

    Path traditionalChineseBundle = tempDir.resolve("verification-zh-TW");
    HtmlBundleWriter.verification(traditionalChineseBundle, view, ReportLanguage.ZH_TW);
    assertEquals(
        Files.readString(bundle.resolve("data.json")),
        Files.readString(traditionalChineseBundle.resolve("data.json")));

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page =
          client.getPage(traditionalChineseBundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertEquals("zh-TW", ((HtmlElement) page.querySelector("html")).getAttribute("lang"));
      assertEquals("驗證報告", page.getTitleText());
      assertTrue(page.asNormalizedText().contains("交付遭拒，驗證失敗"));
      assertEquals(
          "驗證報告篩選器",
          ((HtmlElement) page.querySelector(".filter-controls")).getAttribute("aria-label"));
      assertTrue(page.asNormalizedText().contains("JUNIT"));
      assertTrue(page.asNormalizedText().contains("FAIL"));
      assertFalse(page.asNormalizedText().contains("Delivery rejected — verification failed"));
    }
  }

  @Test
  void verificationExplainsPitsGlobalOutcomeSeparatelyFromPerAcceptanceDetection()
      throws Exception {
    String mutator = ToppleCatManagedMutationProfile.operatorIds().getFirst();
    PitMutationEvidence killed =
        new PitMutationEvidence(
            true,
            "KILLED",
            "example.Checkout",
            mutator,
            "changed checkout total",
            List.of("example.CheckoutAcceptance#matches()V"),
            List.of("example.CheckoutAcceptance#matches()V"),
            List.of(),
            List.of("AC-COUPON"));
    PitMutationAttribution attribution =
        new PitMutationAttribution(
            ToppleCatManagedMutationProfile.PIT_VERSION,
            ToppleCatManagedMutationProfile.PROFILE_ID,
            ToppleCatManagedMutationProfile.operatorIds(),
            2,
            2,
            0,
            List.of(new PitOutcomeCount("KILLED", true, 2)),
            List.of(),
            List.of(),
            List.of(
                new PitMutationAssessment(
                    "AC-COUPON",
                    List.of("example.CheckoutAcceptance#matches()V"),
                    7,
                    4,
                    100,
                    57,
                    List.of(new PitOutcomeCount("KILLED", true, 4)),
                    false),
                new PitMutationAssessment(
                    "AC-SHIPPING",
                    List.of("example.ShippingAcceptance#matches()V"),
                    8,
                    8,
                    100,
                    100,
                    List.of(new PitOutcomeCount("KILLED", true, 8)),
                    false)),
            List.of(killed, killed));
    VerificationView view =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            NOW,
            CaseResultStatus.PASS,
            true,
            List.of(
                new EvidenceGate("CONTRACT_INTEGRITY", EvidenceVerdict.PASS, null),
                new EvidenceGate("JUNIT", EvidenceVerdict.PASS, null),
                new EvidenceGate("REVIEWER_JUNIT", EvidenceVerdict.PASS, null),
                new EvidenceGate("EXPECTED_CONSUMPTION", EvidenceVerdict.PASS, null),
                new EvidenceGate("PROPERTY", EvidenceVerdict.NOT_APPLICABLE, null),
                new EvidenceGate(
                    "MUTATION", EvidenceVerdict.FAIL, "Per-AC detection missed threshold.")),
            List.of(),
            null,
            attribution,
            new VerificationRunSummary("run-mutation", NOW, NOW, 1, 0, 0, 0));
    Path bundle = tempDir.resolve("mutation-verification");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      String text = page.asNormalizedText();
      assertTrue(text.contains("PIT global outcome"));
      assertTrue(text.contains("2/2 mutants were detected by at least one test."));
      assertTrue(
          text.contains("does not mean that every Acceptance Method detected every mutant."));
      assertTrue(text.contains("Per-AC Acceptance Method detection"));
      assertTrue(text.contains("does not blend this rate with PIT's global outcome."));
      assertTrue(text.contains("KILLED"));
    }
  }
}
