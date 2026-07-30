# Troubleshooting

## The acceptance method breaks Scenario authoring

`toppleCatCheck` requires a public `@ToppleAcceptanceTest("AC-...")` method
that uses direct `scenario.given|when|then|and(stage).step(...)` calls. Declare `ToppleCase` first, one non-generic
`ToppleScenario` second, then distinct non-final concrete `ToppleStage`
parameters with accessible no-argument constructors. Move setup, service calls,
assertions, local variables, helper calls, and control flow into Stage methods.
## A row or selected Spec AC has no acceptance binding

Add one compilable public `@ToppleAcceptanceTest` with the same literal AC ID,
or correct the `acId`/`--spec` selection. A hidden row cannot create an AC.

## Reviewer coverage is incomplete while a Property passed

This is intentional. `REVIEWER_JUNIT` requires executed hidden typed rows;
Property-Based Testing has independent input and evidence. Add a hidden row for
each selected AC, or explicitly disable `hiddenTests` and reseal the policy if
the team intentionally uses PBT without Hidden Tests.

## Property evidence is incomplete

Check that a Property has a literal existing AC, exactly one `PropertyTrials`
parameter, one `forAll(...).check(...)`, bounded generators, and valid trial,
discard, shrink, and coverage limits. A filter that exhausts its discard budget
or a failed coverage requirement is `INCOMPLETE`, not a passing assertion.

An actual counterexample is different: it is `PROPERTY=FAIL` and Verify still
runs the later Mutation safeguard. `PROPERTY=INCOMPLETE` instead means the
Property task did not complete, or its current events/JUnit evidence was
missing or damaged. Do not inspect an earlier run as a replacement.

## Mutation evidence is missing or fails

The managed PIT producer targets public acceptance classes. A missing or
malformed producer report is `INCOMPLETE`; a usable report that does not cover
an acceptance method is `FAIL`. Configure a custom producer with
`mutationTesting { producerTask.set(...); reportFile.set(...) }` only when it
still writes a full current report.

A surviving mutant is `MUTATION=FAIL`; Verify writes reports, safe feedback,
and re-hides reviewer source before returning that aggregate failure. A producer
that was interrupted is `INCOMPLETE`; a task that completed without a usable
current report is also `INCOMPLETE`. Verify removes the configured producer
report before Gradle evaluates producer skip conditions, so stale mutation
evidence cannot be reused.

## Contract integrity failed

The sealed acceptance source closure, public rows, selected scope, Gradle logic,
semantic definition, or policy changed. Restore reviewer custody, make the
intended change, then Check, Review, and Reseal. Do not edit sealed public
contract inputs in the implementation handoff. If Verify says an existing
Mechanical Seal is missing, run `toppleCatSeal`; Verify never replaces approval
on its own.

## Custody cannot be restored

The 0.0.8 custody state is reviewer-local under
`~/.topplecat/projects/<sha256-project-key>/escrow/`. The project must be opened
at the same resolved path and with the reviewer state available. Prior-format
custody is not migrated; create a new sealed reviewer state.

## The public report is absent

`reports/public/index.html` is published only after a Verify run whose contract
integrity passes. Contract Review is the pre-handoff reviewer-only report.
