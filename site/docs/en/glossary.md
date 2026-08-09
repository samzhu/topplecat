---
title: Glossary
description: The formal ToppleCat vocabulary for executable contracts, safeguards, evidence, and delivery boundaries.
page_id: glossary
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/glossary/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: glossary/
alternate_zh_tw: zh-TW/glossary/
markdown_url: glossary.md
copy_label: Copy Markdown
copied_label: Copied
---

# Glossary

These meanings are the shared ToppleCat language. They describe the executable
acceptance boundary, not a new authoring language.

## Executable Contract {#executable-contract}

The human-authored Acceptance Methods and Typed Case Rows that define what
ToppleCat mechanically verifies. Humans remain responsible for whether rules
and examples are correct and complete.

## Acceptance Condition

A stable externally chosen `AC-...` rule that ToppleCat binds to executable
acceptance work. ToppleCat does not invent an omitted rule.

## Acceptance Method

The one public Java/JUnit method that binds an Acceptance Condition to executable
examples and describes its Scenario.

## Scenario, Stage, and Step

A Scenario is one ordered Given, When, Then, and And execution for one Typed
Case Row. A Stage is a reusable business-capability object that supplies related
Steps and holds ordinary state for one Scenario. A Step is one business action
or observation selected within that Scenario.

## Typed Case Row

An authored JSON or YAML example containing an AC ID, inputs, and expected
results. A generated trial is not a Typed Case Row.

## Independent Safeguard {#independent-safeguard}

A safeguard whose current-run evidence answers only its own question and cannot
be replaced by another safeguard's evidence. Hidden Tests, Property-Based
Testing, and Mutation Testing stay separate.

## Mutation Attribution

The mapping of a Mutation Testing observation to the exact public Acceptance
Method and Acceptance Condition whose execution covered it. PIT's official
outcome names remain unchanged.

## Mechanical Seal {#mechanical-seal}

The content-based integrity record over the complete executable contract and
verification policy. It confirms consistency, not human or organizational
approval.

## Current-run Evidence and Aggregate Verdict

Current-run Evidence is produced by the active formal verification run. The
Aggregate Verdict is its `PASS`, `FAIL`, or `INCOMPLETE` conclusion for the
selected Delivery Scope. `PASS` is evidence for the checked scope, not proof or
sign-off.

## Spec Review and Verification Report

Spec Review is the reviewer-only projection before handoff. Verification Report
is the reviewer-only projection of one formal Verify run and its diagnostics.
Neither is an implementation-agent handoff or a public actual-delivery report.

See [Architecture](architecture.md#information-boundary) for the ownership and
information flow behind these terms.
