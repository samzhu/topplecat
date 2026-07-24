package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptorJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression fixtures compiled through javac and the shipped processor. */
class ToppleScenarioProcessorTest {
    @TempDir
    Path tempDir;

    @Test
    void emitsExactOverloadDescriptorForNestedPackagePrivateStages() throws Exception {
        Compilation result = compile("fixture/ScenarioFixture.java", """
                package fixture;
                import io.github.samzhu.topplecat.junit.*;
                class ScenarioFixture {
                  @ToppleStageField Given given;
                  @ToppleStageField Then then;
                  @ToppleTest("AC-EXACT") void accepts(ToppleCase c) {
                    given.a_cart(c.input("cart", Cart.class));
                    then.matches(c);
                  }
                  record Cart(String customerId) {}
                  static final class Given extends ToppleStage<Given> {
                    @As("準備顧客 {0}") Given a_cart(Cart cart) { recorded(cart.customerId()); return self(); }
                    Given a_cart(String ignored) { recorded(ignored); return self(); }
                  }
                  static final class Then extends ToppleStage<Then> {
                    @As("驗證結果") Then matches(ToppleCase c) { recorded(); return self(); }
                  }
                }
                """);

        assertTrue(result.success(), result.messages());
        Path contracts = result.classes().resolve("META-INF/topplecat/contracts");
        List<String> index = Files.readAllLines(contracts.resolve("index"));
        assertEquals(1, index.size());
        CompilerScenarioDescriptor descriptor = CompilerScenarioDescriptorJson.read(
                Files.readString(contracts.resolve(index.getFirst())));
        assertEquals("AC-EXACT", descriptor.acId());
        assertEquals("(Lfixture/ScenarioFixture$Cart;)Lfixture/ScenarioFixture$Given;",
                descriptor.steps().getFirst().stepId().substring(descriptor.steps().getFirst().stepId().indexOf('(')));
        assertEquals("/inputs/cart/customerId", descriptor.steps().getFirst().argumentBindings().getFirst().jsonPointer());
    }

