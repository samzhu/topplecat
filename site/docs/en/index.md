---
title: Documentation home
description: See how ToppleCat checks an AI coding agent's done claim and gives a human fresh evidence before accepting a Java delivery.
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

# Make an AI delivery earn its PASS {#documentation-home}

An AI coding agent says the change is done. The public tests are green. What do
you actually know?

You know that the examples you wrote passed. You do not yet know whether the
agent implemented the rule or merely found a shortcut through those examples.
ToppleCat gives the human responsible for the delivery a repeatable way to
check the difference. It locks the agreed Java/JUnit acceptance work, runs
fresh independent checks after the done claim, and explains why the delivery
did or did not earn `PASS`.

## Start here {#start-here}

Suppose a checkout rule says a coupon subtracts 100 from the order total. A
public example proves that one checkout worked. A narrow implementation could
recognize only that exact input and still make the test green.

The executable sample in this repository demonstrates that failure on purpose.
ToppleCat reruns the public rule, adds independently chosen reviewer checks, and
asks whether the public acceptance work notices temporary changes to production
behaviour. The narrow implementation is rejected; the corrected implementation
earns a fresh `PASS`.

Choose the route that matches what you need:

- **Quick proof:** [run the sample and watch ToppleCat reject a bad
  delivery](getting-started.md#sample-workflow).
- **Adopt it in a project:** [turn your own rules into executable
  checks](authoring-contracts.md#contract-example), then [verify the agent's
  delivery](verification-and-evidence.md#delivery-example).

The business rules still come from people. ToppleCat can test the rules that
were written down; it cannot discover a discount, exception, or approval policy
that nobody specified.

## Choose your task {#choose-your-task}

| What you need | Where to go |
| --- | --- |
| See the product catch a convincing but wrong implementation | [Getting started](getting-started.md) |
| Tell ToppleCat what “correct” means for my feature | [Turn rules into checks](authoring-contracts.md) |
| Decide what a `PASS`, `FAIL`, or incomplete run tells me | [Verify a delivery](verification-and-evidence.md) |
| Fix a setup or verification problem | [Troubleshooting](troubleshooting.md) |
| Decide whether ToppleCat fits our workflow | [What ToppleCat does](product-definition.md) |
| Understand the trust and information boundaries | [How ToppleCat works](architecture.md) |
| Look up an exact ToppleCat term | [Glossary](glossary.md) |
| Check what version 0.1.0 supports | [What's in 0.1.0](release-notes.md) |

## Let an AI help you read

Every page has a **Copy Markdown** button. Give that exact page to an AI and ask
it to explain the idea in your domain, point your developer to the relevant
commands, or carry out the public setup in a Java project. The Markdown is the
same human-authored content shown on the page; it is not an automatic
translation or a hidden API.

Keep two decisions with a person: whether the business rules are complete, and
whether the evidence is sufficient to accept the delivery.

## About these docs

This site publishes current English and Traditional Chinese guidance. Actual
delivery reports and reviewer-controlled material stay private. For the exact
ownership boundary, read [What ToppleCat does](product-definition.md#responsibility-boundary).

For the project story and the open-source source tree, return to the
[ToppleCat project page](/) or [GitHub](https://github.com/samzhu/topplecat).
