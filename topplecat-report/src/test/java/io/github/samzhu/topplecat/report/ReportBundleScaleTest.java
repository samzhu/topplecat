package io.github.samzhu.topplecat.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.AttachmentRef;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.EvidenceGate;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Deterministic report-scale gate: 100 ACs, 1,000 cases, 5,000 steps, and 100 thumbnails. */
class ReportBundleScaleTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @TempDir Path tempDir;

  @Test
  void writesTheSpecifiedLargeOfflineVerificationBundleWithinTwoSeconds() throws Exception {
    Map<String, String> titles = new LinkedHashMap<>();
    Map<String, List<StepTemplate>> templates = new LinkedHashMap<>();
    Map<String, ReportViews.CaseExecution> executions = new LinkedHashMap<>();
    List<ToppleCaseData> cases = new ArrayList<>();
    AttachmentRef thumbnail =
        new AttachmentRef(
            "a".repeat(64),
            "Checkout screenshot",
            "image/png",
            67,
            CaseVisibility.PUBLIC,
            "attachments/" + "a".repeat(64) + ".png");
    for (int ac = 0; ac < 100; ac++) {
      String acId = "AC-SCALE-%03d".formatted(ac);
      titles.put(acId, "長標題驗收條件 " + ac + " 這是一個可換行的 CJK 報表標題");
      templates.put(acId, steps(acId));
      for (int row = 0; row < 10; row++) {
        String caseId = acId + "-case-" + row + "-long-unbroken-identifier-for-layout-validation";
        cases.add(
            new ToppleCaseData(
                caseId,
                acId,
                CaseVisibility.PUBLIC,
                JSON.readTree("{\"request\":{\"index\":" + row + ",\"nested\":{\"value\":\"x\"}}}"),
                JSON.readTree("{\"response\":{\"accepted\":true,\"index\":" + row + "}}"),
                Path.of("scale.json")));
        List<NarrativeStep> stepRuns = new ArrayList<>();
        for (StepTemplate template : templates.get(acId)) {
          List<AttachmentRef> attachments =
              row == 0 && template.stepId().endsWith("step2()V") ? List.of(thumbnail) : List.of();
          stepRuns.add(
              new NarrativeStep(
                  template.stepId(),
                  template.tokens().getLast().value(),
                  NarrativeStepStatus.PASS,
                  1_000_000,
                  List.of(JSON.readTree("{\"case\":\"" + caseId + "\"}")),
                  attachments,
                  ""));
        }
        executions.put(
            caseId,
            new ReportViews.CaseExecution(
                CaseResultStatus.PASS, null, stepRuns, Map.of("response", "ASSERTED")));
      }
    }

    long started = System.nanoTime();
    VerificationView view =
        ReportViews.verificationFromTemplates(
            titles,
            cases,
            executions,
            templates,
            true,
            List.of(new EvidenceGate("CONTRACT_INTEGRITY", EvidenceVerdict.PASS, null)),
            Instant.parse("2026-07-24T00:00:00Z"));
    view = ReportViews.withMutationAttribution(view, null);
    Path bundle = tempDir.resolve("verification");
    HtmlBundleWriter.verification(bundle, view);
    Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

    assertEquals(100, view.acceptanceConditions().size());
    assertEquals(
        1_000, view.acceptanceConditions().stream().mapToInt(ac -> ac.cases().size()).sum());
    assertEquals(
        5_000,
        view.acceptanceConditions().stream()
            .flatMap(ac -> ac.cases().stream())
            .mapToInt(row -> row.steps().size())
            .sum());
    assertEquals(
        100,
        view.acceptanceConditions().stream()
            .flatMap(ac -> ac.cases().stream())
            .flatMap(row -> row.steps().stream())
            .flatMap(step -> step.attachments().stream())
            .count());
    assertTrue(Files.size(bundle.resolve("data.json")) > 100_000);
    assertTrue(
        elapsed.compareTo(Duration.ofSeconds(2)) < 0,
        "large report generation took " + elapsed.toMillis() + "ms");

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);
      assertEquals(100, page.querySelectorAll(".ac-card").getLength());
      assertEquals(100, page.querySelectorAll(".ac-reader[hidden]").getLength());
      assertEquals(1_000, page.querySelectorAll("details[data-lazy-case]").getLength());
      assertEquals(0, page.querySelectorAll("details[data-lazy-case][open]").getLength());
      assertEquals(0, page.querySelectorAll("[data-lazy-case][data-loaded='true']").getLength());
      assertTrue(
          !page.asNormalizedText().contains("AC-SCALE-000-case-0-long-unbroken-identifier"),
          "initial key-result rendering must not expose case reader material");

      HtmlElement global = (HtmlElement) page.querySelector("[data-global-reading]");
      HtmlElement bulkStatus = (HtmlElement) page.querySelector("[data-bulk-status]");
      global.click();
      waitForCondition(
          client,
          () -> {
            int completed = Integer.parseInt(bulkStatus.getAttribute("data-completed"));
            return completed > 0 && completed < 100;
          },
          "bulk expansion should expose non-zero progress before completion");
      int completedBeforeStop = Integer.parseInt(bulkStatus.getAttribute("data-completed"));
      int loadedBeforeStop =
          page.querySelectorAll("[data-lazy-case][data-loaded='true']").getLength();
      assertEquals(completedBeforeStop * 10, loadedBeforeStop);

      global.click();
      waitForCondition(
          client,
          () -> bulkStatus.getTextContent().contains("Expansion stopped after"),
          "bulk cancellation should report an interrupted count");
      int completedAfterStop = Integer.parseInt(bulkStatus.getAttribute("data-completed"));
      assertTrue(completedAfterStop >= completedBeforeStop);
      assertTrue(completedAfterStop > 0);
      assertTrue(
          bulkStatus
              .getTextContent()
              .contains("Expansion stopped after " + completedAfterStop + " of 100"));
      assertEquals(0, page.querySelectorAll(".ac-card[data-expanded='true']").getLength());
      assertEquals(0, page.querySelectorAll("details[data-lazy-case][open]").getLength());
      int loadedAfterStop =
          page.querySelectorAll("[data-lazy-case][data-loaded='true']").getLength();
      assertEquals(completedAfterStop * 10, loadedAfterStop);
      client.waitForBackgroundJavaScript(100);
      assertEquals(
          loadedAfterStop,
          page.querySelectorAll("[data-lazy-case][data-loaded='true']").getLength());
      assertEquals(completedAfterStop, Integer.parseInt(bulkStatus.getAttribute("data-completed")));

      global.click();
      waitForCondition(
          client,
          () -> bulkStatus.getTextContent().contains("All AC reader details are open."),
          "bulk expansion should report its completed state",
          () ->
              "status="
                  + bulkStatus.getTextContent()
                  + ", completed="
                  + bulkStatus.getAttribute("data-completed")
                  + ", expanded="
                  + page.querySelectorAll(".ac-card[data-expanded='true']").getLength()
                  + ", loaded="
                  + page.querySelectorAll("[data-lazy-case][data-loaded='true']").getLength(),
          1_000);
      assertEquals("100", bulkStatus.getAttribute("data-completed"));
      assertEquals("true", global.getAttribute("aria-expanded"));
      assertEquals(100, page.querySelectorAll(".ac-card[data-expanded='true']").getLength());
      assertEquals(1_000, page.querySelectorAll("details[data-lazy-case][open]").getLength());
      assertEquals(
          1_000, page.querySelectorAll("[data-lazy-case][data-loaded='true']").getLength());
      assertEquals(0, page.querySelectorAll(".ac-technical[open]").getLength());
    }
  }

  @Test
  void selectedSpecReviewKeepsOneHundredProjectionsAndNestedRowsBehindAccessibleDetails()
      throws Exception {
    List<ReviewDocument> documents = new ArrayList<>();
    List<ReviewAcceptanceCondition> conditions = new ArrayList<>();
    for (int index = 0; index < 100; index++) {
      String acId = "AC-REVIEW-SCALE-%03d".formatted(index);
      String path = "specs/checkout-%03d.md".formatted(index);
      documents.add(
          new ReviewDocument(
              path,
              "a".repeat(64),
              List.of(
                  new SpecMarkdownBlock(
                      SpecMarkdownBlock.Kind.HEADING, 2, "Rule " + index, List.of()),
                  new SpecMarkdownBlock(
                      SpecMarkdownBlock.Kind.ACCEPTANCE_MARKER,
                      0,
                      "",
                      List.of(),
                      "",
                      "",
                      "",
                      List.of(),
                      List.of(),
                      acId)),
              List.of()));
      List<ReviewCase> cases = new ArrayList<>();
      for (int row = 0; row < 2; row++) {
        cases.add(
            new ReviewCase(
                CaseVisibility.PUBLIC,
                acId + "-case-" + row,
                JSON.readTree("{\"nested\":{\"input\":\"" + acId + "-input\"}}"),
                JSON.readTree("{\"nested\":{\"expected\":\"" + acId + "-expected\"}}"),
                List.of(new ReviewScenarioStep(StepPhase.GIVEN, "the authored state is ready"))));
      }
      conditions.add(
          new ReviewAcceptanceCondition(
              acId,
              "A long selected review title " + acId,
              new ReviewAcLocation(path, 2, path + "#review-" + acId),
              cases,
              new ReviewMethod(List.of(), "")));
    }
    ReviewView view =
        new ReviewView(
            ReviewView.SCHEMA_VERSION,
            Instant.parse("2026-08-01T00:00:00Z"),
            documents,
            conditions,
            null,
            List.of());
    Path bundle = tempDir.resolve("review-scale");
    long started = System.nanoTime();
    HtmlBundleWriter.review(bundle, view);
    assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(2)) < 0);
    assertTrue(Files.size(bundle.resolve("data.json")) > 100_000);
    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      client.getOptions().setScreenWidth(360);
      client.getOptions().setScreenHeight(800);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(100);
      assertEquals(1, page.querySelectorAll("#selected-documents").getLength());
      assertEquals(100, page.querySelectorAll("#selected-documents article.ac-review").getLength());
      assertEquals(200, page.querySelectorAll("details.case-values").getLength());
      assertEquals(0, page.querySelectorAll("details.case-values[open]").getLength());
      assertFalse(
          (Boolean)
              page.executeJavaScript("document.documentElement.scrollWidth > window.innerWidth")
                  .getJavaScriptResult());
    }
  }

  private static void waitForCondition(
      WebClient client, BooleanSupplier condition, String failureMessage) throws Exception {
    waitForCondition(client, condition, failureMessage, () -> "", 20);
  }

  private static void waitForCondition(
      WebClient client,
      BooleanSupplier condition,
      String failureMessage,
      Supplier<String> failureState)
      throws Exception {
    waitForCondition(client, condition, failureMessage, failureState, 20);
  }

  private static void waitForCondition(
      WebClient client,
      BooleanSupplier condition,
      String failureMessage,
      Supplier<String> failureState,
      long pollMillis)
      throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() >= deadline) {
        throw new AssertionError(failureMessage + " (" + failureState.get() + ")");
      }
      client.waitForBackgroundJavaScript(pollMillis);
    }
  }

  private static List<StepTemplate> steps(String acId) {
    List<StepTemplate> steps = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      StepPhase phase = index == 0 ? StepPhase.GIVEN : index == 1 ? StepPhase.WHEN : StepPhase.THEN;
      steps.add(
          new StepTemplate(
              acId + "#step" + index + "()V",
              phase,
              List.of(
                  new StepToken(StepTokenKind.PHASE, phase.name()),
                  new StepToken(StepTokenKind.LITERAL, "步驟 " + index)),
              List.of(),
              new SourceRef("ScaleFixture.java", index + 1, 1)));
    }
    return steps;
  }
}
