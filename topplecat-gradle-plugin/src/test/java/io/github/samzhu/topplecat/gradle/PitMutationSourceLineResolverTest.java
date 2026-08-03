package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.samzhu.topplecat.pitest.PitMutation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PitMutationSourceLineResolverTest {
  @TempDir Path tempDir;

  @Test
  void resolvesOnlyTheUniqueOriginalSourceLine() throws Exception {
    Path sourceRoot = tempDir.resolve("src/main/java");
    Path source = sourceRoot.resolve("example/CouponService.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source, "package example;\npublic final class CouponService {\n  return 1;\n}\n");

    PitMutation mutation = mutation("CouponService.java", "example.CouponService", 3);

    assertEquals(
        "  return 1;",
        PitMutationSourceLineResolver.forDirectories(List.of(sourceRoot)).apply(mutation));
  }

  @Test
  void withholdsSourceContextWhenTheFileCannotBeUniquelyResolved() throws Exception {
    Path first = tempDir.resolve("one/example/CouponService.java");
    Path second = tempDir.resolve("two/example/CouponService.java");
    Files.createDirectories(first.getParent());
    Files.createDirectories(second.getParent());
    Files.writeString(first, "package example;\nclass CouponService {}\n");
    Files.writeString(second, "package example;\nclass CouponService {}\n");

    PitMutation mutation = mutation("CouponService.java", "", 2);

    assertNull(
        PitMutationSourceLineResolver.forDirectories(List.of(first.getParent(), second.getParent()))
            .apply(mutation));
  }

  private static PitMutation mutation(String sourceFile, String mutatedClass, int line) {
    return new PitMutation(
        false,
        "SURVIVED",
        mutatedClass.isBlank() ? "example.OtherService" : mutatedClass,
        sourceFile,
        "calculate",
        "()I",
        line,
        0,
        0,
        "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
        "Replaced integer addition with subtraction",
        List.of(),
        List.of(),
        List.of());
  }
}
