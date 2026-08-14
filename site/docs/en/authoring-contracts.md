---
title: Turn rules into checks
description: Write down what a Java delivery must do so the implementation agent and ToppleCat are checked against the same public contract.
page_id: authoring-contracts
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/authoring-contracts/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: authoring-contracts/
alternate_zh_tw: zh-TW/authoring-contracts/
markdown_url: authoring-contracts.md
copy_label: Copy Markdown
copied_label: Copied
---

# Turn business rules into executable checks

Your authoring source may target Java 17, 21, or 25, but the Gradle daemon
that loads ToppleCat and runs its compiler must use JDK 21 or 25. This is a
source compatibility choice, not Java 17 runtime support: a JDK 17-only
environment cannot execute the published Java 21-targeted artifact. The
initial support policy uses the daemon's system compiler for contract source;
do not assume a different consumer toolchain without a separately tested
compiler seam.

Before asking an AI to implement a feature, answer one question in observable
terms: what result would convince you that this rule works?

## Start with the rule, not the annotation {#contract-example}

Suppose the rule is: “an accepted order returns a receipt with its final
total.” Write at least one concrete example that a developer, product owner, and
AI agent can all read: this cart goes in; this receipt must come back.

ToppleCat does not decide what an accepted order means. It preserves the rule
and examples you chose, then verifies that exact agreement after implementation.

In ToppleCat, the Java method that tells the story is called an **Acceptance
Method**. The JSON or YAML examples are **Typed Case Rows**. Together they form
the public **Executable Contract**.

## Describe the behaviour in Java {#acceptance-method}

Each selected rule, or Acceptance Condition, has one public
`@ToppleAcceptanceTest("AC-...")` method. Give it a name that explains the
business result:

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("SAVE100 reduces the order subtotal")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
}
```

Keep this method short enough to read as a story. `ToppleCase` supplies the
current example. `ToppleScenario` records the Given, When, and Then sequence.
The `CouponStage` methods perform the real setup, service calls, and assertions.

Want a complete, runnable version of this pattern? The optional
[JUnit cart-orders learning project](https://github.com/samzhu/topplecat/tree/main/samples/junit-cart-orders)
uses the locally published 0.2.1 release-line artifact (Maven Central publication is a separate maintainer action) and lets you choose five synthetic safeguard
lessons. Each one leaves a local HTML Verification Report for that lesson under
`build/topplecat/demo-reports/`. You do not need to run it before following
this guide.

The exact method contract matters: `ToppleCase` comes first, followed by one
`ToppleScenario` and one or more distinct concrete Stage types. Stages must be
non-final and have an accessible no-argument constructor. Each statement is a
direct `scenario.given|when|then|and(stage).step(...)` call. Put control flow,
helpers, and assertions inside Stage methods.

Use `@DisplayName` and `@As` for wording that a Reviewer understands. ToppleCat
keeps those authored words unchanged in the contract and reports.

## Add examples with inputs and expected results {#typed-case-rows}

Public case rows live under `src/test/resources/topplecat/cases/`:

```yaml
- caseId: order-public-example
  acId: AC-ORDER-CREATE
  inputs:
    request: {items: [{sku: example-sku, quantity: 1}]}
  expected:
    response: {accepted: true}
```

A row has four parts: its own ID, the rule it belongs to, the input, and the
expected result. Public rows teach the implementation agent what the rule looks
like. Reviewer-controlled rows use the same rule and method but exercise
independently chosen boundaries. They do not add secret requirements.

The agent receives the public contract. Formal Verify later runs those same
public bytes; ToppleCat does not swap in a different public specification after
the handoff.

## Canonical Markdown Spec selection {#canonical-markdown-spec}

The Review command accepts repository-relative `.md` paths. Each selected AC
must be declared by a visible ATX h1–h6 or Setext h1/h2 heading in the form
`AC-ID: business title` (full-width `：` is also accepted), followed by the
exact standalone marker `<!-- topplecat:acceptance -->` after that AC's rules
and examples. Ordinary references in prose or fenced code do not select scope.
Missing, duplicate, orphaned, or misplaced declarations and markers produce a
repairable `TC-SPEC-AC-*` diagnostic. ToppleCat does not read or translate
Gherkin `.feature` files.

The acceptance skill carries the exact repository-relative Markdown path or
paths supplied by the human or upstream workflow. It does not maintain a
registry, stable Spec identity, current selection, approval, status, history,
or reviewer-owned value. Absolute paths and path guesses are rejected; bind
directly from the selected canonical Markdown and never read or translate a
`.feature` file.

## Make sure expected results are really checked

Reading an expected value is not the same as asserting it. Use
`c.verify("receipt", actual)` to compare the complete observed receipt with the
authored expected receipt. ToppleCat records whether each top-level expected
value was actually asserted, merely read, or never reached.

When one rule should hold across many inputs, you can also write a public
`@ToppleProperty`. For example, reordering line items should not change the
total. Properties use bounded generated inputs and report through their own
independent check; they do not replace concrete examples.

## Decide what to give the AI {#human-completeness}

An AI can write the Java plumbing and case files from rules you have approved.
Give it this page, the selected business rule, and the public examples. Ask it
to keep one Acceptance Method per rule and to verify complete observable
results.

A person still decides whether the rule and examples are complete. ToppleCat
will not infer a refund exception, VIP discount, or legal requirement that is
absent from the contract. It also does not decide whether your organization
should approve the delivery.

Next, read [How ToppleCat verifies a delivery](verification-and-evidence.md#delivery-example) to see how
the same public contract becomes current-run evidence. Exact parameter and
generator rules remain available in the repository's
[authoring guide](https://github.com/samzhu/topplecat/blob/main/docs/guide/authoring.md).
