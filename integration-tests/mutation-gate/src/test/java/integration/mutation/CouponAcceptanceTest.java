package integration.mutation;

import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
import io.github.samzhu.topplecat.junit.ToppleCase;
import io.github.samzhu.topplecat.junit.ToppleScenario;
import io.github.samzhu.topplecat.junit.ToppleStage;

class CouponAcceptanceTest {
  @ToppleAcceptanceTest("AC-MUTATION-GATE")
  void acceptsThePublicCase(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.then(coupon).matches_the_contract(c);
  }

  static class CouponStage extends ToppleStage {
    void matches_the_contract(ToppleCase c) {
      c.verify("discount", c.input("discount", Integer.class));
    }
  }
}