    @Test
    void rejectsHelperCallsNestedInCanonicalArgumentsWithDomainPositionAndRepair() throws Exception {
        Compilation result = compile("fixture/BadScenario.java", """
                package fixture;
                import io.github.samzhu.topplecat.junit.*;
                class BadScenario {
                  @ToppleStageField Given given;
                  Service service = new Service();
                  @ToppleTest("AC-P1-A") void rejects(ToppleCase c) {
                    given.a_cart(service.decorate(c.input("cart", String.class)));
                  }
                  static final class Service { String decorate(String value) { return value; } }
                  static final class Given extends ToppleStage<Given> {
                    Given a_cart(String cart) { recorded(cart); return self(); }
                  }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.messages().contains("AC AC-P1-A at BadScenario.java:"), result.messages());
        assertTrue(result.messages().contains("helper, SUT, constructor, and unrelated method calls"), result.messages());
        assertTrue(result.messages().contains("Move that execution into a ToppleStage step"), result.messages());
        assertFalse(Files.exists(result.classes().resolve("META-INF/topplecat/contracts/index")));
    }

    @Test
    void rejectsNewAndControlFlowWithoutReadingCommentsOrTextBlocksAsAnnotations() throws Exception {
        Compilation result = compile("fixture/BadSyntax.java", """
                package fixture;
                import io.github.samzhu.topplecat.junit.*;
                class BadSyntax {
                  // @ToppleTest("AC-NOT-REAL")
                  String ignored = \"""
                    @ToppleTest("AC-NOT-REAL")
                    \""";
                  @ToppleStageField Given given;
                  @ToppleTest("AC-NEW") void rejects(ToppleCase c) {
                    if (true) { given.a_cart(new String("cart")); }
                  }
                  static final class Given extends ToppleStage<Given> {
                    Given a_cart(String cart) { recorded(cart); return self(); }
                  }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.messages().contains("AC AC-NEW at BadSyntax.java:"), result.messages());
        assertFalse(result.messages().contains("AC-NOT-REAL at"), result.messages());
    }

    @Test
    void resolvesSameSimpleNameAcrossPackagesAndTheExactOverload() throws Exception {
        Compilation result = compile(Map.of(
                "a/SharedStage.java", """
                        package a;
                        import io.github.samzhu.topplecat.junit.ToppleStage;
                        public final class SharedStage extends ToppleStage<SharedStage> {
                          public SharedStage load(String value) { recorded(value); return self(); }
                        }
                        """,
                "b/Cart.java", "package b; public record Cart(String id) {}",
                "b/SharedStage.java", """
                        package b;
                        import io.github.samzhu.topplecat.junit.*;
                        public final class SharedStage extends ToppleStage<SharedStage> {
                          @As("準備 {0}") public SharedStage load(Cart value) { recorded(value.id()); return self(); }
                          public SharedStage load(String value) { recorded(value); return self(); }
                        }
                        """,
                "fixture/ExactPackages.java", """
                        package fixture;
                        import io.github.samzhu.topplecat.junit.*;
                        class ExactPackages {
                          @ToppleStageField b.SharedStage given;
                          @ToppleTest("AC-P1-B") void usesResolvedOverload(ToppleCase c) {
                            given.load(c.input("cart", b.Cart.class));
                          }
                        }
                        """));

        assertTrue(result.success(), result.messages());
        Path contracts = result.classes().resolve("META-INF/topplecat/contracts");
        CompilerScenarioDescriptor descriptor = CompilerScenarioDescriptorJson.read(
                Files.readString(contracts.resolve(Files.readAllLines(contracts.resolve("index")).getFirst())));
        assertTrue(descriptor.steps().getFirst().stepId().startsWith("b.SharedStage#load(Lb/Cart;)"));
        assertEquals("/inputs/cart/id", descriptor.steps().getFirst().argumentBindings().getFirst().jsonPointer());
    }

    @Test
    void acceptsLiteralsAndRecordOrBeanPropertiesRootedAtCaseData() throws Exception {
        Compilation result = compile("fixture/AllowedArguments.java", """
                package fixture;
                import io.github.samzhu.topplecat.junit.*;
                class AllowedArguments {
                  @ToppleStageField Given given;
                  @ToppleTest("AC-ARGUMENTS") void accepts(ToppleCase c) {
                    given.prepares("fixed", c.input("cart", Cart.class).customerId(), c.expected("receipt", Receipt.class).isAccepted());
                  }
                  record Cart(String customerId) {}
                  static final class Receipt { boolean accepted; boolean isAccepted() { return accepted; } }
                  static final class Given extends ToppleStage<Given> {
                    @As("準備 {0} {1} {2}") Given prepares(String literal, String customer, boolean accepted) {
                      recorded(literal, customer, accepted); return self();
                    }
                  }
                }
                """);

        assertTrue(result.success(), result.messages());
        Path contracts = result.classes().resolve("META-INF/topplecat/contracts");
        CompilerScenarioDescriptor descriptor = CompilerScenarioDescriptorJson.read(
                Files.readString(contracts.resolve(Files.readAllLines(contracts.resolve("index")).getFirst())));
        assertEquals(List.of("", "/inputs/cart/customerId", "/expected/receipt/accepted"), descriptor.steps().getFirst()
                .argumentBindings().stream().map(binding -> binding.jsonPointer()).toList());
    }

    @Test
    void rejectsConstructorCallsInCanonicalArgumentsWithTheDomainRepair() throws Exception {
        Compilation result = compile("fixture/NewArgument.java", """
                package fixture;
                import io.github.samzhu.topplecat.junit.*;
                class NewArgument {
                  @ToppleStageField Given given;
                  @ToppleTest("AC-NEW-ARGUMENT") void rejects(ToppleCase c) { given.a_value(new Service()); }
                  static final class Service {}
                  static final class Given extends ToppleStage<Given> {
                    Given a_value(Service value) { recorded(value); return self(); }
                  }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.messages().contains("AC AC-NEW-ARGUMENT at NewArgument.java:"), result.messages());
        assertTrue(result.messages().contains("allowed case-data grammar"), result.messages());
        assertTrue(result.messages().contains("Use a literal, ToppleCase input/expected accessor"), result.messages());
    }

    @Test
    void rejectsEveryForbiddenCanonicalControlOrLocalForm() throws Exception {
        for (Map.Entry<String, String> fixture : Map.of(
                "local", "String value = c.input(\"value\", String.class);",
                "assignment", "this.value = c.input(\"value\", String.class);",
                "if", "if (true) { given.a_value(c.input(\"value\", String.class)); }",
                "loop", "for (int i = 0; i < 1; i++) { given.a_value(c.input(\"value\", String.class)); }",
                "switch", "switch (1) { default -> given.a_value(c.input(\"value\", String.class)); }",
                "lambda", "Runnable work = () -> given.a_value(c.input(\"value\", String.class));"
        ).entrySet()) {
            Compilation result = compile("fixture/Forbidden" + fixture.getKey() + ".java", """
                    package fixture;
                    import io.github.samzhu.topplecat.junit.*;
                    class Forbidden%s {
                      @ToppleStageField Given given;
                      String value;
                      @ToppleTest("AC-FORBIDDEN-%s") void rejects(ToppleCase c) { %s }
                      static final class Given extends ToppleStage<Given> {
                        Given a_value(String value) { recorded(value); return self(); }
                      }
                    }
                    """.formatted(fixture.getKey(), fixture.getKey().toUpperCase(java.util.Locale.ROOT), fixture.getValue()));
            assertFalse(result.success(), fixture.getKey());
            assertTrue(result.messages().contains("AC AC-FORBIDDEN-" + fixture.getKey().toUpperCase(java.util.Locale.ROOT)
                    + " at Forbidden" + fixture.getKey() + ".java:"), result.messages());
            assertTrue(result.messages().contains("only direct Stage method calls are allowed"), result.messages());
        }
    }

    @Test
    void preservesNativeJavacTypeErrorsAndDoesNotEmitAPartialDescriptor() throws Exception {
        Compilation result = compile("fixture/TypeError.java", """
                package fixture;
                import io.github.samzhu.topplecat.junit.*;
                class TypeError {
                  @ToppleStageField Given given;
                  @ToppleTest("AC-TYPE-ERROR") void rejects(ToppleCase c) { given.a_value(42); }
                  static final class Given extends ToppleStage<Given> {
                    Given a_value(String value) { recorded(value); return self(); }
                  }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.messages().contains("incompatible types"), result.messages());
        assertFalse(Files.exists(result.classes().resolve("META-INF/topplecat/contracts/index")));
    }

    private Compilation compile(String relative, String source) throws Exception {
        return compile(Map.of(relative, source));
    }

    private Compilation compile(Map<String, String> sources) throws Exception {
        Path root = tempDir.resolve("src");
        List<Path> files = sources.entrySet().stream().map(entry -> {
            try {
                Path file = root.resolve(entry.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, entry.getValue());
                return file;
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }).toList();
        Path classes = tempDir.resolve("classes-" + Math.abs(sources.hashCode()));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics, List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-processorpath", System.getProperty("java.class.path"),
                    "-processor", ToppleScenarioProcessor.class.getName(),
                    "-d", classes.toString()), null, manager.getJavaFileObjectsFromPaths(files));
            return new Compilation(Boolean.TRUE.equals(task.call()), classes, diagnostics.getDiagnostics().stream()
                    .map(Object::toString).reduce("", (left, right) -> left + "\n" + right));
        }
    }

    private record Compilation(boolean success, Path classes, String messages) {
    }
}
