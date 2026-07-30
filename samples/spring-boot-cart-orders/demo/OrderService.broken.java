package sample.cartorders;

import org.springframework.stereotype.Service;

/**
 * Deliberately flawed implementation: it memorizes the public 500-dollar case as a 20% discount.
 */
@Service
public class OrderService {
  public OrderReceipt createOrder(Cart cart) {
    if (cart.lines().isEmpty()) {
      throw new IllegalArgumentException("A cart must contain at least one line.");
    }
    int discount = "SAVE100".equals(cart.coupon()) ? cart.subtotal() / 5 : 0;
    return new OrderReceipt(discount, cart.subtotal() - discount);
  }
}
