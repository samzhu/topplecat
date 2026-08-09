---
title: Troubleshooting
description: Diagnose ToppleCat symptoms by separating what ran, what happened, and what the evidence supports.
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

Start with the user-visible symptom, then identify the external observation,
ToppleCat attribution, Gate consequence, and safe next action. A status word by
itself is not a diagnosis.

## The Acceptance Method does not compile

**Observed:** `toppleCatCheck` reports a binding, parameter, Stage, or direct
Scenario-authoring problem.

**Attribution:** the selected AC is not bound to the required ordinary
Java/JUnit Acceptance Method shape, so no formal contract can be trusted yet.

**Gate consequence:** Contract Integrity cannot establish downstream evidence.

**Next action:** keep `ToppleCase` first, one non-generic `ToppleScenario` second,
then distinct non-final concrete Stages with accessible no-argument
constructors. Put setup and assertions inside Stage methods.

## A row or selected AC has no binding

**Observed:** a typed row or selected Spec AC names no compilable public
`@ToppleAcceptanceTest` method.

**Attribution:** the row cannot create a new rule; it must target an existing
public AC binding.

**Gate consequence:** Check fails before trustworthy formal evidence exists.

**Next action:** correct the literal AC ID, selected Spec or `--ac` input, or
add the missing public method. Humans still decide whether the rule itself is
complete.

## Public Acceptance fails {#public-acceptance}

**Observed:** the JUnit Acceptance Method compared an authored expected value
with an actual result and found a mismatch.

**Attribution:** the mismatch belongs to that public case and Acceptance
Method; it does not explain intent or unstated cases.

**Gate consequence:** the `JUNIT` Gate records the completed problem. Mutation
Testing is `INCOMPLETE` because it lacks a passing baseline, while independent
Hidden Tests and Properties may still report their own evidence.

**Next action:** inspect the public input and expected/actual comparison, then
fix the implementation or the human-authored contract as appropriate. Do not
replace the current run with an earlier artifact.

## Evidence is incomplete {#incomplete-evidence}

**Observed:** a safeguard did not produce trustworthy current-run evidence, for
example because a task was interrupted, its current sidecar was missing, or a
Property lifecycle did not match the sealed declaration.

**Attribution:** ToppleCat cannot honestly attribute a complete observation to
the current run. A previous archive is diagnostic only.

**Gate consequence:** that safeguard is `INCOMPLETE`; aggregate `PASS` is not
supported.

**Next action:** rerun the documented workflow from a clean current run. Check
the current task output and generated evidence, not an archived run.

## Hidden coverage or mutation evidence is missing

**Observed:** a selected AC has no executed hidden typed row, or the managed
mutation producer has no usable full matrix.

**Attribution:** Hidden Tests and Mutation Testing answer separate questions;
one cannot lend evidence to the other. Mutation Testing also requires a passing
Public Acceptance baseline.

**Gate consequence:** the relevant safeguard remains `INCOMPLETE` (or an
explicitly sealed `DISABLED` / `NOT_APPLICABLE` state), with its reason kept.

**Next action:** add the independently chosen hidden row and reseal, or repair
the supported formal workflow. Do not expose reviewer-owned source or values to
an Implementation Agent.

## Safe next action {#safe-next-action}

If the symptom remains unclear, read the Verification Report's plain-language
reason first, then the canonical technical evidence. Keep the three layers
separate: what an external producer observed, how ToppleCat attributed it, and
which Gate conclusion the current evidence supports.
