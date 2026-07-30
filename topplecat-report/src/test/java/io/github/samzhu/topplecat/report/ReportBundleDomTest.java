package io.github.samzhu.topplecat.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlDetails;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Exercises the offline bundle as a DOM after its bundled JavaScript has rendered it. */
class ReportBundleDomTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @TempDir Path tempDir;

  @Test
  void reviewerBundleSupportsAcDisclosureCaseSwitchingSearchAndSafeEscaping() throws Exception {
    ReviewView view =
        new ReviewView(
            ReviewView.SCHEMA_VERSION,
            Instant.parse("2026-07-28T00:00:00Z"),
            List.of(
                acceptance(
                    "AC-FIRST",
                    "First business result",
                    List.of(
                        row(CaseVisibility.PUBLIC, "public-case", "customer-public", 500),
                        row(CaseVisibility.HIDDEN, "hidden-case", "customer-<hidden>", 800))),
                acceptance(
                    "AC-HIDDEN-ONLY",
                    "Hidden only result",
                    List.of(
                        row(
                            CaseVisibility.HIDDEN,
                            "hidden-only-case",
                            "customer-hidden-only",
                            40)))));
    Path bundle = tempDir.resolve("review");
    HtmlBundleWriter.review(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      DomNodeList<DomNode> acs = page.querySelectorAll("details.ac");
      assertEquals(2, acs.size());
      assertTrue(((HtmlDetails) acs.get(0)).isOpen(), "the first AC starts expanded");
      assertFalse(((HtmlDetails) acs.get(1)).isOpen(), "later ACs start collapsed");
      assertEquals(
          "true",
          ((DomElement) page.querySelector("button[data-case='public-case']"))
              .getAttribute("aria-selected"));
      assertEquals(
          "true",
          ((DomElement) page.querySelector("button[data-case='hidden-only-case']"))
              .getAttribute("aria-selected"));
      assertEquals(2, page.querySelectorAll("details.source").size());
      assertFalse(((HtmlDetails) page.querySelector("details.source")).isOpen());
      assertTrue(
          page.querySelector("section.case-detail").asNormalizedText().contains("customer-public"));
      assertFalse(
          page.asXml().contains("<hidden>"), "case text must be escaped before it enters the DOM");

      DomElement hiddenRow = (DomElement) page.querySelector("tr[data-case='hidden-case']");
      assertEquals("button", hiddenRow.getAttribute("role"));
      assertEquals("0", hiddenRow.getAttribute("tabindex"));
      page.executeJavaScript(
          "document.querySelector(\"tr[data-case='hidden-case']\")"
              + ".dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }))");
      client.waitForBackgroundJavaScript(250);
      assertEquals(
          "true",
          ((DomElement) page.querySelector("button[data-case='hidden-case']"))
              .getAttribute("aria-selected"));
      assertTrue(
          page.querySelector("section.case-detail")
              .asNormalizedText()
              .contains("customer-<hidden>"));

      HtmlButton collapse = (HtmlButton) page.querySelector("button[data-action='collapse']");
      page = collapse.click();
      client.waitForBackgroundJavaScript(250);
      assertTrue(page.querySelectorAll("details.ac[open]").isEmpty());
      HtmlButton expand = (HtmlButton) page.querySelector("button[data-action='expand']");
      page = expand.click();
      client.waitForBackgroundJavaScript(250);
      assertEquals(2, page.querySelectorAll("details.ac[open]").size());

      HtmlInput search = (HtmlInput) page.querySelector("#query");
      search.type("hidden-only-case");
      client.waitForBackgroundJavaScript(250);
      assertEquals(1, page.querySelectorAll("details.ac").size());
      assertTrue(((HtmlDetails) page.querySelector("details.ac")).isOpen());
    }
  }

  @Test
  void verificationBundleUsesRuntimeNarrativeAndKeepsExecutionEvidenceInTheSelectedCase()
      throws Exception {
    VerificationCase caseResult =
        new VerificationCase(
            "runtime-case",
            CaseVisibility.HIDDEN,
            JSON.readTree("{\"cart\":{\"subtotal\":800}}"),
            JSON.readTree("{\"receipt\":{\"discountedSubtotal\":700}}"),
            CaseResultStatus.FAIL,
            Map.of("receipt", "ASSERTED"),
            List.of(
                new NarrativeStep(
                    "runtime-step",
                    "actual runtime sentence",
                    NarrativeStepStatus.FAIL,
                    2_500_000,
                    List.of(),
                    List.of(),
                    "step evidence reference"),
                new NarrativeStep(
                    "runtime-skipped",
                    "Then runtime step was skipped",
                    NarrativeStepStatus.SKIPPED,
                    0,
                    List.of(),
                    List.of(),
                    null)),
            "case-level mismatch");
    VerificationAcceptanceCondition acceptance =
        new VerificationAcceptanceCondition(
            "AC-RUNTIME",
            "Runtime title",
            List.of("Given template sentence that must not replace runtime evidence"),
            CaseResultStatus.FAIL,
            List.of(caseResult),
            List.of(),
            Map.of(
                "runtime-step", new SourceRef("RuntimeTest.java", 19, 4),
                "runtime-skipped", new SourceRef("RuntimeTest.java", 20, 4)),
            Map.of("runtime-step", StepPhase.GIVEN, "runtime-skipped", StepPhase.THEN));
    VerificationView view =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            Instant.parse("2026-07-28T00:00:00Z"),
            CaseResultStatus.FAIL,
            true,
            List.of(),
            List.of(acceptance));
    Path bundle = tempDir.resolve("verification");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      DomElement detail = (DomElement) page.querySelector("section.case-detail");
      String rendered = detail.asNormalizedText();
      assertTrue(rendered.contains("actual runtime sentence"));
      assertFalse(rendered.contains("template sentence that must not replace runtime evidence"));
      assertTrue(rendered.contains("Given"));
      assertTrue(rendered.contains("FAIL"));
      assertTrue(rendered.contains("2.5 ms"));
      assertTrue(rendered.contains("RuntimeTest.java:19"));
      assertTrue(rendered.contains("step evidence reference"));
      assertTrue(rendered.contains("runtime step was skipped"));
      assertTrue(rendered.contains("SKIPPED"));
      assertTrue(rendered.contains("case-level mismatch"));
      assertTrue(rendered.contains("ASSERTED"));
    }
  }

  private static ReviewAcceptanceCondition acceptance(
      String id, String title, List<ReviewCase> cases) {
    return new ReviewAcceptanceCondition(
        id,
        title,
        cases,
        List.of(),
        new ReviewMethod(
            List.of("Given fallback", "When fallback", "Then fallback"),
            "@ToppleAcceptanceTest(\"" + id + "\")\nvoid example() {}"));
  }

  private static ReviewCase row(CaseVisibility visibility, String id, String customer, int subtotal)
      throws Exception {
    return new ReviewCase(
        visibility,
        id,
        JSON.readTree(
            "{\"cart\":{\"customerId\":\""
                + customer
                + "\",\"lines\":[{\"sku\":\"BOOK\",\"quantity\":2}],\"subtotal\":"
                + subtotal
                + "}}"),
        JSON.readTree(
            "{\"receipt\":{\"discount\":100,\"discountedSubtotal\":" + (subtotal - 100) + "}}"),
        List.of(
            new ReviewScenarioStep(StepPhase.GIVEN, "prepares " + customer + " cart"),
            new ReviewScenarioStep(StepPhase.WHEN, "creates order"),
            new ReviewScenarioStep(StepPhase.THEN, "shows discount")));
  }
}
