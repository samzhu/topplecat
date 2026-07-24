package sample.cartorders;

import io.github.samzhu.topplecat.junit.ToppleAc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = CartOrdersApplication.class)
class ReviewerSpringBoundaryTest {
    @Autowired
    private OrderService service;

    @Test
    @ToppleAc(value = "AC-CART-EMPTY", title = "Spring Boot rejects an empty cart")
    void rejectsEmptyCart() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(new Cart("customer-1", java.util.List.of(), 0, "SAVE100")));
    }
}
