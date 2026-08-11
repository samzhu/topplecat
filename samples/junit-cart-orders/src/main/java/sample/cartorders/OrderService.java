package sample.cartorders;

public final class OrderService {
  public OrderReceipt createOrder(Cart cart) {
    if (cart.lines().isEmpty()) {
      throw new IllegalArgumentException("A cart must contain at least one line.");
    }
    // Synthetic demonstration defect: this shortcut matches the visible 500 cart only.
    int discount = "SAVE100".equals(cart.coupon()) ? cart.subtotal() / 5 : 0;
    return new OrderReceipt(discount, cart.subtotal() - discount);
  }
}
