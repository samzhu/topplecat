package sample.cartorders;

import io.github.samzhu.topplecat.junit.As;
import io.github.samzhu.topplecat.junit.ExpectedState;
import io.github.samzhu.topplecat.junit.ProvidedState;
import io.github.samzhu.topplecat.junit.ToppleCase;
import io.github.samzhu.topplecat.junit.ToppleStage;
import io.github.samzhu.topplecat.junit.ToppleStageField;
import io.github.samzhu.topplecat.junit.ToppleTest;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CartOrdersApplication.class)
class SpringCouponAcceptanceTest {
    @ToppleStageField
    CouponGiven given;
    @ToppleStageField
    CouponWhen when;
    @ToppleStageField
    CouponThen then;

    @ToppleTest("AC-CART-COUPON")
    @DisplayName("Spring Boot 套用 SAVE100 折抵訂單小計")
    void appliesCoupon(ToppleCase c) {
        given.a_cart(c.input("cart", Cart.class), c.input("cart", Cart.class).customerId(),
                c.input("cart", Cart.class).subtotal(), "case-data coupon");
        when.creates_an_order();
        then.receipt_shows_discount_and_discounted_subtotal(c);
    }

    @ToppleTest("AC-CART-NO-COUPON")
    @DisplayName("Spring Boot 未使用優惠券時維持原始小計")
    void createsOrderWithoutCoupon(ToppleCase c) {
        given.a_cart(c.input("cart", Cart.class), c.input("cart", Cart.class).customerId(),
                c.input("cart", Cart.class).subtotal(), "case-data coupon");
        when.creates_an_order();
        then.receipt_shows_discount_and_discounted_subtotal(c);
    }

    static final class CouponGiven extends ToppleStage<CouponGiven> {
        @ProvidedState
        OrderService service;
        @ProvidedState
        Cart cart;

        @As("準備顧客 {0} 金額為 {1} 元、優惠券 {2} 的購物車")
        CouponGiven a_cart(Cart cart, String customerId, int subtotal, String couponLabel) {
            recorded(customerId, subtotal, couponLabel);
            this.service = new OrderService();
            this.cart = cart;
            return self();
        }
    }

    static final class CouponWhen extends ToppleStage<CouponWhen> {
        @ExpectedState(required = true)
        OrderService service;
        @ExpectedState(required = true)
        Cart cart;
        @ProvidedState
        OrderReceipt receipt;

        @As("套用優惠券並建立訂單")
        CouponWhen creates_an_order() {
            recorded();
            receipt = service.createOrder(cart);
            return self();
        }
    }

    static final class CouponThen extends ToppleStage<CouponThen> {
        @ExpectedState(required = true)
        OrderReceipt receipt;

        @As("收據顯示折扣與折扣後小計")
        CouponThen receipt_shows_discount_and_discounted_subtotal(ToppleCase c) {
            recorded();
            c.verify("receipt", receipt);
            return self();
        }
    }
}
