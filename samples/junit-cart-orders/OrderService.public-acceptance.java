package sample.cartorders;

/** Synthetic lesson variant: ignores the public SAVE100 promise. */
public final class OrderService {
  public OrderReceipt createOrder(Cart cart) {
    if (cart.lines().isEmpty()) {
      throw new IllegalArgumentException("A cart must contain at least one line.");
    }
    return new OrderReceipt(0, cart.subtotal());
  }
}
