package integration.mutation;

/** Production target for the managed PIT survivor attack. */
public final class CouponService {
  private CouponService() {}

  public static int discountedTotal(int subtotal) {
    if (subtotal >= 110) {
      recordAudit();
      return subtotal - 10;
    }
    return subtotal;
  }

  private static void recordAudit() {}
}
