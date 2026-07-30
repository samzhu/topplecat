# Verification and evidence

ToppleCat verifies an executable Java/JUnit acceptance contract. It does not
select the current work, manage delivery history, grant organizational approval,
or provide an operating-system security boundary.

## Reviewer workflow

```bash
./gradlew toppleCatCheck --spec specs/023-checkout/spec.md
./gradlew toppleCatReview --spec specs/023-checkout/spec.md
./gradlew toppleCatSeal --spec specs/023-checkout/spec.md
./gradlew test
./gradlew toppleCatVerify --spec specs/023-checkout/spec.md
```

`--spec` is repeatable and selects the delivery at invocation time. Use the
same selection for Check, Review, Seal, and Verify. `--all-hidden-tests`
widens only hidden typed rows. Public acceptance and PBT follow the selected
ACs; Mutation Testing remains full-contract.

`toppleCatRestore` is a reviewer-only recovery and editing command. To revise
custody, use:

```text
toppleCatRestore
    -> edit src/hiddenTest
    -> toppleCatCheck
    -> toppleCatReview
    -> any external organizational review
    -> toppleCatReseal
```

The 0.0.7 custody and approval schemas are current-only. A prior schema is not
migrated or read for verification; seal a new reviewer state instead.

## Independent formal work

`./gradlew test` is ordinary development feedback. It runs public project tests
and public acceptance methods, but produces no formal ToppleCat evidence.

`toppleCatVerify` creates a fresh formal public acceptance run and evaluates
the enabled safeguards independently:

| Capability | Evidence that can pass its gate | Cannot be supplemented by |
| --- | --- | --- |
| Hidden Tests | Executed hidden typed rows for the selected ACs | Properties or mutation reports |
| Mutation Testing | A current usable producer report attributed to public acceptance methods | Hidden rows or Properties |
| Property-Based Testing | Current matching Property events and JUnit XML | Hidden rows or mutation reports |

If Hidden Tests are enabled and a selected AC has no executed hidden row,
`REVIEWER_JUNIT=INCOMPLETE`. A Property may run and pass at the same time, but
it cannot change that result. A team choosing PBT without hidden rows
must explicitly disable `hiddenTests`, reseal the policy, and receives
`REVIEWER_JUNIT=DISABLED` with the actual `PROPERTY` result.

The managed PIT producer uses public acceptance classes only. Consumer-owned
`targetTests` and custom producer tasks are preserved. A usable report that
does not cover a public acceptance method is `MUTATION=FAIL`; missing,
malformed, or interrupted mutation output is `MUTATION=INCOMPLETE`.

## Gates and verdicts

Every formal run records this fixed gate order:

```text
CONTRACT_INTEGRITY
JUNIT
REVIEWER_JUNIT
EXPECTED_CONSUMPTION
PROPERTY
MUTATION
```

Each gate is `PASS`, `FAIL`, `INCOMPLETE`, `DISABLED`, or `NOT_APPLICABLE`.
Contract integrity is mandatory and cannot be disabled. `DISABLED` is an
explicit sealed policy decision; `NOT_APPLICABLE` is an enabled safeguard with
no applicable declaration. Neither is presented as a pass.

The aggregate evidence verdict is only `PASS`, `FAIL`, or `INCOMPLETE`.
`toppleCatVerify` and `toppleCatReport` finish evidence, reports, safe feedback,
and rehide before failing Gradle for an aggregate `FAIL` or `INCOMPLETE`.

Contract integrity seals the compiler-derived acceptance source closure, public
typed rows, project Gradle logic, semantic definition, selected scope, and
effective verification policy. It excludes production source and unrelated
ordinary tests. An approval mismatch is `FAIL`; missing current approval is
`INCOMPLETE`. Downstream gates then do not run and are recorded `INCOMPLETE`;
no earlier artifact fills a gap.

## Reports and information boundary

| Artifact | Purpose | Audience |
| --- | --- | --- |
| Contract Review | Static contract projection before handoff | Reviewer only |
| Public Spec | Safe public contract projection after Verify | Public |
| Verification Evidence | Current execution results and private diagnostics | Reviewer only |

Their stable paths are respectively:

```text
build/topplecat/reports/review/index.html
build/topplecat/reports/public/index.html
build/topplecat/reports/verification/index.html
```

Verification Evidence can show Property classifications, generator choices,
shrunk counterexamples, and replay tokens. It also shows disabled safeguards
as `DISABLED`. Public Spec and `agent-feedback.json` never expose reviewer case
IDs, values, source names or paths, Property trial material, tokens,
attachments, or raw private failures.

`build/topplecat/evidence.json` is the machine verdict for the current run.
Each run starts in `build/topplecat/runs/current/`, receives a fresh UUID when
archived, and retains only a small recent archive set. Stable copies are for
inspection, never inputs to a later verdict.

## Custody boundary

Seal stores reviewer-only source below
`~/.topplecat/projects/<sha256-project-key>/escrow/`. This is plaintext
mechanical custody—not encryption, a sandbox, or protection from a process
running as the same OS user. A public handoff must exclude reviewer source,
custody state, build artifacts, and any Git history containing reviewer
material. `./gradlew clean` does not delete reviewer custody.
