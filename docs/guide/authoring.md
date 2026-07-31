# Authoring contracts

## Acceptance methods and typed rows

Bind each AC to exactly one literal public `@ToppleAcceptanceTest("AC-...")`
method. Give it an optional `@DisplayName` that says what a reviewer is checking.
The method is a small Scenario orchestration, not a home for setup, assertions,
or business calls.

```java
@ToppleAcceptanceTest("AC-ORDER-CREATE")
@DisplayName("Create an accepted order")
void createsOrder(ToppleCase c, ToppleScenario scenario, OrderStage order) {
    scenario.given(order).an_order_request(c.input("request", OrderRequest.class));
    scenario.when(order).submits_it();
    scenario.then(order).confirms_accepted_order(c);
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
void lineOrderDoesNotChangeTotal(PropertyTrials trials) {
    trials.forAll(Generators.lists(Generators.integers(0, 10), 0, 8))
        .check(lines -> assertEquals(total(lines), total(reversed(lines))));
}
```

The method returns `void`, receives exactly one `PropertyTrials`, uses positive
tries and non-negative discard/shrink limits, and calls `forAll(...).check(...)`
once. Built-ins cover bounded scalar values, ordered values and enums,
explicit-alphabet strings, lists, optionals, `oneOf`, `map`, `filter`, and
two/three-input `combine`. Recursive generation, custom engines, `flatMap`,
and custom shrinkers are unsupported.

Use `classify` and `requireCoverage` for a named business boundary. An unmet
coverage requirement, exhausted filter, malformed generator, or unstable replay
makes evidence `INCOMPLETE`. Generated trial values are current-run evidence,
not case rows; reports use generator-preserved JSON choices rather than an
arbitrary Java object's `toString()`.

## Reviewer material and external context

Choose hidden rows from independently derived business boundaries—thresholds,
mixed carts, idempotency, conflicts, validation errors, or nested response
shape—not merely a changed visible literal. Passing hidden rows does not prove
all shortcuts are impossible; it is evidence for Hidden Tests only.

An external workflow supplies optional Markdown context with repeated `--spec`
paths. Each selected `AC-...` needs an executable public binding. The same
selection is used for Check, Review, Seal, and Verify, and becomes part of the
mechanical approval. Markdown is reading context, not another authoring
language or source of executable truth.

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
