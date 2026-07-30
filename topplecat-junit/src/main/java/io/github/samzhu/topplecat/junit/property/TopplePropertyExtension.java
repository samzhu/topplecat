package io.github.samzhu.topplecat.junit.property;

import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.PropertyExecutionEvent;
import io.github.samzhu.topplecat.core.PropertyExecutionState;
import io.github.samzhu.topplecat.core.PropertyResult;
import io.github.samzhu.topplecat.junit.ToppleJunit;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/** JUnit lifecycle adapter for deterministic generated trials and current-run event emission. */
public final class TopplePropertyExtension
    implements ParameterResolver,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        ExecutionCondition {
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(TopplePropertyExtension.class);
  private static final String SESSION = "session";

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    if (!ToppleJunit.shouldFilterAcceptanceTests()) {
      return ConditionEvaluationResult.enabled(
          "ToppleCat Property filtering is not enabled for this test task.");
    }
    ToppleProperty property = context.getRequiredTestMethod().getAnnotation(ToppleProperty.class);
    return ToppleJunit.acceptanceConditionSelected(property.value())
        ? ConditionEvaluationResult.enabled(
            "Property is inside the selected ToppleCat formal acceptance scope.")
        : ConditionEvaluationResult.disabled(
            "Property is outside the selected ToppleCat formal acceptance scope.");
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    Method method = context.getRequiredTestMethod();
    ToppleProperty property = method.getAnnotation(ToppleProperty.class);
    Session session = new Session(property, methodIdentity(method), sourceDigest(method));
    context.getStore(NAMESPACE).put(SESSION, session);
    ToppleJunit.recordPropertyEvent(
        new PropertyExecutionEvent(
            PropertyExecutionEvent.SCHEMA_VERSION,
            runId(),
            property.value(),
            session.identity,
            session.sourceDigest,
            PropertyExecutionState.STARTED,
            null));
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == PropertyTrials.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(SESSION, Session.class);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    Session session = context.getStore(NAMESPACE).remove(SESSION, Session.class);
    if (session == null) {
      return;
    }
    PropertyResult terminal = session.terminal(context.getExecutionException().isPresent());
    ToppleJunit.recordPropertyEvent(
        new PropertyExecutionEvent(
            PropertyExecutionEvent.SCHEMA_VERSION,
            runId(),
            session.declaration.value(),
            session.identity,
            session.sourceDigest,
            terminal.state(),
            terminal));
    if (terminal.state() == PropertyExecutionState.COMPLETED_INCOMPLETE
        && context.getExecutionException().isEmpty()) {
      throw new AssertionError(terminal.incompleteReason());
    }
  }

  private static String runId() {
    return System.getProperty("topplecat.property.runId", "unmanaged");
  }

  private static String sourceDigest(Method method) {
    return System.getProperty(
        "topplecat.property.sourceDigest." + methodIdentity(method),
        Hashing.sha256(methodIdentity(method).getBytes(StandardCharsets.UTF_8)));
  }

  private static String methodIdentity(Method method) {
    StringBuilder descriptor = new StringBuilder("(");
    for (Class<?> parameter : method.getParameterTypes()) descriptor.append(descriptor(parameter));
    return method.getDeclaringClass().getName() + "#" + method.getName() + descriptor.append(")V");
  }

  private static String descriptor(Class<?> type) {
    if (type.isArray()) return type.getName().replace('.', '/');
    if (!type.isPrimitive()) return "L" + type.getName().replace('.', '/') + ";";
    return switch (type.getName()) {
      case "boolean" -> "Z";
      case "byte" -> "B";
      case "short" -> "S";
      case "int" -> "I";
      case "long" -> "J";
      case "char" -> "C";
      case "float" -> "F";
      case "double" -> "D";
      case "void" -> "V";
      default -> throw new IllegalArgumentException(type.getName());
    };
  }

  private static final class Session implements PropertyTrials {
    private final ToppleProperty declaration;
    private final String identity;
    private final String sourceDigest;
    private int forAllCalls;
    private PropertyResult result;
    private String invalidReason;

    private Session(ToppleProperty declaration, String identity, String sourceDigest) {
      this.declaration = declaration;
      this.identity = identity;
      this.sourceDigest = sourceDigest;
    }

    @Override
    public <T> PropertyCheck<T> forAll(Generator<T> generator) {
      if (++forAllCalls != 1 || generator == null) {
        invalidReason = "A Property must call forAll(...) exactly once with a generator.";
        throw new IllegalStateException("A Property must call forAll(...) exactly once.");
      }
      return new Check<>(this, generator);
    }

    private PropertyResult incomplete(String reason) {
      return new PropertyResult(
          declaration.value(),
          identity,
          PropertyExecutionState.COMPLETED_INCOMPLETE,
          declaration.tries(),
          0,
          0,
          0,
          0,
          List.of(),
          seed(),
          false,
          null,
          null,
          null,
          0,
          false,
          reason);
    }

    private long seed() {
      String digest =
          Hashing.sha256(
              (PropertyEngine.VERSION
                      + "\n"
                      + identity
                      + "\n"
                      + System.getProperty("topplecat.property.executionContext", "unmanaged"))
                  .getBytes(StandardCharsets.UTF_8));
      return Long.parseUnsignedLong(digest.substring(0, 16), 16);
    }

    private void complete(PropertyResult terminal) {
      result = terminal;
    }

    private PropertyResult terminal(boolean junitFailed) {
      if (invalidReason != null) {
        return incomplete(invalidReason);
      }
      if (result == null) {
        return incomplete(
            forAllCalls == 0
                ? "Property method did not call forAll(...).check(...)."
                : "Property method did not complete exactly one check(...).");
      }
      if (junitFailed && result.state() == PropertyExecutionState.COMPLETED_PASS) {
        return incomplete("Property method failed after a passing check(...).");
      }
      return result;
    }
  }

  private static final class Check<T> implements PropertyCheck<T> {
    private final Session session;
    private final Generator<T> generator;
    private final Map<String, Predicate<? super T>> classifications = new LinkedHashMap<>();
    private final Map<String, Double> coverage = new LinkedHashMap<>();

    private Check(Session session, Generator<T> generator) {
      this.session = session;
      this.generator = generator;
    }

    @Override
    public PropertyCheck<T> classify(String label, Predicate<? super T> predicate) {
      if (label == null
          || label.isBlank()
          || predicate == null
          || classifications.putIfAbsent(label, predicate) != null) {
        throw new IllegalArgumentException(
            "Property classification labels must be non-blank and unique.");
      }
      return this;
    }

    @Override
    public PropertyCheck<T> requireCoverage(String label, double minimumPercent) {
      if (!classifications.containsKey(label)
          || Double.isNaN(minimumPercent)
          || Double.isInfinite(minimumPercent)
          || minimumPercent < 0
          || minimumPercent > 100) {
        throw new IllegalArgumentException(
            "Property coverage requires a known label and a percentage from 0 to 100.");
      }
      coverage.put(label, minimumPercent);
      return this;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void check(Consumer<? super T> assertion) {
      if (assertion == null || session.result != null) {
        session.invalidReason = "A Property must complete exactly one check(...).";
        throw new IllegalStateException("A Property must complete exactly one check(...).");
      }
      List<PropertyEngine.ClassificationRule<?>> rules = new ArrayList<>();
      classifications.forEach(
          (label, predicate) -> rules.add(new PropertyEngine.ClassificationRule(label, predicate)));
      PropertyEngine.Outcome outcome =
          PropertyEngine.execute(
              new PropertyEngine.Config(
                  session.declaration.value(),
                  session.identity,
                  session.declaration.tries(),
                  session.declaration.maxDiscards(),
                  session.declaration.maxShrinks(),
                  session.seed(),
                  System.getProperty("topplecat.property.executionContext", "unmanaged"),
                  System.getProperty("topplecat.property.replay", "")),
              generator,
              rules,
              coverage,
              (Consumer) assertion);
      session.complete(outcome.result());
      if (outcome.failure() != null) {
        if (outcome.failure() instanceof Error error) throw error;
        throw new AssertionError(outcome.failure().getMessage(), outcome.failure());
      }
    }
  }
}
