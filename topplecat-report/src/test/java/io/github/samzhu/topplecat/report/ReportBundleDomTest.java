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
import io.github.samzhu.topplecat.core.SelectedSpecDocument;
import io.github.samzhu.topplecat.core.SelectedSpecScope;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import io.github.samzhu.topplecat.pitest.PitMutationAttribution;
import io.github.samzhu.topplecat.pitest.PitMutationEvidence;
import io.github.samzhu.topplecat.pitest.PitOutcomeCount;
import io.github.samzhu.topplecat.pitest.ToppleCatManagedMutationProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                                    io.github.samzhu.topplecat.core.StepPhase.GIVEN,
                                    "準備可結帳的購物車")))),
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
      assertTrue(page.asNormalizedText().contains("Specification prepared, not executed"));
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
    view = ReportViews.withMutationAttribution(view, null);
    Path bundle = tempDir.resolve("verification");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertEquals("Verification Report", page.getTitleText());
      assertTrue(page.asNormalizedText().contains("Verification found a problem"));
      assertTrue(
          ((HtmlElement) page.querySelector("#summary"))
              .getTextContent()
              .contains(
                  "Contract Integrity passed: the complete executable contract matches its"
                      + " Mechanical Seal."));
      assertNotNull(page.querySelector("#problems"));
      assertNotNull(page.querySelector("#contract-integrity"));
      assertNotNull(page.querySelector("#public-acceptance"));
      assertNotNull(page.querySelector("#hidden-tests"));
      assertNotNull(page.querySelector("#property-testing"));
      assertNotNull(page.querySelector("#mutation-testing"));
      HtmlDetails technicalEvidence = (HtmlDetails) page.querySelector("#technical-evidence");
      assertTrue(technicalEvidence.getTextContent().contains("Sealed policy disabled mutation."));
      assertNotNull(page.querySelector("#ac-reader-AC-CHECKOUT[hidden]"));
      HtmlElement localControl =
          (HtmlElement) page.querySelector("#verification-AC-CHECKOUT [data-ac-toggle]");
      localControl.click();
      client.waitForBackgroundJavaScript(50);
      assertEquals("true", localControl.getAttribute("aria-expanded"));
      assertNotNull(page.querySelector("#ac-reader-AC-CHECKOUT:not([hidden])"));
      assertTrue(page.asNormalizedText().contains("Expected compared with actual"));
      HtmlDetails stepData = (HtmlDetails) page.querySelector(".step-data details");
      assertTrue(stepData.getParentNode().getTextContent().contains("Values passed to Steps"));
      assertFalse(stepData.isOpen(), "Step data stays collapsed by default");
      assertTrue(stepData.getTextContent().contains("visible-value"));
      HtmlDetails failedCase = (HtmlDetails) page.querySelector("#case-case-fail");
      assertTrue(failedCase.isOpen(), "expanding an AC opens every case reader layer");
      assertFalse(((HtmlDetails) page.querySelector("#ac-technical-AC-CHECKOUT")).isOpen());
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
      assertTrue(page.asNormalizedText().contains("驗證發現問題"));
      assertTrue(
          ((HtmlElement) page.querySelector("#summary"))
              .getTextContent()
              .contains("契約完整性已通過：完整可執行契約符合機械封印。"));
      ((HtmlElement) page.querySelector("#verification-AC-CHECKOUT [data-ac-toggle]")).click();
      client.waitForBackgroundJavaScript(50);
      assertEquals(
          "驗證報告篩選器",
          ((HtmlElement) page.querySelector(".filter-controls")).getAttribute("aria-label"));
      assertTrue(page.asNormalizedText().contains("公開驗收"));
      assertTrue(page.asNormalizedText().contains("未出現在比對中的規則"));
      assertTrue(page.asNormalizedText().contains("FAIL"));
      assertFalse(page.asNormalizedText().contains("交付遭拒"));
    }
  }

  @Test
  void scopedVerificationPassExplainsThatItDoesNotCoverTheCompleteContract() throws Exception {
    VerificationView scoped =
        verificationPassView(
            DeliveryScope.from(
                SelectedSpecScope.create(List.of(), List.of("AC-CHECKOUT")),
                "SELECTED_ACCEPTANCE_CONDITIONS",
                "SELECTED_ACCEPTANCE_CONDITIONS",
                "SELECTED_ACCEPTANCE_CONDITIONS",
                1,
                0));
    Path scopedEnglish = tempDir.resolve("scoped-verification-en");
    Path scopedTraditionalChinese = tempDir.resolve("scoped-verification-zh-TW");
    HtmlBundleWriter.verification(scopedEnglish, scoped);
    HtmlBundleWriter.verification(scopedTraditionalChinese, scoped, ReportLanguage.ZH_TW);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage english = client.getPage(scopedEnglish.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);
      assertTrue(
          ((HtmlElement) english.querySelector("#summary"))
              .getTextContent()
              .contains(
                  "This PASS covers only the selected ACs; it does not mean the complete"
                      + " executable contract passed."));
      assertTrue(
          ((HtmlElement) english.querySelector("#summary"))
              .getTextContent()
              .contains("Selected by explicit AC IDs: AC-CHECKOUT."));

      HtmlPage traditionalChinese =
          client.getPage(scopedTraditionalChinese.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);
      assertTrue(
          ((HtmlElement) traditionalChinese.querySelector("#summary"))
              .getTextContent()
              .contains("這個 PASS 只代表選定的 AC 通過，不代表完整可執行契約通過。"));
      assertTrue(
          ((HtmlElement) traditionalChinese.querySelector("#summary"))
              .getTextContent()
              .contains("由明確 AC ID 選取：AC-CHECKOUT。"));
    }

    VerificationView fullContract =
        verificationPassView(
            DeliveryScope.from(SelectedSpecScope.empty(), "ALL", "ALL", "FULL_CONTRACT", 1, 0));
    Path fullContractBundle = tempDir.resolve("full-contract-verification");
    HtmlBundleWriter.verification(fullContractBundle, fullContract);
    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(fullContractBundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);
      assertFalse(
          ((HtmlElement) page.querySelector("#summary"))
              .getTextContent()
              .contains("This PASS covers only the selected ACs"));
    }
  }

  @Test
  void verificationReadingControlsKeepEveryStatusScannableAndSupportLocalBulkModes()
      throws Exception {
    VerificationView view = readingControlsView();
    Path bundle = tempDir.resolve("reading-controls");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(50);

      assertEquals(6, page.querySelectorAll(".ac-card").getLength());
      assertEquals(30, page.querySelectorAll(".safeguard-chip").getLength());
      assertTrue(page.querySelectorAll(".safeguard-chip.requires-attention").getLength() > 0);
      assertTrue(page.querySelectorAll(".safeguard-chip-reason").getLength() > 0);
      assertEquals(1, page.querySelectorAll(".contract-integrity-summary").getLength());
      HtmlElement overview =
          (HtmlElement) page.querySelector("#verification-AC-READ-FAIL .safeguard-overview");
      assertOrder(
          overview.getTextContent(),
          "Public Acceptance",
          "Hidden Tests",
          "Expected Result Check",
          "Property-Based Testing",
          "Mutation Testing");
      assertTrue(
          ((HtmlElement)
                  page.querySelector(
                      "#verification-AC-READ-FAIL .safeguard-chip.requires-attention"))
              .getTextContent()
              .contains("At least one recorded case produced a result different"));
      for (String acId : List.of("AC-READ-FAIL", "AC-READ-INCOMPLETE", "AC-READ-PASS-2")) {
        assertNotNull(page.querySelector("#ac-reader-" + acId + "[hidden]"));
        HtmlElement card = (HtmlElement) page.querySelector("#verification-" + acId);
        assertTrue(card.getTextContent().contains(acId));
        assertTrue(card.getTextContent().contains("Verification result"));
        assertEquals(
            "false",
            ((HtmlElement) card.querySelector("[data-ac-toggle]")).getAttribute("aria-expanded"));
      }
      assertEquals(0, page.querySelectorAll("[data-lazy-case][data-loaded='true']").getLength());
      assertEquals(0, page.querySelectorAll("[data-lazy-case][open]").getLength());

      HtmlElement failCard = (HtmlElement) page.querySelector("#verification-AC-READ-FAIL");
      HtmlElement failControl = (HtmlElement) failCard.querySelector("[data-ac-toggle]");
      failControl.click();
      client.waitForBackgroundJavaScript(50);
      assertEquals("true", failControl.getAttribute("aria-expanded"));
      assertEquals(failControl, page.getFocusedElement());
      assertNotNull(page.querySelector("#ac-reader-AC-READ-FAIL:not([hidden])"));
      assertEquals(2, failCard.querySelectorAll("details[data-lazy-case][open]").getLength());
      assertEquals(
          2, failCard.querySelectorAll("[data-lazy-case][data-loaded='true']").getLength());
      assertEquals(0, failCard.querySelectorAll(".ac-technical[open]").getLength());

      failControl.click();
      client.waitForBackgroundJavaScript(50);
      assertNotNull(page.querySelector("#ac-reader-AC-READ-FAIL[hidden]"));
      assertEquals("false", failControl.getAttribute("aria-expanded"));
      assertNotNull(page.querySelector("#ac-reader-AC-READ-PASS-2[hidden]"));

      HtmlElement global = (HtmlElement) page.querySelector("[data-global-reading]");
      global.click();
      assertTrue(
          ((HtmlElement) page.querySelector("[data-bulk-status]"))
              .getTextContent()
              .contains("Expanding 0 of 6"));
      global.click();
      client.waitForBackgroundJavaScript(50);
      assertTrue(
          ((HtmlElement) page.querySelector("[data-bulk-status]"))
              .getTextContent()
              .contains("Expansion stopped after 0 of 6 ACs."));
      assertEquals(global, page.getFocusedElement());
      assertEquals(0, page.querySelectorAll(".ac-card[data-expanded='true']").getLength());
      assertEquals(6, page.querySelectorAll(".ac-reader[hidden]").getLength());

      global.click();
      client.waitForBackgroundJavaScript(500);
      assertEquals("All ACs: key results only", global.getTextContent());
      assertEquals("true", global.getAttribute("aria-expanded"));
      assertEquals(global, page.getFocusedElement());
      assertEquals(6, page.querySelectorAll(".ac-card[data-expanded='true']").getLength());
      assertEquals(12, page.querySelectorAll("details[data-lazy-case][open]").getLength());
      assertEquals(0, page.querySelectorAll(".ac-technical[open]").getLength());

      global.click();
      client.waitForBackgroundJavaScript(50);
      assertEquals(6, page.querySelectorAll(".ac-reader[hidden]").getLength());
      assertEquals("Expand all ACs", global.getTextContent());
      assertEquals("false", global.getAttribute("aria-expanded"));
    }
  }

  @Test
  void verificationLinksRevealOnlyTheRequiredAncestorsAndRespectFragments() throws Exception {
    VerificationView view = readingControlsView();
    Path bundle = tempDir.resolve("reading-link-controls");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page =
          client.getPage(
              (bundle.resolve("index.html").toUri().toURL().toString()
                  + "#raw-failure-AC-READ-FAIL-public"));
      client.waitForBackgroundJavaScript(100);

      assertNotNull(page.querySelector("#ac-reader-AC-READ-FAIL:not([hidden])"));
      assertTrue(((HtmlDetails) page.querySelector("#raw-failure-AC-READ-FAIL-public")).isOpen());
      assertTrue(((HtmlDetails) page.querySelector("#case-AC-READ-FAIL-public")).isOpen());
      assertFalse(
          ((HtmlDetails) page.querySelector("#complete-expected-AC-READ-FAIL-public")).isOpen());
      assertFalse(((HtmlDetails) page.querySelector("#execution-AC-READ-FAIL-public")).isOpen());
      assertEquals(0, page.querySelectorAll(".ac-technical[open]").getLength());
      HtmlElement safeguardLink =
          (HtmlElement) page.querySelector("#verification-AC-READ-INCOMPLETE .safeguard-chip");
      safeguardLink.click();
      client.waitForBackgroundJavaScript(100);
      assertNotNull(page.querySelector("#ac-reader-AC-READ-INCOMPLETE:not([hidden])"));
      assertEquals(0, page.querySelectorAll(".ac-technical[open]").getLength());

      page.executeJavaScript("window.location.hash = '#execution-AC-READ-FAIL-hidden';");
      client.waitForBackgroundJavaScript(100);
      assertTrue(((HtmlDetails) page.querySelector("#execution-AC-READ-FAIL-hidden")).isOpen());
      assertFalse(
          ((HtmlDetails) page.querySelector("#complete-expected-AC-READ-FAIL-hidden")).isOpen());
      assertFalse(((HtmlDetails) page.querySelector("#raw-failure-AC-READ-FAIL-hidden")).isOpen());
      assertEquals(0, page.querySelectorAll(".ac-technical[open]").getLength());

      page.executeJavaScript("window.location.hash = '#complete-expected-AC-READ-FAIL-hidden';");
      client.waitForBackgroundJavaScript(100);
      assertTrue(
          ((HtmlDetails) page.querySelector("#complete-expected-AC-READ-FAIL-hidden")).isOpen());
      assertEquals(0, page.querySelectorAll(".ac-technical[open]").getLength());
    }
  }

  @Test
  void verificationSummaryExplainsSealMismatchWithoutPresentingAcExecutionAsFailure()
      throws Exception {
    VerificationView view =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            NOW,
            CaseResultStatus.NOT_REPORTED,
            true,
            List.of(
                new EvidenceGate(
                    "CONTRACT_INTEGRITY", EvidenceVerdict.FAIL, "The sealed contract changed."),
                new EvidenceGate("JUNIT", EvidenceVerdict.INCOMPLETE, null),
                new EvidenceGate("REVIEWER_JUNIT", EvidenceVerdict.INCOMPLETE, null),
                new EvidenceGate("EXPECTED_CONSUMPTION", EvidenceVerdict.INCOMPLETE, null),
                new EvidenceGate("PROPERTY", EvidenceVerdict.INCOMPLETE, null),
                new EvidenceGate("MUTATION", EvidenceVerdict.INCOMPLETE, null)),
            List.of(mutationAcceptanceCondition("AC-CHECKOUT", "Checkout total")),
            null,
            null,
            new VerificationRunSummary("run-integrity", NOW, NOW, 1, 0, 0, 0));
    view = ReportViews.withMutationAttribution(view, null);

    Path englishBundle = tempDir.resolve("integrity-failure-en");
    HtmlBundleWriter.verification(englishBundle, view);
    Path traditionalChineseBundle = tempDir.resolve("integrity-failure-zh-TW");
    HtmlBundleWriter.verification(traditionalChineseBundle, view, ReportLanguage.ZH_TW);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(englishBundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertTrue(
          ((HtmlElement) page.querySelector("#summary"))
              .getTextContent()
              .contains(
                  "Contract Integrity failed: the complete executable contract no longer matches"
                      + " its Mechanical Seal, so downstream AC work did not run."));
      assertTrue(
          page.querySelector("#all-acs")
              .getTextContent()
              .contains("downstream AC work did not run"));
      assertEquals(0, page.querySelectorAll(".ac-card").getLength());
    }

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page =
          client.getPage(traditionalChineseBundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertTrue(
          ((HtmlElement) page.querySelector("#summary"))
              .getTextContent()
              .contains("契約完整性失敗：完整可執行契約已不符合機械封印，因此未執行下游 AC 工作。"));
      assertTrue(page.querySelector("#all-acs").getTextContent().contains("未執行下游 AC 工作"));
      assertEquals(0, page.querySelectorAll(".ac-card").getLength());
    }
  }

  @Test
  void verificationPresentsAcFirstPlainLanguageMutationResultsAndKeepsPitDetailsCollapsed()
      throws Exception {
    String mutator = ToppleCatManagedMutationProfile.operatorIds().getFirst();
    PitMutationEvidence killed =
        new PitMutationEvidence(
            true,
            "KILLED",
            "example.Production",
            "Production.java",
            "discountedTotal",
            "(I)I",
            12,
            0,
            0,
            mutator,
            "Replaced integer addition with subtraction",
            List.of("example.MeetsAcceptance#matches()V", "example.BelowAcceptance#matches()V"),
            List.of("example.BelowAcceptance#matches()V"),
            List.of("example.MeetsAcceptance#matches()V"),
            List.of("AC-MEETS", "AC-BELOW"),
            List.of("AC-BELOW"),
            "return subtotal + 10;");
    PitMutationEvidence survived =
        new PitMutationEvidence(
            false,
            "SURVIVED",
            "example.Production",
            "Production.java",
            "discountedTotal",
            "(I)I",
            18,
            0,
            1,
            mutator,
            "changed another production behavior",
            List.of("example.MeetsAcceptance#matches()V"),
            List.of(),
            List.of("example.MeetsAcceptance#matches()V"),
            List.of("AC-MEETS"),
            List.of(),
            "return subtotal - 10;");
    PitMutationEvidence detected =
        new PitMutationEvidence(
            true,
            "KILLED",
            "example.Production",
            "Production.java",
            "discountedTotal",
            "(I)I",
            14,
            0,
            2,
            mutator,
            "Replaced integer subtraction with addition",
            List.of("example.MeetsAcceptance#matches()V"),
            List.of("example.MeetsAcceptance#matches()V"),
            List.of(),
            List.of("AC-MEETS"),
            List.of("AC-MEETS"),
            "return subtotal - 10;");
    PitMutationAttribution attribution =
        new PitMutationAttribution(
            ToppleCatManagedMutationProfile.PIT_VERSION,
            ToppleCatManagedMutationProfile.PROFILE_ID,
            ToppleCatManagedMutationProfile.operatorIds(),
            10,
            10,
            0,
            List.of(
                new PitOutcomeCount("KILLED", true, 9), new PitOutcomeCount("SURVIVED", false, 1)),
            List.of(),
            List.of(),
            List.of(
                new PitMutationAssessment(
                    "AC-MEETS",
                    List.of("example.MeetsAcceptance#matches()V"),
                    10,
                    8,
                    List.of(
                        new PitOutcomeCount("KILLED", true, 9),
                        new PitOutcomeCount("SURVIVED", false, 1)),
                    false),
                new PitMutationAssessment(
                    "AC-BELOW",
                    List.of("example.BelowAcceptance#matches()V"),
                    1,
                    1,
                    List.of(new PitOutcomeCount("KILLED", true, 1)),
                    false)),
            List.of(
                killed, survived, detected, detected, detected, detected, detected, detected,
                detected, detected));
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
                    "MUTATION",
                    EvidenceVerdict.FAIL,
                    "One selected AC did not detect every attributed alteration.")),
            List.of(
                mutationAcceptanceCondition("AC-MEETS", "Meets its requirement"),
                mutationAcceptanceCondition("AC-BELOW", "Misses its requirement"),
                mutationAcceptanceCondition("AC-NO-DATA", "Has no Mutation result")),
            null,
            attribution,
            new VerificationRunSummary("run-mutation", NOW, NOW, 1, 0, 0, 0));
    view = ReportViews.withMutationAttribution(view, attribution);
    Path bundle = tempDir.resolve("mutation-verification");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      HtmlElement meetsControl =
          (HtmlElement) page.querySelector("#verification-AC-MEETS [data-ac-toggle]");
      meetsControl.click();
      client.waitForBackgroundJavaScript(50);
      String text = page.asNormalizedText();
      assertTrue(
          text.contains(
              "Mutation Testing evaluates whether the public acceptance can detect simulated faults"
                  + " in production code."));
      assertTrue(text.contains("This AC was assessed against 10"));
      assertTrue(text.contains("8 detected, 2 undetected."));
      HtmlElement meets = (HtmlElement) page.querySelector("#verification-AC-MEETS");
      Map.of(
              "mutationTesting",
                  "A missed change does not prove the original production code is wrong.",
              "attributedChanges", "Only simulated changes exactly associated",
              "undetectedMutation",
                  "This simulated change still passed this AC's unchanged public acceptance.",
              "originalSourceLine", "not necessarily the changed program text",
              "descriptor", "A JVM method signature used for exact technical location.")
          .forEach(
              (key, description) -> {
                HtmlElement wrapper =
                    (HtmlElement) meets.querySelector("[data-info-key='" + key + "']");
                assertNotNull(wrapper, "missing information control for " + key);
                assertNotNull(wrapper.querySelector("[data-info-button]"));
                assertTrue(
                    ((HtmlElement) wrapper.querySelector("[data-info-popover]"))
                        .getTextContent()
                        .contains(description));
              });
      HtmlElement mutationInfo =
          (HtmlElement) meets.querySelector("[data-info-key='mutationTesting']");
      HtmlElement mutationInfoButton =
          (HtmlElement) mutationInfo.querySelector("[data-info-button]");
      HtmlElement mutationInfoPopover =
          (HtmlElement) mutationInfo.querySelector("[data-info-popover]");
      assertEquals("More about Mutation Testing", mutationInfoButton.getAttribute("aria-label"));
      assertEquals(
          mutationInfoPopover.getAttribute("id"), mutationInfoButton.getAttribute("aria-controls"));
      assertEquals(
          mutationInfoPopover.getAttribute("id"),
          mutationInfoButton.getAttribute("aria-describedby"));
      assertEquals("tooltip", mutationInfoPopover.getAttribute("role"));
      assertEquals("false", mutationInfoButton.getAttribute("aria-expanded"));
      assertNotNull(meets.querySelector(".ac-result"), "the key result remains visible");

      HtmlElement attributedInfoButton =
          (HtmlElement)
              meets.querySelector("[data-info-key='attributedChanges'] [data-info-button]");
      attributedInfoButton.click();
      client.waitForBackgroundJavaScript(25);
      assertEquals("true", attributedInfoButton.getAttribute("aria-expanded"));
      assertTrue(
          ((HtmlElement)
                  meets.querySelector("[data-info-key='attributedChanges'] [data-info-popover]"))
              .getTextContent()
              .contains("Only simulated changes exactly associated"));
      HtmlElement undetectedInfoButton =
          (HtmlElement)
              meets.querySelector("[data-info-key='undetectedMutation'] [data-info-button]");
      undetectedInfoButton.click();
      client.waitForBackgroundJavaScript(25);
      assertEquals("false", attributedInfoButton.getAttribute("aria-expanded"));
      assertEquals("true", undetectedInfoButton.getAttribute("aria-expanded"));
      assertEquals(1, meets.querySelectorAll("[data-info-popover]:not([hidden])").getLength());
      page.executeJavaScript(
          "document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}));");
      client.waitForBackgroundJavaScript(25);
      assertEquals("false", undetectedInfoButton.getAttribute("aria-expanded"));
      attributedInfoButton.click();
      page.executeJavaScript("document.body.click();");
      client.waitForBackgroundJavaScript(25);
      assertEquals("false", attributedInfoButton.getAttribute("aria-expanded"));
      page.executeJavaScript(
          "document.querySelector(\"#verification-AC-MEETS [data-info-key='mutationTesting']"
              + " [data-info-button]\").focus();");
      client.waitForBackgroundJavaScript(25);
      assertEquals("true", mutationInfoButton.getAttribute("aria-expanded"));
      page.executeJavaScript(
          "document.querySelector(\"#verification-AC-MEETS"
              + " [data-info-key='mutationTesting']\").dispatchEvent(new Event('mouseenter',"
              + " {bubbles: true}));");
      assertEquals("true", mutationInfoButton.getAttribute("aria-expanded"));
      assertEquals(
          0,
          page.querySelectorAll("#verification-AC-BELOW [data-lazy-case][data-loaded='true']")
              .getLength());
      assertFalse(((HtmlDetails) page.querySelector("#mutation-technical-details")).isOpen());
      HtmlElement noData = (HtmlElement) page.querySelector("#verification-AC-NO-DATA");
      ((HtmlElement) noData.querySelector("[data-ac-toggle]")).click();
      assertTrue(
          noData
              .getTextContent()
              .contains("No mutation was exactly attributed to this AC in the current run."));
      assertTrue(meets.getTextContent().contains("Public Acceptance"));
      assertTrue(meets.getTextContent().contains("Hidden Tests"));
      assertTrue(meets.getTextContent().contains("Mutation Testing"));
      assertEquals(2, meets.querySelectorAll(".undetected-mutation").getLength());
      assertTrue(meets.getTextContent().contains("The operator changed from + to -."));
      assertTrue(
          meets
              .getTextContent()
              .contains("ToppleCat cannot safely state an exact before/after replacement"));
      assertTrue(meets.getTextContent().contains("Production.java"));
      assertTrue(meets.getTextContent().contains("Line: 12"));
      assertTrue(
          meets.getTextContent().contains("This AC's unchanged public acceptance still passed."));
      assertFalse(meets.getTextContent().contains("PIT"));
      HtmlElement below = (HtmlElement) page.querySelector("#verification-AC-BELOW");
      assertTrue(below.getTextContent().contains("This AC was assessed against 1"));
      assertTrue(below.getTextContent().contains("1 detected, 0 undetected."));
      assertEquals(0, below.querySelectorAll(".undetected-mutation").getLength());
      HtmlDetails technicalDetails =
          (HtmlDetails) page.querySelector("#mutation-technical-details");
      assertFalse(technicalDetails.isOpen(), "PIT evidence stays collapsed by default");
      assertTrue(technicalDetails.getTextContent().contains("KILLED"));
      assertTrue(technicalDetails.getTextContent().contains("SURVIVED"));
      assertTrue(
          technicalDetails.getTextContent().contains("Replaced integer addition with subtraction"));
    }

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      client.getOptions().setScreenWidth(360);
      client.getOptions().setScreenHeight(800);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);
      ((HtmlElement) page.querySelector("#verification-AC-MEETS [data-ac-toggle]")).click();
      HtmlElement infoButton =
          (HtmlElement)
              page.querySelector(
                  "#verification-AC-MEETS [data-info-key='mutationTesting'] [data-info-button]");
      infoButton.click();
      client.waitForBackgroundJavaScript(50);
      assertTrue(
          ((HtmlElement)
                  page.querySelector(
                      "#verification-AC-MEETS [data-info-key='mutationTesting']"
                          + " [data-info-popover]"))
              .getTextContent()
              .contains("ToppleCat temporarily simulates"));
      assertFalse(
          (Boolean)
              page.executeJavaScript("document.documentElement.scrollWidth > window.innerWidth")
                  .getJavaScriptResult(),
          "the pinned explanation must not create horizontal overflow on a narrow viewport");
      String reportCss = Files.readString(bundle.resolve("assets/report.css"));
      assertTrue(
          reportCss.contains("width: 30rem; max-width: calc(100vw - 2rem);"),
          "the generated report must use a readable desktop width with narrow-viewport margins");
    }

    String englishData = Files.readString(bundle.resolve("data.json"));
    VerificationView recorded = ReportJson.readVerification(englishData);
    assertEquals(view, recorded);
    assertFalse(englishData.contains("detectionRate"));
    assertFalse(englishData.contains("sealedThreshold"));
    assertEquals("KILLED", recorded.mutationAttribution().mutations().getFirst().status());

    Path traditionalChineseBundle = tempDir.resolve("mutation-verification-zh-TW");
    HtmlBundleWriter.verification(traditionalChineseBundle, view, ReportLanguage.ZH_TW);
    assertEquals(englishData, Files.readString(traditionalChineseBundle.resolve("data.json")));

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page =
          client.getPage(traditionalChineseBundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      ((HtmlElement) page.querySelector("#verification-AC-MEETS [data-ac-toggle]")).click();
      client.waitForBackgroundJavaScript(50);
      String text = page.asNormalizedText();
      assertTrue(text.contains("突變測試用於評估公開驗收能否辨識正式程式中的模擬錯誤。"));
      assertTrue(text.contains("這個 AC 共評估 10 個"));
      assertTrue(text.contains("偵測到 8 個，未偵測到 2 個。"));
      HtmlElement traditionalInfoButton =
          (HtmlElement)
              page.querySelector(
                  "#verification-AC-MEETS [data-info-key='mutationTesting'] [data-info-button]");
      assertEquals("更多關於突變測試", traditionalInfoButton.getAttribute("aria-label"));
      traditionalInfoButton.click();
      client.waitForBackgroundJavaScript(25);
      assertTrue(
          ((HtmlElement)
                  page.querySelector(
                      "#verification-AC-MEETS [data-info-key='mutationTesting']"
                          + " [data-info-popover]"))
              .getTextContent()
              .contains("ToppleCat 會暫時模擬正式程式的小幅改動"));
      assertTrue(page.asNormalizedText().contains("描述子"));
      Map.of(
              "attributedChanges", "這裡只計入精確關聯到這個 AC 公開驗收方法的模擬改動。",
              "undetectedMutation", "這個模擬改動仍然通過了這個 AC 未改變的公開驗收。",
              "originalSourceLine", "不一定是改動後的程式文字。",
              "descriptor", "用於精確技術定位的 JVM 方法簽名。")
          .forEach(
              (key, description) ->
                  assertTrue(
                      ((HtmlElement)
                              page.querySelector(
                                  "#verification-AC-MEETS [data-info-key='"
                                      + key
                                      + "'] [data-info-popover]"))
                          .getTextContent()
                          .contains(description)));
      HtmlElement below = (HtmlElement) page.querySelector("#verification-AC-BELOW");
      assertTrue(below.getTextContent().contains("這個 AC 共評估 1 個"));
      assertTrue(below.getTextContent().contains("偵測到 1 個，未偵測到 0 個。"));
      assertEquals(0, below.querySelectorAll(".undetected-mutation").getLength());
      HtmlElement noData = (HtmlElement) page.querySelector("#verification-AC-NO-DATA");
      ((HtmlElement) noData.querySelector("[data-ac-toggle]")).click();
      assertTrue(noData.getTextContent().contains("本次執行沒有突變被精確歸因到這個 AC。"));
      HtmlDetails technicalDetails =
          (HtmlDetails) page.querySelector("#mutation-technical-details");
      assertFalse(technicalDetails.isOpen(), "技術細節預設維持收合");
      assertTrue(technicalDetails.getTextContent().contains("KILLED"));
      assertTrue(technicalDetails.getTextContent().contains("SURVIVED"));
      assertTrue(technicalDetails.getTextContent().contains("Production.java"));
      assertTrue(technicalDetails.getTextContent().contains("AC-BELOW"));
    }
  }

  @Test
  void verificationKeepsEveryAcReadableWhenSeveralIndependentSafeguardsFindProblems()
      throws Exception {
    VerificationView view =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            NOW,
            CaseResultStatus.FAIL,
            true,
            List.of(
                new EvidenceGate("CONTRACT_INTEGRITY", EvidenceVerdict.PASS, null),
                new EvidenceGate("JUNIT", EvidenceVerdict.FAIL, "One public example failed."),
                new EvidenceGate(
                    "REVIEWER_JUNIT", EvidenceVerdict.FAIL, "One reviewer example failed."),
                new EvidenceGate(
                    "EXPECTED_CONSUMPTION",
                    EvidenceVerdict.FAIL,
                    "One expected result was not compared."),
                new EvidenceGate(
                    "PROPERTY", EvidenceVerdict.FAIL, "One Property found a counterexample."),
                new EvidenceGate(
                    "MUTATION",
                    EvidenceVerdict.NOT_APPLICABLE,
                    "Mutation Testing is unavailable for this fixture.")),
            List.of(
                multiFailureAc(
                    "AC-PUBLIC",
                    "Reject an invalid public checkout",
                    CaseResultStatus.FAIL,
                    "ASSERTED",
                    true,
                    false),
                multiFailureAc(
                    "AC-HIDDEN",
                    "Reject the reviewer checkout boundary",
                    CaseResultStatus.PASS,
                    "ASSERTED",
                    false,
                    true),
                multiFailureAc(
                    "AC-EXPECTED",
                    "Compare the complete receipt",
                    CaseResultStatus.PASS,
                    "READ",
                    false,
                    false),
                multiFailureAc(
                    "AC-PROPERTY",
                    "Keep the payable total valid",
                    CaseResultStatus.PASS,
                    "ASSERTED",
                    false,
                    false),
                multiFailureAc(
                    "AC-CONTROL-ONE",
                    "Keep the first control checkout",
                    CaseResultStatus.PASS,
                    "ASSERTED",
                    false,
                    false),
                multiFailureAc(
                    "AC-CONTROL-TWO",
                    "Keep the second control checkout",
                    CaseResultStatus.PASS,
                    "ASSERTED",
                    false,
                    false)),
            DeliveryScope.from(
                SelectedSpecScope.create(
                    List.of(
                        new SelectedSpecDocument("specs/cart-pricing/spec.md", "a".repeat(64)),
                        new SelectedSpecDocument("specs/checkout/spec.md", "b".repeat(64))),
                    List.of(
                        "AC-CONTROL-ONE",
                        "AC-CONTROL-TWO",
                        "AC-EXPECTED",
                        "AC-HIDDEN",
                        "AC-PROPERTY",
                        "AC-PUBLIC")),
                "enabled",
                "enabled",
                "enabled",
                6,
                1),
            null,
            null);
    view =
        ReportViews.withVerificationProperties(
            view,
            Map.of(
                "AC-PROPERTY",
                List.of(
                    new VerificationProperty(
                        "Generated legal carts never have a negative payable total",
                        "CheckoutProperties#payableTotalIsNonNegative(PropertyTrials)",
                        "FAIL",
                        200,
                        12,
                        0,
                        12,
                        1,
                        List.of(),
                        1L,
                        true,
                        "replay-token",
                        new VerificationCounterexample("{\"cart\":\"large\"}", List.of()),
                        new VerificationCounterexample("{\"cart\":\"minimal\"}", List.of(0)),
                        1,
                        true,
                        List.of(new VerificationDiscardedInput("{\"cart\":\"discarded\"}")),
                        null))));
    view = ReportViews.withMutationAttribution(view, null);

    Path bundle = tempDir.resolve("multi-failure-verification");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertEquals(6, page.querySelectorAll(".ac-card").getLength());
      assertTrue(
          ((HtmlElement) page.querySelector("#all-acs"))
              .getAttribute("class")
              .contains("verification-workspace"));
      String problemLinks = page.querySelector("#problems").getTextContent();
      assertTrue(problemLinks.contains("AC-PUBLIC"));
      assertTrue(problemLinks.contains("AC-HIDDEN"));
      assertTrue(problemLinks.contains("AC-EXPECTED"));
      assertTrue(problemLinks.contains("AC-PROPERTY"));
      assertFalse(problemLinks.contains("AC-CONTROL-ONE"));
      assertFalse(problemLinks.contains("AC-CONTROL-TWO"));
      assertNotNull(page.querySelector("#problems a[href='#verification-AC-PUBLIC']"));
      assertNotNull(page.querySelector("#problems a[href='#verification-AC-HIDDEN']"));
      assertNotNull(page.querySelector("#problems a[href='#verification-AC-EXPECTED']"));
      assertNotNull(page.querySelector("#problems a[href='#verification-AC-PROPERTY']"));

      for (String acId :
          List.of(
              "AC-PUBLIC",
              "AC-HIDDEN",
              "AC-EXPECTED",
              "AC-PROPERTY",
              "AC-CONTROL-ONE",
              "AC-CONTROL-TWO")) {
        assertOrder(
            ((HtmlElement) page.querySelector("#verification-" + acId)).getTextContent(),
            "Public Acceptance",
            "Hidden Tests",
            "Expected Result Check",
            "Property-Based Testing",
            "Mutation Testing");
      }

      HtmlElement affected = (HtmlElement) page.querySelector("#verification-AC-PROPERTY");
      String safeguardOrder = affected.getTextContent();
      assertTrue(
          safeguardOrder.contains("Generated legal carts never have a negative payable total"));
      assertTrue(safeguardOrder.contains("12 of 200 requested generated inputs completed"));
      assertTrue(safeguardOrder.contains("discarded inputs: 1"));
      assertTrue(
          safeguardOrder.contains(
              "A generated input violated this Property, so this check stopped early."));
      assertTrue(
          ((HtmlElement) page.querySelector("#verification-AC-CONTROL-ONE"))
              .getAttribute("class")
              .contains("PASS"));
      assertTrue(
          ((HtmlElement) page.querySelector("#verification-AC-CONTROL-TWO"))
              .getAttribute("class")
              .contains("PASS"));
    }

    Path traditionalChineseBundle = tempDir.resolve("multi-failure-verification-zh-TW");
    HtmlBundleWriter.verification(traditionalChineseBundle, view, ReportLanguage.ZH_TW);
    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page =
          client.getPage(traditionalChineseBundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertEquals("zh-TW", ((HtmlElement) page.querySelector("html")).getAttribute("lang"));
      assertEquals("驗證報告", page.getTitleText());
      assertTrue(page.asNormalizedText().contains("所有 AC"));
      assertTrue(page.asNormalizedText().contains("Keep the first control checkout"));
    }
  }

  @Test
  void verificationMarksMutationEvidenceUnavailableWhenPublicAcceptanceHasNoReliableBaseline()
      throws Exception {
    String mutator = ToppleCatManagedMutationProfile.operatorIds().getFirst();
    PitMutationAttribution attribution =
        new PitMutationAttribution(
            ToppleCatManagedMutationProfile.PIT_VERSION,
            ToppleCatManagedMutationProfile.PROFILE_ID,
            ToppleCatManagedMutationProfile.operatorIds(),
            1,
            1,
            0,
            List.of(new PitOutcomeCount("SURVIVED", false, 1)),
            List.of(),
            List.of(),
            List.of(
                new PitMutationAssessment(
                    "AC-BASELINE",
                    List.of("example.CheckoutAcceptance#checks()V"),
                    1,
                    0,
                    List.of(),
                    false)),
            List.of(
                new PitMutationEvidence(
                    false,
                    "SURVIVED",
                    "example.CheckoutService",
                    "CheckoutService.java",
                    "total",
                    "(I)I",
                    21,
                    0,
                    0,
                    mutator,
                    "Replaced integer addition with subtraction",
                    List.of("example.CheckoutAcceptance#checks()V"),
                    List.of(),
                    List.of("example.CheckoutAcceptance#checks()V"),
                    List.of("AC-BASELINE"),
                    List.of(),
                    "return subtotal + tax;")));
    VerificationView view =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            NOW,
            CaseResultStatus.FAIL,
            true,
            List.of(
                new EvidenceGate("CONTRACT_INTEGRITY", EvidenceVerdict.PASS, null),
                new EvidenceGate("JUNIT", EvidenceVerdict.FAIL, "A public example failed."),
                new EvidenceGate("REVIEWER_JUNIT", EvidenceVerdict.PASS, null),
                new EvidenceGate("EXPECTED_CONSUMPTION", EvidenceVerdict.PASS, null),
                new EvidenceGate("PROPERTY", EvidenceVerdict.NOT_APPLICABLE, null),
                new EvidenceGate(
                    "MUTATION",
                    EvidenceVerdict.INCOMPLETE,
                    "Mutation Testing could not establish a reliable baseline because public"
                        + " acceptance found a problem in this run.")),
            List.of(mutationAcceptanceCondition("AC-BASELINE", "Calculate the checkout total")),
            null,
            attribution,
            null);
    view = ReportViews.withMutationAttribution(view, attribution);

    assertEquals(
        EvidenceVerdict.INCOMPLETE,
        view.acceptanceConditions().getFirst().safeguards().stream()
            .filter(safeguard -> safeguard.name().equals("MUTATION_TESTING"))
            .findFirst()
            .orElseThrow()
            .verdict());

    Path bundle = tempDir.resolve("mutation-baseline-unavailable");
    HtmlBundleWriter.verification(bundle, view);
    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      HtmlElement card = (HtmlElement) page.querySelector("#verification-AC-BASELINE");
      assertTrue(card.getTextContent().contains("Unable to assess"));
      assertTrue(
          card.getTextContent()
              .contains(
                  "Public Acceptance already found a problem in the original program. Without a"
                      + " passing baseline"));
      assertTrue(
          card.getTextContent()
              .contains(
                  "Mutation Testing could not establish a reliable baseline because public"
                      + " acceptance found a problem in this run."));
      assertFalse(
          card.getTextContent().contains("This AC was assessed against 1 attributed changes"));
      HtmlDetails technical = (HtmlDetails) page.querySelector("#mutation-technical-details");
      assertFalse(technical.isOpen());
      assertTrue(technical.getTextContent().contains("SURVIVED"));
      assertTrue(technical.getTextContent().contains("Replaced integer addition with subtraction"));
    }
  }

  private VerificationView verificationPassView(DeliveryScope deliveryScope) throws Exception {
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
                new EvidenceGate("MUTATION", EvidenceVerdict.NOT_APPLICABLE, null)),
            List.of(mutationAcceptanceCondition("AC-CHECKOUT", "Checkout total")),
            deliveryScope,
            null,
            new VerificationRunSummary("run-scoped", NOW, NOW, 1, 0, 0, 0));
    return ReportViews.withMutationAttribution(view, null);
  }

  private VerificationView readingControlsView() throws Exception {
    Map<String, String> titles = new LinkedHashMap<>();
    List<ToppleCaseData> cases = new ArrayList<>();
    Map<String, ReportViews.CaseExecution> executions = new LinkedHashMap<>();
    for (int index = 0; index < 6; index++) {
      String acId =
          index == 0 ? "AC-READ-FAIL" : index == 1 ? "AC-READ-INCOMPLETE" : "AC-READ-PASS-" + index;
      titles.put(acId, "Read " + acId);
      for (CaseVisibility visibility : List.of(CaseVisibility.PUBLIC, CaseVisibility.HIDDEN)) {
        String caseId = acId + (visibility == CaseVisibility.PUBLIC ? "-public" : "-hidden");
        CaseResultStatus status =
            index == 0 && visibility == CaseVisibility.PUBLIC
                ? CaseResultStatus.FAIL
                : index == 1 ? CaseResultStatus.NOT_REPORTED : CaseResultStatus.PASS;
        cases.add(
            new ToppleCaseData(
                caseId,
                acId,
                visibility,
                JSON.readTree("{\"input\":\"" + caseId + "\"}"),
                JSON.readTree("{\"result\":true}"),
                Path.of("reading-controls.json")));
        executions.put(
            caseId,
            new ReportViews.CaseExecution(
                status,
                status == CaseResultStatus.FAIL ? "The public example found a problem." : null,
                List.of(),
                status == CaseResultStatus.NOT_REPORTED ? Map.of() : Map.of("result", "ASSERTED")));
      }
    }
    VerificationView view =
        ReportViews.verification(
            titles,
            cases,
            executions,
            true,
            List.of(
                new EvidenceGate("CONTRACT_INTEGRITY", EvidenceVerdict.PASS, null),
                new EvidenceGate("JUNIT", EvidenceVerdict.PASS, null),
                new EvidenceGate("REVIEWER_JUNIT", EvidenceVerdict.PASS, null),
                new EvidenceGate("EXPECTED_CONSUMPTION", EvidenceVerdict.PASS, null),
                new EvidenceGate("PROPERTY", EvidenceVerdict.NOT_APPLICABLE, null),
                new EvidenceGate("MUTATION", EvidenceVerdict.NOT_APPLICABLE, null)),
            NOW);
    return ReportViews.withMutationAttribution(view, null);
  }

  private VerificationAcceptanceCondition multiFailureAc(
      String acId,
      String title,
      CaseResultStatus publicStatus,
      String expectedConsumption,
      boolean publicFails,
      boolean hiddenFails)
      throws Exception {
    List<VerificationCase> cases =
        List.of(
            new VerificationCase(
                acId + "-public",
                CaseVisibility.PUBLIC,
                JSON.readTree("{\"cart\":\"public\"}"),
                JSON.readTree("{\"receipt\":{\"accepted\":true}}"),
                publicFails ? CaseResultStatus.FAIL : publicStatus,
                Map.of("receipt", expectedConsumption),
                List.of(),
                publicFails ? "The public example found a problem." : null),
            new VerificationCase(
                acId + "-hidden",
                CaseVisibility.HIDDEN,
                JSON.readTree("{\"cart\":\"reviewer-example\"}"),
                JSON.readTree("{\"receipt\":{\"accepted\":true}}"),
                hiddenFails ? CaseResultStatus.FAIL : CaseResultStatus.PASS,
                Map.of("receipt", "ASSERTED"),
                List.of(),
                hiddenFails ? "The reviewer example found a problem." : null));
    return new VerificationAcceptanceCondition(acId, title, CaseResultStatus.PASS, cases);
  }

  private static void assertOrder(String text, String... fragments) {
    int previous = -1;
    for (String fragment : fragments) {
      int current = text.indexOf(fragment);
      assertTrue(current > previous, "expected " + fragment + " after the prior safeguard");
      previous = current;
    }
  }

  private VerificationAcceptanceCondition mutationAcceptanceCondition(String acId, String title)
      throws Exception {
    return new VerificationAcceptanceCondition(
        acId,
        title,
        CaseResultStatus.PASS,
        List.of(
            new VerificationCase(
                acId + "-public",
                CaseVisibility.PUBLIC,
                JSON.readTree("{}"),
                JSON.readTree("{}"),
                CaseResultStatus.PASS,
                Map.of(),
                List.of(),
                null),
            new VerificationCase(
                acId + "-hidden",
                CaseVisibility.HIDDEN,
                JSON.readTree("{}"),
                JSON.readTree("{}"),
                CaseResultStatus.PASS,
                Map.of(),
                List.of(),
                null)));
  }
}
