# Executable acceptance boundary

**Status:** Implemented

**Date:** 2026-07-29

## User example

A checkout project has ordinary JUnit tests beside its executable acceptance
contract. A developer needs fast feedback from `./gradlew test`, while a
reviewer needs a separate command that proves the selected checkout ACs against
public rows, reviewer-owned hidden rows, Properties, and mutation testing.

The two results answer different questions. A green acceptance verification
cannot hide a failing unit test, and a green unit-test run cannot become formal
ToppleCat evidence.

## Decision

ToppleCat has two independent pipelines:

```text
./gradlew test
    ordinary project tests and public acceptance tests; no ToppleCat evidence

./gradlew toppleCatVerify --spec path/to/spec.md
    selected executable acceptance contract; fresh ToppleCat evidence
```

An AC has exactly one public `@ToppleAcceptanceTest("AC-...")` method. Typed
hidden rows reuse that method in `HIDDEN_ONLY` mode. Ordinary public and hidden
JUnit tests are outside ToppleCat gates. `@ToppleProperty` is an independent,
bounded check tied to the same AC and runs only in its dedicated tasks.

The public command sequence is Check, Review, Seal, test, and Verify. Reviewer
changes use Restore, Check, Review, and Reseal. The 0.0.7 names are the only
supported public interface.

## Scope and safeguards

`--spec` is the only delivery selection input. Repeated paths produce the
selected AC set; no option means all ACs. `--all-hidden-tests` widens only
hidden typed rows. Public Properties follow the selected ACs. Mutation remains
the full public acceptance contract.

`JUNIT`, `REVIEWER_JUNIT`, `PROPERTY`, and `MUTATION` are independent gates.
Hidden rows are the sole evidence for `REVIEWER_JUNIT`; Property and mutation
cannot supplement it. Disabled safeguards remain `DISABLED` in evidence.

## Integrity and reports

Approval seals compiler-derived acceptance source closure, selected Specs and
ACs, public typed rows used by mutation, effective policy, Gradle logic, and
the compiler semantic definition. It excludes production source and unrelated
ordinary test source. Check fails closed when an in-project test helper used by
the acceptance closure cannot be traced by javac symbols.

Review and Verification reports are reviewer-only. The safe public projection
is `reports/public/index.html`; it and `agent-feedback.json` contain no hidden
identifiers, source locations, generated trial material, or raw failures.

## Consequences

The published API names are `@ToppleAcceptanceTest`, `PropertyTrials`,
`toppleCatSeal`, `toppleCatReseal`, and `--all-hidden-tests`. This is a breaking
0.0.7 rename: the previous public annotations, reviewer-Java evidence,
previous acceptance terminology, and the former combined DSL are removed
without aliases.
