---
title: From rules to results
description: Follow a business rule as it becomes an executable check, goes through AI implementation and fresh verification, and returns as a result a person can read.
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

# From rules to results

This page connects the complete flow: people state what correct means, an AI
implements from public information, and ToppleCat rechecks the same agreement.
The second half explains the technical modules and where private information
stays.

If you only want to see the result, [run the sample](getting-started.md#sample-workflow).

## The complete delivery flow {#execution-flow}

```text
human selects rules and examples
    -> public Java/JUnit executable contract
    -> Reviewer checks and seals the complete contract
    -> AI agent implements with ordinary test feedback
    -> formal Verify runs fresh public and independent checks
    -> current evidence and a private Verification Report
    -> human decides what to do with the delivery
```

Ordinary `./gradlew test` runs the public project tests and acceptance methods.
It produces development feedback, not formal ToppleCat evidence.

`./gradlew toppleCatVerify` first checks that the complete contract and policy
still match the Mechanical Seal. It then runs public acceptance and every
enabled independent safeguard, writes the current evidence and reports, and
returns one aggregate result.

## What is authoritative {#contract-authority}

The public Java/JUnit Acceptance Methods and typed JSON or YAML case rows are
the executable contract. Generated JSON and HTML explain that contract and the
observed results; they never become another place to author rules.

One public `@ToppleAcceptanceTest("AC-...")` method owns each selected rule.
Public and reviewer-controlled rows run the same method in separate modes.
`@ToppleProperty` declarations are public invariants with their own generated
evidence. Generated Property choices never become hidden case rows.

This matters because the implementation agent must be judged against the same
public contract it received. ToppleCat does not reinterpret that contract in
the report layer.

## Four modules {#four-modules}

| Module | What it owns |
| --- | --- |
| `topplecat-core` | Case, evidence, custody, Property, and safe-feedback data models |
| `topplecat-junit` | Acceptance annotations, typed rows, Scenario/Stage execution, expected-value checking, and Properties |
| `topplecat-report` | The private Spec Review and Verification Report projections |
| `topplecat-gradle-plugin` | Commands, task order, scope, custody, integrity, and managed Mutation Testing |

The public website and executable samples explain and verify the product; they
are not extra runtime modules.

## Independent checks

Reviewer examples, Property-Based Testing, and Mutation Testing answer different
questions. They share the delivery scope, integrity check, and final report, but
not evidence. Once contract integrity passes, reviewer examples and Properties
can still produce results when Public Acceptance fails. Mutation Testing needs a
passing public baseline before it can assess temporary production changes.

Formal Mutation Testing uses ToppleCat's fixed managed PIT profile and maps PIT
observations to exact public Acceptance Methods. Project-specific PIT tasks stay
outside ToppleCat evidence.

## Custody and information boundary {#information-boundary}

`toppleCatSeal` moves reviewer-controlled source into local plaintext custody
and records a content-based seal over the complete executable contract and
verification policy. This detects contract changes. It is not encryption,
hostile-process isolation, or an operating-system security boundary.

The Implementation Agent receives the public contract and safe Gate-level
feedback. The Reviewer keeps Spec Review, Verification Report, private examples,
counterexamples, producer diagnostics, and raw failures. A public site may show
clearly labelled synthetic demonstrations, never material from an actual
delivery.

For exact product terms, use the [Glossary](glossary.md#executable-contract).
For the human responsibility boundary, read
[When ToppleCat is useful](product-definition.md#responsibility-boundary).
