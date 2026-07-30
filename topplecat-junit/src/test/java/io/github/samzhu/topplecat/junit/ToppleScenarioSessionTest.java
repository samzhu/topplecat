package io.github.samzhu.topplecat.junit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.ArgumentBinding;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.NarrativeExecution;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.ScenarioStage;
import io.github.samzhu.topplecat.core.ScenarioTemplate;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;
import tools.jackson.databind.json.JsonMapper;

/** Production regressions for the accepted Scenario proxy state machine. */
class ToppleScenarioSessionTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @TempDir Path tempDir;

  @Test
  void selectedStepConsumesOneDescriptorEntryAndNestedCallsStayTransparent() {
    ToppleScenarioSession session = session(StepPhase.WHEN, CheckoutStage.class, "charges");
    CheckoutStage stage = session.stage(2, CheckoutStage.class);

    session.scenario().when(stage).charges();

    assertEquals(1, stage.chargesCalls);
    assertEquals(1, stage.helperCalls);
    assertEquals(1, session.nextStep());
    assertEquals(ToppleScenarioSession.State.READY, session.state());
    assertTrue(session.isClean());
    assertEquals(1, session.invocations().size());
    assertEquals("PASS", session.invocations().getFirst().status());
    assertEquals(1, session.invocations().getFirst().attachmentCount());
    assertDoesNotThrow(session::finish);
    session.close();
  }

  @Test
  void unarmedAndMismatchedTopLevelCallsFailBeforeTheirBodiesRun() {
    ToppleScenarioSession unarmed = session(StepPhase.WHEN, CheckoutStage.class, "charges");
    CheckoutStage direct = unarmed.stage(2, CheckoutStage.class);

    assertThrows(ToppleCatException.class, direct::charges);
    assertEquals(0, direct.chargesCalls);
    assertEquals(ToppleScenarioSession.State.FAILED, unarmed.state());
    assertTrue(unarmed.isClean());
    unarmed.close();

    ToppleScenarioSession wrongPhase = session(StepPhase.WHEN, CheckoutStage.class, "charges");
    CheckoutStage selected = wrongPhase.stage(2, CheckoutStage.class);

    assertThrows(ToppleCatException.class, () -> wrongPhase.scenario().given(selected).charges());
    assertEquals(0, selected.chargesCalls);
    assertEquals(ToppleScenarioSession.State.FAILED, wrongPhase.state());
    assertTrue(wrongPhase.isClean());
    wrongPhase.close();
  }

  @Test
  void caughtNestedThrowableDoesNotPoisonTheSelectedStepButEscapedFailuresPropagateUnchanged() {
    ToppleScenarioSession caughtSession =
        session(StepPhase.WHEN, CheckoutStage.class, "catches_helper");
    CheckoutStage caught = caughtSession.stage(2, CheckoutStage.class);
    caught.throwFromHelper = true;

    assertDoesNotThrow(() -> caughtSession.scenario().when(caught).catches_helper());
    assertEquals(1, caughtSession.invocations().size());
    assertEquals("PASS", caughtSession.invocations().getFirst().status());
    assertEquals(ToppleScenarioSession.State.READY, caughtSession.state());
    caughtSession.close();

    AssertionError original = new AssertionError("original failure");
    ToppleScenarioSession failureSession = session(StepPhase.THEN, FailureStage.class, "fails");
    FailureStage failure = failureSession.stage(2, FailureStage.class);
    failure.throwable = original;

    AssertionError thrown =
        assertThrows(AssertionError.class, () -> failureSession.scenario().then(failure).fails());
    assertSame(original, thrown);
    assertEquals("FAIL", failureSession.invocations().getFirst().status());
    assertEquals(ToppleScenarioSession.State.FAILED, failureSession.state());
    assertTrue(failureSession.isClean());
    failureSession.close();
  }

  @Test
  void rejectsAnAcceptanceMethodThatCatchesAnEscapedStepFailure() {
    AssertionError original = new AssertionError("original failure");
    ToppleScenarioSession session = session(StepPhase.THEN, FailureStage.class, "fails");
    FailureStage stage = session.stage(2, FailureStage.class);
    stage.throwable = original;

    assertThrows(AssertionError.class, () -> session.scenario().then(stage).fails());

    AssertionError failure = assertThrows(AssertionError.class, session::finish);
    assertEquals(
        "A ToppleScenario Step failed, but the acceptance method completed without propagating the"
            + " failure.",
        failure.getMessage());
    session.close();
  }

  @Test
  void rowsReceiveFreshProxiesWhileTheGeneratedProxyClassIsReused() {
    ToppleScenarioSession firstSession = session(StepPhase.WHEN, CheckoutStage.class, "charges");
    CheckoutStage first = firstSession.stage(2, CheckoutStage.class);
    ToppleScenarioSession secondSession = session(StepPhase.WHEN, CheckoutStage.class, "charges");
    CheckoutStage second = secondSession.stage(2, CheckoutStage.class);

    assertFalse(first == second);
    assertSame(first.getClass(), second.getClass());
    firstSession.close();
    secondSession.close();
  }

  @Test
  void supportsAndForTheNextCompilerDescribedStepOnTheSameCapabilityStage() {
    ToppleScenarioSession session =
        session(
            List.of(
                step(CheckoutStage.class, StepPhase.WHEN, "charges"),
                step(CheckoutStage.class, StepPhase.AND, "confirms")),
            List.of(CheckoutStage.class));
    CheckoutStage stage = session.stage(2, CheckoutStage.class);

    session.scenario().when(stage).charges();
    session.scenario().and(stage).confirms();

    assertEquals(1, stage.chargesCalls);
    assertEquals(1, stage.confirmationsCalls);
    assertEquals(2, session.nextStep());
    assertDoesNotThrow(session::finish);
    session.close();
  }

  @Test
  void oneScenarioCoordinatesTwoCapabilityStages() {
    ToppleScenarioSession session =
        session(
            List.of(
                step(CheckoutStage.class, StepPhase.GIVEN, "prepares"),
                step(PaymentStage.class, StepPhase.WHEN, "authorizes"),
                step(PaymentStage.class, StepPhase.THEN, "matches")),
            List.of(CheckoutStage.class, PaymentStage.class));
    CheckoutStage checkout = session.stage(2, CheckoutStage.class);
    PaymentStage payment = session.stage(3, PaymentStage.class);

    session.scenario().given(checkout).prepares();
    session.scenario().when(payment).authorizes();
    session.scenario().then(payment).matches();

    assertEquals(1, checkout.preparesCalls);
    assertEquals(1, payment.authorizationsCalls);
    assertEquals(1, payment.matchesCalls);
    assertDoesNotThrow(session::finish);
    session.close();
  }

  @Test
  void rejectsAStageProxyFromAnotherScenarioSessionBeforeItsBodyRuns() {
    ToppleScenarioSession first = session(StepPhase.WHEN, CheckoutStage.class, "charges");
    CheckoutStage firstStage = first.stage(2, CheckoutStage.class);
    ToppleScenarioSession second = session(StepPhase.WHEN, CheckoutStage.class, "charges");

    assertThrows(ToppleCatException.class, () -> second.scenario().when(firstStage));
    assertEquals(0, firstStage.chargesCalls);
    assertEquals(ToppleScenarioSession.State.FAILED, second.state());
    assertTrue(second.isClean());
    first.close();
    second.close();
  }

  @Test
  void boundCaseRejectsAForeignThreadBeforeItsFirstStepBodyRuns() throws InterruptedException {
    ToppleScenarioSession session = session(StepPhase.WHEN, CheckoutStage.class, "charges");
    CheckoutStage stage = session.stage(2, CheckoutStage.class);
    session.bindCase(caseData("foreign-thread"));
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread foreign =
        new Thread(
            () -> {
              try {
                session.scenario().when(stage).charges();
              } catch (Throwable throwable) {
                failure.set(throwable);
              }
            });

    foreign.start();
    foreign.join();

    assertEquals(ToppleCatException.class, failure.get().getClass());
    assertEquals(0, stage.chargesCalls);
    assertEquals(ToppleScenarioSession.State.FAILED, session.state());
    session.close();
  }

  @Test
  void toppleStepCannotBeUsedOutsideASelectedStep() {
    CheckoutStage unbound = new CheckoutStage();

    assertThrows(ToppleCatException.class, unbound::attaches_without_selected_step);
  }

  @Test
  void missingAndExtraStepsWriteCompleteNarrativeEvidence() throws Exception {
    Path missingSidecar = tempDir.resolve("missing-narrative.jsonl");
    String previousNarrative = System.getProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY);
    try {
      System.setProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, missingSidecar.toString());
      ToppleScenarioSession missing =
          session(
              List.of(
                  step(CheckoutStage.class, StepPhase.GIVEN, "prepares"),
                  step(CheckoutStage.class, StepPhase.WHEN, "confirms"),
                  step(CheckoutStage.class, StepPhase.THEN, "catches_helper")),
              List.of(CheckoutStage.class));
      CheckoutStage stage = missing.stage(2, CheckoutStage.class);
      missing.bindCase(caseData("missing-step"));

      missing.scenario().given(stage).prepares();
      missing.scenario().when(stage).confirms();

      AssertionError missingFailure = assertThrows(AssertionError.class, missing::finish);
      assertEquals(
          "The Scenario executed 2 of 3 compiler-described Steps.", missingFailure.getMessage());
      assertEquals(ToppleScenarioSession.State.FAILED, missing.state());
      NarrativeExecution missingNarrative =
          JSON.readValue(Files.readString(missingSidecar).trim(), NarrativeExecution.class);
      assertEquals(
          List.of(NarrativeStepStatus.PASS, NarrativeStepStatus.PASS, NarrativeStepStatus.SKIPPED),
          missingNarrative.steps().stream().map(step -> step.status()).toList());
      missing.close();

      Path extraSidecar = tempDir.resolve("extra-narrative.jsonl");
      System.setProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, extraSidecar.toString());
      ToppleScenarioSession extra = session(StepPhase.WHEN, CheckoutStage.class, "confirms");
      CheckoutStage extraStage = extra.stage(2, CheckoutStage.class);
      extra.bindCase(caseData("extra-step"));

      extra.scenario().when(extraStage).confirms();
      assertThrows(ToppleCatException.class, () -> extra.scenario().and(extraStage).prepares());
      assertThrows(AssertionError.class, extra::finish);
      NarrativeExecution extraNarrative =
          JSON.readValue(Files.readString(extraSidecar).trim(), NarrativeExecution.class);
      assertEquals(1, extraNarrative.steps().size());
      assertEquals(NarrativeStepStatus.PASS, extraNarrative.steps().getFirst().status());
      extra.close();
    } finally {
      restore(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, previousNarrative);
    }
  }

  @Test
  void abortedAndFailedRowsDoNotLeakStateIntoTheNextRow() {
    ToppleScenarioSession aborted = session(StepPhase.THEN, FailureStage.class, "fails");
    FailureStage abortedStage = aborted.stage(2, FailureStage.class);
    TestAbortedException abort = new TestAbortedException("not applicable");
    abortedStage.throwable = abort;

    assertSame(
        abort,
        assertThrows(
            TestAbortedException.class, () -> aborted.scenario().then(abortedStage).fails()));
    assertDoesNotThrow(() -> aborted.finishWithEvidence(abort, Map.of()));
    aborted.close();

    ToppleScenarioSession failed = session(StepPhase.THEN, FailureStage.class, "fails");
    FailureStage failedStage = failed.stage(2, FailureStage.class);
    AssertionError failure = new AssertionError("failed row");
    failedStage.throwable = failure;

    assertSame(
        failure,
        assertThrows(AssertionError.class, () -> failed.scenario().then(failedStage).fails()));
    assertDoesNotThrow(() -> failed.finishWithEvidence(failure, Map.of()));
    failed.close();

    ToppleScenarioSession next = session(StepPhase.WHEN, CheckoutStage.class, "charges");
    CheckoutStage nextStage = next.stage(2, CheckoutStage.class);
    next.scenario().when(nextStage).charges();

    assertEquals(1, nextStage.chargesCalls);
    assertDoesNotThrow(next::finish);
    next.close();
  }

  private static ToppleScenarioSession session(
      StepPhase phase, Class<? extends ToppleStage> stage, String methodName) {
    return session(List.of(step(stage, phase, methodName)), List.of(stage));
  }

  private static ToppleScenarioSession session(
      List<StepTemplate> steps, List<? extends Class<? extends ToppleStage>> stages) {
    List<ScenarioStage> parameters =
        java.util.stream.IntStream.range(0, stages.size())
            .mapToObj(index -> new ScenarioStage(index + 2, stages.get(index).getName()))
            .toList();
    ScenarioTemplate scenario =
        new ScenarioTemplate(
            "AC-RUNTIME|fixture#method(LToppleCase;)V",
            "fixture#method(LToppleCase;)V",
            new SourceRef("ToppleScenarioSessionTest.java", 1, 1),
            steps,
            1,
            parameters);
    return new ToppleScenarioSession(scenario, new StageProxyFactory());
  }

  private static StepTemplate step(
      Class<? extends ToppleStage> stage,
      StepPhase phase,
      String methodName,
      Class<?>... parameters) {
    Method method = method(stage, methodName, parameters);
    String descriptor =
        MethodType.methodType(method.getReturnType(), method.getParameterTypes())
            .descriptorString();
    return new StepTemplate(
        stage.getName() + "#" + methodName + descriptor,
        phase,
        List.of(
            new StepToken(StepTokenKind.PHASE, phase.name()),
            new StepToken(StepTokenKind.LITERAL, methodName)),
        List.<ArgumentBinding>of(),
        new SourceRef("ToppleScenarioSessionTest.java", 1, 1),
        stage.getName());
  }

  private static Method method(Class<?> type, String name, Class<?>... parameters) {
    try {
      return type.getDeclaredMethod(name, parameters);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }

  private static ToppleCase caseData(String caseId) {
    return new ToppleCase(
        new ToppleCaseData(
            caseId,
            "AC-RUNTIME",
            CaseVisibility.PUBLIC,
            JSON.readTree("{}"),
            JSON.readTree("{\"result\":\"ok\"}"),
            Path.of("ToppleScenarioSessionTest.json")));
  }

  private static void restore(String property, String value) {
    if (value == null) {
      System.clearProperty(property);
    } else {
      System.setProperty(property, value);
    }
  }

  static class CheckoutStage extends ToppleStage {
    int chargesCalls;
    int helperCalls;
    int preparesCalls;
    int confirmationsCalls;
    boolean throwFromHelper;

    void charges() {
      chargesCalls++;
      helper();
      step().attach(ToppleAttachment.text("receipt", "ok"));
    }

    void catches_helper() {
      try {
        helper();
      } catch (IllegalStateException ignored) {
        // The selected top-level Step owns normal Java error handling.
      }
    }

    void prepares() {
      preparesCalls++;
    }

    void confirms() {
      confirmationsCalls++;
    }

    void attaches_without_selected_step() {
      step().attach(ToppleAttachment.text("receipt", "outside"));
    }

    void helper() {
      helperCalls++;
      if (throwFromHelper) {
        throw new IllegalStateException("nested");
      }
    }
  }

  static class PaymentStage extends ToppleStage {
    int authorizationsCalls;
    int matchesCalls;

    void authorizes() {
      authorizationsCalls++;
    }

    void matches() {
      matchesCalls++;
    }
  }

  static class FailureStage extends ToppleStage {
    Throwable throwable;

    void fails() {
      if (throwable instanceof AssertionError error) {
        throw error;
      }
      if (throwable instanceof TestAbortedException aborted) {
        throw aborted;
      }
      throw new AssertionError("test fixture requires a failure");
    }
  }
}
