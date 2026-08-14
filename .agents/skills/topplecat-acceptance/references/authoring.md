# Authoring format

## Acceptance binding

Put public acceptance source under `src/test/java`. Bind each AC in the
human-selected delivery scope to exactly one literal method. When External
Workflow supplies Selected Spec Document paths, include every AC loaded by an
exact standalone ID-bearing marker in each complete document. With no
`--spec` selection, every bound AC is in scope. The marker—not a heading,
nearby prose, or ordinary AC reference—is the machine identity and Review
insertion point.

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

Use stable, business-readable method names and give every selected acceptance
method a `@DisplayName` that states what the Reviewer is checking. Give every
selected Step an `@As` sentence that states its
business-visible state, action, or observable result. Do not merely replace
underscores in a Java method name or expose placeholders such as `<scenario>`,
Java types, signatures, or technical parameter names as Reviewer prose. Use
named `@As` placeholders only when a case value makes the sentence more
specific without exposing implementation structure.

`@DisplayName` and `@As` are human-authored display prose. Keep the chosen text
exact, including Traditional Chinese and named `@As` placeholders: ToppleCat
preserves it through the compiler contract and Reviewer reports. Do not
translate it or create a second language field. Reviewer HTML presentation may
be chosen per invocation with `--language en` or `--language zh-TW`; that
localizes only ToppleCat-owned report prose, not authored values or canonical
machine outcomes.

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

## Canonical Markdown selection

The external workflow supplies one or more exact repository-relative `.md` paths
for selected work. The complete selected document is read; its AC inventory
comes only from the product CommonMark parser's exact standalone ID-bearing
markers (`<!-- topplecat:acceptance:AC-ID -->`). Do not search for
a current Spec, follow a wrapper, normalize an absolute path, or use a `.feature`
file as a substitute. Carry the same paths through Check, Review, and scoped
Verify. Missing, ambiguous, absolute, missing-file, structurally invalid,
duplicate/malformed/legacy marker, structurally invalid, insufficient, and thin-wrapper selections return to the human or upstream Spec
owner before a selected handoff is formed.

## Property declarations

Put optional Properties under `src/test` and tie each one to an existing AC:

```java
@ToppleProperty("AC-CHECKOUT-001")
@DisplayName("合法購物車的應付金額不得小於 0")
void payableTotalNeverBecomesNegative(PropertyTrials trials) {
    trials.forAll(Generators.integers(0, 10_000))
        .check(total -> assertTrue(checkout.payable(total) >= 0));
}
```

Every Property must declare a JUnit `@DisplayName`. State both:

- the generated input or repeated situation, such as legal carts or replaying
  one checkout request with the same idempotency key; and
- the invariant checked for every completed trial, such as a non-negative
  payable total or no second payment and order.

Use the Spec's business language. Do not use only a Java method name, a generic
title such as `checkout property`, generator terminology, or a configured trial
count. The title describes what is checked; Current-run Evidence supplies how
many trials actually completed. Keep one invariant per Property. Good titles
include `合法購物車的應付金額不得小於 0` and
`相同結帳識別碼重送時不得重複付款或建立第二張訂單`.

The method returns `void`, receives exactly one `PropertyTrials`, and calls one
`forAll(...).check(...)`. `tries` must be between 1 and 100,000; discard and
shrink limits are non-negative. The defaults are 200 tries, 1,000 discards, and
500 shrink steps.

Built-in generators cover booleans, bounded integers, longs and decimals,
ordered values and enums, explicit-alphabet strings, lists, optionals, `oneOf`,
`map`, `filter`, and two- or three-input `combine`. Use `classify` and
`requireCoverage` for a named business boundary. Recursive generators,
`flatMap`, custom engines, and custom shrinkers are outside the supported API.

A Property states a recorded invariant. Generated trials are Current-run
Evidence, not Typed Case Rows. ToppleCat preserves the required `@DisplayName`
as its Reviewer-facing title so the report can combine that authored meaning
with runtime counts without inferring business meaning from generator code.
Every discarded generator input is retained as canonical JSON Property evidence
with a neutral explanation and may be paginated in the Verification Report; it
does not become a case row or a new rule.
