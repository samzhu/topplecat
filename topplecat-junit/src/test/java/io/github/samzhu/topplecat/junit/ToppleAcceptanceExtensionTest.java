package io.github.samzhu.topplecat.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ExpectedConsumptionExecution;
import io.github.samzhu.topplecat.core.NarrativeExecution;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.ScenarioStage;
import io.github.samzhu.topplecat.core.ScenarioTemplate;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import tools.jackson.databind.json.JsonMapper;

class ToppleAcceptanceExtensionTest {
  private static final String FIXTURE_RUN = "topplecat.fixtureRun";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @TempDir Path tempDir;

  @Test
  void failsAOtherwiseSuccessfulTestWhenExpectedValueWasNotVerified() throws Exception {
    Path cases = tempDir.resolve("cases.json");
    Files.writeString(
        cases,
        """
        [{"caseId":"coupon-public-500","acId":"AC-CART-COUPON",
          "inputs":{"subtotal":500},"expected":{"discount":100}}]
        """);
    String previous = System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
    String previousDefinition = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
    Path definition = tempDir.resolve("hollow-definition.json");
    Files.writeString(definition, ContractDefinitionJson.write(hollowDefinition()));
    try {
      System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, cases.toString());
      System.setProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, definition.toString());
      SummaryGeneratingListener summary = new SummaryGeneratingListener();
      LauncherDiscoveryRequest request =
          LauncherDiscoveryRequestBuilder.request()
              .selectors(DiscoverySelectors.selectClass(HollowFixture.class))
              .configurationParameter(FIXTURE_RUN, "true")
              .build();
      Launcher launcher = LauncherFactory.create();
      launcher.execute(request, summary);

