package example;

// Synthetic, read-only fixture material for the deterministic package/source/assertion contract.
// The test observes the authored contract; it never compiles or edits it.
final class CheckoutAcceptance {
  @ToppleAcceptanceTest("AC-CHECKOUT-001")
  @DisplayName("Apply the checkout discount when the cart qualifies")
  void checkoutAppliesDiscount(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).cart_qualifies(c.input("cart", Cart.class));
    scenario.when(checkout).submits_the_cart();
    scenario.then(checkout).total_includes_the_discount(c);
  }

  @ToppleAcceptanceTest("AC-CHECKOUT-002")
  @DisplayName("Keep the order identity in the completed receipt")
  void receiptKeepsIdentity(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).completed_checkout(c.input("checkout", Checkout.class));
    scenario.when(checkout).generates_the_receipt();
    scenario.then(checkout).receipt_contains_the_order_identity(c);
  }

  @ToppleAcceptanceTest("AC-CHECKOUT-003")
  @DisplayName("Keep the payment identity in the confirmation")
  void paymentKeepsIdentity(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).payment_has_been_authorized(c.input("payment", Payment.class));
    scenario.when(checkout).issues_the_confirmation();
    scenario.then(checkout).confirmation_retains_the_payment_identity(c);
  }

  @ToppleAcceptanceTest("AC-OLD")
  @DisplayName("Keep the earlier contract available to whole-contract maintenance")
  void oldContract(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).old_contract_is_available();
    scenario.when(checkout).checks_the_earlier_contract();
    scenario.then(checkout).earlier_contract_remains_available(c);
  }

  static final class CheckoutStage extends ToppleStage {
    @As("the cart qualifies for checkout")
    void cart_qualifies(Cart cart) {}

    @As("the customer submits the cart")
    void submits_the_cart() {}

    @As("the checkout total includes the discount")
    void total_includes_the_discount(ToppleCase c) {
      c.verify("discountApplied", true);
    }

    @As("the completed checkout exists")
    void completed_checkout(Checkout checkout) {}

    @As("the receipt is generated")
    void generates_the_receipt() {}

    @As("the receipt contains the order identity")
    void receipt_contains_the_order_identity(ToppleCase c) {
      c.verify("orderIdentityPresent", true);
    }

    @As("the payment has been authorized")
    void payment_has_been_authorized(Payment payment) {}

    @As("the confirmation is issued")
    void issues_the_confirmation() {}

    @As("the confirmation retains the payment identity")
    void confirmation_retains_the_payment_identity(ToppleCase c) {
      c.verify("paymentIdentityPresent", true);
    }

    @As("the earlier contract is available")
    void old_contract_is_available() {}

    @As("the earlier contract is checked")
    void checks_the_earlier_contract() {}

    @As("the earlier contract remains available")
    void earlier_contract_remains_available(ToppleCase c) {
      c.verify("oldContractAvailable", true);
    }
  }
}
