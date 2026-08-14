---
title: What's in ToppleCat 0.2.2
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

# What's in ToppleCat 0.2.2

If ToppleCat is new to you, start with [What is ToppleCat?](index.md#documentation-home).
This page is for people preparing an adoption or upgrade: it lists the current
capabilities, environment requirements, and limits.

For the full historical record, use the repository's
[0.2.1 release notes](https://github.com/samzhu/topplecat/blob/main/docs/releases/0.2.1.md).

## Current release line {#current-release}

## Put each acceptance projection exactly where the business Spec says

Previously, a selected Markdown document coupled a visible heading to a generic
marker. Now one exact standalone marker, for example
`<!-- topplecat:acceptance:AC-CHECKOUT-001 -->`, is both the AC identity and
the Spec Review insertion point. A heading may use any wording or level, and
ordinary AC mentions stay prose. This removes accidental scope changes when a
team edits its business-document structure.

Each selected AC marker must be unique across the supplied repository-relative
`--spec` paths. Check reports every duplicate or malformed selected directive,
and Review inserts one complete checked projection per valid marker in source
order. Markers may be consecutive or appear before or after related prose.

## Upgrade {#upgrade-notes}

Projects using the 0.2.1 selected-Spec contract must replace every generic
`<!-- topplecat:acceptance -->` marker with an exact ID-bearing marker. Do not
depend on headings, marker proximity, ordinary references, or `.feature` files
to select scope. Pass the same exact relative `--spec` paths to Check, Review,
and scoped Verify.

## Requirements and limits

ToppleCat runs on JDK 21 or 25; consumer source may target Java 17, 21, or 25
when Gradle runs on a supported execution JDK. This release line is not yet in
Maven Central, so build it locally with `./gradlew publishToMavenLocal` and put
`mavenLocal()` before `mavenCentral()` until a maintainer completes that
separate publication.

Spec Review and Verification Report remain reviewer-only HTML surfaces. A
scoped `PASS` covers only its selected ACs; it does not prove the business Spec
is complete or grant organizational approval.

## Documentation surface {#documentation-surface}

The English and Traditional-Chinese documentation, samples, and acceptance
skill describe the same marker contract and local-Maven adoption boundary.

To adopt the current release line, [build 0.2.2 locally and add `mavenLocal()`](getting-started.md#ai-assisted-authoring).
