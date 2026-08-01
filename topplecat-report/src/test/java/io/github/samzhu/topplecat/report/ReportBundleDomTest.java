package io.github.samzhu.topplecat.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.EvidenceGate;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import io.github.samzhu.topplecat.pitest.PitMutationAttribution;
import io.github.samzhu.topplecat.pitest.PitMutationEvidence;
import io.github.samzhu.topplecat.pitest.PitMutatorSummary;
import io.github.samzhu.topplecat.pitest.PitOutcomeCount;
import io.github.samzhu.topplecat.pitest.ToppleCatManagedMutationProfile;
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

  @Test
  void verificationBundleSeparatesMechanicalAndFunctionalSafeguards() throws Exception {
    VerificationView view =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            Instant.parse("2026-07-28T00:00:00Z"),
            CaseResultStatus.FAIL,
            true,
            List.of(
                new EvidenceGate("CONTRACT_INTEGRITY", EvidenceVerdict.PASS),
                new EvidenceGate("REVIEWER_JUNIT", EvidenceVerdict.PASS),
                new EvidenceGate("PROPERTY", EvidenceVerdict.PASS),
                new EvidenceGate("MUTATION", EvidenceVerdict.INCOMPLETE, "current report missing")),
            List.of());
    Path bundle = tempDir.resolve("safeguards");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      String rendered = page.asNormalizedText();
      assertTrue(rendered.contains("Mechanical Seal / Contract Integrity"));
      assertTrue(rendered.contains("Hidden Tests"));
      assertTrue(rendered.contains("Property-Based Testing"));
      assertTrue(rendered.contains("Mutation Testing"));
      assertTrue(rendered.contains("No current managed PIT attribution"));
    }
  }

  @Test
  void verificationBundleRendersNonemptyManagedPitAttributionOnlyInItsOwnReviewerSection()
      throws Exception {
    VerificationView view =
        new VerificationView(
            VerificationView.SCHEMA_VERSION,
            Instant.parse("2026-08-01T00:00:00Z"),
            CaseResultStatus.FAIL,
            true,
            List.of(
                new EvidenceGate("CONTRACT_INTEGRITY", EvidenceVerdict.PASS),
                new EvidenceGate("REVIEWER_JUNIT", EvidenceVerdict.PASS),
                new EvidenceGate("PROPERTY", EvidenceVerdict.PASS),
                new EvidenceGate("MUTATION", EvidenceVerdict.FAIL)),
            List.of(),
            null,
            managedPitAttribution());
    Path bundle = tempDir.resolve("managed-pit-attribution");
    HtmlBundleWriter.verification(bundle, view);

    try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
      client.getOptions().setThrowExceptionOnScriptError(true);
      HtmlPage page = client.getPage(bundle.resolve("index.html").toUri().toURL());
      client.waitForBackgroundJavaScript(250);

      assertEquals(1, page.querySelectorAll("section.mechanical-seal-summary").size());
      assertEquals(2, page.querySelectorAll("section.safeguard-summary").size());
      assertEquals(1, page.querySelectorAll("section.mutation-summary").size());
      ((HtmlDetails) page.querySelector("details.raw-case")).setOpen(true);
      String rendered = page.asNormalizedText();
      assertTrue(rendered.contains("Mechanical Seal / Contract Integrity"));
      assertTrue(rendered.contains("Hidden Tests"));
      assertTrue(rendered.contains("Property-Based Testing"));
      assertTrue(rendered.contains("Mutation Testing"));
      assertFalse(rendered.contains("Aggregate score"));
      assertFalse(rendered.contains("Blended quality"));

      assertTrue(rendered.contains("PIT 1.25.5"));
      assertTrue(rendered.contains("topplecat-managed-v1"));
      ToppleCatManagedMutationProfile.operatorIds()
          .forEach(operator -> assertTrue(rendered.contains(operator)));
      assertTrue(rendered.contains("2 producer mutants"));
      assertTrue(rendered.contains("1 uniquely attributed to public Acceptance Methods"));
      assertTrue(rendered.contains("1 unattributed"));
      assertTrue(rendered.contains("Per-mutator summary"));
      assertTrue(rendered.contains("MathMutator"));
      assertTrue(rendered.contains("VoidMethodCallMutator"));

      assertTrue(rendered.contains("AC-COVERED"));
      assertTrue(rendered.contains("AC-GAP"));
      assertTrue(rendered.contains("Covered mutants"));
      assertTrue(rendered.contains("Detected by this Acceptance Method"));
      assertTrue(rendered.contains("Sealed threshold"));
      assertTrue(rendered.contains("Detection rate"));
      assertTrue(rendered.contains("100%"));
      assertTrue(rendered.contains("沒有取得本次 managed mutation profile 的歸因證據"));

      assertTrue(rendered.contains("KILLED"));
      assertTrue(rendered.contains("detected true"));
      assertTrue(rendered.contains("MathMutator"));
      assertTrue(rendered.contains("Replaced integer subtraction with addition"));
      assertTrue(rendered.contains("coveringTests"));
      assertTrue(rendered.contains("killingTests"));
      assertTrue(rendered.contains("succeedingTests"));
      assertTrue(rendered.contains("shop.CouponAcceptanceTest#appliesCoupon()V"));
      assertTrue(
          rendered.contains("shop.CouponAcceptanceTest#appliesCoupon()[method:public-case]"));
    }
  }

  private static PitMutationAttribution managedPitAttribution() {
    return new PitMutationAttribution(
        ToppleCatManagedMutationProfile.PIT_VERSION,
        ToppleCatManagedMutationProfile.PROFILE_ID,
        ToppleCatManagedMutationProfile.operatorIds(),
        2,
        1,
        1,
        List.of(new PitOutcomeCount("KILLED", true, 1), new PitOutcomeCount("SURVIVED", false, 1)),
        List.of(new PitOutcomeCount("SURVIVED", false, 1)),
        List.of(
            new PitMutatorSummary(
                "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
                1,
                List.of(new PitOutcomeCount("KILLED", true, 1))),
            new PitMutatorSummary(
                "org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator",
                1,
                List.of(new PitOutcomeCount("SURVIVED", false, 1)))),
        List.of(
            new PitMutationAssessment(
                "AC-COVERED",
                List.of("shop.CouponAcceptanceTest#appliesCoupon()V"),
                1,
                1,
                100,
                100,
                List.of(new PitOutcomeCount("KILLED", true, 1)),
                false),
            new PitMutationAssessment(
                "AC-GAP",
                List.of("shop.OtherAcceptanceTest#notCovered()V"),
                0,
                0,
                100,
                0,
                List.of(),
                true)),
        List.of(
            new PitMutationEvidence(
                true,
                "KILLED",
                "shop.CouponService",
                "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
                "Replaced integer subtraction with addition",
                List.of("shop.CouponAcceptanceTest#appliesCoupon()V"),
                List.of("shop.CouponAcceptanceTest#appliesCoupon()[method:public-case]"),
                List.of("shop.CouponAcceptanceTest#appliesCoupon()[test-template:coupon]"),
                List.of("AC-COVERED")),
            new PitMutationEvidence(
                false,
                "SURVIVED",
                "shop.CouponService",
                "org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator",
                "removed call to audit",
                List.of(),
                List.of(),
                List.of("shop.CouponAcceptanceTest#appliesCoupon()V"),
                List.of())));
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
