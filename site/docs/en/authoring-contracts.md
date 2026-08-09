---
title: Authoring contracts
description: Bind human-selected Acceptance Conditions to ordinary Java/JUnit methods and typed case rows.
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

# Authoring contracts

## Start with one concrete rule {#contract-example}

Take a checkout rule: an order with subtotal 1,000 or more has a receipt total
of 900. A human decides that rule and chooses a public case such as
`subtotal: 1000`, `total: 900`. ToppleCat does not invent a lower-bound rule,
choose the example, or decide whether more cases are needed.

The public Java method and typed row are the Executable Contract. Generated
JSON and HTML are projections of that contract, not a second authoring
language.

## Acceptance Method shape {#acceptance-method}

Bind each Acceptance Condition to exactly one literal public
`@ToppleAcceptanceTest("AC-...")` method. Give it a business-readable JUnit
`@DisplayName` and keep the method as a small Scenario orchestration:

```java
@ToppleAcceptanceTest("AC-ORDER-CREATE")
@DisplayName("Create an accepted order")
void createsOrder(ToppleCase c, ToppleScenario scenario, OrderStage order) {
    scenario.given(order).an_order_request(c.input("request", OrderRequest.class));
    scenario.when(order).submits_it();
    scenario.then(order).confirms_accepted_order(c);
}
```

The parameters are `ToppleCase`, one non-generic `ToppleScenario`, then one or
more distinct concrete `ToppleStage` types. A Stage must be non-final,
proxyable, and constructible with an accessible no-argument constructor. Move
setup, service calls, branching, and assertions into ordinary Stage methods.

Each direct call is exactly `scenario.given|when|then|and(stage).step(...)`.
The compiler owns phase order, Stage selection, overload identity, and the
rendered Step. `@As` supplies business-visible prose but does not let runtime
code rewrite the compiler-described Step.

## Typed Case Rows {#typed-case-rows}

Public rows live under `src/test/resources/topplecat/cases/`:

```yaml
- caseId: order-public-example
  acId: AC-ORDER-CREATE
  inputs:
    request: {items: [{sku: example-sku, quantity: 1}]}
  expected:
    response: {accepted: true}
```

A row has exactly `caseId`, `acId`, `inputs`, and `expected`. Reviewer-owned
rows use the same schema in reviewer custody and target an existing public AC;
they add an independently chosen example rather than a new rule. The public
contract handed to the Implementation Agent is byte-for-byte the public
contract formal Verify runs.

## Expected values and Properties

Every top-level expected value begins `UNTOUCHED`. `c.verify("receipt", actual)`
compares it and marks it `ASSERTED`; `c.expected("receipt", Type.class)` only
reads it and marks it `READ`; no access leaves it untouched. Only `ASSERTED`
fulfils expected-consumption enforcement.

Use `@ToppleProperty` for a human-approved invariant that examples do not cover.
It has its own independent `PROPERTY` Gate, uses bounded generators, and cannot
create rows or improve Mutation Testing. Generated inputs are Current-run
Evidence, not Typed Case Rows.

## Human completeness {#human-completeness}

Humans or an External Workflow select the current Spec and remain responsible
for making its rules and cases complete. ToppleCat binds those selected ACs to
ordinary Java/JUnit work, checks the compiler-defined Scenario, and later runs
the sealed contract. It does not judge missing requirements, choose an
organizational sign-off, or become a task manager.

For a complete sample-backed path, see [Getting started](getting-started.md#sample-workflow).
For how the same public contract reaches formal evidence, see
[Architecture](architecture.md#contract-authority) and
[Verification and evidence](verification-and-evidence.md#three-evidence-layers).
