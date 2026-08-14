---
title: When ToppleCat is useful
description: Decide whether ToppleCat fits a team that lets AI implement Java features but keeps acceptance with a person.
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

# When ToppleCat is useful

ToppleCat fills a specific gap: a team lets AI write a Java feature but does
not want to accept the result using only tests that the AI already saw.

It keeps the human-confirmed rules fixed. After the AI says done, ToppleCat
reruns the public examples, adds independent checks, and gives the current
results to the person responsible for acceptance. The tool records evidence;
the person makes the decision.

## When it is useful {#use-moment}

Suppose a product owner defines a coupon rule and a developer or AI connects it
to Java/JUnit checks. Before implementation starts, the team reviews the rule,
examples, and additional checks that will run. After the AI says done,
ToppleCat verifies that same agreement.

ToppleCat is most useful when:

- the project uses Java and JUnit;
- an AI coding agent implements selected features;
- people can state observable rules and examples before implementation; and
- the team wants current evidence before accepting or merging the result.

The person responsible for acceptance does not have to write Java. They may be
a developer, product owner, tester, or another person who understands the
expected business result and is accountable for the delivery. A developer or
AI can handle the technical wiring.

## What changes in the workflow

### Before the AI starts

People select the rules for this delivery and turn the expected behaviour into
runnable checks. Spec Review requires a canonical Markdown Spec path, shows the
complete selected document, and inserts each bound acceptance card at its exact
marker. ToppleCat then records the content of the complete contract and its
verification settings.

### While the AI implements

The AI sees only public rules, public examples, and ordinary project tests. It
can keep using `./gradlew test` for fast feedback. Reviewer-controlled material
does not enter that handoff.

### After the AI says done

ToppleCat first confirms that the reviewed material did not change, then runs
the public and additional checks. Verification Report explains what happened
for each rule. The run records `PASS` only when every required check passes.

`PASS` is not an automatic approval. The responsible person still decides
whether the rules are complete and the evidence is enough.

## Responsibility boundary {#responsibility-boundary}

| ToppleCat is responsible for | People, teams, or the existing delivery process are responsible for |
| --- | --- |
| Connecting selected rules to runnable Java/JUnit acceptance work | Selecting the feature and making its rules and examples complete |
| Detecting changes to reviewed contract or verification settings | Deciding who reviews, approves, or signs off |
| Rerunning verification and keeping each check's result separate | Deciding whether it runs locally, in CI, or elsewhere |
| Producing private reports and AI feedback that omits reviewer material | Managing tasks, delivery history, pull requests, and releases |
| Running ToppleCat's fixed mutation checks | Ordinary unit/QA tests, performance, and security work |

## What ToppleCat will not do {#what-topplecat-does-not-own}

ToppleCat does not manage tasks or Spec versions, and it does not approve a
delivery for an organization. It is not a CI service, general test framework,
or security sandbox. Its local reviewer custody helps keep private material out
of the implementation handoff; it is not encryption.

Most importantly, it cannot answer: “Did we forget a business rule that
matters?” If the contract omits a VIP discount, refund exception, or regulatory
requirement, ToppleCat will not invent it.

If this matches your use case, [add ToppleCat to your Java project](getting-started.md#ai-assisted-authoring).
To see each check, read [How ToppleCat verifies a delivery](verification-and-evidence.md#delivery-example).
