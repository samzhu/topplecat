package sample.cartorders;

public final class OrderService {
    public OrderReceipt createOrder(Cart cart) {
        if (cart.lines().isEmpty()) {
            throw new IllegalArgumentException("A cart must contain at least one line.");
        }
        int discount = "SAVE100".equals(cart.coupon()) ? 100 : 0;
        return new OrderReceipt(discount, cart.subtotal() - discount);
    }
}
