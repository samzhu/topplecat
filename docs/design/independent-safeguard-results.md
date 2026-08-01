# Independent safeguard results

**Status:** Implemented

**Date:** 2026-07-30

**Target:** ToppleCat 0.0.8

## User example

An implementation returns the approved total for the one public cart but
hard-codes that answer. The public acceptance row passes, a reviewer-owned cart
fails, a Property finds a counterexample, and Mutation Testing leaves a mutant
alive. A useful verification result shows all three independent findings from
that one run; it does not stop after the first failed safeguard.

## Problem

In 0.0.7, a failed JUnit-like verification task could stop Gradle before later
enabled safeguards produced their own current-run evidence. That made a real
finding appear to be the only result and concealed whether the other safeguards
ran, failed, or were interrupted. Verify also scheduled the public Seal task as
part of its preparation, which obscured that it reused an existing approval
rather than conducting a new review or approval.

## Decision and product boundaries

`CONTRACT_INTEGRITY` is the sole execution precondition. When it is `PASS`,
Verify runs enabled safeguards in this fixed order: public acceptance, hidden
typed-row retest, Property-Based Testing, Mutation Testing, expected-value
consumption aggregation, evidence/report/safe feedback, and reviewer-source
rehide. A `FAIL` is a completed safeguard that found a problem and never
prevents a later safeguard from running. An `INCOMPLETE` has no trustworthy
current-run result and also never borrows an earlier artifact.

When contract integrity is `FAIL` or `INCOMPLETE`, downstream safeguards do not
run and are recorded `INCOMPLETE`. This is the only permitted short circuit.
The fixed evidence gate names and order remain unchanged. This adds no
configuration switch, annotation, DSL, command-line input, test type, task
manager, approval workflow, or security boundary.

Verify reuses an existing Mechanical Seal through an internal custody check. It
requires an existing hidden reviewer custody state and approval, records that
the existing seal is reused and approval is not updated, and tells the user to
run `toppleCatSeal` if none exists. Only the existing public `toppleCatSeal`
and `toppleCatReseal` workflows create or replace approval.

Before that custody comparison, Verify always runs the current public Check to
rebuild the compiler definition. Integrity therefore compares the active seal
with the source closure and Property declarations that exist now, not a
definition left by an earlier Check.

The Mutation producer is part of the Mutation Testing safeguard, not a
preflight exception. In a formal Verify graph it runs only after contract
integrity and every earlier enabled safeguard, including Property-Based
Testing. If it interrupts, the report records Mutation as `INCOMPLETE`, writes
safe feedback, and re-hides reviewer source before Gradle returns failure.
Formal Verify also disables incremental and cached reuse for that producer: its
PIT report is current-run evidence only, even when a consumer declared the
report as a Gradle output. Run preparation removes the configured report before
Gradle evaluates any producer `onlyIf` rule, so a skipped producer also leaves
Mutation `INCOMPLETE` instead of supplying a prior report.

## Visible interface and behavior

During `toppleCatVerify`, JUnit-like gate tasks retain their direct diagnostic
failure behavior when users invoke them alone. In a formal Verify graph they
write XML, sidecars, and completion markers even when assertions fail, and
return control so later enabled safeguards execute. Mutation assessment writes
its complete result before the aggregate failure exit. `toppleCatReport` waits
for every enabled producer or its explicit interruption, writes evidence,
reports, and safe feedback, then emits the single aggregate Gradle failure.
Rehide remains a finalizer for every result path.

Property and Mutation feedback distinguishes a discovered counterexample or
surviving mutant (`FAIL`) from an incomplete task, missing current-run artifact,
or malformed artifact (`INCOMPLETE`). No gate may reuse stable or archived
evidence to turn either condition into a pass.

`ToppleCase.verify()` keeps its existing truthful accounting. A value compared
before its assertion fails is `ASSERTED`; values after that thrown assertion are
`UNTOUCHED`. Authors should verify one full receipt projection where practical.
When values must be independent, JUnit `assertAll` gives every assertion a
chance to call `verify`; ToppleCat does not add a `verifyAll` API.

## Failure and integrity rules

The report treats unreadable or missing JUnit XML, runtime narrative and
expected-consumption sidecars, Property events, mutation results, or completion
markers as current-run `INCOMPLETE`, with safe generic reasons. A usable
Property counterexample or mutation report with a surviving mutant is `FAIL`.
Every formal run starts by discarding an unarchived active workspace, so old
markers and results cannot survive an archive interruption. Expected
Consumption may fail alongside JUnit without duplicating a business finding: an
assertion can stop later expected values from being checked, which is precisely
what the consumption gate reports.

No hidden case identifier, value, source name/path, Property choice, replay
token, or assertion text crosses into `agent-feedback.json`.

## Acceptance evidence

Functional coverage exercises public, hidden, Property, and mutation failures
in the same run; public JUnit failure followed by later safeguards; interrupted
or malformed mutation evidence; skipped producers, abandoned workspaces, and
missing narrative sidecars; contract tampering that blocks all downstream
tasks; report/feedback/rehide completion on failure; and direct task failure
outside formal Verify. Unit coverage keeps failed `verify()` keys `ASSERTED`
and later keys `UNTOUCHED`, while an `assertAll` example records all attempted
keys.

## Consequences and alternatives

The aggregate failure appears later, after evidence is complete, so a Verify
run may take longer after its first failure. That is deliberate: independent
safeguards answer different questions and their evidence cannot substitute for
one another. Stopping on the first failure is rejected because it leaves later
enabled safeguards without a result. Continuing after integrity failure is
rejected because it would run an untrusted contract.
