package sample.cartorders;

import io.github.samzhu.topplecat.junit.As;
import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
import io.github.samzhu.topplecat.junit.ToppleCase;
import io.github.samzhu.topplecat.junit.ToppleScenario;
import io.github.samzhu.topplecat.junit.ToppleStage;
import io.github.samzhu.topplecat.junit.property.Generators;
import io.github.samzhu.topplecat.junit.property.PropertyTrials;
import io.github.samzhu.topplecat.junit.property.ToppleProperty;
import org.junit.jupiter.api.DisplayName;

class CouponAcceptanceTest {
  @ToppleProperty("AC-CART-COUPON")
  @DisplayName("SAVE100 applies a fixed 100 discount to every payable cart")
  void save100HasTheApprovedFixedDiscountForAPayableCart(PropertyTrials trial) {
    trial
        .forAll(Generators.integers(100, 2_000))
        .classify("shipping-boundary", subtotal -> subtotal >= 1_000)
        .requireCoverage("shipping-boundary", 1.0)
        .check(
            subtotal -> {
              Cart cart =
                  new Cart(
                      "property",
                      java.util.List.of(new CartLine("property", 1, subtotal)),
                      subtotal,
                      "SAVE100");
              org.junit.jupiter.api.Assertions.assertEquals(
                  100, new OrderService().createOrder(cart).discount());
            });
  }

  @ToppleAcceptanceTest("AC-CART-COUPON")
  @DisplayName("SAVE100 reduces the order subtotal")
  void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
  }

  @ToppleAcceptanceTest("AC-CART-NO-COUPON")
  @DisplayName("An order without a coupon keeps its original subtotal")
  void createsOrderWithoutCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
  }

  static class CouponStage extends ToppleStage {
    private final OrderService orders = new OrderService();
    private Cart cart;
    private OrderReceipt receipt;

    @As("Prepare a payable cart")
    void a_payable_cart(Cart cart) {
      this.cart = cart;
    }

    @As("Check out the cart")
    void checks_out() {
      receipt = orders.createOrder(cart);
    }

    @As("Show the discount and discounted subtotal on the receipt")
    void receipt_shows_discount_and_discounted_subtotal(ToppleCase c) {
      c.verify("receipt", receipt);
    }
  }
}
