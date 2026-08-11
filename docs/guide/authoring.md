# Authoring contracts

This guide is also available as the [official English technical documentation](https://topplecat.samzhu.dev/docs/authoring-contracts/)
and [Traditional Chinese documentation](https://topplecat.samzhu.dev/docs/zh-TW/authoring-contracts/).

## Acceptance methods and typed rows

Bind each AC to exactly one literal public `@ToppleAcceptanceTest("AC-...")`
method. Give it a business-readable JUnit `@DisplayName` that says what a
reviewer is checking.
The method is a small Scenario orchestration, not a home for setup, assertions,
or business calls.

`@DisplayName` is authored business prose, not ToppleCat interface copy. It
may use Traditional Chinese or other Unicode text and is preserved exactly in
the compiler descriptor, contract, runtime narrative, JSON projection, and
Reviewer HTML. The same rule applies to the required JUnit `@DisplayName` on a
`@ToppleProperty`: it is the Property title, never text to be translated.

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("SAVE100 reduces the order subtotal")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
}
```

`ToppleCase` is first, followed by exactly one non-generic `ToppleScenario`,
then one or more distinct concrete `ToppleStage` parameters. A Stage must be
non-final, proxyable, and have an accessible no-argument constructor.
ToppleCat creates a fresh proxy for each typed row; the same proxy holds
ordinary business state across its Given, When, Then, and And calls.

Each statement is exactly `scenario.given|when|then|and(stage).step(...)`.
The compiler owns phase order, Stage selection, overload identity, and rendered
Step. Put setup, service calls, branching, and assertions inside ordinary Stage
methods. Give business-visible methods `@As` sentences. `step().attach(...)` is available
only while a selected Step runs and cannot rename or otherwise edit the
compiler-described Step.

`@As` is also authored prose and survives unchanged. A named placeholder such
as `@As("準備可結帳的購物車 {cart.customerId}")` is resolved by the compiler to
the recorded argument binding; choosing a Traditional Chinese Reviewer report
only changes the surrounding ToppleCat presentation and its Given/When/Then
labels, never the Step sentence, placeholder, underlying phase, or Step order.

The [JUnit cart-orders learning project](../../samples/junit-cart-orders/) is
an optional, runnable copy of this SDK pattern. It consumes the released 0.1.0
artifacts from Maven Central and provides five clearly labelled synthetic
lessons; readers do not need to run it to author their own contract.

Public rows are JSON or YAML under `src/test/resources/topplecat/cases/`.
Reviewer-owned rows use the same schema under
`src/hiddenTest/resources/topplecat/cases/`:

```yaml
- caseId: order-public-example
  acId: AC-ORDER-CREATE
  inputs:
    request: {items: [{sku: example-sku, quantity: 1}]}
  expected:
    response: {accepted: true}
```

A row has exactly `caseId`, `acId`, `inputs`, and `expected`. A hidden row must
target an existing public AC; it adds a reviewer-chosen example, not a new rule.

## Expected values

Every top-level `expected` key begins `UNTOUCHED`.

| Action | Result |
| --- | --- |
| `c.verify("response", actual)` | Deep comparison and `ASSERTED`. |
| `c.expected("response", Response.class)` | Deserialization and `READ`. |
| no access | `UNTOUCHED`. |

Only `ASSERTED` fulfils the obligation while expected-consumption enforcement is
enabled. JSON numeric equality is mathematical: `200`, `200.0`, and `200.00`
compare equal. Use a string or another explicit field when presentation scale
is itself a rule.

Prefer projecting a complete receipt into one top-level expected value and
verifying it once, for example `c.verify("receipt", receipt)`. That records one
complete observable result and avoids a partial chain of assertions. When
separate top-level values are genuinely needed, give each one a chance to run:

```java
assertAll(
    () -> c.verify("subtotal", receipt.subtotal()),
    () -> c.verify("total", receipt.total()));
```

`verify()` marks the key `ASSERTED` before comparing it. Therefore a mismatched
first value is truthfully `ASSERTED`, while a later `verify()` never reached
because that assertion threw remains `UNTOUCHED`. An Expected Consumption
failure beside a JUnit failure is not necessarily duplicate reporting: it can
show exactly which later declared values did not receive a verification
attempt. ToppleCat does not provide a `verifyAll` API.

## Property-Based Testing

Use `@ToppleProperty` for a human-approved invariant that examples alone do
not exercise broadly. It is supplementary and has an independent gate; it
cannot create rows, consume expected values, or improve mutation results.

```java
@ToppleProperty("AC-ORDER-CREATE")
@DisplayName("Reordering lines preserves the order total")
void lineOrderDoesNotChangeTotal(PropertyTrials trials) {
    trials.forAll(Generators.lists(Generators.integers(0, 10), 0, 8))
        .check(lines -> assertEquals(total(lines), total(reversed(lines))));
}
```

The method returns `void`, receives exactly one `PropertyTrials`, uses 1 to
100,000 tries and non-negative discard/shrink limits, and calls
`forAll(...).check(...)` once. Built-ins cover bounded scalar values, ordered
values and enums, explicit-alphabet strings, lists, optionals, `oneOf`, `map`,
`filter`, and two/three-input `combine`. Recursive generation, custom engines,
`flatMap`, and custom shrinkers are unsupported.

`@ToppleProperty` defaults to 200 tries, 1,000 discards, and 500 shrink steps.
Authors may set `tries` from 1 through 100,000 and non-negative `maxDiscards`
and `maxShrinks` values on the annotation.

Use `classify` and `requireCoverage` for a named business boundary. An unmet
coverage requirement, exhausted filter, malformed generator, or unstable replay
makes evidence `INCOMPLETE`. Generated trial values are current-run evidence,
not case rows; reports use generator-preserved JSON choices rather than an
arbitrary Java object's `toString()`. The `@DisplayName` must name the
generated input or situation and the invariant being checked. Every discarded
generator input is retained as JSON evidence with a neutral explanation; the
Verification Report paginates that list when it is long.

## Reviewer material and external context

Choose hidden rows from independently derived business boundaries—thresholds,
mixed carts, idempotency, conflicts, validation errors, or nested response
shape—not merely a changed visible literal. Passing hidden rows does not prove
all shortcuts are impossible; it is evidence for Hidden Tests only.

An external workflow supplies optional Markdown context with repeated `--spec`
paths. Each selected `AC-...` needs an executable public binding. Check and
Review use those paths to validate and read the complete Markdown Spec. Seal
always approves the complete executable contract. Verify normally runs that
complete contract, but a Reviewer may ask for quick scoped evidence with either
`--spec` paths or repeated `--ac AC-...` IDs; the two forms cannot be combined.
Markdown is reading context, not another authoring language or source of
executable truth.

Run `./gradlew toppleCatCheck` before handoff. An authorized reviewer then runs
`./gradlew toppleCatReview`; its output contains reviewer material and must not
be given to an implementation agent.

Check can emit a reviewer-only Contract Quality Advisory when a hidden row has
an expected-output shape unlike every public variant, or when public and hidden
rows expose distinct nonblank literals at the same `...Id`, `...Key`, or
`...Token` expected field. This does not say the rule is wrong or incomplete;
it is a prompt to review the approved examples. The advisory contains only its
rule code, AC, expected path, and counts, and never changes the contract,
approval, or verification result.

`EXPECTED_SHAPE_VARIANT_MISSING` compares recursive expected-field paths while
ignoring object-key order, scalar values, and scalar types; arrays are terminal
fields. `EXPECTED_OPAQUE_IDENTIFIER_LITERALS` requires the same suffix-matching
path to have at least two public and two hidden non-empty string values, all
distinct. Blank, duplicate, non-string, array-content, and other field-name
candidates do not produce that advisory.
