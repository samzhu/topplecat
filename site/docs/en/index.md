---
title: "What is ToppleCat?"
description: "Meet ToppleCat from the beginning: why it rechecks AI-delivered Java, what it tests, and which decisions still belong to people."
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

# What is ToppleCat? {#documentation-home}

ToppleCat is an open-source verification tool for Java/JUnit projects. When an
AI coding agent says a feature is finished, ToppleCat does not take that claim
at face value. It rechecks the delivery against business rules that a person
confirmed beforehand and records what happened in this run.

It is not another AI that writes code. Think of it as the acceptance check at
the end of an AI delivery: it gives the result marked `PASS` a careful push to
see whether it stands up or only looked convincing against the public examples.

## The problem it addresses {#problem}

Suppose the rule says that coupon `SAVE100` subtracts 100 at checkout. The AI
finishes the feature, and the public test confirms `1,000 → 900`. That looks
done, but the implementation might simply recognize those numbers. A different
legal order could still fail.

Ordinary tests remain important. The difficulty is that the AI saw the public
rules and examples while it was coding. Reusing only that same material at
acceptance time cannot tell you whether the rule was implemented or the known
answers were merely satisfied.

ToppleCat keeps the public acceptance work and challenges the delivery from
several independent angles. When it finds a problem, the report identifies the
rule and the kind of check that stopped the run. The delivery earns `PASS` only
when every required check has trustworthy current results and passes.

## What ToppleCat checks {#checks}

Start with the question each check answers. The right column gives the exact
name used in the documentation and reports.

| What you want to know | ToppleCat check |
| --- | --- |
| Do the public examples that the AI could see still pass? | Public Acceptance |
| Does another legal example, chosen outside the AI handoff, pass the same rule? | Hidden Tests |
| Did the test compare the expected result, rather than merely read it? | Expected Result Check |
| Can bounded generated inputs find a counterexample to an approved rule? | Property-Based Testing |
| Does the original acceptance method notice a temporary change in program behaviour? | Mutation Testing |
| Did the accepted rules or verification settings change after review? | Contract Integrity |

These checks stay independent. Trying another example, generating many inputs,
and temporarily changing code expose different failure modes. One passing
result cannot replace missing evidence from another.

## How one delivery flows {#start-here}

```text
people state what correct means
    → review the prepared rules, examples, and additional checks
    → the AI implements from public information and uses ordinary tests
    → ToppleCat reruns the work and adds independent checks
    → a person reads the current result and decides whether to accept it
```

ToppleCat calls the executable form of “what correct means” the Executable
Contract. The name is formal; the content is ordinary Java/JUnit methods plus
JSON or YAML examples with inputs and expected results.

A person reviews that contract before implementation. After the AI finishes,
ToppleCat runs the same public agreement again rather than introducing a second
set of public rules. See [From rules to results](architecture.md#execution-flow)
for the complete flow.

## The two reports you will see {#reports}

Both reports stay inside your project. They are not published on this website
and are not given to the implementation AI.

### Before implementation: Spec Review

This page lets the person responsible for acceptance answer: Which business
rules did we select? Which public and additional examples are prepared? What
will actually run later? It is review material, not an execution result.

### After the AI says done: Verification Report

This page starts with whether the current run passed, found a problem, or lacked
enough evidence. It then shows each rule and each check. Inputs,
expected-versus-actual differences, and deeper technical evidence are available
when someone needs to investigate.

`PASS` means every required check passed for the selected scope in this run. It
does not mean ToppleCat approved the delivery for your organization or proved
that nobody omitted a business rule.

## Who it is for {#audience}

ToppleCat fits teams that use Java/JUnit, delegate some implementation to AI,
and keep the acceptance decision with a person.

The report reader does not have to write Java. They may be a developer, product
owner, tester, or another person who understands the expected business result
and is accountable for the delivery. A developer or AI can help with technical
setup; people still decide whether the rules are complete and the evidence is
enough.

If you are evaluating the fit, read [When ToppleCat is useful](product-definition.md#use-moment).

## Five terms worth knowing {#terms}

- **Acceptance Condition:** one human-selected business rule with an observable
  result.
- **Executable Contract:** the Java/JUnit checks and case data that make those
  rules runnable.
- **Reviewer:** the person who reads the prepared rules and current report, then
  decides what happens to the delivery.
- **Independent Safeguard:** a check that answers one verification question and
  cannot be replaced by another result.
- **Current-run Evidence:** the result of this formal run; an older report cannot
  fill a gap in it.

You do not need to guess the other capitalized terms. Use the
[Glossary](glossary.md#executable-contract).

## Choose your next step {#choose-your-task}

| What you want to do | Next page |
| --- | --- |
| Add ToppleCat to an existing Java project | [Getting started](getting-started.md#ai-assisted-authoring) |
| Decide whether it fits the way your team works | [When ToppleCat is useful](product-definition.md) |
| Understand every check and the meaning of `PASS` or `FAIL` | [How ToppleCat verifies a delivery](verification-and-evidence.md) |
| Follow the full flow and private information boundaries | [From rules to results](architecture.md) |
| Connect your own business rules to Java/JUnit | [Turn rules into executable checks](authoring-contracts.md) |
| Fix an installation or verification problem | [Troubleshooting](troubleshooting.md) |
| Check the current version and environment requirements | [What's in ToppleCat 0.2.2](release-notes.md) |

## Let an AI help you read or install it {#ai-help}

Every page has a **Copy Markdown** button. Copy the page and give it to an AI.
You can ask it to:

- explain ToppleCat in your industry or business context;
- tell a developer what the project must prepare; or
- install the plugin in a Java project and create checks from public rules that
  a person has already confirmed.

Do not give the implementation AI private reports, reviewer-chosen cases, or
other private values. Do not ask it to guess business requirements that were
never written down.

To start using ToppleCat, read [Getting started](getting-started.md#ai-assisted-authoring).
For the project story, return to the [ToppleCat project page](/). Source code
and contributor material are on [GitHub](https://github.com/samzhu/topplecat).
