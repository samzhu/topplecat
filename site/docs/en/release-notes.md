---
title: What's in 0.1.0
description: Check the Java requirements, supported verification workflow, reports, and limits in the current ToppleCat release.
page_id: release-notes
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/release-notes/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: release-notes/
alternate_zh_tw: zh-TW/release-notes/
markdown_url: release-notes.md
copy_label: Copy Markdown
copied_label: Copied
---

# What's in ToppleCat 0.1.0

If ToppleCat is new to you, start with [What is ToppleCat?](index.md#documentation-home).
This page is for people preparing an adoption or upgrade: it lists the current
capabilities, environment requirements, and limits.

For the full historical record, use the repository's
[0.1.0 release notes](https://github.com/samzhu/topplecat/blob/main/docs/releases/0.1.0.md).

## Current release {#current-release}

ToppleCat 0.1.0 supports Java 25, JUnit 6.1.1, and a compatible Gradle version.
It is published as a Gradle plugin plus a JUnit library for consumer projects.

In this release a team can:

- bind selected rules to public Java/JUnit Acceptance Methods and typed JSON or
  YAML case rows;
- review and mechanically seal the complete executable contract before an AI
  implementation handoff;
- run fresh Public Acceptance, reviewer-controlled examples, expected-value
  checking, Property-Based Testing, and managed Mutation Testing; and
- read an AC-first private Verification Report plus machine-readable evidence
  and safe implementation-agent feedback.

The normal CI command is `./gradlew toppleCatVerify` without a Spec or AC
selection. It verifies the complete contract and records `PASS` only when every
required Gate passes in that run.

## Reports and languages

ToppleCat creates two private reports inside your project. Spec Review lets a
person confirm what will be checked. Verification Report explains the result
of the current run after the AI finishes. Both are HTML pages that work
offline; their content is not sent to this public website or an external
service.

Headings, buttons, and ToppleCat explanations are in English by default. Add
`--language zh-TW` to the Review or Verify command to show that interface text
in Traditional Chinese. Your business rules, case IDs, inputs, expected values,
and outcomes recorded by external test tools are never translated or rewritten.

For a faster check of a just-finished feature, select one or more Spec files or
list one or more AC IDs; do not combine the two forms. The report shows exactly
which rules ran. A scoped `PASS` means only those listed rules passed, not the
whole project. CI should use the complete verification command without a scope.

## Documentation surface {#documentation-surface}

The public documentation is available in English and human-authored Traditional
Chinese. Learning pages lead with the delivery problem and executable sample;
reference pages retain exact commands, terms, and boundaries.

Every page has **Copy Markdown** so you can give that page to an AI for an
explanation or public setup help. The site does not translate pages at runtime
or make private reports available to an AI.

## Limits and upgrade notes {#upgrade-notes}

- ToppleCat 0.1.0 is current-only. Reviewer custody and its Mechanical Seal are
  not migrated from another schema version; restore, Check, Review, and Reseal
  the contract for the current release.
- Formal Mutation Testing uses ToppleCat's fixed managed PIT profile. A
  project's custom PIT task remains separate.
- Scoped verification accepts Spec files or AC IDs, never both together.
  Sealing always covers the complete contract.
- Reviewer material is kept in local plaintext storage. This is not encryption
  or process isolation.
- `PASS` is evidence for the checked scope. It does not prove that every
  business rule was specified or grant organizational approval.

To try the supported path, [run the executable sample](getting-started.md#sample-workflow).
To understand a result, read
[How ToppleCat verifies a delivery](verification-and-evidence.md#gates-and-verdicts).
