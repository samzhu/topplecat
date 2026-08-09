---
title: Verification and evidence
description: Run ToppleCat's formal verification and distinguish observations, contract attribution, and Gate verdicts.
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

# Verification and evidence

## One delivery example {#delivery-example}

Assume the checkout contract says a 1,000-dollar order receives a 100-dollar
discount. During formal Verify, the public Acceptance Method runs that authored
case and the enabled safeguards run their own work. If a managed mutation
changes the discount boundary and the same public method still passes, the
Mutation Gate has evidence that this AC did not distinguish that temporary
change. That is different from claiming that the original production program
already contains the mutation.

## Three evidence layers {#three-evidence-layers}

Read every result in three layers:

1. **External observation:** a JUnit task, a Property engine, or the managed PIT
   producer records what it observed and preserves its official outcome names.
2. **Contract attribution:** ToppleCat connects that observation to the exact
   public Acceptance Method, Typed Case Row, Property declaration, or sealed
   policy that owns the question.
3. **ToppleCat Gate verdict:** the sealed policy decides whether that safeguard
   is `PASS`, `FAIL`, `INCOMPLETE`, `DISABLED`, or `NOT_APPLICABLE`, then the
   aggregate run records `PASS`, `FAIL`, or `INCOMPLETE`.

Generated JSON and HTML only project checked contract material and producer
outcomes. They never add a rule, case, expected value, or scenario step.

## Run the formal workflow

Development feedback remains ordinary `./gradlew test`. The normal CI command
is:

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
./gradlew test
./gradlew toppleCatVerify
```

Verify normally covers the complete Executable Contract. A Reviewer can scope a
quick report with repeated `--spec` paths or repeated `--ac AC-...` values, but
not both. Sealing and integrity always cover the complete contract.

## Gates and verdicts {#gates-and-verdicts}

The formal Gate order is:

```text
CONTRACT_INTEGRITY
JUNIT
REVIEWER_JUNIT
EXPECTED_CONSUMPTION
PROPERTY
MUTATION
```

Hidden Tests, Property-Based Testing, and Mutation Testing are Independent
Safeguards. Hidden rows cannot substitute for a Property result; a Property
cannot supply mutation detection. Mutation Testing additionally needs a
passing Public Acceptance baseline, otherwise its result is `INCOMPLETE`.

`PASS` means every required Gate passed under the sealed policy in this current
run. It is evidence, not proof that the business rules are complete and not
organizational approval. A scoped `PASS` is explicitly limited to its selected
Delivery Scope.

## Reviewer boundary {#reviewer-boundary}

Spec Review and Verification Report are reviewer-only HTML surfaces. Safe
Implementation Agent feedback contains Gate-level reasons without reviewer
values, source names, paths, tokens, counterexamples, or raw private failures.
The public site may use clearly labelled synthetic demonstrations for education,
but this documentation publishes no actual delivery material.

Read [Troubleshooting](troubleshooting.md#symptom-map) when the visible result
is incomplete or unexpected. The [Architecture](architecture.md#execution-flow)
page explains where each piece of evidence is produced and retained.
