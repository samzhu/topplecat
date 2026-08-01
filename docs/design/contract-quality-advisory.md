# Contract quality advisory

**Status:** Implemented

**Date:** 2026-07-31

**Target:** ToppleCat 0.0.9

## User example

A public order receipt has two approved output shapes: one includes a shipping
breakdown and another does not. A reviewer-owned row may use either approved
shape without a warning. A hidden row that introduces an unreviewed expected
path is worth flagging for reviewer attention, but it is not evidence that the
business rule is wrong and it must not block the delivery.

## Problem

ToppleCat preserves public and reviewer rows, but it gives reviewers no concise
signal when hidden expected outputs differ structurally from every public
example or when a likely opaque identifier is exposed as a distinct literal in
both public and hidden data. Such a signal must not make ToppleCat infer a
missing business requirement, alter the executable contract, or disclose a
reviewer-only value.

## Decision and product boundaries

ToppleCat adds a shared core analyzer for two reviewer-only, non-blocking
advisories. `EXPECTED_SHAPE_VARIANT_MISSING` compares each hidden row's
recursive expected-field-path shape against all public shape variants for the
same AC. Object-key order, scalar values, and scalar types are ignored; an
array is a terminal field. Any public variant accepts the hidden shape.

`EXPECTED_OPAQUE_IDENTIFIER_LITERALS` considers expected field names that end
exactly in `Id`, `Key`, or `Token`. It warns only when the same expected path
has at least two public and two hidden non-empty string values and every value
across both visibility groups is distinct. It never guesses business meaning,
does not inspect array contents, and ignores blank, duplicate, non-string, and
non-suffix candidates.

Each advisory has only rule code, AC ID, expected path, public count, and
hidden count, sorted deterministically. It includes no value, case ID, source
path, or failure. Advisories are computed from public/reviewer rows by the
shared core analyzer. A direct `toppleCatCheck` logs reviewer warnings; the
internal Check executed as part of `toppleCatVerify` suppresses that output.
Spec Review displays the advisories and its schema is bumped.

Advisories are neither contract fields nor executable contract input. They do
not enter the ContractDefinition, approval, Seal digest, Verify evidence,
public handoff or `agent-feedback.json`; they never alter any
Gate or aggregate verdict. This adds no configuration API, source language,
CLI, workflow, or hidden business rule.

## Visible interface and behavior

The reviewer sees a stable sorted advisory list in direct Check output and in
Spec Review. A no-warning review has an empty list. A Verify-created
reviewer definition may calculate no advisory output because Verify is an
evidence run, not a reviewer authoring checkpoint.

## Failure and integrity rules

The analyzer treats only well-formed typed rows supplied by the existing case
reader. It has no failure mode that changes Check, Seal, Verify, or a gate
verdict. Its output must be excluded from public and integrity-bearing
artifacts, so a warning cannot add a reviewer-only byte to a public handoff or
Mechanical Seal.

## Acceptance evidence

Tests cover same-shape different-value rows, accepted public variants, shape
drift, nested paths, array terminals, identifier minimum counts, duplicate,
blank, non-string, and non-suffix exclusions, deterministic ordering, direct
Check output, Verify suppression, review schema output, and leakage checks for
every prohibited artifact.

## Consequences and alternatives

Reviewers receive useful prompts without treating heuristic patterns as failed
business behavior. Blocking on an advisory is rejected because only people can
decide whether the selected rules are complete. Emitting concrete values or
case IDs is rejected because it would cross the reviewer-information boundary.
