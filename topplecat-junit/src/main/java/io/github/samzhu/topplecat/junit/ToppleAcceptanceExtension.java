package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.ScenarioTemplate;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/** Publishes AC metadata and enforces expected-value consumption after each invocation. */
public final class ToppleAcceptanceExtension
    implements BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        InvocationInterceptor,
        ExecutionCondition,
        ParameterResolver {
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(ToppleAcceptanceExtension.class);
  private static final String INVOCATION_KEY = "invocation";
  private static final String NEW_SCENARIO_KEY = "new-scenario";

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    if (!ToppleJunit.shouldFilterAcceptanceTests()) {
      return ConditionEvaluationResult.enabled(
          "ToppleCat acceptance filtering is not enabled for this test task.");
    }
    return ToppleAnnotations.find(context)
        .filter(binding -> !ToppleJunit.acceptanceConditionSelected(binding.acId()))
        .map(
            binding ->
                ConditionEvaluationResult.disabled(
                    "AC is outside the selected ToppleCat formal acceptance scope."))
        .orElseGet(
            () ->
                ConditionEvaluationResult.enabled(
                    "AC is inside the selected ToppleCat formal acceptance scope."));
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    ToppleAnnotations.find(context)
        .ifPresent(
            binding -> {
              context.publishReportEntry(Map.of(ToppleJunit.AC_ID_ENTRY, binding.acId()));
            });
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    CaseInvocation invocation =
        context.getStore(NAMESPACE).remove(INVOCATION_KEY, CaseInvocation.class);
    if (invocation == null) {
      return;
    }
    ToppleCase testCase = invocation.testCase();
    ToppleScenarioSession scenarioSession = invocation.scenarioSession();
    Map<String, String> entries = new LinkedHashMap<>();
    testCase
        .expectedConsumption()
        .forEach(
            (key, state) ->
                entries.put(
                    ToppleJunit.EXPECTED_CONSUMPTION_ENTRY_PREFIX + key,
                    state.name().toLowerCase()));
    if (!entries.isEmpty()) {
      context.publishReportEntry(entries);
    }
    Throwable failure = context.getExecutionException().orElse(null);
    if (failure == null && expectedConsumptionEnforced()) {
      String key = testCase.firstUnassertedExpectedKey();
      if (key != null) {
        failure =
            new AssertionError(
                "Topple case "
                    + testCase.caseId()
                    + " expected."
                    + key
                    + " was declared by "
                    + testCase.acId()
                    + " but never verified. "
                    + "Call c.verify(\""
                    + key
                    + "\", actual).");
      }
    }
    try {
      scenarioSession.finishWithEvidence(failure, testCase.expectedConsumption());
    } catch (Throwable scenarioFailure) {
      if (failure == null) {
        failure = scenarioFailure;
      }
    } finally {
      scenarioSession.close();
      context.getStore(NAMESPACE).remove(NEW_SCENARIO_KEY);
    }
    if (failure != null && context.getExecutionException().isEmpty()) {
      throwAsUnchecked(failure);
    }
  }

  private static boolean expectedConsumptionEnforced() {
    return Boolean.parseBoolean(
        System.getProperty(ToppleJunit.EXPECTED_CONSUMPTION_ENFORCEMENT_PROPERTY, "true"));
  }

  @Override
  public void interceptTestTemplateMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext context)
      throws Throwable {
    capture(invocationContext, context);
    invocation.proceed();
  }

  @Override
  public void interceptTestMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext context)
      throws Throwable {
    capture(invocationContext, context);
    invocation.proceed();
  }

  private static void capture(
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context) {
    invocationContext.getArguments().stream()
        .filter(ToppleCase.class::isInstance)
        .map(ToppleCase.class::cast)
        .findFirst()
        .ifPresent(
            testCase -> {
              NewScenarioInvocation newScenario = newScenario(context);
              if (newScenario == null) {
                throw new ParameterResolutionException(
                    "ToppleCat acceptance methods require a compiler-described Scenario session.");
              }
              newScenario.session().bindCase(testCase);
              context
                  .getStore(NAMESPACE)
                  .put(INVOCATION_KEY, new CaseInvocation(testCase, newScenario.session()));
            });
  }

  @Override
  public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
    Class<?> type = parameter.getParameter().getType();
    if (type != ToppleScenario.class && !ToppleStage.class.isAssignableFrom(type)) {
      return false;
    }
    NewScenarioInvocation invocation = newScenario(context);
    if (invocation == null) {
      return false;
    }
    int index = parameter.getIndex();
    if (type == ToppleScenario.class) {
      return index == invocation.descriptor().scenarioParameterIndex();
    }
    return invocation.descriptor().stageParameters().stream()
        .anyMatch(
            stage ->
                stage.parameterIndex() == index && stage.stageBinaryName().equals(type.getName()));
  }

  @Override
  public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
    NewScenarioInvocation invocation = newScenario(context);
    if (invocation == null) {
      throw new ParameterResolutionException(
          "ToppleCat cannot resolve a Scenario parameter without a compiler-approved descriptor.");
    }
    if (parameter.getParameter().getType() == ToppleScenario.class) {
      return invocation.session().scenario();
    }
    Class<? extends ToppleStage> stageType =
        parameter.getParameter().getType().asSubclass(ToppleStage.class);
    return invocation.session().stage(parameter.getIndex(), stageType);
  }

  private static NewScenarioInvocation newScenario(ExtensionContext context) {
    NewScenarioInvocation existing =
        context.getStore(NAMESPACE).get(NEW_SCENARIO_KEY, NewScenarioInvocation.class);
    if (existing != null) {
      return existing;
    }
    return ToppleAnnotations.find(context)
        .map(
            binding -> {
              ScenarioTemplate descriptor =
                  ToppleScenarioDescriptors.find(context.getRequiredTestMethod(), binding.acId());
              NewScenarioInvocation created =
                  new NewScenarioInvocation(
                      descriptor, new ToppleScenarioSession(descriptor, new StageProxyFactory()));
              context.getStore(NAMESPACE).put(NEW_SCENARIO_KEY, created);
              return created;
            })
        .orElse(null);
  }

  private record CaseInvocation(ToppleCase testCase, ToppleScenarioSession scenarioSession) {}

  private static final class NewScenarioInvocation implements AutoCloseable {
    private final ScenarioTemplate descriptor;
    private final ToppleScenarioSession session;

    private NewScenarioInvocation(ScenarioTemplate descriptor, ToppleScenarioSession session) {
      this.descriptor = descriptor;
      this.session = session;
    }

    ScenarioTemplate descriptor() {
      return descriptor;
    }

    ToppleScenarioSession session() {
      return session;
    }

    @Override
    public void close() {
      session.close();
    }
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
