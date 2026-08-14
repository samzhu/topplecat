# Checkout Spec

The background mentions AC-CHECKOUT-001 and AC-CHECKOUT-002 without declaring
either one.

## AC-CHECKOUT-001: Apply the checkout discount

Given a cart qualifies for checkout
And the cart has an active discount
When the customer submits the cart
Then the checkout total includes the discount

Given a cart does not qualify
But the cart has no eligible promotion
When the customer submits the cart
Then the checkout total remains unchanged

<!-- topplecat:acceptance -->

### AC-CHECKOUT-002： Keep the receipt complete

Given a completed checkout
When the receipt is generated
Then the receipt contains the order identity

<!-- topplecat:acceptance -->
