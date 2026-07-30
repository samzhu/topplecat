package sample.cartorders;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReviewerBoundaryTest {
  @Test
  void rejectsEmptyCart() {
    Cart empty = new Cart("customer-1", java.util.List.of(), 0, "SAVE100");
    assertThrows(IllegalArgumentException.class, () -> new OrderService().createOrder(empty));
  }
}
