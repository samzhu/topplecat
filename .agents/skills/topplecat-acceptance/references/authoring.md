# Authoring format

## Acceptance binding

Put public acceptance source under `src/test/java`. Bind each AC in the
human-selected delivery scope to exactly one literal method. When External
Workflow supplies Selected Spec Document paths, include every AC anchored in
each complete document. With no `--spec` selection, every bound AC is in scope.

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

Use stable, business-readable method names. Add `@DisplayName` when it makes the
reviewer-facing title clearer; otherwise ToppleCat derives the title from the
method name. Use optional `@As` text for compiler-described Step sentences and
case IDs that identify the behavior or boundary.

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

Prefer one `c.verify("receipt", projection)` for a complete observable result.
When several independent top-level expected values are necessary, put their
`verify` calls in JUnit `assertAll` so each receives an assertion opportunity.
A mismatching `verify` remains `ASSERTED`; a later call that never runs remains
`UNTOUCHED`. ToppleCat has no `verifyAll` API.

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
`forAll(...).check(...)`. `tries` must be between 1 and 100,000; discard and
shrink limits are non-negative. The defaults are 200 tries, 1,000 discards, and
500 shrink steps.

Built-in generators cover booleans, bounded integers, longs and decimals,
ordered values and enums, explicit-alphabet strings, lists, optionals, `oneOf`,
`map`, `filter`, and two- or three-input `combine`. Use `classify` and
`requireCoverage` for a named business boundary. Recursive generators,
`flatMap`, custom engines, and custom shrinkers are outside the supported API.

A Property states a recorded invariant.
Generated trials are Current-run Evidence, not Typed Case Rows.
