package sample.cartorders;

import java.util.List;

public record Cart(String customerId, List<CartLine> lines, int subtotal, String coupon) {}
