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

This page answers a practical question: what can you rely on in the current
release? For the full historical record, use the repository's
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

Spec Review and Verification Report are private, self-contained HTML bundles.
English is the default. Add `--language zh-TW` to the Review or Verify command
when the Reviewer wants ToppleCat-owned interface text in Traditional Chinese.
Authored business prose, IDs, values, and producer outcomes remain unchanged.

A Reviewer can request a faster formal report for selected Spec files or
repeated AC IDs. The report labels that scope, and a scoped `PASS` does not
claim that the complete project passed.

## Documentation surface {#documentation-surface}

The public documentation is available in English and human-authored Traditional
Chinese. Learning pages lead with the delivery problem and executable sample;
reference pages retain exact commands, terms, and boundaries.

Every page has a same-topic Markdown resource for people who want to give the
content to an AI. These small page resources and language manifests are a
reading convenience, not a docs API or a whole-site `llms-full` bundle.

## Limits and upgrade notes {#upgrade-notes}

- ToppleCat 0.1.0 is current-only. Reviewer custody and its Mechanical Seal are
  not migrated from another schema version; restore, Check, Review, and Reseal
  the contract for the current release.
- Formal Mutation Testing uses ToppleCat's fixed managed PIT profile. A
  project's custom PIT task remains separate.
- Selected Verify accepts Spec files or AC IDs, never both together. Sealing
  always covers the complete contract.
- Reviewer custody is plaintext mechanical storage, not encryption or process
  isolation.
- `PASS` is evidence for the checked scope. It does not prove that every
  business rule was specified or grant organizational approval.

To try the supported path, [run the executable sample](getting-started.md#sample-workflow).
To understand a result, read [Verify a delivery](verification-and-evidence.md#gates-and-verdicts).
