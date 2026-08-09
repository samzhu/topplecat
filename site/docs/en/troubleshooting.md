---
title: Troubleshooting
description: Find the ToppleCat message that matches what you see, understand what it means, and take the safest next action.
page_id: troubleshooting
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/troubleshooting/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: troubleshooting/
alternate_zh_tw: zh-TW/troubleshooting/
markdown_url: troubleshooting.md
copy_label: Copy Markdown
copied_label: Copied
---

# Troubleshooting

## Symptom map {#symptom-map}

Start with what you can see. The report or command output should say what ran,
what happened, and why that supports the result. A bare `FAIL` or
`INCOMPLETE` is not enough to diagnose a delivery.

| What you see | Start here |
| --- | --- |
| `toppleCatCheck` rejects the Java method | [The Acceptance Method does not compile](#acceptance-method-does-not-compile) |
| A Spec rule or case row has no matching method | [A rule has no public binding](#missing-public-binding) |
| The public example disagrees with the implementation | [Public Acceptance fails](#public-acceptance) |
| A check says it could not be assessed | [Evidence is incomplete](#incomplete-evidence) |
| Reviewer examples or mutation evidence are missing | [An independent check has no evidence](#independent-check-missing) |
| The contract no longer matches its seal | [Contract Integrity fails](#contract-integrity-fails) |

## The Acceptance Method does not compile {#acceptance-method-does-not-compile}

This means ToppleCat cannot turn the selected rule into a trustworthy executable
contract yet. Read the first Check error; it normally identifies the method,
parameter, Stage, or Scenario call that broke the required shape.

Keep `ToppleCase` first, one `ToppleScenario` second, and distinct concrete
Stages after them. Stages must be non-final and have an accessible no-argument
constructor. Put setup, conditionals, service calls, and assertions inside
Stage methods.

Run `./gradlew toppleCatCheck` again. Do not seal or verify until Check can
describe the complete contract.

## A rule has no public binding {#missing-public-binding}

A selected Spec rule or case row names an AC ID that has no compilable public
`@ToppleAcceptanceTest` method. Correct the ID or add the missing method. A
reviewer-controlled case can exercise an existing rule; it cannot create a new
rule that the implementation agent never saw.

## Public Acceptance fails {#public-acceptance}

Open the failed public case in Verification Report. Read the input first, then
the expected and actual values. That tells you which authored example disagreed
with the implementation.

Fix the production code if the implementation is wrong. Change the contract
only if the human-authored rule or expected result was wrong. In either case,
the intended contract change must go back through Check, Review, and Seal.

Mutation Testing will be incomplete in this run because it needs a passing
public baseline. Reviewer examples and Properties are independent and may still
have useful current results.

## Evidence is incomplete {#incomplete-evidence}

`INCOMPLETE` means ToppleCat cannot support either pass or fail with trustworthy
evidence from this run. A task may have been interrupted, a current sidecar may
be missing, or Property events may not match the sealed declaration.

Read the reason beside the affected check, repair that cause, and run formal
Verify again. Archived output can help diagnose history, but it cannot supply
missing evidence to a new run.

## An independent check has no evidence {#independent-check-missing}

If reviewer examples are enabled, every selected rule needs an executed
reviewer-controlled case. Add the missing case, review the complete contract,
and reseal it. If the team intentionally does not use that check, change the
policy explicitly and reseal; another check cannot stand in for it.

If Mutation Testing has no usable result, confirm that Public Acceptance passed
first. Then read the managed producer reason. ToppleCat uses its own fixed PIT
profile and current-run report; a project PIT task or old report cannot replace
it.

## Contract Integrity fails {#contract-integrity-fails}

The public acceptance work, Gradle logic, semantic definition, or verification
policy no longer matches the Mechanical Seal. If the change was intended,
restore reviewer custody, run Check and Review, then reseal the complete
contract. If it was not intended, revert the contract change.

Verify never creates a missing seal or silently approves new contract bytes.

## Safe next action {#safe-next-action}

If the message is still unclear, give an AI the public error, this page's
Markdown, and the relevant public code. Ask it to explain what ran and propose a
public fix. Do not give it the private Verification Report or
reviewer-controlled values.

The human Reviewer should read the plain-language reason first and open
technical evidence only when needed. An unexplained status is a reporting
problem; do not guess what `FAIL` or `INCOMPLETE` means.
