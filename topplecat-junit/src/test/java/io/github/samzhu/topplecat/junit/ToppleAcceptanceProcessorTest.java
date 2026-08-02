package io.github.samzhu.topplecat.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.CompilerPropertyDescriptor;
import io.github.samzhu.topplecat.core.CompilerPropertyDescriptorJson;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptorJson;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Regression fixtures compiled through javac and the shipped processor. */
class ToppleAcceptanceProcessorTest {
  @TempDir Path tempDir;

  @Test
  void emitsACompilerOwnedDescriptorForScenarioStagesAndNamedArguments() throws Exception {
    Compilation result =
        compile(
            "fixture/ScenarioFixture.java",
            """
            package fixture;
            import io.github.samzhu.topplecat.junit.*;
            import org.junit.jupiter.api.DisplayName;
            class ScenarioFixture {
              @ToppleAcceptanceTest("AC-EXACT")
              @DisplayName("套用 SAVE100 折抵訂單小計")
              void accepts(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
                scenario.given(coupon).a_cart(c.input("cart", Cart.class));
                scenario.when(coupon).checks_out();
                scenario.then(coupon).matches(c);
              }
              record Cart(String customerId) {}
              static class CouponStage extends ToppleStage {
                @As("準備可結帳的購物車 {cart.customerId}") void a_cart(Cart cart) {}
                void checks_out() {}
                void matches(ToppleCase c) {}
              }
            }
            """);

    assertTrue(result.success(), result.messages());
    Path contracts = result.classes().resolve("META-INF/topplecat/contracts");
    CompilerScenarioDescriptor descriptor =
        CompilerScenarioDescriptorJson.read(
            Files.readString(
                contracts.resolve(Files.readAllLines(contracts.resolve("index")).getFirst())));
    assertEquals("AC-EXACT", descriptor.acId());
    assertEquals("套用 SAVE100 折抵訂單小計", descriptor.title());
    assertEquals(1, descriptor.scenarioParameterIndex());
    assertEquals(2, descriptor.stageParameters().getFirst().parameterIndex());
    assertEquals(
        List.of(StepPhase.GIVEN, StepPhase.WHEN, StepPhase.THEN),
        descriptor.steps().stream().map(StepTemplate::phase).toList());
    assertEquals(
        "fixture.ScenarioFixture$CouponStage#a_cart(Lfixture/ScenarioFixture$Cart;)V",
        descriptor.steps().getFirst().stepId());
    assertEquals(
        "/inputs/cart/customerId",
        descriptor.steps().getFirst().argumentBindings().getFirst().jsonPointer());
    assertEquals("準備可結帳的購物車 ", descriptor.steps().getFirst().tokens().get(1).value());
  }

  @Test
  void rejectsMethodsWithoutTheRequiredScenarioAndStageParameters() throws Exception {
    Compilation result =
        compile(
            "fixture/MissingScenario.java",
            """
            package fixture;
            import io.github.samzhu.topplecat.junit.*;
            class MissingScenario {
              @ToppleAcceptanceTest("AC-MISSING")
              void accepts(ToppleCase c) {}
            }
            """);

    assertFalse(result.success());
    assertTrue(
        result.messages().contains("ToppleScenario must be the one non-generic second parameter"));
    assertTrue(result.messages().contains("requires one or more concrete Stage parameters"));
    assertFalse(Files.exists(result.classes().resolve("META-INF/topplecat/contracts/index")));
  }

  @Test
  void rejectsInvalidPhaseOrderInheritedStepsAndNonVoidSteps() throws Exception {
    Compilation result =
        compile(
            "fixture/InvalidScenario.java",
            """
            package fixture;
            import io.github.samzhu.topplecat.junit.*;
            class InvalidScenario {
              @ToppleAcceptanceTest("AC-INVALID")
              void rejects(ToppleCase c, ToppleScenario scenario, ChildStage child) {
                scenario.given(child).inherited_step();
                scenario.then(child).returns_a_value();
                scenario.when(child).checks_out();
              }
              static class ParentStage extends ToppleStage { void inherited_step() {} }
              static class ChildStage extends ParentStage {
                String returns_a_value() { return "not allowed"; }
                void checks_out() {}
              }
            }
            """);

    assertFalse(result.success());
    assertTrue(
        result
            .messages()
            .contains("must be declared directly on the concrete Stage parameter type"),
        result.messages());
    assertTrue(result.messages().contains("non-private, non-static, non-final void method"));
    assertTrue(result.messages().contains("primary phases cannot move backward"));
  }

