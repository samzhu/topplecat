Feature: old translated surface
  Scenario: repeated selected AC references
    Given AC-CHECKOUT-001 and AC-CHECKOUT-002
    Then the feature must never enter ToppleCat scope
