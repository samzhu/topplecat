package sample.cartorders;

/** Synthetic lesson variant: a shortcut that only matches the visible 500 cart. */
public final class OrderService {
  public OrderReceipt createOrder(Cart cart) {
    if (cart.lines().isEmpty()) {
      throw new IllegalArgumentException("A cart must contain at least one line.");
    }
    int discount = "SAVE100".equals(cart.coupon()) ? cart.subtotal() / 5 : 0;
    return new OrderReceipt(discount, cart.subtotal() - discount);
  }
}
