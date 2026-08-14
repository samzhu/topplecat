package example;

// Synthetic reviewer-owned fixture material; it must remain out of public handoff.
final class CheckoutHiddenTest {
  @ToppleAcceptanceTest("AC-CHECKOUT-001")
  @DisplayName("Reject a cart that does not qualify for the discount")
  void checkoutBoundary(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).cart_does_not_qualify(c.input("cart", Cart.class));
    scenario.when(checkout).submits_the_cart();
    scenario.then(checkout).total_remains_unchanged(c);
  }

  @ToppleAcceptanceTest("AC-CHECKOUT-003")
  @DisplayName("Reject a confirmation that loses payment identity")
  void paymentBoundary(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).payment_has_been_authorized(c.input("payment", Payment.class));
    scenario.when(checkout).issues_the_confirmation();
    scenario.then(checkout).confirmation_loses_the_payment_identity(c);
  }

  @ToppleAcceptanceTest("AC-OLD")
  @DisplayName("Challenge the earlier contract boundary")
  void oldBoundary(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).old_contract_is_available();
    scenario.when(checkout).checks_the_earlier_contract();
    scenario.then(checkout).earlier_contract_can_be_rejected(c);
  }
}
