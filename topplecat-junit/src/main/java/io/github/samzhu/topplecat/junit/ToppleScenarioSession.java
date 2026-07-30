package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.ScenarioStage;
import io.github.samzhu.topplecat.core.ScenarioTemplate;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.opentest4j.TestAbortedException;

/**
 * Internal, per-row execution guard for compiler-described new-style Scenarios.
 *
 * <p>The descriptor, rather than a runtime method call, determines every accepted phase, Stage, and
 * canonical Step identity. This type intentionally has no report model or global ThreadLocal.
 */
final class ToppleScenarioSession implements ToppleScenario.Selector, AutoCloseable {
  enum State {
    READY,
    ARMED,
    ACTIVE,
    FAILED,
    CLOSED
  }

  record Invocation(String stepId, StepPhase phase, String status, int attachmentCount) {}

  private final ScenarioTemplate descriptor;
  private final ToppleScenario scenario = new ToppleScenario();
  private final StageProxyFactory factory;
  private final Map<Integer, ToppleStage> stagesByPosition = new LinkedHashMap<>();
  private final Map<Object, StageProxyFactory.Binding> bindings = new IdentityHashMap<>();
  private final List<Invocation> invocations = new ArrayList<>();
  private State state = State.READY;
  private Selector selector;
  private Active active;
  private ToppleNarrative.Session narrative;
  private Thread invocationThread;
  private int nextStep;

  ToppleScenarioSession(ScenarioTemplate descriptor, StageProxyFactory factory) {
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    this.factory = Objects.requireNonNull(factory, "factory");
    if (descriptor.scenarioParameterIndex() < 0 || descriptor.stageParameters().isEmpty()) {
      throw new ToppleCatException(
          "ToppleScenarioSession requires a compiler descriptor for new Scenario authoring.");
    }
    scenario.bind(this);
  }

  ToppleScenario scenario() {
    return scenario;
  }

  <T extends ToppleStage> T stage(int parameterIndex, Class<T> stageType) {
    ensureOpen();
    ScenarioStage expected =
        descriptor.stageParameters().stream()
            .filter(stage -> stage.parameterIndex() == parameterIndex)
            .findFirst()
            .orElseThrow(
                () ->
                    new ToppleCatException(
                        "The compiler descriptor does not declare a Stage at parameter position "
                            + parameterIndex
                            + "."));
    if (!expected.stageBinaryName().equals(stageType.getName())) {
      throw new ToppleCatException(
          "The compiler descriptor expects Stage "
              + expected.stageBinaryName()
              + " at parameter position "
              + parameterIndex
              + ", not "
              + stageType.getName()
              + ".");
    }
    @SuppressWarnings("unchecked")
    T existing = (T) stagesByPosition.get(parameterIndex);
    if (existing != null) {
      return existing;
    }
    T proxy = factory.create(stageType, this);
    stagesByPosition.put(parameterIndex, proxy);
    return proxy;
  }

  @Override
  public synchronized <T extends ToppleStage> T select(StepPhase phase, T stage) {
    ensureInvocationThread();
    ensureOpen();
    if (state != State.READY || selector != null || active != null) {
      fail("A phase selector requires a Ready Scenario session.");
    }
    StageProxyFactory.Binding binding = bindings.get(stage);
    if (binding == null || binding.session() != this) {
      fail("A Scenario selector requires a Stage proxy from the same acceptance invocation.");
    }
    selector = new Selector(phase, binding);
    state = State.ARMED;
    return stage;
  }

  synchronized Object intercept(
      StageProxyFactory.Binding binding, Method origin, Object[] arguments, Callable<?> superCall)
      throws Throwable {
    ensureInvocationThread();
    ensureOpen();
    if (state == State.ACTIVE) {
      return superCall.call();
    }
    if (state != State.ARMED || selector == null) {
      fail("A top-level Stage method requires one armed Scenario phase selector.");
    }
    if (nextStep >= descriptor.steps().size()) {
      fail("The Scenario has no remaining compiler-described Step to consume.");
    }
    StepTemplate expected = descriptor.steps().get(nextStep);
    String canonical = canonicalStepId(binding.authorStage(), origin);
    if (selector.binding() != binding
        || selector.phase() != expected.phase()
        || !expected.stageBinaryName().equals(binding.authorStage().getName())
        || !expected.stepId().equals(canonical)) {
      fail(
          "The armed selector does not match the next compiler-described phase, Stage, and Step"
              + " identity.");
    }

    selector = null;
    active = new Active(binding, new ArrayList<>());
    state = State.ACTIVE;
    try {
      if (narrative != null) {
        narrative.beginScenarioStep(canonical, arguments);
      }
      Object result = superCall.call();
      invocations.add(
          new Invocation(canonical, expected.phase(), "PASS", active.attachments().size()));
      if (narrative != null) {
        narrative.finishScenarioStep(null);
      }
      nextStep++;
      clearTo(State.READY);
      return result;
    } catch (Throwable throwable) {
      invocations.add(
          new Invocation(
              canonical,
              expected.phase(),
              throwable instanceof TestAbortedException ? "ABORT" : "FAIL",
              active.attachments().size()));
      if (narrative != null) {
        narrative.finishScenarioStep(throwable);
      }
      nextStep++;
      clearTo(State.FAILED);
      throw throwable;
    }
  }

