---
title: Current release notes
description: Current supported ToppleCat 0.1.0 behavior and the documentation surface that explains it.
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

# Current release notes

## ToppleCat 0.1.0 {#current-release}

ToppleCat 0.1.0 connects the work before and after an AI implementation
handoff. The Reviewer reads the selected Spec and executable contract before
handoff, then reads fresh AC-attributed evidence after the agent's done claim.
The delivery decision stays with the human.

The supported verification model includes Contract Integrity, Public Acceptance,
Hidden Tests, Expected Consumption, Property-Based Testing, and managed Mutation
Testing. Each safeguard keeps its own evidence and official producer outcomes.

## Documentation surface {#documentation-surface}

This release's official technical documentation is available in English and
Traditional Chinese. It is current-only, task-oriented, search-free, and built
as a static GitHub Pages artifact. Each page has a same-topic language pair and
page-level Markdown. The Markdown manifests are an experimental convenience,
not an API, crawler guarantee, or whole-site bundle.

## Upgrade notes {#upgrade-notes}

For the full historical note, read the repository's
[0.1.0 release notes](https://github.com/samzhu/topplecat/blob/main/docs/releases/0.1.0.md).
The important current rules are:

- Use `toppleCatVerify` without `--spec` or `--ac` for the complete CI run.
- Scoped Verify is a quick Reviewer report, not a substitute for full-project
  verification; do not combine `--spec` and `--ac`.
- Use ToppleCat's managed PIT profile for formal Mutation Testing; project PIT
  tasks remain outside ToppleCat evidence.
- Reviewer custody and the Mechanical Seal are current-version-only. Restore,
  Check, Review, and Reseal an old suite before formal Verify.
- A ToppleCat `PASS` means every required Gate passed in this run. It does not
  say that the Spec is complete or that a human has approved the delivery.

See [Getting started](getting-started.md#formal-verify) for the current command
sequence and [Verification and evidence](verification-and-evidence.md#gates-and-verdicts)
for result interpretation.
