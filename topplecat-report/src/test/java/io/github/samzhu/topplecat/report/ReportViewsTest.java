package io.github.samzhu.topplecat.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.ArgumentBinding;
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
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class ReportViewsTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Instant NOW = Instant.parse("2026-07-21T00:00:00Z");

  @TempDir Path tempDir;

  @Test
  void specViewKeepsOnlyPublicContractMatrixAfterPublicAndHiddenCasesRun() throws Exception {
    ToppleCaseData publicCase = row("coupon-public", CaseVisibility.PUBLIC, "500", "400");
    ToppleCaseData hiddenCase =
        row("coupon-hidden-800", CaseVisibility.HIDDEN, "800", "700-secret-value");
    SpecView view =
        ReportViews.spec(
            Map.of("AC-CART-COUPON", "Apply coupon"),
            List.of(publicCase),
            Map.of(),
            Map.of(
                "AC-CART-COUPON",
                List.of("Given a public coupon cart", "Then the public receipt matches")),
            NOW);
    String json = ReportJson.writeSpec(view);
    Path bundle = tempDir.resolve("spec");
    HtmlBundleWriter.spec(bundle, view);
    String html = Files.readString(bundle.resolve("index.html"));

    assertTrue(json.contains("coupon-public"));
    assertTrue(json.contains("Given a public coupon cart"));
    assertFalse(json.contains("\"steps\""));
    assertFalse(json.contains("\"status\""));
    assertFalse(json.contains("\"failure\""));
    assertFalse(json.contains("\"attachments\""));
    assertFalse(json.contains("coupon-hidden-800"));
    assertFalse(json.contains("800 元"));
    assertFalse(html.contains("coupon-hidden-800"));
    assertFalse(html.contains("800 元"));
    assertFalse(html.contains("700-secret-value"));
    assertTrue(html.contains("id=\"topplecat-report-data\""));
    assertTrue(html.contains("assets/report.js"));
    assertTrue(html.contains("Content-Security-Policy"));
    assertEquals(view, ReportJson.readSpec(json));

    ToppleCatException error =
        assertThrows(
            ToppleCatException.class,
            () -> ReportViews.spec(Map.of(), List.of(publicCase, hiddenCase), NOW));
    assertTrue(error.getMessage().contains("public cases only"));
  }

  @Test
  void verificationViewKeepsReviewerCaseDetailsSeparate() throws Exception {
    ToppleCaseData hiddenCase =
        row("coupon-hidden-800", CaseVisibility.HIDDEN, "800", "700-secret-value");
    VerificationView view =
        ReportViews.verification(
            Map.of(),
            List.of(hiddenCase),
            Map.of(
                "coupon-hidden-800",
                new ReportViews.CaseExecution(
                    CaseResultStatus.FAIL,
                    "discount mismatch",
                    List.of(
                        new NarrativeStep(
                            "test-step",
                            "驗證折抵後的訂單結果",
                            NarrativeStepStatus.FAIL,
                            0,
                            List.of(),
                            List.of(),
                            "")))),
            NOW);

    String json = ReportJson.writeVerification(view);
    Path bundle = tempDir.resolve("verification");
    HtmlBundleWriter.verification(bundle, view);
    String html = Files.readString(bundle.resolve("index.html"));

    assertTrue(json.contains("700-secret-value"));
    assertTrue(json.contains("驗證折抵後的訂單結果"));
    assertTrue(html.contains("discount mismatch"));
    assertTrue(html.contains("FAIL"));
    assertTrue(html.contains("assets/report.js"));
    assertEquals(CaseResultStatus.FAIL, view.verdict());
    assertEquals(view, ReportJson.readVerification(json));
  }

  @Test
  void verificationViewUsesRuntimeConsumptionEvenWhenTheCaseFailsLater() throws Exception {
    ToppleCaseData publicCase = row("coupon-public", CaseVisibility.PUBLIC, "500", "400");
    VerificationView view =
        ReportViews.verification(
            Map.of(),
            List.of(publicCase),
            Map.of(
                "coupon-public",
                new ReportViews.CaseExecution(
                    CaseResultStatus.FAIL,
                    "later assertion failed",
                    List.of(),
                    Map.of("receipt", "ASSERTED"))),
            NOW);

    assertEquals(CaseResultStatus.FAIL, view.verdict());
    assertEquals(
        "ASSERTED",
        view.acceptanceConditions()
            .getFirst()
            .cases()
            .getFirst()
            .expectedConsumption()
            .get("receipt"));
  }

  @Test
  void verificationUsesTheGateFailureAsTheDisplayedSuiteVerdict() throws Exception {
    ToppleCaseData publicCase = row("coupon-public", CaseVisibility.PUBLIC, "500", "400");

    VerificationView view =
        ReportViews.verification(
            Map.of(),
            List.of(publicCase),
            Map.of(
                "coupon-public",
                new ReportViews.CaseExecution(CaseResultStatus.PASS, null, List.of())),
            Map.of(),
            true,
            List.of(
                new EvidenceGate("JUNIT", EvidenceVerdict.FAIL, "the public verification failed.")),
            NOW);

    assertEquals(CaseResultStatus.FAIL, view.verdict());
  }

  @Test
  void verificationPlacesFailuresFirstAndExposesCompilerStepSourceReferences() throws Exception {
    ToppleCaseData passing =
        new ToppleCaseData(
            "case-pass",
            "AC-PASS",
            CaseVisibility.PUBLIC,
            JSON.readTree("{\"cart\":{\"subtotal\":500}}"),
            JSON.readTree("{\"receipt\":{\"discountedSubtotal\":\"400\"}}"),
            Path.of("public.json"));
    ToppleCaseData failing =
        new ToppleCaseData(
            "case-fail",
            "AC-FAILED",
            CaseVisibility.HIDDEN,
            JSON.readTree("{\"cart\":{\"subtotal\":800}}"),
            JSON.readTree("{\"receipt\":{\"discountedSubtotal\":\"700\"}}"),
            Path.of("hidden.yaml"));
    StepTemplate sourceStep =
        new StepTemplate(
            "example.CouponThen#matches()Lexample/CouponThen;",
            StepPhase.THEN,
            List.of(
                new StepToken(StepTokenKind.PHASE, "THEN"),
                new StepToken(StepTokenKind.LITERAL, "驗證結果")),
            List.of(),
            new SourceRef("CouponAcceptanceTest.java", 42, 9));
    VerificationView view =
        ReportViews.verificationFromTemplates(
            Map.of("AC-PASS", "Passing AC", "AC-FAILED", "Failing AC"),
            List.of(passing, failing),
            Map.of(
                "case-pass",
                new ReportViews.CaseExecution(CaseResultStatus.PASS, null, List.of()),
                "case-fail",
                new ReportViews.CaseExecution(
                    CaseResultStatus.FAIL,
                    "expected mismatch",
                    List.of(
                        new NarrativeStep(
                            sourceStep.stepId(),
                            "驗證結果",
                            NarrativeStepStatus.FAIL,
                            1,
                            List.of(),
                            List.of(),
                            "case-failure:case-fail")))),
            Map.of(),
            Map.of("AC-FAILED", List.of(sourceStep)),
            true,
            List.of(),
            NOW);

    assertEquals(
        List.of("AC-FAILED", "AC-PASS"),
        view.acceptanceConditions().stream().map(VerificationAcceptanceCondition::acId).toList());
    assertEquals(
        new SourceRef("CouponAcceptanceTest.java", 42, 9),
        view.acceptanceConditions().getFirst().stepSources().get(sourceStep.stepId()));
    Path bundle = tempDir.resolve("source-reference-verification");
    HtmlBundleWriter.verification(bundle, view);
    String data = Files.readString(bundle.resolve("data.json"));
    assertTrue(data.contains("CouponAcceptanceTest.java"), data);
  }

  @Test
  void nonStageCasesKeepExecutionDataOutOfTheSpecModel() throws Exception {
    ToppleCaseData publicCase = row("order-public", CaseVisibility.PUBLIC, "60", "60");
    SpecView spec = ReportViews.spec(Map.of(), List.of(publicCase), NOW);
    VerificationView verification =
        ReportViews.verification(
            Map.of(),
            List.of(publicCase),
            Map.of(
                "order-public",
                new ReportViews.CaseExecution(CaseResultStatus.PASS, null, List.of())),
            NOW);

    assertFalse(ReportJson.writeSpec(spec).contains("\"steps\""));
    assertTrue(verification.acceptanceConditions().getFirst().cases().getFirst().steps().isEmpty());
    Path specBundle = tempDir.resolve("spec-roundtrip");
    Path verificationBundle = tempDir.resolve("verification-roundtrip");
    HtmlBundleWriter.spec(specBundle, spec);
    HtmlBundleWriter.verification(verificationBundle, verification);
    assertEquals(spec, ReportJson.readSpec(Files.readString(specBundle.resolve("data.json"))));
    assertEquals(
        verification,
        ReportJson.readVerification(Files.readString(verificationBundle.resolve("data.json"))));
  }

  @Test
  void reviewerReviewKeepsExternalNarrativeAndAllCaseRowsWithoutExecutionData() throws Exception {
    ToppleCaseData publicCase = row("coupon-public", CaseVisibility.PUBLIC, "500", "400");
    ToppleCaseData hiddenCase =
        row("coupon-hidden-800", CaseVisibility.HIDDEN, "800", "700-secret-value");
    Map<String, List<SpecMarkdownBlock>> narrative =
        Map.of(
            "AC-CART-COUPON",
            List.of(
                new SpecMarkdownBlock(
                    SpecMarkdownBlock.Kind.PARAGRAPH,
                    0,
                    "Public `<script>window.injected = true</script>` text with `inline code`.",
                    List.of())));

    ReviewView review =
        ReportViews.review(
            Map.of("AC-CART-COUPON", "Apply coupon"),
            List.of(publicCase, hiddenCase),
            narrative,
            Map.of(
                "AC-CART-COUPON",
                new ReviewMethod(
                    List.of("準備 cart.subtotal() 元", "驗證折抵結果"),
                    "void appliesCoupon(ToppleCase c) { given.a_cart(c.input(\"cart\","
                        + " Cart.class)); }")),
            NOW);
    Path bundle = tempDir.resolve("review");
    HtmlBundleWriter.review(bundle, review);
    String html = Files.readString(bundle.resolve("index.html"));

    String data = Files.readString(bundle.resolve("data.json"));
    assertTrue(html.contains("topplecat-report-data"));
    assertTrue(data.contains("window.injected = true"));
    assertFalse(html.contains("<script>window.injected = true</script>"));
    assertTrue(data.contains("coupon-hidden-800"));
    assertTrue(data.contains("700-secret-value"));
    assertTrue(data.indexOf("coupon-public") < data.indexOf("coupon-hidden-800"), data);
    assertTrue(html.contains("assets/report.css"));
    assertFalse(html.contains(">PASS<"));
    assertFalse(html.contains(">FAIL<"));
    assertEquals(review, ReportJson.readReview(Files.readString(bundle.resolve("data.json"))));
  }

  @Test
  void reviewerReviewResolvesEachCaseFromCompilerBindingsWithoutInterpretingJava()
      throws Exception {
    ToppleCaseData publicCase =
        new ToppleCaseData(
            "public-rich",
            "AC-CART-COUPON",
            CaseVisibility.PUBLIC,
            JSON.readTree(
                """
                {"cart":{"customer":"customer-public","amount":500,"lines":[{"sku":"BOOK","quantity":2}]},
                 "enabled":true,"optional":null}
                """),
            JSON.readTree("{\"receipt\":{\"discount\":100,\"accepted\":true}}"),
            Path.of("public.json"));
    ToppleCaseData hiddenCase =
        new ToppleCaseData(
            "hidden-rich",
            "AC-CART-COUPON",
            CaseVisibility.HIDDEN,
            JSON.readTree(
                """
                {"cart":{"customer":"customer-hidden","amount":800,"lines":["A","B"]},
                 "enabled":false,"optional":null}
                """),
            JSON.readTree("{\"receipt\":{\"discount\":200,\"accepted\":false}}"),
            Path.of("hidden.json"));
    StepTemplate given =
        template(
            "given",
            StepPhase.GIVEN,
            "prepares ",
            List.of(
                binding(0, "customer", "/inputs/cart/customer"),
                    binding(1, "amount", "/inputs/cart/amount"),
                binding(2, "enabled", "/inputs/enabled"),
                    binding(3, "optional", "/inputs/optional"),
                binding(4, "lines", "/inputs/cart/lines"),
                    binding(5, "receipt", "/expected/receipt"),
                binding(6, "unbound", ""), binding(7, "missing", "/inputs/no-such-value")));
    StepTemplate then =
        template(
            "then",
            StepPhase.THEN,
            "expects discount ",
            List.of(binding(0, "discount", "/expected/receipt/discount")));

    ReviewView review =
        ReportViews.review(
            Map.of("AC-CART-COUPON", "Coupon"),
            List.of(hiddenCase, publicCase),
            Map.of(),
            Map.of("AC-CART-COUPON", new ReviewMethod(List.of("Given fallback"), "")),
            Map.of("AC-CART-COUPON", List.of(given, then)),
            NOW);

    assertEquals(ReviewView.SCHEMA_VERSION, review.schemaVersion());
    assertEquals(
        List.of("public-rich", "hidden-rich"),
        review.acceptanceConditions().getFirst().cases().stream().map(ReviewCase::caseId).toList());
    ReviewCase publicRow = review.acceptanceConditions().getFirst().cases().getFirst();
    ReviewCase hiddenRow = review.acceptanceConditions().getFirst().cases().get(1);
    assertEquals(
        "prepares customer-public 500 true null [{\"sku\":\"BOOK\",\"quantity\":2}]"
            + " {\"discount\":100,\"accepted\":true} <unbound> <missing>",
        publicRow.scenario().getFirst().sentence());
    assertEquals("expects discount 100", publicRow.scenario().get(1).sentence());
    assertEquals(
        "prepares customer-hidden 800 false null [\"A\",\"B\"]"
            + " {\"discount\":200,\"accepted\":false} <unbound> <missing>",
        hiddenRow.scenario().getFirst().sentence());
    assertEquals(StepPhase.GIVEN, publicRow.scenario().getFirst().phase());
    assertTrue(
        new ReviewCase(CaseVisibility.PUBLIC, "legacy", publicCase.inputs(), publicCase.expected())
            .scenario()
            .isEmpty());
    String json = ReportJson.writeReview(review);
    assertEquals(review, ReportJson.readReview(json));
    assertThrows(
        RuntimeException.class,
        () ->
            ReportJson.readReview(
                json.replace(ReviewView.SCHEMA_VERSION, "topplecat.review-view.v1")));
  }

  private static StepTemplate template(
      String id, StepPhase phase, String prefix, List<ArgumentBinding> bindings) {
    List<StepToken> tokens = new java.util.ArrayList<>();
    tokens.add(new StepToken(StepTokenKind.PHASE, phase.name()));
    tokens.add(new StepToken(StepTokenKind.LITERAL, prefix));
    for (int index = 0; index < bindings.size(); index++) {
      tokens.add(new StepToken(StepTokenKind.ARGUMENT, String.valueOf(index)));
      if (index + 1 < bindings.size()) {
        tokens.add(new StepToken(StepTokenKind.LITERAL, " "));
      }
    }
    return new StepTemplate(id, phase, tokens, bindings, new SourceRef("ReviewFixture.java", 1, 1));
  }

  private static ArgumentBinding binding(int index, String name, String pointer) {
    return new ArgumentBinding(index, name, pointer);
  }

  private static ToppleCaseData row(
      String id, CaseVisibility visibility, String subtotal, String total) throws Exception {
    return new ToppleCaseData(
        id,
        "AC-CART-COUPON",
        visibility,
        JSON.readTree("{\"cart\":{\"subtotal\":" + subtotal + "}}"),
        JSON.readTree("{\"receipt\":{\"discountedSubtotal\":\"" + total + "\"}}"),
        Path.of(id + ".json"));
  }
}
