# Managed mutation profile and verification evidence

**Status:** Implemented

**Date:** 2026-08-01

**Target:** ToppleCat 0.0.10

## User example

An implementation agent completes a checkout rule whose public Java Acceptance
Method verifies that a 1,000-dollar order receives a 100-dollar discount. Three
independent safeguards challenge that delivery in different ways:

- Hidden Tests run the same approved rule with reviewer-owned carts that the
  agent did not see.
- Property-Based Testing generates bounded carts and checks a human-authored
  invariant such as "a discount never increases the total."
- Mutation Testing changes existing production behavior, such as replacing
  subtraction with addition, and observes whether the exact public Acceptance
  Method detects the change.

The Verification Evidence report must finish and show all three results. A
passing Hidden Test or Property cannot give the public Acceptance Method credit
for detecting a PIT mutant. Likewise, one failed safeguard does not stop the
other enabled safeguards from producing their own current-run evidence.

ToppleCat runs a fixed, product-owned PIT producer for formal Verify. It selects
operators that directly challenge weak result assertions, one-sided branches,
boundary examples, unobserved side effects, and arithmetic formulas. A team
may run a different PIT configuration separately, but that result is not a
ToppleCat Gate input.

## Problem

ToppleCat 0.0.9 can consume a consumer-selected producer task and report path,
and its default PIT producer does not declare a product-specific operator set.
That permits two formal Verify runs to use materially different mutation
questions while both appear as ToppleCat Mutation Testing. PIT group names also
change independently of ToppleCat, so selecting `DEFAULTS`, `STRONGER`, or
`ALL` would not make the effective operator list stable or reviewable.

The 0.0.9 reviewer artifact preserves PIT status and selector relationships but
drops the raw mutator identity and description. A reviewer can see that a
mutant survived without seeing what PIT changed. Its Gate also fails every AC
that covered no mutant. Under a deliberately focused operator profile, zero
covered mutants can mean either a weak Acceptance Method or simply that none
of the selected operators applies to that method's production path. ToppleCat
cannot infer which explanation is true.

The product must retain its established boundary: Mutation Testing measures
whether public acceptance work detects changes to existing production
bytecode. It does not discover a business rule that the executable contract
omitted, prove that expected values are correct, or replace Hidden Tests and
Property-Based Testing.

## Decision and product boundaries

### ToppleCat-owned formal producer

Formal `toppleCatVerify` always uses a PIT producer managed by ToppleCat. The
producer is fixed to PIT 1.25.5, targets compiler-emitted public Acceptance
Methods, enables PIT's full mutation matrix, writes non-timestamped XML, and is
rerun for every formal Verify. ToppleCat removes the public `producerTask` and
`reportFile` mutation configuration. It neither reads nor merges a consumer's
PIT task or report.

Users remain free to run other PIT tasks outside formal ToppleCat verification.
Those tasks have no ToppleCat configuration contract, attribution, evidence,
safe feedback, or Gate effect. This is an intentional incompatible 0.0.10
change and does not introduce a replacement extension point.

For example, a project can keep a broad `tasks.withType(PitestTask)` convention
for its own PIT reports. Formal Verify's producer is a ToppleCat task, not a
consumer `PitestTask`, so that convention cannot replace its target methods,
operator profile, output format, report location, or runtime. This isolates
ordinary project mutation workflow from formal ToppleCat evidence without
preventing the project from running it separately.

### ToppleCat Managed Mutation Profile

The formal producer uses **ToppleCat Managed Mutation Profile／ToppleCat
託管突變設定**, identified in evidence as `topplecat-managed-v1`. The profile
configures these 12 exact PIT operator IDs rather than a PIT group name:

```text
TRUE_RETURNS
FALSE_RETURNS
PRIMITIVE_RETURNS
EMPTY_RETURNS
NULL_RETURNS
REMOVE_CONDITIONALS_EQUAL_IF
REMOVE_CONDITIONALS_EQUAL_ELSE
REMOVE_CONDITIONALS_ORDER_IF
REMOVE_CONDITIONALS_ORDER_ELSE
CONDITIONALS_BOUNDARY
VOID_METHOD_CALLS
MATH
```

