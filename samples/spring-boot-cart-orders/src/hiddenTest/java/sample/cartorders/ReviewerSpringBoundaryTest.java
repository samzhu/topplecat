package sample.cartorders;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CartOrdersApplication.class)
class ReviewerSpringBoundaryTest {
  @Autowired private OrderService service;

  @Test
  void rejectsEmptyCart() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.createOrder(new Cart("customer-1", java.util.List.of(), 0, "SAVE100")));
  }
}
