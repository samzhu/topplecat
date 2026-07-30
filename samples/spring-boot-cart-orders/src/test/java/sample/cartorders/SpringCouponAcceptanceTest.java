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
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CartOrdersApplication.class)
class SpringCouponAcceptanceTest {
  @ToppleProperty("AC-CART-COUPON")
  void springCouponPropertyUsesTheApprovedFixedDiscount(PropertyTrials trial) {
    trial
        .forAll(Generators.integers(100, 2_000))
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
  @DisplayName("Spring Boot 套用 SAVE100 折抵訂單小計")
  void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
  }

  @ToppleAcceptanceTest("AC-CART-NO-COUPON")
  @DisplayName("Spring Boot 未使用優惠券時維持原始小計")
  void createsOrderWithoutCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
  }

  static class CouponStage extends ToppleStage {
    private final OrderService service = new OrderService();
    private Cart cart;
    private OrderReceipt receipt;

    @As("準備可結帳的購物車")
    void a_payable_cart(Cart cart) {
      this.cart = cart;
    }

    @As("套用優惠券並建立訂單")
    void checks_out() {
      receipt = service.createOrder(cart);
    }

    @As("收據顯示折扣與折扣後小計")
    void receipt_shows_discount_and_discounted_subtotal(ToppleCase c) {
      c.verify("receipt", receipt);
    }
  }
}