Their behavior follows PIT's
[official mutator definitions](https://pitest.org/quickstart/mutators/) as
implemented by the pinned PIT version.

The operators form five product-focused signal families:

| Signal family | Question asked of the public Acceptance Method |
| --- | --- |
| Return replacement | Does it distinguish true, false, zero, empty, null, and the approved result? |
| Forced conditional branch | Does it observe both when a branch should and should not affect behavior? |
| Conditional boundary | Does it distinguish the exact threshold from values immediately around it? |
| Void-call removal | Does it observe required persistence, events, clearing, or another side effect? |
| Math replacement | Does its input and expected result distinguish an incorrect arithmetic formula? |

This is a ToppleCat product policy, not a PIT-defined AI profile and not a claim
that these operators are universally optimal. `INCREMENTS`, `INVERT_NEGS`,
`NEGATE_CONDITIONALS`, `INLINE_CONSTS`, `CONSTRUCTOR_CALLS`,
`NON_VOID_METHOD_CALLS`, switch operators, experimental operators, and `ALL`
are outside `topplecat-managed-v1`. Some may be useful in a general mutation
workflow, but 0.0.10 does not have enough direct product value to add their
noise to the formal Gate.

Runtime improvement is not a 0.0.10 success criterion. The smaller set is a
signal policy, not a performance promise.

### Exact attribution and threshold

The structural selector and exact class, method, overload, and parameter-type
matching defined by the 0.0.9 mutation-attribution record remain authoritative.
Only a public Acceptance Method receives Mutation Gate credit. Hidden Test
executions, `@ToppleProperty` executions, helper tests, and other JUnit tests
cannot supply an AC's mutation coverage or detection.

For one AC, ToppleCat computes its contract-scoped detection rate as:

```text
unique mutants whose killingTests exactly match its Acceptance Method
---------------------------------------------------------------------
unique mutants whose coveringTests exactly match its Acceptance Method
```

For example, when two Acceptance Methods cover one mutant but only one appears
in `killingTests`, both receive one covered mutant and only the killing method
receives one detected mutant. PIT's mutant still retains its one global raw
`status` and `detected` value; ToppleCat does not relabel it as a different PIT
status for the other AC.

The project threshold remains reviewer-controlled verification policy, defaults
to 100, and applies equally to every attributed mutant. Lowering it is an
explicit human acceptance of a weaker policy, not an agent-selected relaxation.
There is no operator weighting, blended score, or independent per-family Gate.

### Evidence fidelity and the current schema

The sole mutation artifact remains `topplecat.mutation-results.v1`. Because the
product has no existing consumer, 0.0.10 changes that one current structure in
place and supplies no predecessor reader, migration, dual writer, or alternate
schema. The reviewer-only artifact adds:

- configured PIT version, managed profile ID, and all 12 operator IDs;
- producer, uniquely attributed, and unattributed mutant totals;
- raw PIT `status`, `detected`, `mutator`, and `description` for every mutant;
- raw covering, killing, and succeeding selector relationships;
- summaries grouped by raw mutator identity; and
- per-AC covered, detected, rate, and attribution-gap information.

Raw PIT status names and the `detected` boolean remain unchanged, including
`NO_COVERAGE` and future values that ToppleCat does not know. The managed PIT
version pins the mapping between configured operator IDs and PIT's raw mutator
identities. A raw identity outside that mapping, a missing required identity,
or evidence that does not match the declared managed profile makes only the
Mutation Testing result `INCOMPLETE`.

The meaning of each raw status remains PIT's
[official outcome meaning](https://pitest.org/quickstart/basic_concepts/), not a
ToppleCat status-to-score mapping.

The raw description explains what PIT changed; ToppleCat may organize and
label the reviewer view but does not rewrite that description into a business
conclusion. A `SURVIVED` mutant is evidence that the tests did not detect that
change, not proof of a fake test or incorrect implementation. A reviewer still
decides whether it exposes a useful assertion gap, an equivalent mutant, or an
irrelevant change.

### Attribution gaps and Gate rules

Mutation assessment follows these rules in order:

1. A missing, interrupted, stale, malformed, non-full-matrix, profile-mismatched,
   or zero-mutant managed producer result is `MUTATION=INCOMPLETE`.
2. A usable non-empty producer result with zero exact attribution anywhere in
   the public acceptance contract is `MUTATION=FAIL`.
3. Once at least one AC has exact attribution, unattributed producer mutants
   remain reviewer evidence and do not directly affect the Gate.
4. An attributed AC whose contract-scoped detection rate is below its threshold
   makes `MUTATION=FAIL`.
5. An AC with zero covered mutants is an attribution gap. Once another AC has
   valid attribution, that gap is shown for reviewer judgment and does not
   directly affect the Gate.
6. With usable evidence and at least one exact attribution, `MUTATION=PASS` when
   every AC that has covered mutants meets its threshold.

An attribution gap says only that an AC has no evidence from this managed
profile. It does not say that the AC passed mutation testing, that its business
behavior is wrong, or that the safeguard is not applicable. The reviewer view
uses explanatory text rather than inventing a PIT status for the AC.

This presentation borrows the useful separation of tested mutations and
coverage gaps from
[`clj-mutate`](https://github.com/unclebob/clj-mutate/tree/e27dd5df63c4efdd66438587d1c5f49e73661b69),
as used by
[`unclebob/missile-command`](https://github.com/unclebob/missile-command/tree/33db078d69ff7f6bf2ef335e102e277162756a9d).
Those projects do not use PIT and do not provide PIT-to-Acceptance-Method
attribution, so their score and zero-mutation behavior are not ToppleCat Gate
precedent.

## Visible interface and behavior

The reviewer-only Verification Evidence at
`build/topplecat/reports/verification/index.html` presents Hidden Tests,
Property-Based Testing, and Mutation Testing as three peer functional-testing
sections. The top summary retains their independent Gate verdicts and the
aggregate verdict. It never averages or blends their scores.

The Hidden Tests and Property-Based Testing execution and Gate semantics do not
change in 0.0.10. Their existing evidence remains in their own sections. The
Mutation Testing section shows:

- PIT version, `topplecat-managed-v1`, the exact operator list, and threshold;
- producer, attributed, and unattributed totals;
- per-AC covered mutants, detected mutants, and detection rate;
- a plain-language, non-blocking attribution-gap note for each zero-covered AC;
- summaries grouped by mutator; and
- expandable raw mutant status, detected value, mutator, description, and
  exact selector relationships.

Mechanical Seal and `CONTRACT_INTEGRITY` remain a separate integrity concern
and a separate report area. Managed profile metadata and mutation results are
processed and displayed only by Mutation Testing; a producer/profile mismatch
does not become a Mechanical Seal failure. Mutation `enabled` and `threshold`
remain human-selected verification policy, but their Mutation Testing display
does not blend integrity and execution results.

Once contract integrity passes, all enabled Independent Safeguards run to a
current-run result even when an earlier one fails or becomes incomplete. The
report is written after all three functional-testing sections have evidence or
an explicit incomplete result. A Mutation `INCOMPLETE` does not change a Hidden
or Property result; the aggregate is `INCOMPLETE` when no safeguard failed, and
`FAIL` when any completed safeguard failed.

The Public Spec and `agent-feedback.json` retain only safe, generic Gate-level
mutation guidance. They never contain PIT versions, profile or operator IDs,
counts, descriptions, selectors, classes, methods, hidden values, Property
trial material, or raw private failures.

## Failure and integrity rules

Formal Verify clears the managed PIT XML and prior mutation result before the
producer runs and disables task-output and build-cache reuse for the producer
and Gate. The PIT XML, v1 mutation artifact, completion marker, evidence, and
Verification HTML must belong to the same run. A tracked report path remains an
internal ToppleCat implementation detail rather than consumer configuration.

ToppleCat validates that the report is a complete full matrix, every
acceptance-looking selector is reliably parseable, raw mutator identity is
present, and every identity belongs to the version-pinned managed profile.
Damage or mismatch produces generic safe feedback and detailed reviewer-only
diagnostics. It never becomes a synthetic `SURVIVED`, `KILLED`, or business
failure.

`CONTRACT_INTEGRITY` remains the only short-circuit. After it passes, Hidden
Tests, Property-Based Testing, and Mutation Testing all complete or explicitly
record why they are incomplete before ToppleCat writes the aggregate result and
returns its one formal Gradle failure.

## Acceptance evidence

Core and serialization tests cover:

- all 12 configured operator IDs and their mapping through the actual PIT 1.25.5
  `Mutator.byName(...)` / raw factory-identity API;
- raw `status`, `detected`, `mutator`, `description`, and three selector groups;
- unknown PIT statuses without normalization;
- exact template/method selectors, whitespace around parameter commas,
  overloads, arrays, wrong classes and methods, helpers, and partial names;
- one mutant covered by multiple Acceptance Methods but killed by only one;
- v1 round trips for profile, producer, per-mutator, per-AC, raw mutant, and
  attribution-gap evidence; and
- missing, unknown, and profile-external raw mutator identities.

Gradle functional tests cover:

- formal Verify always selecting the ToppleCat-managed producer and rejecting
  the removed `producerTask` and `reportFile` configuration;
- a real PIT fixture proving the fixed 12-ID effective profile, producer
  isolation from consumer `tasks.withType(PitestTask)` conventions, and fresh
  execution on a repeated formal Verify;
- zero producer mutants (`INCOMPLETE`), usable zero public attribution (`FAIL`),
  an actual survivor threshold failure, safe feedback, and reviewer-source
  rehide;
- one formal run where public Acceptance passes, Hidden Tests and a Property
  deliberately fail, and Mutation Testing fails from a real managed-PIT
  `SURVIVED` void-call mutant; all three safeguards still complete before the
  aggregate failure writes its report, safe feedback, and rehide evidence; and
- current-run cleanup of a skipped managed producer output and the resulting
  explicit incomplete evidence.

Core and Gate-result tests cover partial unattributed mutants, zero-covered AC
attribution gaps, profile-external mutators, unknown future status values, and
a reviewer-lowered threshold that passes while retaining raw survivor evidence.

The managed-PIT integration fixture reads the real v1 artifact to prove all
five signal families occur by raw identity—return replacement, forced
conditional, conditional boundary, void-call removal, and arithmetic
replacement—and retains a raw `SURVIVED` void-call mutant for the expected Gate
attack. Report DOM tests use nonempty attribution to cover the separate
Mechanical Seal, Hidden Tests, Property-Based Testing, and Mutation Testing
sections, their raw PIT details, the attribution-gap reminder, and the absence
of a blended quality score. Functional safe-feedback checks prove actual
profile/version, raw status, mutator, description, class, Acceptance Method,
selector, and count data stay out of both the public report and
`agent-feedback.json`. Release verification retains the expected Mutation Gate
attack failure using an operator in `topplecat-managed-v1`.

Implementation verification runs the narrowest affected module tests, then:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
python3 scripts/verify-docs.py
git diff --check
```

The historical `topplecat-vif` experiments and evidence are not modified or
rerun for this product release.

## Consequences and alternatives

ToppleCat 0.0.10 becomes deterministic about which PIT producer and mutation
questions count as formal evidence. This narrows configuration freedom and
intentionally breaks projects that configured a custom producer, but avoids a
new compatibility surface before product adoption. A separate user PIT run can
still answer broader project-specific questions without being mistaken for
ToppleCat evidence.

The focused profile may give an AC no covered mutants. Reporting that gap
without an automatic failure avoids claiming that a selected operator should
exist on every legitimate production path. Requiring every AC to cover at
least one selected mutant is rejected because it turns profile applicability
into a business verdict. Ignoring the gap is also rejected because a trivial or
disconnected Acceptance Method must remain visible to the reviewer.

PIT `DEFAULTS`, `STRONGER`, and `ALL` are rejected because they do not express a
stable ToppleCat product policy. Consumer-owned producers are rejected because
formal Verify could no longer guarantee one evidence meaning. Weighting
operators or creating per-family Gates is rejected because business importance
belongs to the human-authored contract, not to a generic bytecode operator.

Mutation Testing remains one independent functional-testing aspect. It can
expose a public Acceptance Method that does not distinguish a production
change, but it cannot prove business correctness, supply missing hidden
examples, validate a human-authored Property, or replace reviewer judgment.
