---
title: Documentation home
description: Start with ToppleCat's task-oriented documentation for executable acceptance contracts and fresh verification evidence.
page_id: home
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: ./
alternate_zh_tw: zh-TW/
markdown_url: index.md
copy_label: Copy Markdown
copied_label: Copied
---

# Documentation home {#documentation-home}

ToppleCat is a delegation-verification gate for Java/JUnit projects. These
pages explain the supported workflow from a human-selected Spec to a fresh
Verification Report. They are a public explanation surface; the executable
contract remains ordinary Java/JUnit Acceptance Methods and typed JSON or YAML
case rows.

## Start here {#start-here}

Imagine a checkout rule that says a 1,000-dollar order receives a 100-dollar
discount. A public example can check `1,000 -> 900`, but that one example cannot
show that every legal checkout follows the rule. ToppleCat keeps that human-
authored rule fixed, then runs independent checks after an implementation agent
claims to be done.

1. Read [Getting started](getting-started.md#contract-example) for a complete
   sample-backed path.
2. Use [Authoring contracts](authoring-contracts.md#acceptance-method) to bind
   Acceptance Conditions to executable Java and typed rows.
3. Run [Verification and evidence](verification-and-evidence.md#gates-and-verdicts)
   to interpret the current-run Gate verdict.

ToppleCat records `PASS` only when every required Gate passes in this run. A
green development test or a ToppleCat `PASS` is evidence for the checked
contract, not proof that the upstream Spec contains every business rule or that
a human must accept the delivery.

## Choose your task {#choose-your-task}

| You want to... | Read |
| --- | --- |
| Install ToppleCat and verify one delivery | [Getting started](getting-started.md) |
| Write Acceptance Methods and typed case rows | [Authoring contracts](authoring-contracts.md) |
| Interpret Gates and evidence | [Verification and evidence](verification-and-evidence.md) |
| Diagnose a visible symptom | [Troubleshooting](troubleshooting.md) |
| Understand ownership and use moments | [Product definition](product-definition.md) |
| Understand modules and information flow | [Architecture](architecture.md) |
| Look up formal project vocabulary | [Glossary](glossary.md) |
| Read the current supported release | [Current release notes](release-notes.md) |

## Public boundary

The site publishes only current, human-authored technical pages in English and
Traditional Chinese. It does not publish Javadoc, actual delivery reports,
reviewer-owned values, private repository workspaces, gated run notes, or task-
coordination records, and it has no `llms-full` bundle. The two small
language-specific Markdown manifests are an experimental convenience; ordinary
HTML navigation and the sitemap remain the discovery path.

For the project story and the open-source source tree, return to the
[ToppleCat project page](/) or [GitHub](https://github.com/samzhu/topplecat).
