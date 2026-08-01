# Mutation attribution and gate

**Status:** Implemented

**Date:** 2026-07-31

**Target:** ToppleCat 0.0.9

## User example

One checkout class contains two public Acceptance Methods: one accepts a coupon
and one rejects an empty cart. PIT may report that both methods execute a
mutant, while only the coupon method kills it. ToppleCat must show that the
coupon acceptance work detected the change and the empty-cart acceptance work
did not. It must not award the empty-cart AC credit for another AC's test.

## Problem

The prior mutation gate reduced PIT's full matrix to one per-AC score based on
`coveringTests` and converted `detected` into a synthetic `killed` outcome.
That lost the difference between execution and detection, could credit a
different Acceptance Method's result, and reinterpreted the producer's raw
outcome.

## Decision and product boundaries

ToppleCat parses the PIT full mutation matrix structurally and preserves each
producer `status`, `detected`, `coveringTests`, `killingTests`, and
`succeedingTests` value. A selector matches an AC only when the fully qualified
class, method name, overload, and every parameter type match its compiler
identity. The comparison accepts only whitespace around parameter commas.
`[test-template:...]` and `[method:...]` selectors are supported; class-only,
helper, wrong class, wrong method, and wrong signature selectors never match.

`coveringTests` decides which Acceptance Methods covered a mutant;
`killingTests` independently decides which of those methods detected it.
`succeedingTests` remains reviewer evidence for tests that ran and passed.
`NO_COVERAGE` with no covering selector is a valid unattributed PIT outcome.
An acceptance-looking selector that cannot be parsed reliably makes the
current mutation evidence `INCOMPLETE`.

The only mutation artifact is `topplecat.mutation-results.v1`. Its direct
fields record producer totals, unique attributed and unattributed mutant counts,
original `(status, detected)` outcome counts, and a result for each AC. A mutant
may be attributed to more than one AC, so the sum of per-AC counts need not
equal the unique count. There is no predecessor reader, migration, or dual
format boundary.

For each AC, the detection rate is the number of unique mutants whose
`killingTests` match that Acceptance Method divided by the number whose
`coveringTests` match it. This is a contract-scoped ToppleCat measure, not
PIT's global `mutationThreshold`. The sealed project threshold defaults to 100
and remains the only project-controlled threshold.

Mutation evidence is `INCOMPLETE` if the producer is absent or interrupted,
the report is missing, damaged, or not a complete full matrix, a required
selector cannot be parsed, or the producer creates zero mutants. With a usable
non-empty matrix, no exact public attribution is `FAIL`; unattributed mutants
beside some valid attribution remain reviewer evidence and do not themselves
fail the gate. An AC fails when it covered no mutants or its detection rate is
below the sealed threshold. The mutation gate passes only when every AC
passes.

This decision adds no mutation configuration API, task manager, CLI, approval
workflow, or compatibility reader. It keeps public reports and
`agent-feedback.json` at gate-level, generic remediation only. Reviewer-only
mutation results and Verification Report may show the detailed, unaltered
PIT summary and PIT's official outcome labels.

## Visible interface and behavior

`toppleCatMutationGate` writes a current-run reviewer artifact named
`mutation-results.json` in schema v1. `toppleCatVerify` uses that artifact for
the `MUTATION` Gate and adds it to reviewer evidence. The gate reports a
generic missing/incomplete reason, a generic no-public-attribution reason, or
a generic contract-detection failure; it does not leak counts, selectors,
classes, methods, case IDs, paths, or raw PIT failures to safe feedback.

## Failure and integrity rules

The producer matrix is current-run evidence. Formal Verify clears stale producer
and Gate output, then disables producer and Gate output reuse so the PIT report,
`mutation-results.json`, and completion marker are all from one run. A missing
or damaged report, a report with missing matrix fields, and an ambiguous
Acceptance Method selector cannot be treated as passing evidence. PIT outcome
strings, boolean flags, and raw test-selector relationships are never normalized
into a ToppleCat status-to-score mapping.

## Acceptance evidence

Core tests cover whitespace and tab normalization, template/method selectors,
overloads, primitives and arrays, helpers, class-only names, exact structured
class matching, wrong class/method/signatures, malformed selectors, and
separate covering/killing ACs. Functional tests cover zero mutants, zero
attribution, partial unattributed results, an AC with zero coverage,
configurable thresholds, aggregate results, producer interruption, stale Gate
workspace clearing, current-run producer/Gate execution, unknown PIT statuses,
safe feedback, and rehide. Report tests confirm detailed PIT evidence remains
reviewer-only.

## Consequences and alternatives

One direct v1 artifact avoids inventing a migration boundary when there is no
existing consumer. Treating all covered mutants as killed is rejected because
it conflates execution with detection. Counting only global PIT status is
rejected because it cannot show which exact public Acceptance Method supplied
evidence.
