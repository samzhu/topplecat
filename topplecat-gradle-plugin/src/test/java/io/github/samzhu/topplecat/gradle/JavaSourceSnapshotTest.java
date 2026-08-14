package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JavaSourceSnapshotTest {
  @Test
  void keepsMethodBodyWhenStringsAndCommentsContainBraces() {
    String source =
        JavaSourceSnapshot.capture(
            List.of(
                "    @ToppleAcceptanceTest(\"AC-A\")",
                "    void accepts() {",
                "      String expected = \"} {\"; // } comment",
                "      /* { a comment } */",
                "      assertEquals(expected, \"value\");",
                "    }",
                "    void after() {}"),
            2);

    assertTrue(source.contains("assertEquals(expected, \"value\");"));
    assertTrue(source.trim().endsWith("}"));
    assertTrue(!source.contains("void after()"));
  }

  @Test
  void keepsPropertyBodyAndJavaTextBlockBraces() {
    String source =
        JavaSourceSnapshot.capture(
            List.of(
                "  @ToppleProperty(\"AC-A\")",
                "  void property(PropertyTrials trials) {",
                "    String json = \"\"\"",
                "      {\"expected\": true}",
                "      \"\"\";",
                "    // } must not close the property",
                "    trials.forAll(value -> { });",
                "  }",
                "  void after() {}"),
            2);

    assertTrue(source.contains("{\"expected\": true}"));
    assertTrue(source.contains("trials.forAll(value -> { });"));
    assertTrue(!source.contains("void after()"));
  }
}
