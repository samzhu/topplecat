package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ToppleCatException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PitMutationParserTest {
    @Test
    void attributesMutantsByAcInvocationWhenOneClassContainsSeveralAcceptanceConditions() {
        PitMutationReport report = new PitMutationParser().parse("""
                <mutations>
                  <mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
                    <coveringTests>shop.OrderAcceptanceTest.[engine:junit-jupiter]/[class:shop.OrderAcceptanceTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase)]/[test-template-invocation:#1]</coveringTests></mutation>
                  <mutation detected="false" status="SURVIVED"><mutatedClass>shop.OrderService</mutatedClass>
                    <coveringTests>shop.OrderAcceptanceTest.[engine:junit-jupiter]/[class:shop.OrderAcceptanceTest]/[test-template:rejectsEmpty(io.github.samzhu.topplecat.junit.ToppleCase)]/[test-template-invocation:#1]</coveringTests></mutation>
                </mutations>
                """);

        List<PitMutationAssessment> assessments = PitMutationAttributor.assess(report, Map.of(
                "AC-COUPON", Set.of("shop.OrderAcceptanceTest#appliesCoupon(Lio/github/samzhu/topplecat/junit/ToppleCase;)V"),
                "AC-EMPTY", Set.of("shop.OrderAcceptanceTest#rejectsEmpty(Lio/github/samzhu/topplecat/junit/ToppleCase;)V")), 100);

        assertEquals(2, assessments.size());
        assertEquals(100, assessments.getFirst().mutationScore());
        assertEquals(EvidenceVerdict.PASS, assessments.getFirst().verdict());
        assertEquals(0, assessments.get(1).mutationScore());
        assertEquals(EvidenceVerdict.FAIL, assessments.get(1).verdict());
    }

    @Test
    void doesNotAttributeAnUnmarkedInvocationByClassNameAlone() {
        PitMutationReport report = new PitMutationParser().parse("""
                <mutations>
                  <mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass>
                    <coveringTests>shop.OrderAcceptanceTest.helperCoverage</coveringTests></mutation>
                </mutations>
                """);

        PitMutationAssessment assessment = PitMutationAttributor.assess(report,
                Map.of("AC-COUPON",
                        Set.of("shop.OrderAcceptanceTest#appliesCoupon(Lio/github/samzhu/topplecat/junit/ToppleCase;)V")),
                100).getFirst();

        assertEquals(0, assessment.totalMutations());
        assertEquals(EvidenceVerdict.FAIL, assessment.verdict());
    }

    @Test
    void rejectsReportsWithoutTheFullCoverageMatrix() {
        PitMutationReport report = new PitMutationParser().parse("""
                <mutations><mutation detected="true" status="KILLED"><mutatedClass>shop.OrderService</mutatedClass></mutation></mutations>
                """);

        assertThrows(ToppleCatException.class,
                () -> PitMutationAttributor.assess(report, Map.of("AC-COUPON",
                        Set.of("shop.CouponTest#appliesCoupon(Lio/github/samzhu/topplecat/junit/ToppleCase;)V")), 100));
    }
}
