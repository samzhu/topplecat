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

Formal Verify owns the PIT producer. It pins PIT 1.25.5 and the fixed
`topplecat-managed-v1` profile, targets only compiler-emitted public Acceptance
Methods, and writes its internal current-run XML. Do not configure a custom
producer, custom report path, or project `pitest` task for ToppleCat; those are
not supported formal-evidence inputs. A missing, damaged, interrupted, stale,
profile-mismatched, non-full-matrix, or zero-mutant managed result leaves the
Mutation Gate incomplete.

Project-wide `tasks.withType(PitestTask)` configuration remains available for a
separate project PIT workflow. It cannot alter formal Verify's managed producer
or its evidence.

When PIT produced mutants but none map exactly to a public Acceptance Method,
the Mutation Gate fails because ToppleCat cannot claim public-contract
attribution. When an AC did cover mutants but its own method did not kill enough
of them for the sealed threshold, the Gate also fails. An AC with no covered
managed-profile mutant is instead an attribution gap for reviewer judgment once
another AC has exact attribution. Inspect the reviewer-only
`mutation-results.json` and Verification Report to see the unmodified PIT
outcomes, mutators, descriptions, and selector relationships. Verify writes
reports, safe feedback, and re-hides reviewer source before returning the
aggregate failure; stale producer reports cannot be reused.

## Contract integrity failed

The sealed acceptance source closure, public rows, selected scope, Gradle logic,
semantic definition, or policy changed. Restore reviewer custody, make the
intended change, then Check, Review, and Reseal. Do not edit sealed public
contract inputs in the implementation handoff. If Verify says an existing
Mechanical Seal is missing, run `toppleCatSeal`; Verify never replaces approval
on its own.

## Custody cannot be restored

The 0.0.16 custody state is reviewer-local under
`~/.topplecat/projects/<sha256-project-key>/escrow/`. The project must be opened
at the same resolved path and with the reviewer state available. Prior-format
custody is not migrated; create a new sealed reviewer state.
