package io.github.samzhu.topplecat.pitest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ToppleCatException;
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
        EvidenceVerdict.PASS,
        attribution.assessments().getFirst().verdict(),
        "a raw detected=false flag must not cancel exact killingTests evidence");
    assertEquals(100, attribution.assessments().getFirst().detectionRate());
    assertEquals("UNKNOWN_FUTURE_STATUS", attribution.mutations().getFirst().status());
    assertEquals(false, attribution.mutations().getFirst().detected());
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
    assertEquals(EvidenceVerdict.FAIL, coupon.verdict());
    assertEquals("AC-EMPTY", empty.acId());
    assertEquals(1, empty.coveredMutantCount());
    assertEquals(1, empty.killedByAcceptanceMethodMutantCount());
    assertEquals(EvidenceVerdict.PASS, empty.verdict());
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
                    """
                    <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
                      <coveringTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,long, java.lang.String[])]</coveringTests>
                      <killingTests>shop.OrderAcceptanceTest.[class:shop.OrderAcceptanceTest]/[method:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,long,java.lang.String[])]</killingTests>
                      <succeedingTests></succeedingTests>
                    </mutation></mutations>
                    """),
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

  private static PitMutationAttribution attribute(String xml) {
    return PitMutationAttributor.attribute(
        new PitMutationParser().parse(xml),
        Map.of("AC-COUPON", Set.of(COUPON), "AC-EMPTY", Set.of(EMPTY)),
        100);
  }
}
