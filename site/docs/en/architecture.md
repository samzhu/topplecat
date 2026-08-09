---
title: Architecture
description: Understand ToppleCat's four modules, execution flow, evidence, custody, and information boundary.
page_id: architecture
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/architecture/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: architecture/
alternate_zh_tw: zh-TW/architecture/
markdown_url: architecture.md
copy_label: Copy Markdown
copied_label: Copied
---

# Architecture

## Four modules {#four-modules}

| Module | Responsibility |
| --- | --- |
| `topplecat-core` | Case, evidence, custody, Property, and safe-feedback models |
| `topplecat-junit` | Acceptance annotations, typed rows, compiler-described Scenario/Stage proxies, expected consumption, and Properties |
| `topplecat-report` | Reviewer-only Spec Review and Verification Report projections |
| `topplecat-gradle-plugin` | Commands, task wiring, scope, custody, integrity, and mutation orchestration |

The repository keeps exactly these four product modules. Samples and maintainer
validation infrastructure are not additional ToppleCat product modules.

## Contract authority {#contract-authority}

Ordinary Java/JUnit Acceptance Methods and typed JSON/YAML case rows are
authoritative. One public `@ToppleAcceptanceTest("AC-...")` binds each AC; the
compiler defines its Scenario phases, Stage selection, overload identity, and
rendered Steps. Public rows run in `PUBLIC_ONLY` mode and hidden rows reuse the
same method in `HIDDEN_ONLY` mode. Generated JSON and HTML are projections.

`@ToppleProperty` is a separate public declaration for a human-approved
invariant. Its generated choices are current evidence and never become case
rows or hidden contract input.

## Execution flow {#execution-flow}

```text
ordinary ./gradlew test
    -> public project tests and public acceptance methods

./gradlew toppleCatVerify
    -> current public acceptance
    -> enabled Hidden Tests, Properties, Expected Consumption, Mutation Testing
    -> Current-run Evidence and reviewer reports
```

Contract Integrity first compares the complete contract and verification policy
with the Mechanical Seal. When it passes, the independent safeguards run in
their fixed order. A failing Public Acceptance does not erase independent Hidden
or Property evidence; Mutation Testing instead lacks a trustworthy baseline.

## Custody and integrity

`toppleCatSeal` keeps reviewer-owned source in local plaintext mechanical
custody and records an integrity seal over the complete contract and policy.
Custody is not encryption, hostile-process isolation, CI isolation, or an
operating-system security boundary. Verify reuses the existing seal; it does
not create approval or update it.

## Information boundary {#information-boundary}

Spec Review and Verification Report are reviewer-only, human-readable
projections. Safe Implementation Agent feedback contains Gate-level remediation
without reviewer values, identifiers, paths, source names, tokens, raw private
failures, or Property trial material. The public project page may show a labelled
synthetic demonstration for education, never a real delivery.

For the product owner and use moments, read [Product definition](product-definition.md#responsibility-boundary).
For exact terms, read the [Glossary](glossary.md#independent-safeguard).
