package io.github.samzhu.topplecat.report;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportViewsTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Instant NOW = Instant.parse("2026-07-21T00:00:00Z");

    @TempDir
    Path tempDir;

    @Test
    void specViewKeepsOnlyPublicContractMatrixAfterPublicAndHiddenCasesRun() throws Exception {
        ToppleCaseData publicCase = row("coupon-public", CaseVisibility.PUBLIC, "500", "400");
        ToppleCaseData hiddenCase = row("coupon-hidden-800", CaseVisibility.HIDDEN, "800", "700-secret-value");
        SpecView view = ReportViews.spec(Map.of("AC-CART-COUPON", "Apply coupon"), List.of(publicCase), Map.of(),
                Map.of("AC-CART-COUPON", List.of("Given a public coupon cart", "Then the public receipt matches")), NOW);
        String json = ReportJson.writeSpec(view);
        Path bundle = tempDir.resolve("spec");
        HtmlBundleWriter.spec(bundle, view);
        String html = Files.readString(bundle.resolve("index.html"));
        String script = Files.readString(bundle.resolve("assets/report.js"));

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
        assertTrue(script.contains("renderSpecCases"));
        assertTrue(script.contains("renderReviewCases"));
        assertTrue(script.contains("renderVerificationCases"));
        assertFalse(script.contains("ac.publicCases || ac.cases"));
        assertEquals(view, ReportJson.readSpec(json));

        ToppleCatException error = assertThrows(ToppleCatException.class,
                () -> ReportViews.spec(Map.of(), List.of(publicCase, hiddenCase), NOW));
        assertTrue(error.getMessage().contains("public cases only"));
    }

    @Test
    void verificationViewKeepsReviewerCaseDetailsSeparate() throws Exception {
        ToppleCaseData hiddenCase = row("coupon-hidden-800", CaseVisibility.HIDDEN, "800", "700-secret-value");
        VerificationView view = ReportViews.verification(Map.of(), List.of(hiddenCase),
                Map.of("coupon-hidden-800", new ReportViews.CaseExecution(CaseResultStatus.FAIL, "discount mismatch",
                        List.of(new NarrativeStep("test-step", "驗證折抵後的訂單結果",
                                NarrativeStepStatus.FAIL, 0, List.of(), List.of(), "")))), NOW);

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
        VerificationView view = ReportViews.verification(Map.of(), List.of(publicCase),
                Map.of("coupon-public", new ReportViews.CaseExecution(CaseResultStatus.FAIL, "later assertion failed",
                        List.of(), Map.of("receipt", "ASSERTED"))), NOW);

        assertEquals(CaseResultStatus.FAIL, view.verdict());
        assertEquals("ASSERTED", view.acceptanceConditions().getFirst().cases().getFirst()
                .expectedConsumption().get("receipt"));
    }

    @Test
    void verificationUsesTheGateFailureAsTheDisplayedSuiteVerdict() throws Exception {
        ToppleCaseData publicCase = row("coupon-public", CaseVisibility.PUBLIC, "500", "400");

        VerificationView view = ReportViews.verification(Map.of(), List.of(publicCase),
                Map.of("coupon-public", new ReportViews.CaseExecution(CaseResultStatus.PASS, null, List.of())),
                Map.of(), true, List.of(new EvidenceGate("JUNIT", EvidenceVerdict.FAIL,
                        "the public verification failed.")), NOW);

        assertEquals(CaseResultStatus.FAIL, view.verdict());
    }

    @Test
    void verificationPlacesFailuresFirstAndExposesCompilerStepSourceReferences() throws Exception {
        ToppleCaseData passing = new ToppleCaseData("case-pass", "AC-PASS", CaseVisibility.PUBLIC,
                JSON.readTree("{\"cart\":{\"subtotal\":500}}"), JSON.readTree("{\"receipt\":{\"discountedSubtotal\":\"400\"}}"),
                Path.of("public.json"));
        ToppleCaseData failing = new ToppleCaseData("case-fail", "AC-FAILED", CaseVisibility.HIDDEN,
                JSON.readTree("{\"cart\":{\"subtotal\":800}}"), JSON.readTree("{\"receipt\":{\"discountedSubtotal\":\"700\"}}"),
                Path.of("hidden.yaml"));
        StepTemplate sourceStep = new StepTemplate("example.CouponThen#matches()Lexample/CouponThen;", StepPhase.THEN,
                List.of(new StepToken(StepTokenKind.PHASE, "THEN"), new StepToken(StepTokenKind.LITERAL, "驗證結果")),
                List.of(), new SourceRef("CouponAcceptanceTest.java", 42, 9));
        VerificationView view = ReportViews.verificationFromTemplates(
                Map.of("AC-PASS", "Passing AC", "AC-FAILED", "Failing AC"), List.of(passing, failing),
                Map.of("case-pass", new ReportViews.CaseExecution(CaseResultStatus.PASS, null, List.of()),
                        "case-fail", new ReportViews.CaseExecution(CaseResultStatus.FAIL, "expected mismatch",
                                List.of(new NarrativeStep(sourceStep.stepId(), "驗證結果", NarrativeStepStatus.FAIL,
                                        1, List.of(), List.of(), "case-failure:case-fail")))),
                Map.of(), Map.of("AC-FAILED", List.of(sourceStep)), true, List.of(), NOW);

        assertEquals(List.of("AC-FAILED", "AC-PASS"), view.acceptanceConditions().stream()
                .map(VerificationAcceptanceCondition::acId).toList());
        assertEquals(new SourceRef("CouponAcceptanceTest.java", 42, 9), view.acceptanceConditions().getFirst()
                .stepSources().get(sourceStep.stepId()));
        Path bundle = tempDir.resolve("source-reference-verification");
        HtmlBundleWriter.verification(bundle, view);
        String data = Files.readString(bundle.resolve("data.json"));
        String script = Files.readString(bundle.resolve("assets/report.js"));
        assertTrue(data.contains("CouponAcceptanceTest.java"), data);
        assertTrue(script.contains("Source: ${e(ref.file)}:${e(ref.line)}:${e(ref.column)}"), script);
    }

    @Test
    void nonStageCasesKeepExecutionDataOutOfTheSpecModel() throws Exception {
        ToppleCaseData publicCase = row("order-public", CaseVisibility.PUBLIC, "60", "60");
        SpecView spec = ReportViews.spec(Map.of(), List.of(publicCase), NOW);
        VerificationView verification = ReportViews.verification(Map.of(), List.of(publicCase),
                Map.of("order-public", new ReportViews.CaseExecution(CaseResultStatus.PASS, null, List.of())), NOW);

        assertFalse(ReportJson.writeSpec(spec).contains("\"steps\""));
        assertTrue(verification.acceptanceConditions().getFirst().cases().getFirst().steps().isEmpty());
        Path specBundle = tempDir.resolve("spec-roundtrip");
        Path verificationBundle = tempDir.resolve("verification-roundtrip");
        HtmlBundleWriter.spec(specBundle, spec);
        HtmlBundleWriter.verification(verificationBundle, verification);
        assertEquals(spec, ReportJson.readSpec(Files.readString(specBundle.resolve("data.json"))));
        assertEquals(verification, ReportJson.readVerification(Files.readString(verificationBundle.resolve("data.json"))));
    }

    @Test
    void reviewerReviewKeepsExternalNarrativeAndAllCaseRowsWithoutExecutionData() throws Exception {
        ToppleCaseData publicCase = row("coupon-public", CaseVisibility.PUBLIC, "500", "400");
        ToppleCaseData hiddenCase = row("coupon-hidden-800", CaseVisibility.HIDDEN, "800", "700-secret-value");
        Map<String, List<SpecMarkdownBlock>> narrative = Map.of("AC-CART-COUPON", List.of(
                new SpecMarkdownBlock(SpecMarkdownBlock.Kind.PARAGRAPH, 0,
                        "Public `<script>window.injected = true</script>` text with `inline code`.", List.of())));

        ReviewView review = ReportViews.review(Map.of("AC-CART-COUPON", "Apply coupon"),
                List.of(publicCase, hiddenCase), narrative, Map.of("AC-CART-COUPON",
                        new ReviewMethod(List.of("準備 cart.subtotal() 元", "驗證折抵結果"),
                                "void appliesCoupon(ToppleCase c) { given.a_cart(c.input(\"cart\", Cart.class)); }")), NOW);
        Path bundle = tempDir.resolve("review");
        HtmlBundleWriter.review(bundle, review);
        String html = Files.readString(bundle.resolve("index.html"));

        String data = Files.readString(bundle.resolve("data.json"));
        assertTrue(html.contains("topplecat-report-data"));
        assertTrue(data.contains("window.injected = true"));
        assertFalse(html.contains("<script>window.injected = true</script>"));
        assertTrue(data.contains("coupon-hidden-800"));
        assertTrue(data.contains("700-secret-value"));
        assertTrue(html.contains("assets/report.css"));
        assertFalse(html.contains(">PASS<"));
        assertFalse(html.contains(">FAIL<"));
        assertEquals(review, ReportJson.readReview(Files.readString(bundle.resolve("data.json"))));
    }

    private static ToppleCaseData row(String id, CaseVisibility visibility, String subtotal, String total) throws Exception {
        return new ToppleCaseData(id, "AC-CART-COUPON", visibility,
                JSON.readTree("{\"cart\":{\"subtotal\":" + subtotal + "}}"),
                JSON.readTree("{\"receipt\":{\"discountedSubtotal\":\"" + total + "\"}}"), Path.of(id + ".json"));
    }
}