  synchronized StageProxyFactory.Binding register(ToppleStage proxy, Class<?> authorStage) {
    ensureOpen();
    StageProxyFactory.Binding binding =
        new StageProxyFactory.Binding(
            this, proxy, Objects.requireNonNull(authorStage, "authorStage"));
    bindings.put(proxy, binding);
    proxy.bindStep(attachment -> attach(binding, attachment));
    return binding;
  }

  private synchronized void attach(StageProxyFactory.Binding binding, ToppleAttachment attachment) {
    ensureInvocationThread();
    if (state != State.ACTIVE || active == null || active.binding() != binding) {
      fail("ToppleStep.attach(...) requires an active compiler-described Step.");
    }
    ToppleAttachment accepted = Objects.requireNonNull(attachment, "attachment");
    active.attachments().add(accepted);
    if (narrative != null) {
      narrative.attach(accepted);
    }
  }

  synchronized void bindCase(ToppleCase testCase) {
    // This runs inside JUnit's invocation interceptor, before user acceptance code starts. Capture
    // that owner now so a first selector/Step dispatched to another thread cannot execute its body.
    ensureOpen();
    ensureInvocationThread();
    if (narrative != null) {
      throw new ToppleCatException("ToppleScenario is already bound to a case invocation.");
    }
    narrative =
        ToppleNarrative.startScenario(Objects.requireNonNull(testCase, "testCase"), descriptor);
  }

  synchronized State state() {
    return state;
  }

  synchronized int nextStep() {
    return nextStep;
  }

  synchronized List<Invocation> invocations() {
    return List.copyOf(invocations);
  }

  synchronized boolean isClean() {
    return selector == null && active == null;
  }

  synchronized void finish() {
    finishWithEvidence(null, Map.of());
  }

  synchronized void finishWithEvidence(
      Throwable failure, Map<String, ExpectedConsumption> consumption) {
    ensureInvocationThread();
    ensureOpen();
    Throwable completionFailure = failure;
    if (state == State.ARMED || selector != null || active != null) {
      clearTo(State.FAILED);
      if (completionFailure == null) {
        completionFailure =
            new ToppleCatException(
                "The Scenario finished with an unconsumed phase selector or active Step.");
      }
    }
    if (completionFailure == null && state == State.FAILED) {
      completionFailure =
          new AssertionError(
              "A ToppleScenario Step failed, but the acceptance method completed without"
                  + " propagating the failure.");
    }
    if (completionFailure == null
        && state == State.READY
        && nextStep != descriptor.steps().size()) {
      clearTo(State.FAILED);
      completionFailure =
          new AssertionError(
              "The Scenario executed "
                  + nextStep
                  + " of "
                  + descriptor.steps().size()
                  + " compiler-described Steps.");
    }
    if (narrative != null) {
      narrative.finishScenario(completionFailure, consumption);
    }
    if (failure == null && completionFailure != null) {
      throwUnchecked(completionFailure);
    }
  }

  @Override
  public synchronized void close() {
    if (state == State.CLOSED) {
      return;
    }
    clearTo(State.CLOSED);
    bindings.keySet().forEach(StageProxyFactory::unbind);
    bindings.keySet().forEach(proxy -> ((ToppleStage) proxy).clearStep());
    bindings.clear();
    stagesByPosition.clear();
    invocationThread = null;
  }

  private String canonicalStepId(Class<?> authorStage, Method origin) {
    try {
      Method method = authorStage.getDeclaredMethod(origin.getName(), origin.getParameterTypes());
      if (method.isBridge()
          || method.isSynthetic()
          || Modifier.isPrivate(method.getModifiers())
          || Modifier.isStatic(method.getModifiers())
          || Modifier.isFinal(method.getModifiers())) {
        throw new NoSuchMethodException(method.toString());
      }
      return authorStage.getName()
          + "#"
          + method.getName()
          + MethodType.methodType(method.getReturnType(), method.getParameterTypes())
              .descriptorString();
    } catch (NoSuchMethodException exception) {
      fail(
          "Runtime proxy method "
              + origin
              + " is not a direct visible Step on concrete Stage "
              + authorStage.getName()
              + ".");
      throw new AssertionError("unreachable", exception);
    }
  }

  private void ensureInvocationThread() {
    Thread current = Thread.currentThread();
    if (invocationThread == null) {
      invocationThread = current;
    } else if (invocationThread != current) {
      fail("A Scenario and its Stage proxies must run on their acceptance invocation thread.");
    }
  }

  private void ensureOpen() {
    if (state == State.CLOSED) {
      throw new ToppleCatException("ToppleScenario session is already closed.");
    }
  }

  private void clearTo(State next) {
    selector = null;
    active = null;
    state = next;
  }

  private void fail(String message) {
    clearTo(State.FAILED);
    throw new ToppleCatException(message);
  }

  private static void throwUnchecked(Throwable throwable) {
    ToppleScenarioSession.<RuntimeException>throwAny(throwable);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void throwAny(Throwable throwable) throws T {
    throw (T) throwable;
  }

  private record Selector(StepPhase phase, StageProxyFactory.Binding binding) {}

  private record Active(StageProxyFactory.Binding binding, List<ToppleAttachment> attachments) {}
}
