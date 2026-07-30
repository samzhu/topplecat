# ToppleScenario authoring

**Status:** Implemented

**Date:** 2026-07-30

## User example

A checkout contract should read as one business flow. The author writes the
flow once; public and reviewer Typed Case Rows exercise it with different
examples.

```java
@ToppleAcceptanceTest("AC-CHECKOUT-001")
void checkout(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).a_cart(c.input("cart", Cart.class));
    scenario.when(checkout).creates_an_order();
    scenario.then(checkout).receipt_matches(c);
}

static class CheckoutStage extends ToppleStage {
    private Cart cart;
    private Receipt receipt;

    void a_cart(Cart value) { cart = value; }
    void creates_an_order() { receipt = orders.create(cart); }
    void receipt_matches(ToppleCase c) { c.verify("receipt", receipt); }
}
```

The same fresh `CheckoutStage` keeps ordinary Java state for one case row. A
checkout that needs payment as well receives a second capability Stage, but it
still has one Scenario and one ordered narrative.

## Decision and boundaries

An Acceptance Method receives `ToppleCase`, exactly one `ToppleScenario`, and
one or more distinct concrete `ToppleStage` parameters. Each top-level
statement is a direct `given`, `when`, `then`, or `and` selector followed by a
Step on the supplied Stage.

The compiler defines phase order, selected Stage, Step identity, and report
sentence before execution. Stage methods are ordinary, overridable `void` Java
methods declared directly on the concrete Stage. Setup, service calls,
branching, and assertions belong inside them. A Stage may call
`step().attach(...)` only while its selected Step is active.

ToppleCat constructs one proxy per declared Stage type and typed row. The proxy
rejects an unselected top-level Step, a mismatched phase or Stage, a missing or
extra Step, an escaped failure that the Acceptance Method hides, and use from a
different thread. Nested ordinary Java calls remain implementation details and
do not create a second Step.

ToppleCat owns the Scenario session and evidence projection; it does not own
business state, control flow, or report wording beyond compiler-described Step
sentences. The generated JSON and HTML project that same compiled sequence and
must not add or reinterpret it.

## Failure and report rules

Every selected Step produces one evidence status. A failure or abort marks that
Step and leaves later compiler-described Steps skipped. Missing or extra Steps
fail the invocation after the available evidence is recorded. Attachments are
bound to the active Step and remain subject to the public/reviewer information
boundary.

Public and reviewer rows run the identical Acceptance Method. A reviewer row
can choose a different boundary or combination for an existing Acceptance
Condition; it cannot introduce a reviewer-only rule or Scenario.

## Acceptance evidence

- Compiler tests reject missing Scenario parameters, duplicate Stage types,
  invalid phase order, inherited selected Steps, and non-void Steps.
- Runtime tests cover one and multiple Stages, Given/When/Then/And, method
  identity, missing and extra Steps, attachments, failures, aborts, expected
  consumption, public and hidden rows, and cross-thread rejection.
- The formal verification run checks narrative/report parity from the compiled
  Scenario descriptor.

## Consequences and alternatives

This model keeps Java/JUnit and typed rows as the executable source of truth.
It deliberately does not add Gherkin, Cucumber, a JGiven runtime or report
dependency, a separate Scenario language, or multiple aliases for one Stage
role. JGiven is only a high-level readability reference for the distinction
between Scenario, Stage, and Step.
