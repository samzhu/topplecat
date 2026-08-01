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

The 0.0.12 custody and approval schemas are current-only. A prior schema is not
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

Formal Verify uses only ToppleCat's managed PIT 1.25.5 producer. It targets
compiler-emitted public Acceptance Methods, fixes the
`topplecat-managed-v1` 12-operator profile, and writes a non-timestamped XML
full matrix containing PIT's `coveringTests`, `killingTests`, and
`succeedingTests` groups. A project `pitest` task, custom producer task,
consumer `targetTests`, and consumer report path are not configuration inputs
for ToppleCat evidence. Before every formal run, ToppleCat clears its internal
PIT XML and prior current workspace, then disables task-output and build-cache
reuse for both the producer and Gate. The XML, v1 result, completion marker,
evidence, and report therefore belong to the same run.
Project-wide `tasks.withType(PitestTask)` conventions apply only to the
project's own PIT tasks; they cannot rewrite the formal managed producer.
ToppleCat attributes a result only to the exact public Acceptance Method whose
class, method, overload, and parameter types match the PIT selector.
`coveringTests` says the method ran against that mutant; `killingTests` says
that same method detected it. If PIT produced mutants but none can be exactly
attributed to any public Acceptance Method, the Mutation Gate fails
(`MUTATION=FAIL`). Once at least one mutant is exactly attributed, any remaining
unattributed mutants stay in reviewer evidence and do not directly affect the
Gate. Each AC with covered mutants independently meets or misses its sealed
threshold from exact `killingTests`. An AC with no covered mutant is an
attribution gap: once another AC has exact attribution it is nonblocking and
the reviewer report says `此 AC 沒有取得本次 managed mutation profile 的歸因證據，需要 reviewer 判斷。`
Missing, malformed, interrupted, stale, profile-mismatched, non-full-matrix,
or zero-mutant producer evidence makes Mutation Testing incomplete. PIT's own
status, `detected`, raw mutator, and description remain visible without being
turned into a ToppleCat score.

Contract integrity is the only precondition. Verify first runs the current Check
to rebuild the compiler definition it compares with the Mechanical Seal. If it
is `PASS`, Verify runs public acceptance, hidden typed-row retest,
Property-Based Testing, and Mutation
Testing in that fixed order. A completed safeguard that finds a problem is
`FAIL`, not a reason to skip later safeguards. JUnit-like tasks retain XML,
sidecars, and completion markers after assertion failures; the mutation task
writes usable findings before the aggregate failure exit. The report waits for
all enabled safeguards or their explicit interruption, aggregates expected
consumption, writes evidence, reports, and safe feedback, re-hides reviewer
source, and only then fails Gradle for an aggregate `FAIL` or `INCOMPLETE`.

An interrupted task is `INCOMPLETE`; a completed task with missing or malformed
current-run evidence (including JUnit runtime sidecars) is also `INCOMPLETE`.
Each run discards an unarchived active workspace before it starts. Stable copies
and archives are never inputs to a later verdict. Property feedback distinguishes a discovered
counterexample from an incomplete task or damaged evidence. Mutation feedback
likewise distinguishes surviving mutants from an incomplete task or missing /
damaged producer evidence.

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
`INCOMPLETE`. Verify uses only an existing Mechanical Seal and records that its
approval was not updated. If none exists, run `toppleCatSeal` before Verify;
Verify never creates approval. Downstream gates then do not run and are
recorded `INCOMPLETE`; no earlier artifact fills a gap.

## Reports and information boundary

| Artifact | Purpose | Audience |
| --- | --- | --- |
| Spec Review | Complete selected Markdown document and bound executable material before handoff | Reviewer only |
| Verification Report | Current formal-run conclusion, failure-first diagnostics, and private evidence | Reviewer only |

Their stable paths are respectively:

```text
build/topplecat/reports/review/index.html
build/topplecat/reports/verification/index.html
```

Verification Report can show Property classifications, generator choices,
shrunk counterexamples, replay tokens, and the reviewer-only PIT attribution
matrix. It also shows disabled safeguards as `DISABLED`. Spec Review may show
non-blocking expected-output quality advisories. There is no public HTML report;
`agent-feedback.json` never exposes reviewer case IDs, values, source names or
paths, Property trial material, tokens, attachments, raw private failures, or
quality-advisory output.

`build/topplecat/evidence.json` is the machine verdict for the current run.
Each run starts in `build/topplecat/runs/current/`, receives a fresh UUID when
archived, and retains only a small recent archive set. Stable copies are for
inspection, never inputs to a later verdict.

Spec Review reads the complete selected SDD before executable material. It says
that the specification is prepared but not executed. Verification Report starts
with accepted, rejected, or incomplete, then lists concrete failed findings
before incomplete findings. Its five separate sections preserve the meaning of
Contract Integrity, Public Acceptance and Expected Consumption, Hidden Tests,
Property-Based Testing, and Mutation Testing. A field-level expected/actual
comparison is reviewer-only diagnostic context for the failed compiler Step; it
does not alter the JUnit assertion or Expected Consumption result.

## Custody boundary

Seal stores reviewer-only source below
`~/.topplecat/projects/<sha256-project-key>/escrow/`. This is plaintext
mechanical custody—not encryption, a sandbox, or protection from a process
running as the same OS user. A public handoff must exclude reviewer source,
custody state, build artifacts, and any Git history containing reviewer
material. `./gradlew clean` does not delete reviewer custody.
