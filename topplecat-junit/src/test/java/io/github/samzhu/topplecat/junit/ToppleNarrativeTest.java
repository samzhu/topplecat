package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.NarrativeExecution;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.ExpectedConsumptionExecution;
import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ScenarioTemplate;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.opentest4j.TestAbortedException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.lang.invoke.MethodType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToppleNarrativeTest {
    private static final String FIXTURE_RUN = "topplecat.narrativeFixtureRun";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TempDir
    Path tempDir;

    @Test
    void injectsStagesSharesStateAndWritesPassedCaseSteps() throws Exception {
        NarrativeExecution execution = run(StageFixture.class, 500, 500).narrative();

        assertEquals("coupon-public-500", execution.caseId());
        assertEquals(List.of("準備金額為 500 元的購物車", "建立訂單", "驗證訂單結果"),
                execution.steps().stream().map(step -> step.sentence()).toList());
        assertEquals(List.of(NarrativeStepStatus.PASS, NarrativeStepStatus.PASS, NarrativeStepStatus.PASS),
                execution.steps().stream().map(step -> step.status()).toList());
    }

    @Test
    void marksTheCurrentThenStepFailedWhenTheCaseAssertionFails() throws Exception {
        NarrativeExecution execution = run(StageFixture.class, 500, 400).narrative();

        assertEquals(List.of(NarrativeStepStatus.PASS, NarrativeStepStatus.PASS, NarrativeStepStatus.FAIL),
                execution.steps().stream().map(step -> step.status()).toList());
    }

    @Test
    void derivesAnEnglishSentenceFromUnderscoresAndAppendsArguments() throws Exception {
        NarrativeExecution execution = run(FallbackFixture.class, 500, 500).narrative();

        assertEquals(List.of("a cart with subtotal 500", "總額等於 500"),
                execution.steps().stream().map(step -> step.sentence()).toList());
    }

    @Test
    void marksTheStepFailedWhenRequiredStateIsMissing() throws Exception {
        NarrativeExecution execution = run(RequiredStateFixture.class, 500, 500, 1).narrative();

        assertEquals(NarrativeStepStatus.FAIL, execution.steps().getFirst().status());
    }

    @Test
    void skipsAndPreventsALaterStepAfterTheTestCatchesAStageFailure() throws Exception {
        NarrativeExecution execution = run(SkippingFixture.class, 500, 400, 1).narrative();

        assertEquals(List.of(NarrativeStepStatus.FAIL, NarrativeStepStatus.SKIPPED),
                execution.steps().stream().map(step -> step.status()).toList());
    }

    @Test
    void marksAnAbortedStageInvocation() throws Exception {
        NarrativeExecution execution = run(AbortedFixture.class, 500, 500, 0).narrative();

        assertEquals(List.of(NarrativeStepStatus.ABORTED),
                execution.steps().stream().map(step -> step.status()).toList());
    }

    @Test
    void writesAssertedConsumptionWhenVerificationPassesBeforeALaterFailure() throws Exception {
        ExpectedConsumptionExecution consumption = run(VerifyThenFailsFixture.class, 500, 500, 1).consumption();

        assertEquals("coupon-public-500", consumption.caseId());
        assertEquals("ASSERTED", consumption.expectedConsumption().get("total"));
    }

    @Test
    void storesStepAttachmentsAsRedactedContentAddressedReviewerAssets() throws Exception {
        RunResult result = run(AttachmentFixture.class, 500, 500, 1);

        var attachment = result.narrative().steps().getFirst().attachments().getFirst();
        assertEquals("Server log", attachment.title());
        assertEquals("text/plain; charset=utf-8", attachment.mediaType());
        assertEquals(attachment.sha256() + ".txt", Path.of(attachment.relativePath()).getFileName().toString());
        String asset = Files.readString(result.attachments().resolve(Path.of(attachment.relativePath()).getFileName()));
        assertEquals(true, asset.contains("***REDACTED***"));
        assertEquals(false, asset.contains("super-secret"));
        var screenshot = result.narrative().steps().getFirst().attachments().get(1);
        assertEquals("image/png", screenshot.mediaType());
        assertEquals(screenshot.sha256(), result.narrative().steps().getFirst().attachments().get(2).sha256());
        try (var files = Files.list(result.attachments())) {
            assertEquals(2, files.count(), "two identical screenshot attachments must share one content-addressed asset");
        }
    }

    @Test
    void rejectsAttachmentReportsThatWouldExceedTheAggregateLimitWithoutWritingThem() {
        assertEquals(true, ToppleNarrative.attachmentReportCapacityAllows(100L * 1024 * 1024 - 1, 1));
        assertEquals(false, ToppleNarrative.attachmentReportCapacityAllows(100L * 1024 * 1024, 1));
    }

    @Test
    void bindsRuntimeStepOrderAndIdentityToTheCheckedCompilerDefinition() throws Exception {
        Path definition = tempDir.resolve("definition.json");
        ContractDefinition contract = ContractDefinition.withComputedDigest(List.of(new AcceptanceContract("AC-CART-COUPON",
                "Apply coupon", new ScenarioTemplate("AC-CART-COUPON|fixture", "fixture#appliesCoupon", new SourceRef("Fixture.java", 1, 1),
                List.of(step(CartGiven.class, "a_cart", CartGiven.class, Cart.class, StepPhase.GIVEN),
                        step(OrderWhen.class, "creates_order", OrderWhen.class, StepPhase.WHEN),
                        step(OrderThen.class, "matches_contract", OrderThen.class, ToppleCase.class, StepPhase.THEN))), List.of())));
        Files.writeString(definition, ContractDefinitionJson.write(contract));
        String previous = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
        try {
            System.setProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, definition.toString());
            NarrativeExecution execution = run(StageFixture.class, 500, 500).narrative();
            assertEquals(contract.digest(), execution.definitionDigest());
            assertEquals(contract.acceptanceConditions().getFirst().scenario().steps().stream().map(StepTemplate::stepId).toList(),
                    execution.steps().stream().map(step -> step.stepId()).toList());
        } finally {
            restore(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, previous);
        }
    }

    @Test
    void isolatesOneHundredConcurrentCaseAndStepExecutions() throws Exception {
        Path sidecar = tempDir.resolve("parallel-narrative.jsonl");
        Path consumption = tempDir.resolve("parallel-consumption.jsonl");
        Path attachments = tempDir.resolve("parallel-attachments");
        String previousNarrative = System.getProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY);
        String previousConsumption = System.getProperty(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY);
        String previousAttachments = System.getProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY);
        CountDownLatch ready = new CountDownLatch(12);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(12);
        try {
            System.setProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, sidecar.toString());
            System.setProperty(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY, consumption.toString());
            System.setProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY, attachments.toString());
            var futures = IntStream.range(0, 100).mapToObj(value -> workers.submit(() -> {
                ToppleCase testCase = new ToppleCase(new io.github.samzhu.topplecat.core.ToppleCaseData(
                        "parallel-" + value, "AC-PARALLEL", io.github.samzhu.topplecat.core.CaseVisibility.PUBLIC,
                        JSON.readTree("{\"value\":" + value + "}"), JSON.readTree("{\"result\":" + value + "}"),
                        Path.of("parallel.json")));
                ToppleNarrative.Session narrative = ToppleNarrative.start(testCase);
                testCase.bindNarrative(narrative);
                ParallelFixture fixture = new ParallelFixture();
                narrative.injectStages(fixture);
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                fixture.stage.records(value);
                testCase.verify("result", value);
                narrative.finish(null, testCase.expectedConsumption());
                return value;
            })).toList();
            assertEquals(true, ready.await(10, TimeUnit.SECONDS), "the worker pool must reach the concurrent start barrier");
            start.countDown();
            for (var future : futures) {
                future.get(15, TimeUnit.SECONDS);
            }
            workers.shutdown();
            assertEquals(true, workers.awaitTermination(15, TimeUnit.SECONDS));
            List<NarrativeExecution> executions = Files.readAllLines(sidecar).stream()
                    .map(line -> JSON.readValue(line, NarrativeExecution.class)).toList();
            assertEquals(100, executions.size());
            assertEquals(100, executions.stream().map(NarrativeExecution::caseId).distinct().count());
            for (NarrativeExecution execution : executions) {
                int value = Integer.parseInt(execution.caseId().substring("parallel-".length()));
                assertEquals(1, execution.steps().size(), execution.caseId());
                assertEquals(value, execution.steps().getFirst().actualArguments().getFirst().intValue(), execution.caseId());
            }
        } finally {
            start.countDown();
            workers.shutdownNow();
            restore(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, previousNarrative);
            restore(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY, previousConsumption);
            restore(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY, previousAttachments);
        }
    }

    private static StepTemplate step(Class<?> owner, String name, Class<?> returnType, Class<?> parameter, StepPhase phase) {
        String descriptor = MethodType.methodType(returnType, parameter).descriptorString();
        return new StepTemplate(owner.getName() + "#" + name + descriptor, phase,
                List.of(new StepToken(StepTokenKind.PHASE, phase.name()), new StepToken(StepTokenKind.LITERAL, name)),
                List.of(), new SourceRef("Fixture.java", 1, 1));
    }

    private static StepTemplate step(Class<?> owner, String name, Class<?> returnType, StepPhase phase) {
        String descriptor = MethodType.methodType(returnType).descriptorString();
        return new StepTemplate(owner.getName() + "#" + name + descriptor, phase,
                List.of(new StepToken(StepTokenKind.PHASE, phase.name()), new StepToken(StepTokenKind.LITERAL, name)),
                List.of(), new SourceRef("Fixture.java", 1, 1));
    }

    private RunResult run(Class<?> fixture, int subtotal, int expectedTotal) throws Exception {
        return run(fixture, subtotal, expectedTotal, expectedTotal == subtotal ? 0 : 1);
    }

    private RunResult run(Class<?> fixture, int subtotal, int expectedTotal, int expectedFailures) throws Exception {
        Path cases = tempDir.resolve("cases-" + subtotal + "-" + expectedTotal + ".json");
        Path sidecar = tempDir.resolve("narrative-" + subtotal + "-" + expectedTotal + ".jsonl");
        Path consumptionSidecar = tempDir.resolve("consumption-" + subtotal + "-" + expectedTotal + ".jsonl");
        Path attachments = tempDir.resolve("attachments-" + subtotal + "-" + expectedTotal);
        Files.writeString(cases, """
                [{"caseId":"coupon-public-500","acId":"AC-CART-COUPON",
                  "inputs":{"subtotal":%d},"expected":{"total":%d}}]
                """.formatted(subtotal, expectedTotal));
        String previousCases = System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
        String previousSidecar = System.getProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY);
        String previousConsumptionSidecar = System.getProperty(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY);
        String previousAttachments = System.getProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY);
        try {
            System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, cases.toString());
            System.setProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, sidecar.toString());
            System.setProperty(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY, consumptionSidecar.toString());
            System.setProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY, attachments.toString());
            SummaryGeneratingListener summary = new SummaryGeneratingListener();
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(fixture))
                    .configurationParameter(FIXTURE_RUN, "true")
                    .build();
            Launcher launcher = LauncherFactory.create();
            launcher.execute(request, summary);
            assertEquals(expectedFailures, summary.getSummary().getTestsFailedCount());
            NarrativeExecution narrative = Files.isRegularFile(sidecar)
                    ? JSON.readValue(Files.readString(sidecar).trim(), NarrativeExecution.class) : null;
            ExpectedConsumptionExecution consumption = JSON.readValue(Files.readString(consumptionSidecar).trim(),
                    ExpectedConsumptionExecution.class);
            return new RunResult(narrative, consumption, attachments);
        } finally {
            restore(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, previousCases);
            restore(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, previousSidecar);
            restore(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY, previousConsumptionSidecar);
            restore(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY, previousAttachments);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    static final class FixtureOnlyCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return context.getConfigurationParameter(FIXTURE_RUN).filter("true"::equals)
                    .map(value -> ConditionEvaluationResult.enabled("launcher fixture"))
                    .orElseGet(() -> ConditionEvaluationResult.disabled("nested fixture"));
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class StageFixture {
        @ToppleStageField
        CartGiven given;
        @ToppleStageField
        OrderWhen when;
        @ToppleStageField
        OrderThen then;

        @ToppleTest("AC-CART-COUPON")
        void appliesCoupon(ToppleCase testCase) {
            given.a_cart(new Cart(testCase.input("subtotal", Integer.class)));
            when.creates_order();
            then.matches_contract(testCase);
        }
    }

    static final class CartGiven extends ToppleStage<CartGiven> {
        @ProvidedState
        Cart cart;

        @As("準備金額為 {0} 元的購物車")
        CartGiven a_cart(Cart cart) {
            recorded(cart.subtotal());
            this.cart = cart;
            return self();
        }
    }

    static final class OrderWhen extends ToppleStage<OrderWhen> {
        @ExpectedState(required = true)
        Cart cart;
        @ProvidedState
        Integer total;

        @As("建立訂單")
        OrderWhen creates_order() {
            recorded();
            total = cart.subtotal();
            return self();
        }
    }

    static final class OrderThen extends ToppleStage<OrderThen> {
        @ExpectedState(required = true)
        Integer total;

        @As("驗證訂單結果")
        OrderThen matches_contract(ToppleCase testCase) {
            recorded();
            testCase.verify("total", total);
            return self();
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class FallbackFixture {
        @ToppleStageField
        FallbackStage stage;

        @ToppleTest("AC-CART-COUPON")
        void derivesSentence(ToppleCase testCase) {
            stage.a_cart_with_subtotal(testCase.input("subtotal", Integer.class));
            stage.the_total_matches(testCase);
        }
    }

    static final class FallbackStage extends ToppleStage<FallbackStage> {
        Integer subtotal;

        FallbackStage a_cart_with_subtotal(Integer subtotal) {
            recorded(subtotal);
            this.subtotal = subtotal;
            return self();
        }

        @As("總額等於")
        FallbackStage the_total_matches(ToppleCase testCase) {
            recorded(subtotal);
            testCase.verify("total", subtotal);
            return self();
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class RequiredStateFixture {
        @ToppleStageField
        RequiredStateStage stage;

        @ToppleTest("AC-CART-COUPON")
        void requiresState(ToppleCase ignored) {
            stage.requires_a_cart();
        }
    }

    static final class RequiredStateStage extends ToppleStage<RequiredStateStage> {
        @ExpectedState(required = true)
        Cart cart;

        @As("需要購物車狀態")
        RequiredStateStage requires_a_cart() {
            recorded();
            return self();
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class SkippingFixture {
        @ToppleStageField
        SkippingStage stage;

        @ToppleTest("AC-CART-COUPON")
        void skipsLaterStageAfterFailure(ToppleCase testCase) {
            try {
                stage.fails_the_contract(testCase);
            } catch (AssertionError ignored) {
                // The next stage call proves the terminal narrative lifecycle.
            }
            stage.must_not_run();
        }
    }

    static final class SkippingStage extends ToppleStage<SkippingStage> {
        @As("驗證失敗的結果")
        SkippingStage fails_the_contract(ToppleCase testCase) {
            recorded();
            testCase.verify("total", 500);
            return self();
        }

        @As("這個步驟不應執行")
        SkippingStage must_not_run() {
            recorded();
            throw new AssertionError("Skipped stage body was executed.");
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class AbortedFixture {
        @ToppleStageField AbortedStage stage;

        @ToppleTest("AC-CART-COUPON")
        void abortsAnInvocation(ToppleCase ignored) {
            stage.cannot_continue();
            stage.must_not_run();
        }
    }

    static final class AbortedStage extends ToppleStage<AbortedStage> {
        @As("中止此案例")
        AbortedStage cannot_continue() {
            recorded();
            throw new TestAbortedException("fixture abort");
        }

        @As("此步驟必須跳過")
        AbortedStage must_not_run() {
            recorded();
            throw new AssertionError("aborted case ran a later step");
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class VerifyThenFailsFixture {
        @ToppleTest("AC-CART-COUPON")
        void verifiesThenFails(ToppleCase testCase) {
            testCase.verify("total", testCase.expected("total", Integer.class));
            throw new AssertionError("later assertion failed");
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class AttachmentFixture {
        @ToppleStageField AttachmentStage stage;

        @ToppleTest("AC-CART-COUPON")
        void recordsAttachment(ToppleCase ignored) {
            stage.records_log();
        }
    }

    static final class AttachmentStage extends ToppleStage<AttachmentStage> {
        private static final byte[] PIXEL = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLJ9QAAAABJRU5ErkJggg==");

        @As("記錄伺服器日誌")
        AttachmentStage records_log() {
            recorded();
            attach(ToppleAttachment.text("Server log", "Authorization: super-secret"));
            attach(ToppleAttachment.png("Checkout screenshot", PIXEL));
            attach(ToppleAttachment.png("Checkout screenshot duplicate", PIXEL));
            return self();
        }
    }

    static final class ParallelFixture {
        @ToppleStageField ParallelStage stage;
    }

    static final class ParallelStage extends ToppleStage<ParallelStage> {
        ParallelStage records(int value) {
            recorded(value);
            return self();
        }
    }

    private record RunResult(NarrativeExecution narrative, ExpectedConsumptionExecution consumption, Path attachments) {
    }

    record Cart(int subtotal) {
    }
}
