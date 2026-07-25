# Verification and Evidence

## Workflow

1. Author public Java tests and public JSON/YAML rows.
2. Add independent reviewer rows and, where needed, reviewer JUnit tests under
   `src/hiddenTest`.
3. Run `toppleCatCheck` to validate bindings and case data. An authorized
   reviewer then runs `toppleCatReview` to inspect the combined contract before
   handoff; its HTML contains hidden data and is never implementation input.
4. Run `toppleCatHide` before giving the implementation task to another
   agent or developer.
5. Use plain `test` during implementation.
6. Run `toppleCatVerify` as the reviewer or in CI.

```bash
./gradlew toppleCatCheck toppleCatReview toppleCatHide
./gradlew test
./gradlew toppleCatVerify
```

## Task Reference

| Task | When to run it | Effect |
| --- | --- | --- |
| `toppleCatCheck` | Before review, hiding, and after contract edits. | Validates Java bindings, the canonical Stage DSL, and JSON/YAML case data without executing tests or writing HTML. |
| `toppleCatReview` | Authorized reviewer, before hiding. | Renders the complete reviewer-only static contract review without executing tests. |
| `toppleCatHide` | Before implementation begins. | Moves the entire reviewer source set into plaintext local custody storage. |
| `toppleCatRestore` | Authorized reviewer, only when source must be inspected or changed. | Restores the exact reviewer source set from local custody storage. |
| `toppleCatUpdateEscrow` | Authorized reviewer, after restoring, editing, checking, and accepting a new review. | Validates, stages, audits locally, and atomically activates the complete revised reviewer source set. |
| `test` | During implementation. | Runs public tests and public case rows only. |
| `toppleCatVerify` | Reviewer or CI final gate. | Runs enabled safeguards, writes evidence, and re-hides source after a hidden retest. |

`toppleCatInit` is an optional, non-destructive bootstrap for an otherwise empty
consumer project. `toppleCatRestore` is deliberately outside the normal
sequence: it is a reviewer recovery/editing operation, not part of the
implementation loop. Internal orchestration tasks are not consumer commands.

To evolve an escrowed reviewer suite, an authorized reviewer follows this
explicit custody workflow:

```text
toppleCatRestore
    -> edit src/hiddenTest
    -> toppleCatCheck
    -> toppleCatReview
    -> reviewer accepts the review
    -> toppleCatUpdateEscrow
```

The update task requires a readable `RESTORED` local escrow manifest and a
complete inventory of the current reviewer source. It preserves the prior active
escrow until the revised source, manifest, and reviewer-local audit have all
validated. Ordinary `toppleCatHide` still rejects changed restored source; a
public export has no reviewer source or custody state, so update fails safely.

The verify task enables three safeguards by default: it restores reviewer source
and reruns public tests with reviewer rows, runs reviewer JUnit tests, configures
PIT for mutation verification, and enforces expected-value consumption. It
writes reports/evidence and re-hides reviewer source after a hidden retest.
Without consumer PIT configuration, ToppleCat applies PIT `1.25.5`, its JUnit 5
plugin `1.2.3`, discovered production package targets, XML output, full mutation
matrix, and a 100% per-AC threshold. The first mutation run can be noticeably
slower because PIT has to analyse the project. ToppleCat matches PIT's JUnit
Unique IDs to the compiled canonical method signature, so two `@ToppleTest`
methods in one Java class keep independent mutation sets and scores.

The default producer measures **public executable contract mutation strength**:
it runs `sourceSets.test` with public test classes and public case rows only.
Hidden rows and reviewer-only JUnit tests answer the separate hidden-retest
question and never help default PIT kill a mutant. A boundary that must kill a
mutant belongs in the public contract. ToppleCat adds no per-case mutation score
and does not infer the test scope of a custom third-party producer.

## Verdicts

The aggregate `evidence.json` verdict is `PASS`, `FAIL`, or `INCOMPLETE`. Each
run always records `JUNIT`, `REVIEWER_JUNIT`, `EXPECTED_CONSUMPTION`, and
`MUTATION`; each gate is `PASS`, `FAIL`, `INCOMPLETE`, or `DISABLED`.

