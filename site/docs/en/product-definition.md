---
title: What ToppleCat does
description: Decide whether ToppleCat fits a Java team that delegates implementation to AI but keeps acceptance with a human.
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

# What ToppleCat does

ToppleCat is for a specific moment: a team gives a selected feature to an AI
coding agent, the agent says it is done, and a person still has to decide
whether to accept the delivery.

Without a separate verification step, that decision often rests on the same
public examples the agent used while coding. ToppleCat keeps those examples,
adds independent checks, and produces fresh evidence for the person making the
decision.

## When it is useful {#use-moment}

Imagine that a product owner defines a coupon rule and a developer turns it
into public Java/JUnit acceptance work. Before handoff, the responsible person
reviews the rule, examples, and additional checks that will run. After the
agent's done claim, ToppleCat verifies that sealed agreement.

The Reviewer does not have to be a Java expert. A developer or coding agent can
prepare the public integration. The Reviewer can be the developer, product
owner, tester, or another accountable person who understands the expected
business result and can read the plain-language report.

ToppleCat is most useful when all of these are true:

- the project uses Java and JUnit;
- an AI agent implements selected work;
- people can state observable rules and examples before handoff; and
- the team wants current evidence before accepting or merging the result.

## What changes in the workflow

Before implementation, the human-selected rules become an executable contract.
The Reviewer confirms what will be checked and seals the complete contract and
verification policy.

The agent works only with the public project and ordinary test feedback. After
the agent says done, formal Verify runs the sealed contract and every enabled
independent safeguard. The Reviewer reads the result and decides what happens
next.

ToppleCat records `PASS` only when every required Gate passes in that run. It
does not turn `PASS` into an automatic approval.

## Responsibility boundary {#responsibility-boundary}

| ToppleCat is responsible for | People, teams, projects, or external workflows are responsible for |
| --- | --- |
| Binding selected rules to ordinary executable acceptance work | Choosing the Spec and making its rules and examples complete |
| Detecting changes to the complete contract and verification policy | Deciding who reviews, approves, or signs off |
| Running fresh formal verification and keeping the checks independent | Deciding whether commands run locally, in CI, or elsewhere |
| Writing private Reviewer reports and safe agent feedback | Managing tasks, delivery history, pull requests, and releases |
| Using the fixed managed mutation profile and exact method attribution | Ordinary unit/QA tests, custom PIT, performance, and security work |

This division is deliberate. ToppleCat reports what the checked contract and
current evidence support. It does not infer intent from missing requirements.

## What ToppleCat does not own {#what-topplecat-does-not-own}

ToppleCat is not a task manager, Spec manager, approval system, CI service,
general test framework, or security sandbox. Its local reviewer custody is a
mechanical handoff safeguard, not encryption.

It also cannot answer the most important upstream question: “Did we write every
business rule that matters?” That remains a human responsibility.

If this fits your use moment, [run the executable sample](getting-started.md#sample-workflow).
If you need the technical trust boundaries, read
[How ToppleCat works](architecture.md#information-boundary).
