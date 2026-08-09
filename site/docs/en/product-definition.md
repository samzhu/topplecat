---
title: Product definition
description: Understand ToppleCat's audience, use moments, promise, and responsibility boundary.
page_id: product-definition
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/product-definition/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: product-definition/
alternate_zh_tw: zh-TW/product-definition/
markdown_url: product-definition.md
copy_label: Copy Markdown
copied_label: Copied
---

# Product definition

## Core use moment {#use-moment}

ToppleCat serves Java/JUnit teams that delegate implementation to an AI coding
agent while a human remains accountable for acceptance. Before handoff, the
Reviewer reads the complete selected Spec and the executable contract that will
run. After the agent's done claim, formal Verify produces fresh evidence so the
human can make the delivery decision.

## Promise

AI accelerates implementation; humans strengthen verification. ToppleCat binds
human-selected Acceptance Conditions to ordinary Java/JUnit Acceptance Methods
and typed case rows, seals the complete contract and policy, and records a
current-run `PASS` only when every required Gate passes.

Neither `PASS` nor `FAIL` proves that the upstream Spec has no missing rule.
ToppleCat reports what its checked contract and current evidence support.

## Audience and ownership

The Reviewer reads Spec Review and Verification Report. The Implementation Agent
receives the public contract and safe Gate-level feedback. An External Workflow
chooses the current Spec, decides when and where commands run, manages delivery
history, and applies organizational policy.

## Responsibility boundary {#responsibility-boundary}

| ToppleCat owns | Humans, teams, projects, or External Workflows own |
| --- | --- |
| Binding selected ACs to ordinary executable acceptance work | Selecting the Spec and making rules and cases complete |
| Sealing contract bytes and verification policy | Organizational review, approval, delivery history, and sign-off |
| Fresh formal verification and independent Gate evidence | Where commands run and how CI or PR policy is applied |
| Reviewer reports and safe Implementation Agent feedback | Task management, Spec lifecycle, and project release decisions |
| The managed mutation profile and exact AC attribution | Ordinary QA, custom PIT, performance, and security programs |

## What ToppleCat does not own {#what-topplecat-does-not-own}

ToppleCat is not a task manager, Spec lifecycle manager, approval system, CI
product, general test framework, Javadoc catalogue, or operating-system security
boundary. Public documentation explains the current product but is not a new
authority over the Executable Contract.

See [Architecture](architecture.md#four-modules) for the implemented modules and
[Glossary](glossary.md#executable-contract) for the formal vocabulary.
