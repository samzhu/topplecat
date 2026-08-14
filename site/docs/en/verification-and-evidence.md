---
title: How ToppleCat verifies a delivery
description: Learn how ToppleCat rechecks AI-delivered software and what PASS, FAIL, or insufficient evidence means.
page_id: verification-and-evidence
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/verification-and-evidence/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: verification-and-evidence/
alternate_zh_tw: zh-TW/verification-and-evidence/
markdown_url: verification-and-evidence.md
copy_label: Copy Markdown
copied_label: Copied
---

# How ToppleCat verifies a delivery

ToppleCat does not collapse acceptance into one vague score. It separates the
questions: Did the public examples pass? Does another legal case fail? Were the
expected results actually compared? Can the original acceptance work notice a
temporary change to the program?

Each question gets its own current result. The delivery earns `PASS` only when
every required check has trustworthy evidence and passes.

## What happens to the checkout delivery {#delivery-example}

The public coupon examples pass, so the agent's implementation looks plausible.
ToppleCat then runs reviewer-chosen examples through the same public Acceptance
Method. It also makes temporary changes to production behaviour and asks
whether that unchanged public method notices.

If the method still passes after the discount boundary is temporarily changed,
ToppleCat has found a weakness in the acceptance work for that rule. It has not
claimed that the original program already contained that temporary change. The
Verification Report states what happened, which rule the observation belongs
to, and why the current run cannot earn `PASS`.

## Questions the report answers

The report keeps the checks separate because they answer different questions:

| Question for this delivery | What a problem means | Gate name |
| --- | --- | --- |
| Is this still the contract the Reviewer sealed? | Public acceptance work or verification policy changed after review | `CONTRACT_INTEGRITY` |
| Did the public examples pass? | The implementation disagreed with an example the agent could see | `JUNIT` |
| Did independently chosen examples pass the same method? | The implementation failed a reviewer-controlled boundary | `REVIEWER_JUNIT` |
| Were the authored expected results actually asserted? | The test read or skipped a result instead of checking it | `EXPECTED_CONSUMPTION` |
| Did approved invariants hold over generated inputs? | A counterexample was found, or trustworthy Property evidence was not completed | `PROPERTY` |
| Did each public method notice temporary production changes attributed to it? | The acceptance work was insensitive to a relevant change, or no trustworthy baseline existed | `MUTATION` |

One result cannot cover for another. Passing reviewer examples do not repair a
Property failure. A Property does not prove that the public method detects a
temporary code change.

## Run the workflow

Before handoff, the Reviewer checks what will be executed and seals the complete
contract:

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
```

Check requires each selected Markdown Spec to use a visible AC heading with a
business title and one exact standalone `<!-- topplecat:acceptance -->` marker.
Missing selection is rejected before the dependent Check starts. Structural
heading/marker errors are reported by Check after a path is selected, and a
failed Check produces no Review report.
It reads, hashes, and validates the document once, then writes the checked
projection consumed by Review. Missing, duplicate, orphaned, or misplaced
declarations and markers fail during that Check read/parse with a repairable
`TC-SPEC-AC-*` diagnostic; Review is not produced. Prose references do not
select an AC, and `.feature` files are not read or translated.

The agent implements with ordinary `./gradlew test` feedback. After its done
claim, run:

```bash
./gradlew test
./gradlew toppleCatVerify
```

The Reviewer reads
`build/topplecat/reports/verification/index.html`. Automation reads
`build/topplecat/evidence.json`. Both describe this run; an earlier report
cannot fill a gap in the current evidence.

## From observation to verdict {#three-evidence-layers}

When a result needs technical investigation, read it in three layers:

1. The external tool records what it observed. JUnit, the Property engine, and
   PIT keep their own official outcome names.
2. ToppleCat connects that observation to the exact acceptance method, case,
   Property, or sealed policy responsible for the question.
3. The sealed policy turns that attributed evidence into a Gate result and then
   an aggregate verdict.

This separation stops a producer message from being mistaken for a business
conclusion. Generated JSON and HTML report the checked contract and observed
outcomes; they do not add new rules.

## Gates and verdicts {#gates-and-verdicts}

The overall result is deliberately small:

- `PASS`: every required Gate passed in this run.
- `FAIL`: a completed check found a blocking problem.
- `INCOMPLETE`: ToppleCat could not obtain enough trustworthy current-run
  evidence.

An individual check may also be explicitly disabled or not applicable. Neither
is silently presented as a pass.

The normal CI command verifies the complete contract. A Reviewer can request a
faster report for selected Spec files or AC IDs, but not both at once. A scoped
`PASS` says only that the named scope passed; it does not claim that the whole
project passed.

## Reviewer boundary {#reviewer-boundary}

Spec Review and Verification Report are private reading surfaces for the
Reviewer. The implementation agent receives safe Gate-level feedback that says
what kind of work needs attention without exposing reviewer examples, values,
paths, counterexamples, or raw private failures.

An AI can summarize the public documentation or help fix the public
implementation. The human Reviewer keeps the private report and decides whether
the evidence is enough to accept the delivery.

If a result is unexpected, start with [Troubleshooting](troubleshooting.md#symptom-map).
For the trust and information flow, read [From rules to results](architecture.md#execution-flow).
