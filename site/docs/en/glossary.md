---
title: Glossary
description: Plain explanations of the exact ToppleCat terms used for rules, checks, evidence, reports, and delivery decisions.
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

You do not need to memorize these terms before trying ToppleCat. Use this page
when a report, developer, or AI needs the exact project meaning. The plain
explanation comes first; capitalization marks a formal ToppleCat term.

## Executable Contract {#executable-contract}

The public Acceptance Methods and public and reviewer-controlled Typed Case Rows
that define what ToppleCat will mechanically check. People remain responsible
for whether the rules and examples are correct and complete.

## Acceptance Condition {#acceptance-condition}

One externally chosen rule with a stable `AC-...` ID. ToppleCat binds the rule
to executable acceptance work but never invents a missing rule.

## Acceptance Method

The one public Java/JUnit method that tells the Scenario for an Acceptance
Condition. Both public and reviewer-controlled examples run this same method.

## Typed Case Row

One authored JSON or YAML example with an AC ID, inputs, and expected results.
A value generated during Property-Based Testing is evidence from that run, not
a Typed Case Row.

## Scenario, Stage, and Step

A **Scenario** is one ordered Given, When, Then, and And execution for one case
row. A **Stage** groups related business actions and holds ordinary state during
that Scenario. A **Step** is one selected action or observation within it.

## Reviewer and Implementation Agent

The **Reviewer** is the person who reads the prepared contract and current
Verification Report, then decides what to do with the delivery. The
**Implementation Agent** is the AI coding agent that receives the public
contract and safe feedback. It does not receive private reviewer material.

## Independent Safeguard {#independent-safeguard}

A check whose current-run evidence answers only its own question. Reviewer
examples, Property-Based Testing, and Mutation Testing stay separate; one
passing result cannot fill a gap in another.

## Hidden Tests

Reviewer-controlled Typed Case Rows that run an existing public Acceptance
Method with independently chosen examples. They test the same rule and do not
create secret requirements.

## Property-Based Testing

A check that exercises a human-approved invariant over bounded generated
inputs. It can find a counterexample, but it is testing evidence rather than
mathematical proof.

## Mutation Testing and Mutation Attribution

Mutation Testing temporarily changes production behaviour and asks whether the
unchanged public Acceptance Method notices. **Mutation Attribution** connects a
PIT observation to the exact public method and Acceptance Condition responsible
for detecting it.

## Mechanical Seal {#mechanical-seal}

A content-based integrity record over the complete executable contract and
verification policy. It can show that the agreement changed after review. It is
not human approval, encryption, or a security sandbox.

## Current-run Evidence and Aggregate Verdict

**Current-run Evidence** is produced by the active formal Verify run. The
**Aggregate Verdict** is `PASS`, `FAIL`, or `INCOMPLETE` for that run's delivery
scope. `PASS` means every required Gate passed; it is not proof that every
business rule was written down.

## Spec Review and Verification Report

**Spec Review** is the private pre-handoff reading surface for the selected Spec
and prepared executable contract. **Verification Report** is the private
post-done reading surface for one formal run and its diagnostics.

For the complete canonical vocabulary, see
[CONTEXT.md](https://github.com/samzhu/topplecat/blob/main/CONTEXT.md). For the
information flow behind these terms, read
[How ToppleCat works](architecture.md#information-boundary).