- `PASS` means every required stage in the current run passed.
- `FAIL` means a completed required stage reported a failure.
- `INCOMPLETE` means a required stage was absent, interrupted, or could not be
  proved complete for this run.
- `DISABLED` means the reviewer explicitly chose not to run that safeguard. It
  does not prevent an aggregate `PASS`, but it is never presented as a pass.

Use the `toppleCat.adversarial` tree only when that trade-off is deliberate:

```kotlin
toppleCat {
    adversarial {
        // Global false disables hidden retest, mutation, and expected consumption.
        // enabled.set(false)
        mutation { enabled.set(false) }
        // hiddenRetest { enabled.set(false) }
        // expectedConsumption { enabled.set(false) }
    }
}
```

When expected consumption is disabled, `READ`, `ASSERTED`, and `UNTOUCHED` are
still collected in the Verification report; neither `READ` nor `UNTOUCHED`
independently fails the test. When enabled, only `ASSERTED` satisfies a declared
expected value. When hidden retest is disabled, verify does not restore or run
`src/hiddenTest`. When mutation is disabled, PIT is not added to the task graph.
The Verification report visibly states
`Expected consumption enforcement disabled` when that category is disabled.

`c.verify(...)` compares JSON numbers recursively by exact mathematical value,
so `200`, `200.0`, and `200.00` are equal without any floating-point tolerance.
Formatting or decimal-scale requirements must be represented as a string or a
separate explicit field.

ToppleCat never uses a previous run to fill a gap. Each verification execution
uses a transient workspace under `build/topplecat/runs/current/`, then archives
it under a fresh execution-time UUID after reporting. The latest three
archives are retained. The stable files in `build/topplecat/` are copies
published after the current run is evaluated.

The public `reports/spec/index.html` is therefore a post-verify artifact, not a
pre-handoff review. `reports/review/index.html` is the only pre-handoff HTML and
is reviewer-only.

## Reports and Evidence Boundary

All report pages are static, self-contained offline bundles.

### Reviewer Contract Review

After static validation, `toppleCatReview` writes
`build/topplecat/reports/review/index.html`. It contains public and reviewer
case rows, configured external Markdown context, compiler-defined Stage
sentences, and collapsed canonical source. It identifies itself as
reviewer-only and contains no invented PASS/FAIL status. A later failed check
removes a previous review rather than leaving stale authoring output.

### Public Spec Report

After verification, `build/topplecat/reports/spec/index.html` and `data.json`
show public ACs, public rows, public Stage sentences, and any configured
external context. The context is labelled non-authoritative; Java tests and
typed rows remain the executable contract.

### Reviewer Verification Report

`build/topplecat/reports/verification/index.html` and `data.json` include public
and reviewer executions, expected-consumption state, narrative steps, gate
results, durations, attachments, and applicable private failures. Keep this
bundle reviewer-only. It supports status filters and search by AC, title, or
case ID. A disabled safeguard is visibly `DISABLED` with its reason, never
rendered as a passing gate.

### Machine Evidence and Safe Feedback

`build/topplecat/evidence.json` is the machine verdict and digests for the
current run. `build/topplecat/agent-feedback.json` is the only generated
diagnostic intended for an implementation agent. It communicates gate-level
results, including safe `DISABLED` reasons, without reviewer case identifiers,
inputs, expected values, source paths, private test names, internal task names,
attachments, or raw assertion failures. Generated HTML is evidence for humans,
not input to a later verdict.

## Delivery Hygiene

`toppleCatHide` moves reviewer source into plaintext `.topplecat/escrow/`.
It is not encryption, and `./gradlew clean` does not delete it. The correct order
is hide first, then create an implementation environment that never exposes
reviewer material.

Do not commit reviewer source to Git history the implementation agent can read.
Deleting `src/hiddenTest` or creating another worktree from that history does
not remove the old objects. Use one of these boundaries:

- a public export without `.git`, `.topplecat/`, or `build/`;
- an isolated environment whose repository history never contained reviewer
  material; or
- a public implementation repository paired with a separate private reviewer
  repository or CI environment.

`toppleCatRestore` is for an authorized reviewer who needs to inspect or edit
the exact stored source. It is not an implementation-loop command.
