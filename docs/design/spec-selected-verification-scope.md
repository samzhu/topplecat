# Spec-selected verification scope

**Status:** Implemented

**Date:** 2026-07-28

This record describes the implemented command-scoped verification behavior.
The current product supports the options and scoped execution described below.

## User example

An AI agent delivers a checkout change today. The repository also contains older
coupon and cancellation Specs.

The human wants to run:

```bash
./gradlew toppleCatVerify --spec specs/023-checkout/spec.md
```

ToppleCat should use the acceptance-condition IDs in that checkout Spec to find
the matching executable Java acceptance tests. It should run the checkout
reviewer cases by default, not every older coupon and cancellation reviewer
case. Public regression tests still run normally.

Mutation is different. A checkout change may touch shared money, promotion, or
order code. ToppleCat cannot safely infer the affected production methods from
the Spec, so mutation remains complete by default.

## Problem

Requiring a contributor to edit `build.gradle` for every delivery is too slow
and leaves noisy configuration changes in a repository where AI agents may
deliver several features per day.

Automatically guessing the current Spec from a branch name, Git diff, directory
name, or SDD product would couple ToppleCat to task management and could select
the wrong contract.

ToppleCat therefore needs an invocation-scoped Spec input that remains explicit,
portable across SDD tools, and protected from contract tampering.

## Decision

### Select the Spec at the Gradle task

The author, reviewer, or CI passes one or more Markdown files with a repeatable
`--spec` option:

```bash
./gradlew toppleCatCheck --spec specs/023-checkout/spec.md
./gradlew toppleCatReview --spec specs/023-checkout/spec.md
./gradlew toppleCatHide --spec specs/023-checkout/spec.md
./gradlew toppleCatVerify --spec specs/023-checkout/spec.md
```

A cross-cutting delivery may repeat the option:

```bash
./gradlew toppleCatVerify \
  --spec specs/023-checkout/spec.md \
  --spec specs/024-payment/spec.md
```

Paths are repository-relative Markdown files. Directories are rejected so an
accumulated Spec tree cannot silently become the current delivery. Existing
`specDocs.from(...)` support remains for fixed documentation; an explicit
`--spec` selection replaces it for that invocation rather than merging two
possibly different scopes.

ToppleCat does not store a mutable `current-spec` pointer. The external workflow
knows which Spec belongs to the delivery and supplies it each time.

### Map Spec ACs to executable Java

The selected Markdown files provide public `AC-...` identities.

- Every selected AC requires one canonical `@ToppleTest("AC-...")`.
- `@ToppleAc("AC-...")` may add JUnit coverage for the same AC but cannot
  replace the canonical test.
- A selected Markdown AC without a canonical Java test makes
  `toppleCatCheck` fail.
- Java acceptance conditions outside the selected Spec remain valid project
  contracts but are outside the current hidden-retest scope.

A plain `@Test` has no ToppleCat AC identity. It remains an ordinary unit or
integration test and cannot make the reviewer gate pass.

### Execute each safeguard at the appropriate scope

| Verification work | Default scope |
| --- | --- |
| Public JUnit | Complete public test suite |
| Hidden case rows | Selected Spec ACs |
| Reviewer Java tests | Selected `@ToppleAc` or `@ToppleTest` ACs |
| Expected consumption | Cases executed in the current run |
| Mutation | All required public canonical ACs |

`--all-hidden` is an allowed runtime escalation:

```bash
./gradlew toppleCatVerify \
  --spec specs/023-checkout/spec.md \
  --all-hidden
```

It runs every AC-bound hidden check in reviewer custody. It does not include
plain, unbound `@Test` methods. ToppleCat does not provide runtime
`--skip-hidden` or `--skip-mutation` options because those would weaken a sealed
policy. Existing reviewer-owned Gradle switches remain available and produce a
visible `DISABLED` gate.

### Distinguish acceptance tests from ordinary JUnit

`@ToppleTest` and `@ToppleAc` receive one stable JUnit marker tag. The reviewer
test task includes that marker and filters its AC identity against the selected
Spec.

- A plain test under `src/test` continues to run in the ordinary Gradle test
  lifecycle.
- A plain test under `src/hiddenTest` does not count as ToppleCat reviewer
  evidence. Reviewer diagnostics explain that it must receive `@ToppleAc` if it
  is intended as executable acceptance coverage.
- Helper source without a test remains allowed.

### Seal the selection

Hide and UpdateEscrow seal:

- selected Spec paths and SHA-256 digests;
- sorted selected AC IDs and their deterministic digest;
- effective hidden-retest scope;
- the existing public Java tests, typed cases, Gradle logic, and verification
  policy.

Verify receives the Spec path again and compares it with the seal. A different,
missing, added, or modified Spec produces `CONTRACT_INTEGRITY=FAIL`. Downstream
gates remain `INCOMPLETE`, stale evidence is not reused, and reviewer source is
rehidden.

`--all-hidden` may strengthen a selected run without resealing. It cannot remove
checks from the sealed minimum.

## Failure rules

- A selected Spec with no AC anchor fails Check.
- A selected AC without a canonical `@ToppleTest` fails Check.
- A selected AC without current-run reviewer coverage makes
  `REVIEWER_JUNIT=INCOMPLETE`. Coverage is an actually executed hidden row or
  an AC-bound reviewer Java test that actually entered its test body; source
  comments, strings, and disabled tests never count.
- A changed Spec path, content, AC set, public contract, or sealed policy makes
  `CONTRACT_INTEGRITY=FAIL`.
- Safe agent feedback remains gate-level and never includes hidden case IDs,
  values, test names, source paths, assertions, or stack traces.
- Every Verify outcome rehides reviewer source.

## Product boundaries

ToppleCat does not:

- decide which feature is current;
- inspect Git history or branch names to guess a Spec;
- track task status, delivery history, or organizational approval;
- infer missing business requirements;
- guess a smaller mutation target from the Spec.

The external SDD or delivery workflow chooses the Spec. ToppleCat only connects
its AC identities to executable acceptance and verifies that the selected,
sealed contract is the one that ran.

## Acceptance evidence

The implementation fixture must contain two deliveries, such as shipping and
coupon, with:

- one Spec and canonical public test per delivery;
- public and reviewer rows for both ACs;
- reviewer `@ToppleAc` tests for both ACs;
- one ordinary public unit test and one unbound hidden `@Test`;
- mutation evidence attributed to both canonical ACs.

Verification must prove:

1. Selecting shipping runs the complete public suite, shipping hidden checks,
   and mutation for both canonical contracts.
2. Coupon hidden checks do not run until coupon is selected or `--all-hidden`
   is used.
3. The unbound hidden `@Test` never counts as ToppleCat reviewer evidence.
4. Changing the selected Spec after Hide fails contract integrity.
5. Missing executable or reviewer coverage cannot produce a passing reviewer
   gate. In particular, an AC mentioned only in a source comment or string, or
   on a disabled reviewer test, remains uncovered.
6. Reports, machine evidence, and actual executions describe the same selected
   scope.
7. Safe feedback and reviewer custody boundaries remain unchanged.

## Consequences

The common AI delivery path becomes explicit without repository configuration
edits. Hidden retests become faster and easier to understand, while mutation
keeps the conservative full-project behavior required when production impact
cannot be proved.

The same Spec path appears in several reviewer commands. This repetition is
intentional: it avoids stale mutable selection state and makes each invocation
auditable. Agent skills and SDD workflows can carry the known path without
asking a human to edit Gradle files.
