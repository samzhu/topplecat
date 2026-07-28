package io.github.samzhu.topplecat.junit;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/** Publishes AC metadata and enforces expected-value consumption after each invocation. */
public final class ToppleAcExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, InvocationInterceptor,
        ExecutionCondition {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ToppleAcExtension.class);
    private static final String INVOCATION_KEY = "invocation";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!ToppleJunit.shouldFilterContractTests()) {
            return ConditionEvaluationResult.enabled("ToppleCat contract filtering is not enabled for this test task.");
        }
        return ToppleAnnotations.find(context)
                .filter(binding -> !ToppleJunit.hiddenAcceptanceConditionSelected(binding.acId()))
                .map(binding -> ConditionEvaluationResult.disabled("AC is outside the selected ToppleCat hidden scope."))
                .orElseGet(() -> ConditionEvaluationResult.enabled("AC is inside the selected ToppleCat hidden scope."));
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        ToppleAnnotations.find(context).ifPresent(binding -> {
            context.publishReportEntry(Map.of(ToppleJunit.AC_ID_ENTRY, binding.acId()));
            ToppleJunit.recordReviewerJavaExecution(binding.acId());
        });
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        CaseInvocation invocation = context.getStore(NAMESPACE).remove(INVOCATION_KEY, CaseInvocation.class);
        if (invocation == null) {
            return;
        }
        ToppleCase testCase = invocation.testCase();
        ToppleNarrative.Session narrative = invocation.narrative();
        Map<String, String> entries = new LinkedHashMap<>();
        testCase.expectedConsumption().forEach((key, state) ->
                entries.put(ToppleJunit.EXPECTED_CONSUMPTION_ENTRY_PREFIX + key, state.name().toLowerCase()));
        if (!entries.isEmpty()) {
            context.publishReportEntry(entries);
        }
        Throwable failure = context.getExecutionException().orElse(null);
        if (failure == null && expectedConsumptionEnforced()) {
            String key = testCase.firstUnassertedExpectedKey();
            if (key != null) {
                failure = new AssertionError("Topple case " + testCase.caseId() + " expected." + key
                        + " was declared by " + testCase.acId() + " but never verified. "
                        + "Call c.verify(\"" + key + "\", actual).");
            }
        }
        if (failure == null && narrative.hasTerminalFailure()) {
            failure = new AssertionError("A ToppleStage step failed, but the test completed without propagating the failure.");
        }
        if (failure == null) {
            failure = narrative.parityFailure();
        }
        narrative.finish(failure, testCase.expectedConsumption());
        if (failure != null && context.getExecutionException().isEmpty()) {
            throwAsUnchecked(failure);
        }
    }

    private static boolean expectedConsumptionEnforced() {
        return Boolean.parseBoolean(System.getProperty(ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY, "true"));
    }

    @Override
    public void interceptTestTemplateMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext context
    ) throws Throwable {
        capture(invocationContext, context);
        invocation.proceed();
    }

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext context
    ) throws Throwable {
        capture(invocationContext, context);
        invocation.proceed();
    }

    private static void capture(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context) {
        invocationContext.getArguments().stream().filter(ToppleCase.class::isInstance).map(ToppleCase.class::cast)
                .findFirst().ifPresent(testCase -> {
                    ToppleNarrative.Session narrative = ToppleNarrative.start(testCase);
                    testCase.bindNarrative(narrative);
                    context.getStore(NAMESPACE).put(INVOCATION_KEY, new CaseInvocation(testCase, narrative));
                    narrative.injectStages(context.getRequiredTestInstance());
                });
    }

    private record CaseInvocation(ToppleCase testCase, ToppleNarrative.Session narrative) {
    }

    private static void throwAsUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(failure);
    }
}
