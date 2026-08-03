package io.github.samzhu.topplecat.pitest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PitMutationParserTest {
  private static final String COUPON =
      "shop.OrderAcceptanceTest#appliesCoupon("
          + "Lio/github/samzhu/topplecat/junit/ToppleCase;I[Ljava/lang/String;)V";
  private static final String EMPTY =
      "shop.OrderAcceptanceTest#rejectsEmpty(" + "Lio/github/samzhu/topplecat/junit/ToppleCase;)V";
  private static final String COUPON_OVERLOAD =
      "shop.OrderAcceptanceTest#appliesCoupon("
          + "Lio/github/samzhu/topplecat/junit/ToppleCase;J[Ljava/lang/String;)V";

  @Test
  void preservesRawPITOutcomesAndSeparatesCoveringFromExactMethodDetection() {
    PitMutationAttribution attribution =
        attribute(
            """
            <mutations>
              <mutation detected="false" status="UNKNOWN_FUTURE_STATUS"><mutatedClass>shop.OrderService</mutatedClass>
                <coveringTests>shop.OrderAcceptanceTest.[engine:junit-jupiter]/[class:shop.OrderAcceptanceTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase , int,\tjava.lang.String[])]/[test-template-invocation:#1]</coveringTests>
                <killingTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,int,java.lang.String[])]</killingTests>
                <succeedingTests></succeedingTests>
              </mutation>
              <mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
                <coveringTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:rejectsEmpty(io.github.samzhu.topplecat.junit.ToppleCase)]</coveringTests>
                <killingTests></killingTests>
                <succeedingTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:rejectsEmpty(io.github.samzhu.topplecat.junit.ToppleCase)]</succeedingTests>
              </mutation>
              <mutation detected="false" status="NO_COVERAGE"><mutatedClass>shop.OrderService</mutatedClass>
                <killingTests></killingTests><succeedingTests></succeedingTests>
              </mutation>
            </mutations>
            """);

    assertEquals(3, attribution.producerMutationCount());
    assertEquals(2, attribution.uniquelyAttributedMutationCount());
    assertEquals(1, attribution.unattributedMutationCount());
    assertEquals(
        false,
        attribution.assessments().getFirst().attributionGap(),
        "a raw detected=false flag must not cancel exact killingTests evidence");
    assertEquals(100, attribution.assessments().getFirst().detectionRate());
    assertEquals("UNKNOWN_FUTURE_STATUS", attribution.mutations().getFirst().status());
    assertEquals(false, attribution.mutations().getFirst().detected());
    assertEquals(
        "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
        attribution.mutations().getFirst().mutator());
    assertEquals(
        "Replaced integer addition with subtraction",
        attribution.mutations().getFirst().description());
    assertEquals(1, attribution.mutations().get(1).succeedingTests().size());
    assertEquals("NO_COVERAGE", attribution.unattributedOutcomeCounts().getFirst().status());
  }

  @Test
  void givesDetectionCreditOnlyToTheAcceptanceMethodThatKilledTheCoveredMutant() {
    PitMutationAttribution attribution =
        attribute(
            """
            <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
              <coveringTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,int,java.lang.String[])]|shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:rejectsEmpty(io.github.samzhu.topplecat.junit.ToppleCase)]</coveringTests>
              <killingTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:rejectsEmpty(io.github.samzhu.topplecat.junit.ToppleCase)]</killingTests>
              <succeedingTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,int,java.lang.String[])]</succeedingTests>
            </mutation></mutations>
            """);

    PitMutationAssessment coupon = attribution.assessments().getFirst();
    PitMutationAssessment empty = attribution.assessments().get(1);
    assertEquals("AC-COUPON", coupon.acId());
    assertEquals(1, coupon.coveredMutantCount());
    assertEquals(0, coupon.killedByAcceptanceMethodMutantCount());
    assertEquals(false, coupon.attributionGap());
    assertEquals("AC-EMPTY", empty.acId());
    assertEquals(1, empty.coveredMutantCount());
    assertEquals(1, empty.killedByAcceptanceMethodMutantCount());
    assertEquals(false, empty.attributionGap());
    assertEquals(
        List.of("AC-EMPTY"), attribution.mutations().getFirst().detectedAcceptanceConditionIds());
    assertEquals(
        List.of("AC-COUPON", "AC-EMPTY"),
        attribution.mutations().getFirst().attributedAcceptanceConditionIds());
  }

  @Test
  void retainsPitSourceAndBytecodeCoordinatesForMutationsOnTheSameSourceLine() {
    List<PitMutation> mutations =
        new PitMutationParser()
            .parse(
                managedXml(
                    """
                    <mutations>
                      <mutation detected="true" status="KILLED">
                        <sourceFile>OrderService.java</sourceFile><mutatedMethod>calculate</mutatedMethod>
                        <methodDescription>(I)I</methodDescription><lineNumber>42</lineNumber><block>0</block><index>1</index>
                        <mutatedClass>shop.OrderService</mutatedClass>
                        <coveringTests></coveringTests><killingTests></killingTests><succeedingTests></succeedingTests>
                      </mutation>
                      <mutation detected="false" status="SURVIVED">
                        <sourceFile>OrderService.java</sourceFile><mutatedMethod>calculate</mutatedMethod>
                        <methodDescription>(I)I</methodDescription><lineNumber>42</lineNumber><block>0</block><index>2</index>
                        <mutatedClass>shop.OrderService</mutatedClass>
                        <coveringTests></coveringTests><killingTests></killingTests><succeedingTests></succeedingTests>
                      </mutation>
                    </mutations>
                    """))
            .mutations();

    assertEquals(2, mutations.size());
    assertEquals("OrderService.java", mutations.getFirst().sourceFile());
    assertEquals("calculate", mutations.getFirst().mutatedMethod());
    assertEquals("(I)I", mutations.getFirst().methodDescription());
    assertEquals(42, mutations.getFirst().lineNumber());
    assertEquals(0, mutations.getFirst().block());
    assertEquals(1, mutations.getFirst().index());
    assertEquals(2, mutations.get(1).index());
  }

  @Test
  void requiresAnExactStructuredClassAndMethodSignatureForAttribution() {
    PitMutationAttribution attribution =
        attribute(
            """
            <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
              <coveringTests>shop.OrderAcceptanceTest|shop.OrderAcceptanceTest.helperCoverage|other.OrderAcceptanceTest.[class:other.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,int,java.lang.String[])]|shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:wrongName(io.github.samzhu.topplecat.junit.ToppleCase,int,java.lang.String[])]|shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,long,java.lang.String[])]</coveringTests>
              <killingTests></killingTests><succeedingTests></succeedingTests>
            </mutation></mutations>
            """);

    assertEquals(0, attribution.uniquelyAttributedMutationCount());
    assertTrue(
        attribution.assessments().stream().allMatch(result -> result.coveredMutantCount() == 0));
  }

  @Test
  void doesNotFallBackToAClassPrefixWhenTheStructuredClassIsWrong() {
    PitMutationAttribution attribution =
        attribute(
            """
            <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
              <coveringTests>shop.OrderAcceptanceTest.[class:wrong.Test]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,int,java.lang.String[])]|shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTestExtra]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,int,java.lang.String[])]</coveringTests>
              <killingTests></killingTests><succeedingTests></succeedingTests>
            </mutation></mutations>
            """);

    assertEquals(0, attribution.uniquelyAttributedMutationCount());
    assertTrue(
        attribution.assessments().stream().allMatch(result -> result.coveredMutantCount() == 0));
  }

  @Test
  void distinguishesOverloadsWithMultipleParameters() {
    PitMutationAttribution attribution =
        PitMutationAttributor.attribute(
            new PitMutationParser()
                .parse(
                    managedXml(
                        """
                        <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
                          <coveringTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,long, java.lang.String[])]</coveringTests>
                          <killingTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,long,java.lang.String[])]</killingTests>
                          <succeedingTests></succeedingTests>
                        </mutation></mutations>
                        """)),
            Map.of("AC-INT", Set.of(COUPON), "AC-LONG", Set.of(COUPON_OVERLOAD)),
            100);

    assertEquals(0, attribution.assessments().getFirst().coveredMutantCount());
    assertEquals("AC-LONG", attribution.assessments().get(1).acId());
    assertEquals(1, attribution.assessments().get(1).coveredMutantCount());
    assertEquals(1, attribution.assessments().get(1).killedByAcceptanceMethodMutantCount());
  }

  @Test
  void refusesClassOnlyAndMethodNameOnlySelectors() {
    PitMutationAttribution classOnly =
        attribute(
            """
            <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
              <coveringTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]</coveringTests>
              <killingTests></killingTests><succeedingTests></succeedingTests>
            </mutation></mutations>
            """);

    assertEquals(0, classOnly.uniquelyAttributedMutationCount());
    assertThrows(
        ToppleCatException.class,
        () ->
            attribute(
                """
                <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
                  <coveringTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon]</coveringTests>
                  <killingTests></killingTests><succeedingTests></succeedingTests>
                </mutation></mutations>
                """));
  }

  @Test
  void rejectsMalformedSelectorForAnAcceptanceClassAsIncompleteEvidence() {
    assertThrows(
        ToppleCatException.class,
        () ->
            attribute(
                """
                <mutations><mutation detected="false" status="SURVIVED"><mutatedClass>shop.OrderService</mutatedClass>
                  <coveringTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[test-template:appliesCoupon(</coveringTests>
                  <killingTests></killingTests><succeedingTests></succeedingTests>
                </mutation></mutations>
                """));
  }

  @Test
  void rejectsReportsWithoutEveryFullMatrixSelectorGroupExceptNoCoverageCoveringTests() {
    assertThrows(
        ToppleCatException.class,
        () ->
            attribute(
                """
                <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
                  <coveringTests></coveringTests><killingTests></killingTests>
                </mutation></mutations>
                """));
  }

  @Test
  void requiresRawPitMutatorAndDescriptionWithoutNormalizingFutureStatuses() {
    assertThrows(
        ToppleCatException.class,
        () ->
            new PitMutationParser()
                .parse(
                    """
                    <mutations><mutation detected="false" status="FUTURE_STATUS">
                      <description>raw description</description><mutatedClass>shop.OrderService</mutatedClass>
                      <coveringTests></coveringTests><killingTests></killingTests><succeedingTests></succeedingTests>
                    </mutation></mutations>
                    """));
    assertThrows(
        ToppleCatException.class,
        () ->
            new PitMutationParser()
                .parse(
                    """
                    <mutations><mutation detected="false" status="FUTURE_STATUS">
                      <mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>
                      <mutatedClass>shop.OrderService</mutatedClass>
                      <coveringTests></coveringTests><killingTests></killingTests><succeedingTests></succeedingTests>
                    </mutation></mutations>
                    """));
  }

  @Test
  void marksAnAcceptanceMethodWithNoCoverageAsAnAttributionGap() {
    PitMutationAttribution attribution =
        attribute(
            """
            <mutations><mutation detected="false" status="NO_COVERAGE"><mutatedClass>shop.OrderService</mutatedClass>
              <coveringTests></coveringTests><killingTests></killingTests><succeedingTests></succeedingTests>
            </mutation></mutations>
            """);

    assertEquals(0, attribution.uniquelyAttributedMutationCount());
    assertTrue(attribution.assessments().stream().allMatch(PitMutationAssessment::attributionGap));
  }

  @Test
  void retainsRawDescriptionAndSelectorValuesWithoutNormalizingThem() {
    PitMutation mutation =
        new PitMutationParser()
            .parse(
                """
                <mutations><mutation detected="false" status="FUTURE_STATUS">
                  <mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>
                  <description> raw PIT description </description><mutatedClass>shop.OrderService</mutatedClass>
                  <coveringTests> selector one |selector two </coveringTests>
                  <killingTests> selector one </killingTests><succeedingTests>selector two </succeedingTests>
                </mutation></mutations>
                """)
            .mutations()
            .getFirst();

    assertEquals(" raw PIT description ", mutation.description());
    assertEquals(List.of(" selector one ", "selector two "), mutation.coveringTests());
    assertEquals(List.of(" selector one "), mutation.killingTests());
    assertEquals(List.of("selector two "), mutation.succeedingTests());
  }

  private static PitMutationAttribution attribute(String xml) {
    return PitMutationAttributor.attribute(
        new PitMutationParser().parse(managedXml(xml)),
        Map.of("AC-COUPON", Set.of(COUPON), "AC-EMPTY", Set.of(EMPTY)),
        100);
  }

  private static String managedXml(String xml) {
    return xml.replace(
        "<mutatedClass>",
        "<mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>"
            + "<description>Replaced integer addition with subtraction</description>"
            + "<mutatedClass>");
  }
}
