package sample.cartorders;

import io.github.samzhu.topplecat.junit.ToppleAc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewerBoundaryTest {
    @Test
    @ToppleAc(value = "AC-CART-EMPTY", title = "Reject an empty cart")
    void rejectsEmptyCart() {
        Cart empty = new Cart("customer-1", java.util.List.of(), 0, "SAVE100");
        assertThrows(IllegalArgumentException.class, () -> new OrderService().createOrder(empty));
    }
}