  @Test
  void emitsAStableDescriptorForEachValidSupplementaryProperty() throws Exception {
    Compilation result =
        compile(
            "fixture/PropertyFixture.java",
            """
            package fixture;
            import io.github.samzhu.topplecat.junit.*;
            import io.github.samzhu.topplecat.junit.property.*;
            import org.junit.jupiter.api.DisplayName;
            class PropertyFixture {
              @ToppleAcceptanceTest("AC-PROPERTY")
              void examples(ToppleCase c, ToppleScenario scenario, ExampleStage example) {
                scenario.then(example).matches(c);
              }
              @DisplayName("折扣後金額不為負數")
              @ToppleProperty(value = "AC-PROPERTY", tries = 25, maxDiscards = 4, maxShrinks = 3)
              void remainsGeneral(PropertyTrials trial) { }
              static class ExampleStage extends ToppleStage { void matches(ToppleCase c) {} }
            }
            """);

    assertTrue(result.success(), result.messages());
    Path properties = result.classes().resolve("META-INF/topplecat/properties");
    List<String> index = Files.readAllLines(properties.resolve("index"));
    assertEquals(1, index.size());
    CompilerPropertyDescriptor descriptor =
        CompilerPropertyDescriptorJson.read(Files.readString(properties.resolve(index.getFirst())));
    assertEquals("AC-PROPERTY", descriptor.acId());
    assertEquals("折扣後金額不為負數", descriptor.title());
    assertEquals("remainsGeneral", descriptor.methodName());
    assertEquals(25, descriptor.tries());
    assertEquals(4, descriptor.maxDiscards());
    assertEquals(3, descriptor.maxShrinks());
    assertEquals(64, descriptor.sourceDigest().length());
  }

  @Test
  void rejectsInvalidPropertySignaturesAndLimitsWithoutCreatingADescriptor() throws Exception {
    Compilation result =
        compile(
            "fixture/InvalidProperty.java",
            """
            package fixture;
            import io.github.samzhu.topplecat.junit.property.*;
            class InvalidProperty {
              @ToppleProperty(value = "AC-PROPERTY", tries = 0) String invalid(String value) { return value; }
            }
            """);

    assertFalse(result.success());
    assertTrue(result.messages().contains("@ToppleProperty"), result.messages());
    assertFalse(Files.exists(result.classes().resolve("META-INF/topplecat/properties/index")));
  }

  private Compilation compile(String relative, String source) throws Exception {
    return compile(Map.of(relative, source));
  }

  private Compilation compile(Map<String, String> sources) throws Exception {
    Path root = tempDir.resolve("src");
    List<Path> files =
        sources.entrySet().stream()
            .map(
                entry -> {
                  try {
                    Path file = root.resolve(entry.getKey());
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, entry.getValue());
                    return file;
                  } catch (java.io.IOException exception) {
                    throw new IllegalStateException(exception);
                  }
                })
            .toList();
    Path classes = tempDir.resolve("classes-" + Math.abs(sources.hashCode()));
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    try (StandardJavaFileManager manager =
        compiler.getStandardFileManager(diagnostics, null, null)) {
      JavaCompiler.CompilationTask task =
          compiler.getTask(
              null,
              manager,
              diagnostics,
              List.of(
                  "-classpath", System.getProperty("java.class.path"),
                  "-processorpath", System.getProperty("java.class.path"),
                  "-processor", ToppleAcceptanceProcessor.class.getName(),
                  "-d", classes.toString()),
              null,
              manager.getJavaFileObjectsFromPaths(files));
      return new Compilation(
          Boolean.TRUE.equals(task.call()),
          classes,
          diagnostics.getDiagnostics().stream()
              .map(Object::toString)
              .reduce("", (left, right) -> left + "\n" + right));
    }
  }

  private record Compilation(boolean success, Path classes, String messages) {}
}