      assertEquals(1, summary.getSummary().getTestsFailedCount());
      assertEquals(
          true,
          summary
              .getSummary()
              .getFailures()
              .getFirst()
              .getException()
              .getMessage()
              .contains("expected.discount was declared by AC-CART-COUPON but never verified"));
    } finally {
      if (previous == null) {
        System.clearProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
      } else {
        System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, previous);
      }
      restore(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, previousDefinition);
    }
  }

  @Test
  void failsAReadOnlyExpectedValueWhenEnforcementIsEnabled() throws Exception {
    Path cases = tempDir.resolve("cases.json");
    Files.writeString(
        cases,
        """
        [{"caseId":"coupon-public-500","acId":"AC-CART-COUPON",
          "inputs":{"subtotal":500},"expected":{"discount":100}}]
        """);
    String previous = System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
    String previousDefinition = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
    Path definition = tempDir.resolve("read-only-definition.json");
    Files.writeString(definition, ContractDefinitionJson.write(readOnlyDefinition()));
    try {
      System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, cases.toString());
      System.setProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, definition.toString());
      SummaryGeneratingListener summary = new SummaryGeneratingListener();
      LauncherDiscoveryRequest request =
          LauncherDiscoveryRequestBuilder.request()
              .selectors(DiscoverySelectors.selectClass(ReadOnlyFixture.class))
              .configurationParameter(FIXTURE_RUN, "true")
              .build();
      Launcher launcher = LauncherFactory.create();
      launcher.execute(request, summary);

      assertEquals(1, summary.getSummary().getTestsFailedCount());
      String message = summary.getSummary().getFailures().getFirst().getException().getMessage();
      assertEquals(true, message.contains("expected.discount"));
      assertEquals(true, message.contains("Call c.verify(\"discount\", actual)."));
    } finally {
      if (previous == null) {
        System.clearProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
      } else {
        System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, previous);
      }
      restore(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, previousDefinition);
    }
  }

  @Test
  void resolvesHumanTitlesInDeclaredOrder() throws Exception {
    Method display = TitleFixture.class.getDeclaredMethod("displayTitle");
    Method named = TitleFixture.class.getDeclaredMethod("namedTitle");
    Method derived = TitleFixture.class.getDeclaredMethod("derivedFromMethodName");

    assertEquals("Readable title", ToppleTitleResolver.title(display));
    assertEquals("Fallback title", ToppleTitleResolver.title(named));
    assertEquals("derived From Method Name", ToppleTitleResolver.title(derived));
  }

  @Test
  void resolvesCompilerApprovedScenarioAndStageParametersForTheActualJUnitInvocation()
      throws Exception {
    Path cases = tempDir.resolve("new-scenario-cases.json");
    Files.writeString(
        cases,
        """
        [{"caseId":"new-scenario-public","acId":"AC-NEW-RUNTIME",
          "inputs":{"value":"prepared"},"expected":{"result":"prepared"}}]
        """);
    String previous = System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
    String previousDefinition = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
    String previousNarrative = System.getProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY);
    String previousConsumption =
        System.getProperty(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY);
    String previousAttachments = System.getProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY);
    Path definition = tempDir.resolve("new-scenario-definition.json");
    Path narrativeSidecar = tempDir.resolve("new-scenario-narrative.jsonl");
    Path consumptionSidecar = tempDir.resolve("new-scenario-consumption.jsonl");
    Path attachments = tempDir.resolve("new-scenario-attachments");
    Files.writeString(definition, ContractDefinitionJson.write(newScenarioDefinition()));
    NewScenarioFixture.stageClass = null;
    NewScenarioFixture.observedValue = null;
    try {
      System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, cases.toString());
      System.setProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, definition.toString());
      System.setProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, narrativeSidecar.toString());
      System.setProperty(
          ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY, consumptionSidecar.toString());
      System.setProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY, attachments.toString());
      SummaryGeneratingListener summary = new SummaryGeneratingListener();
      LauncherDiscoveryRequest request =
          LauncherDiscoveryRequestBuilder.request()
              .selectors(DiscoverySelectors.selectClass(NewScenarioFixture.class))
              .configurationParameter(FIXTURE_RUN, "true")
              .build();
      LauncherFactory.create().execute(request, summary);

      assertEquals(
          1,
          summary.getSummary().getTestsSucceededCount(),
          summary.getSummary().getFailures().toString());
      assertEquals("prepared", NewScenarioFixture.observedValue);
      assertNotEquals(NewScenarioFixture.RuntimeStage.class, NewScenarioFixture.stageClass);
      NarrativeExecution narrative =
          JSON.readValue(Files.readString(narrativeSidecar).trim(), NarrativeExecution.class);
      assertEquals(newScenarioDefinition().digest(), narrative.definitionDigest());
      assertEquals(
          List.of(
              NewScenarioFixture.RuntimeStage.class.getName() + "#prepares(Ljava/lang/String;)V",
              NewScenarioFixture.RuntimeStage.class.getName()
                  + "#matches(Lio/github/samzhu/topplecat/junit/ToppleCase;)V"),
          narrative.steps().stream().map(step -> step.stepId()).toList());
      assertEquals(
          List.of(NarrativeStepStatus.PASS, NarrativeStepStatus.PASS),
          narrative.steps().stream().map(step -> step.status()).toList());
      assertEquals(1, narrative.steps().getFirst().attachments().size());
      assertEquals("prepared", narrative.steps().getFirst().attachments().getFirst().title());
      assertEquals(
          "prepared\n",
          Files.readString(
              attachments.resolve(
                  Path.of(narrative.steps().getFirst().attachments().getFirst().relativePath())
                      .getFileName())));
      ExpectedConsumptionExecution consumption =
          JSON.readValue(
              Files.readString(consumptionSidecar).trim(), ExpectedConsumptionExecution.class);
      assertEquals("ASSERTED", consumption.expectedConsumption().get("result"));
    } finally {
      if (previous == null) {
        System.clearProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
      } else {
        System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, previous);
      }
      if (previousDefinition == null) {
        System.clearProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
      } else {
        System.setProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, previousDefinition);
      }
      restore(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, previousNarrative);
      restore(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY, previousConsumption);
      restore(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY, previousAttachments);
    }
  }

  @Test
  void missingScenarioStepsWriteSkippedNarrativeEvidenceBeforeTheInvocationFails()
      throws Exception {
    Path cases = tempDir.resolve("missing-scenario-cases.json");
    Files.writeString(
        cases,
        """
        [{"caseId":"missing-scenario-public","acId":"AC-MISSING-SCENARIO",
          "inputs":{"value":"prepared"},"expected":{"result":"prepared"}}]
        """);
    String previousCases = System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
    String previousDefinition = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
    String previousNarrative = System.getProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY);
    Path definition = tempDir.resolve("missing-scenario-definition.json");
    Path narrativeSidecar = tempDir.resolve("missing-scenario-narrative.jsonl");
    Files.writeString(definition, ContractDefinitionJson.write(missingScenarioDefinition()));
    try {
      System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, cases.toString());
      System.setProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, definition.toString());
      System.setProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, narrativeSidecar.toString());
      SummaryGeneratingListener summary = new SummaryGeneratingListener();
      LauncherDiscoveryRequest request =
          LauncherDiscoveryRequestBuilder.request()
              .selectors(DiscoverySelectors.selectClass(MissingScenarioFixture.class))
              .configurationParameter(FIXTURE_RUN, "true")
              .build();

      LauncherFactory.create().execute(request, summary);

      assertEquals(1, summary.getSummary().getTestsFailedCount());
      assertEquals(
          "The Scenario executed 2 of 3 compiler-described Steps.",
          summary.getSummary().getFailures().getFirst().getException().getMessage());
      NarrativeExecution narrative =
          JSON.readValue(Files.readString(narrativeSidecar).trim(), NarrativeExecution.class);
      assertEquals(
          List.of(NarrativeStepStatus.PASS, NarrativeStepStatus.PASS, NarrativeStepStatus.SKIPPED),
          narrative.steps().stream().map(step -> step.status()).toList());
      assertEquals(
          MissingScenarioFixture.RuntimeStage.class.getName() + "#confirms()V",
          narrative.steps().get(2).stepId());
    } finally {
      restore(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, previousCases);
      restore(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, previousDefinition);
      restore(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY, previousNarrative);
    }
  }

  @Test
  void conflictingScenarioResolverFailsBeforeTheAcceptanceBodyRuns() throws Exception {
    Path cases = tempDir.resolve("resolver-conflict-cases.json");
    Files.writeString(
        cases,
        """
        [{"caseId":"resolver-conflict-public","acId":"AC-RESOLVER-CONFLICT",
          "inputs":{},"expected":{"result":"ok"}}]
        """);
    String previousCases = System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
    String previousDefinition = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
    Path definition = tempDir.resolve("resolver-conflict-definition.json");
    Files.writeString(definition, ContractDefinitionJson.write(resolverConflictDefinition()));
    ResolverConflictFixture.bodyRan = false;
    try {
      System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, cases.toString());
      System.setProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, definition.toString());
      SummaryGeneratingListener summary = new SummaryGeneratingListener();
      LauncherDiscoveryRequest request =
          LauncherDiscoveryRequestBuilder.request()
              .selectors(DiscoverySelectors.selectClass(ResolverConflictFixture.class))
              .configurationParameter(FIXTURE_RUN, "true")
              .build();

      LauncherFactory.create().execute(request, summary);

      assertEquals(1, summary.getSummary().getTestsFailedCount());
      assertEquals(false, ResolverConflictFixture.bodyRan);
      assertEquals(
          true,
          summary
              .getSummary()
              .getFailures()
              .getFirst()
              .getException()
              .getMessage()
              .contains("multiple competing ParameterResolvers"));
    } finally {
      restore(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, previousCases);
      restore(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY, previousDefinition);
    }
  }

  static final class FixtureOnlyCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
      return context
          .getConfigurationParameter(FIXTURE_RUN)
          .filter("true"::equals)
          .map(value -> ConditionEvaluationResult.enabled("launcher fixture"))
          .orElseGet(() -> ConditionEvaluationResult.disabled("nested fixture"));
    }
  }

  @ExtendWith(FixtureOnlyCondition.class)
  static final class HollowFixture {
    @ToppleAcceptanceTest("AC-CART-COUPON")
    void acceptsTheCaseButNeverVerifiesIt(
        ToppleCase ignored, ToppleScenario scenario, HollowStage stage) {
      scenario.then(stage).leaves_the_contract_unverified(ignored);
    }

    static class HollowStage extends ToppleStage {
      void leaves_the_contract_unverified(ToppleCase ignored) {}
    }
  }

  @ExtendWith(FixtureOnlyCondition.class)
  static final class ReadOnlyFixture {
    @ToppleAcceptanceTest("AC-CART-COUPON")
    void readsExpectedButNeverVerifiesIt(ToppleCase c, ToppleScenario scenario, ReadStage stage) {
      scenario.then(stage).reads_the_expected_value(c);
    }

    static class ReadStage extends ToppleStage {
      void reads_the_expected_value(ToppleCase c) {
        c.expected("discount", Integer.class);
      }
    }
  }

  @ExtendWith(FixtureOnlyCondition.class)
  static final class NewScenarioFixture {
    static Class<?> stageClass;
    static String observedValue;

    @ToppleAcceptanceTest("AC-NEW-RUNTIME")
    void executesOneCompilerDescribedScenario(
        ToppleCase c, ToppleScenario scenario, RuntimeStage stage) {
      scenario.given(stage).prepares(c.input("value", String.class));
      scenario.then(stage).matches(c);
    }

    static class RuntimeStage extends ToppleStage {
      private String value;

      void prepares(String value) {
        this.value = value;
        stageClass = getClass();
        step().attach(ToppleAttachment.text("prepared", value + System.lineSeparator()));
      }

      void matches(ToppleCase c) {
        observedValue = value;
        c.verify("result", value);
      }
    }
  }

  @ExtendWith(FixtureOnlyCondition.class)
  static final class MissingScenarioFixture {
    @ToppleAcceptanceTest("AC-MISSING-SCENARIO")
    void executesOnlyTheDescriptorPrefix(
        ToppleCase c, ToppleScenario scenario, RuntimeStage stage) {
      scenario.given(stage).prepares(c.input("value", String.class));
      scenario.when(stage).matches(c);
    }

    static class RuntimeStage extends ToppleStage {
      private String value;

      void prepares(String value) {
        this.value = value;
      }

      void matches(ToppleCase c) {
        c.verify("result", value);
      }

      void confirms() {}
    }
  }

  @ExtendWith({FixtureOnlyCondition.class, CompetingScenarioResolver.class})
  static final class ResolverConflictFixture {
    static boolean bodyRan;

    @ToppleAcceptanceTest("AC-RESOLVER-CONFLICT")
    void mustNotRun(ToppleCase ignored, ToppleScenario scenario, RuntimeStage stage) {
      bodyRan = true;
    }

    static class RuntimeStage extends ToppleStage {
      void prepares() {}
    }
  }

  static final class CompetingScenarioResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
      return parameter.getParameter().getType() == ToppleScenario.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
      throw new AssertionError("JUnit should reject the competing resolver before resolution.");
    }
  }

  static final class TitleFixture {
    @Test
    @DisplayName("Readable title")
    void displayTitle() {}

    @Test
    @DisplayName("Fallback title")
    void namedTitle() {}

    @Test
    void derivedFromMethodName() {}
  }

  private static ContractDefinition newScenarioDefinition() {
    Method method;
    try {
      method =
          NewScenarioFixture.class.getDeclaredMethod(
              "executesOneCompilerDescribedScenario",
              ToppleCase.class,
              ToppleScenario.class,
              NewScenarioFixture.RuntimeStage.class);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
    String identity =
        method.getDeclaringClass().getName()
            + "#"
            + method.getName()
            + MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                .descriptorString();
    String stage = NewScenarioFixture.RuntimeStage.class.getName();
    ScenarioTemplate scenario =
        new ScenarioTemplate(
            "AC-NEW-RUNTIME|" + identity,
            identity,
            new SourceRef("ToppleAcceptanceExtensionTest.java", 1, 1),
            List.of(
                new StepTemplate(
                    stage + "#prepares(Ljava/lang/String;)V",
                    StepPhase.GIVEN,
                    List.of(new StepToken(StepTokenKind.PHASE, "GIVEN")),
                    List.of(),
                    new SourceRef("ToppleAcceptanceExtensionTest.java", 1, 1),
                    stage),
                new StepTemplate(
                    stage + "#matches(Lio/github/samzhu/topplecat/junit/ToppleCase;)V",
                    StepPhase.THEN,
                    List.of(new StepToken(StepTokenKind.PHASE, "THEN")),
                    List.of(),
                    new SourceRef("ToppleAcceptanceExtensionTest.java", 1, 1),
                    stage)),
            1,
            List.of(new ScenarioStage(2, stage)));
    return ContractDefinition.withComputedDigest(
        List.of(new AcceptanceContract("AC-NEW-RUNTIME", "New runtime", scenario, List.of())));
  }

  private static ContractDefinition hollowDefinition() {
    return oneStepDefinition(
        HollowFixture.class,
        "acceptsTheCaseButNeverVerifiesIt",
        HollowFixture.HollowStage.class,
        "leaves_the_contract_unverified(Lio/github/samzhu/topplecat/junit/ToppleCase;)V");
  }

  private static ContractDefinition readOnlyDefinition() {
    return oneStepDefinition(
        ReadOnlyFixture.class,
        "readsExpectedButNeverVerifiesIt",
        ReadOnlyFixture.ReadStage.class,
        "reads_the_expected_value(Lio/github/samzhu/topplecat/junit/ToppleCase;)V");
  }

  private static ContractDefinition oneStepDefinition(
      Class<?> fixture,
      String methodName,
      Class<? extends ToppleStage> stageType,
      String stageMethod) {
    try {
      Method method =
          fixture.getDeclaredMethod(methodName, ToppleCase.class, ToppleScenario.class, stageType);
      String identity = methodIdentity(method);
      String stage = stageType.getName();
      ScenarioTemplate scenario =
          new ScenarioTemplate(
              "AC-CART-COUPON|" + identity,
              identity,
              new SourceRef("ToppleAcceptanceExtensionTest.java", 1, 1),
              List.of(step(stage, stageMethod, StepPhase.THEN)),
              1,
              List.of(new ScenarioStage(2, stage)));
      return ContractDefinition.withComputedDigest(
          List.of(
              new AcceptanceContract(
                  "AC-CART-COUPON", "Expected consumption", scenario, List.of())));
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }

  private static ContractDefinition missingScenarioDefinition() {
    Method method;
    try {
      method =
          MissingScenarioFixture.class.getDeclaredMethod(
              "executesOnlyTheDescriptorPrefix",
              ToppleCase.class,
              ToppleScenario.class,
              MissingScenarioFixture.RuntimeStage.class);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
    String identity = methodIdentity(method);
    String stage = MissingScenarioFixture.RuntimeStage.class.getName();
    ScenarioTemplate scenario =
        new ScenarioTemplate(
            "AC-MISSING-SCENARIO|" + identity,
            identity,
            new SourceRef("ToppleAcceptanceExtensionTest.java", 1, 1),
            List.of(
                step(stage, "prepares(Ljava/lang/String;)V", StepPhase.GIVEN),
                step(
                    stage,
                    "matches(Lio/github/samzhu/topplecat/junit/ToppleCase;)V",
                    StepPhase.WHEN),
                step(stage, "confirms()V", StepPhase.THEN)),
            1,
            List.of(new ScenarioStage(2, stage)));
    return ContractDefinition.withComputedDigest(
        List.of(
            new AcceptanceContract(
                "AC-MISSING-SCENARIO", "Missing Scenario", scenario, List.of())));
  }

  private static ContractDefinition resolverConflictDefinition() {
    Method method;
    try {
      method =
          ResolverConflictFixture.class.getDeclaredMethod(
              "mustNotRun",
              ToppleCase.class,
              ToppleScenario.class,
              ResolverConflictFixture.RuntimeStage.class);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
    String identity = methodIdentity(method);
    String stage = ResolverConflictFixture.RuntimeStage.class.getName();
    ScenarioTemplate scenario =
        new ScenarioTemplate(
            "AC-RESOLVER-CONFLICT|" + identity,
            identity,
            new SourceRef("ToppleAcceptanceExtensionTest.java", 1, 1),
            List.of(step(stage, "prepares()V", StepPhase.GIVEN)),
            1,
            List.of(new ScenarioStage(2, stage)));
    return ContractDefinition.withComputedDigest(
        List.of(
            new AcceptanceContract(
                "AC-RESOLVER-CONFLICT", "Resolver conflict", scenario, List.of())));
  }

  private static String methodIdentity(Method method) {
    return method.getDeclaringClass().getName()
        + "#"
        + method.getName()
        + MethodType.methodType(method.getReturnType(), method.getParameterTypes())
            .descriptorString();
  }

  private static StepTemplate step(String stage, String methodDescriptor, StepPhase phase) {
    String methodName = methodDescriptor.substring(0, methodDescriptor.indexOf('('));
    return new StepTemplate(
        stage + "#" + methodDescriptor,
        phase,
        List.of(
            new StepToken(StepTokenKind.PHASE, phase.name()),
            new StepToken(StepTokenKind.LITERAL, methodName)),
        List.of(),
        new SourceRef("ToppleAcceptanceExtensionTest.java", 1, 1),
        stage);
  }

  private static void restore(String property, String value) {
    if (value == null) {
      System.clearProperty(property);
    } else {
      System.setProperty(property, value);
    }
  }
}
