package integration.mutation;

import io.github.samzhu.topplecat.junit.ToppleCase;
import io.github.samzhu.topplecat.junit.ToppleStage;
import io.github.samzhu.topplecat.junit.ToppleStageField;
import io.github.samzhu.topplecat.junit.ToppleTest;

class CouponAcceptanceTest {
    @ToppleStageField
    CouponThen then;

    @ToppleTest("AC-MUTATION-GATE")
    void acceptsThePublicCase(ToppleCase c) {
        then.matches_the_contract(c);
    }

    static final class CouponThen extends ToppleStage<CouponThen> {
        CouponThen matches_the_contract(ToppleCase c) {
            recorded();
            c.verify("discount", c.input("discount", Integer.class));
            return self();
        }
    }
}
