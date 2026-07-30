# Authoring format

## Acceptance binding

Put public acceptance source under `src/test/java`. Bind each selected AC to
exactly one literal method:

```java
@ToppleAcceptanceTest("AC-CHECKOUT-001")
@DisplayName("Create an order from an accepted cart")
void createsOrder(ToppleCase c, ToppleScenario scenario, CheckoutStage checkout) {
    scenario.given(checkout).an_accepted_cart(c.input("cart", Cart.class));
    scenario.when(checkout).creates_the_order();
    scenario.then(checkout).receipt_matches(c);
}
```

Keep the acceptance method as short Scenario orchestration. Its parameters are
`ToppleCase`, exactly one non-generic `ToppleScenario`, then one or more
distinct concrete, non-private, non-final `ToppleStage` types with accessible
no-argument constructors. A nested Stage is static. The same fresh proxy
carries ordinary per-row state across its selected Given, When, Then, and And
calls.

Each statement must be a direct
`scenario.given|when|then|and(stage).step(...)` call. Put setup, service calls,
branching, and assertions inside Step methods declared directly on that
concrete Stage. A selected Step is a non-private, non-static, non-final `void`
method. Use `step().attach(...)` only for an attachment belonging to the
active compiled Step.

Give each Acceptance Method a concrete `@DisplayName`. Use stable,
business-readable method names and optional `@As` text for compiler-described
Step sentences. Use case IDs that identify the behavior or boundary.

## Typed case rows

Put public JSON or YAML rows under
`src/test/resources/topplecat/cases/`. Put reviewer-only rows under
`src/hiddenTest/resources/topplecat/cases/`.

Every row contains exactly:

```yaml
- caseId: checkout-public-card
  acId: AC-CHECKOUT-001
  inputs:
    cart: {items: [{sku: BOOK, quantity: 1, unitPrice: 250}]}
  expected:
    receipt: {accepted: true, total: 250}
```

Use `c.input(...)` for typed inputs. Use `c.verify("receipt", actual)` to compare
and consume each top-level expected value. Reading `c.expected(...)` without an
assertion does not satisfy expected consumption.

A reviewer row reuses an existing public AC and exercises a different value,
boundary, or rule combination. It never creates a rule absent from the public
Spec.

## Property declarations

Put optional Properties under `src/test` and tie each one to an existing AC:

```java
@ToppleProperty("AC-CHECKOUT-001")
void payableTotalNeverBecomesNegative(PropertyTrials trials) {
    trials.forAll(Generators.integers(0, 10_000))
        .check(total -> assertTrue(checkout.payable(total) >= 0));
}
```

The method returns `void`, receives exactly one `PropertyTrials`, and calls one
`forAll(...).check(...)`. Use a positive `tries` value and non-negative
discard and shrink limits. A Property states a confirmed invariant; generated
trials are not approved case rows.
