# Verification and evidence

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

## Task reference

| Task | When to run it | Effect |
| --- | --- | --- |
| `toppleCatCheck` | Before review, hiding, and after contract edits. | Validates Java bindings, the canonical Stage DSL, and JSON/YAML case data without executing tests or writing HTML. |
| `toppleCatReview` | Authorized reviewer, before hiding. | Renders the complete reviewer-only static contract review without executing tests. |
| `toppleCatHide` | Before implementation begins. | Moves the entire reviewer source set into plaintext local custody storage and seals the reviewed public contract and effective verification policy. |
| `toppleCatRestore` | Authorized reviewer, only when source must be inspected or changed. | Restores the exact reviewer source set from local custody storage. |
| `toppleCatUpdateEscrow` | Authorized reviewer, after restoring, editing, checking, and accepting a new review. | Validates, stages, audits locally, and atomically activates the complete revised reviewer source set and approval epoch. |
| `test` | During implementation. | Runs public tests and public case rows only. |
| `toppleCatVerify` | Reviewer or CI final gate. | Freshly checks approval integrity, runs enabled safeguards only after a pass, writes evidence, and re-hides source. |

`toppleCatInit` bootstraps an empty consumer project without overwriting files.
`toppleCatRestore` sits outside the normal sequence because it is a reviewer
recovery and editing command. Internal orchestration tasks are not consumer
commands.

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
validated. Activation requests an atomic filesystem move where supported and
uses the same validation and recovery path otherwise. Ordinary `toppleCatHide`
still rejects changed restored source; a public export has no reviewer source
or custody state, so update fails safely. This is also the only path that
reseals a public-contract or verification-policy change. Ordinary Hide, Restore,
Rehide, and Verify preserve the existing approval rather than refreshing it.

The verify task enables three safeguards by default: it restores reviewer source
and reruns public tests with reviewer rows, runs reviewer JUnit tests, configures
PIT for mutation verification, and enforces expected-value consumption. It
writes reports/evidence and re-hides reviewer source after a hidden retest.
Without consumer PIT configuration, ToppleCat applies PIT `1.25.5`, its JUnit 5
plugin `1.2.3`, discovered production package targets, XML output, full mutation
matrix, and a 100% per-AC threshold. It also derives PIT `targetTests` from the
compiler-emitted descriptors for every approved public canonical `@ToppleTest`
declaring class. This avoids relying on package-name coincidence when production and test
packages differ. The first mutation run can be noticeably slower because PIT
has to analyse the project. ToppleCat matches PIT's JUnit Unique IDs to the
compiled canonical method signature, so two `@ToppleTest` methods in one Java
class keep independent mutation sets and scores.

The default producer measures **public executable contract mutation strength**:
it runs `sourceSets.test` with public test classes and public case rows only.
Hidden rows and reviewer-only JUnit tests answer the separate hidden-retest
question and never help default PIT kill a mutant. A boundary that must kill a
mutant belongs in the public contract. An explicitly configured consumer
`targetTests` is preserved, as is a custom mutation producer; ToppleCat does not
overwrite or infer either scope. If a usable PIT report covers no canonical
test, the affected AC is `FAIL`; if the report is missing, malformed, or
interrupted, mutation is `INCOMPLETE` and cannot become a passing aggregate.
ToppleCat adds no per-case mutation score.

Reviewer rows are executed by the public canonical `@ToppleTest`. Therefore a
reviewer source set containing only hidden rows can still produce
`REVIEWER_JUNIT=PASS` and `EXPECTED_CONSUMPTION=PASS` when those rows pass and
assert every expected value. Java helper sources without an executable JUnit method
are compiled but do not count as hidden tests. Reviewer-only Java tests remain an additional,
independent requirement when present; their failure cannot be masked by passing
rows. With hidden retest enabled but no hidden rows and no hidden Java tests, the
reviewer gate remains `INCOMPLETE`.

No individual gate proves that an agent avoided every hard-coded shortcut.
Reviewer retests cover independently chosen business cases, but a shortcut may
still satisfy them. Mutation checks whether the public contract notices changed
production behavior. Accept a claim only when the current aggregate verdict is
`PASS`; the gate states show what failed.

The [0.0.4 release notes](../releases/0.0.4.md) describe the compiler-backed
canonical PIT target and the mutation verification outcomes. The [0.0.3 release
notes](../releases/0.0.3.md) describe the reviewer approval epoch and mandatory
integrity gate.

## Verdicts

The aggregate `evidence.json` verdict is `PASS`, `FAIL`, or `INCOMPLETE`. Each
run always records `CONTRACT_INTEGRITY`, `JUNIT`, `REVIEWER_JUNIT`,
`EXPECTED_CONSUMPTION`, and `MUTATION` in that order; each gate is `PASS`,
`FAIL`, `INCOMPLETE`, or `DISABLED`. `CONTRACT_INTEGRITY` is mandatory and can
never be `DISABLED`.

- `PASS` means every required stage in the current run passed.
- `FAIL` means a completed required stage reported a failure.
- `INCOMPLETE` means a required stage was absent, interrupted, or could not be
  proved complete for this run.
- `DISABLED` means the reviewer explicitly chose not to run that safeguard. It
  does not prevent an aggregate `PASS`, but it is never presented as a pass.

`toppleCatVerify` and `toppleCatReport` fail the Gradle build when the aggregate
verdict is `FAIL` or `INCOMPLETE`. The report task first writes evidence,
reports, safe feedback, stable copies, and the archived run; reviewer source is
still rehidden by its finalizer. A green final task therefore means aggregate
`PASS`, while `evidence.json` remains the gate-level diagnostic for every
outcome.

`CONTRACT_INTEGRITY` seals exact bytes of the configured public test source set
and public case root, project-local Gradle build logic, the public semantic
definition, and resolved verification policy. A mismatch is a completed `FAIL`;
a legacy v1 escrow or missing approval is `INCOMPLETE`. In either case the four
downstream gates do not run and are recorded as `INCOMPLETE` with a safe
precondition reason. No previous JUnit or mutation artifact is borrowed.

The seal protects the public contract, not the production implementation. An
implementation agent may change production source; that is the work being
verified. It must not change the sealed tests, typed rows, Gradle logic, semantic
definition, or verification policy. In an existing-project handoff, a separate
public handoff manifest may record the starting production source and exported
files. That manifest helps the delivery workflow audit what was handed over; it
does not replace the reviewer approval seal or provide a sandbox.

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
published after the current run is evaluated. Public and reviewer verification
test tasks execute on every run rather than reusing up-to-date or cached test
outputs as current evidence.

The public `reports/spec/index.html` is therefore a post-verify artifact, not a
pre-handoff review. `reports/review/index.html` is the only pre-handoff HTML and
is reviewer-only.

## Reports and evidence boundary

All report pages are static, self-contained offline bundles.

### Reviewer contract review

After static validation, `toppleCatReview` writes
`build/topplecat/reports/review/index.html`. It contains public and reviewer
case rows, configured external Markdown context, compiler-defined Stage
sentences, and collapsed canonical source. It identifies itself as
reviewer-only and contains no invented PASS/FAIL status. A later failed check
removes a previous review rather than leaving stale authoring output.

### Public Spec report

After verification, `build/topplecat/reports/spec/index.html` and `data.json`
show public ACs, public rows, public Stage sentences, and any configured
external context. The context is labelled non-authoritative; Java tests and
typed rows remain the executable contract.

ToppleCat does not publish this public bundle when contract integrity is not
`PASS`; any previous stable Spec bundle is removed rather than presented as
current evidence.

### Reviewer verification report

`build/topplecat/reports/verification/index.html` and `data.json` include public
and reviewer executions, expected-consumption state, narrative steps, gate
results, durations, attachments, and applicable private failures. Keep this
bundle reviewer-only. It supports status filters and search by AC, title, or
case ID. A disabled safeguard is visibly `DISABLED` with its reason, never
rendered as a passing gate.

### Machine evidence and safe feedback

`build/topplecat/evidence.json` is the machine verdict and digests for the
current run. `build/topplecat/agent-feedback.json` is the only generated
diagnostic intended for an implementation agent. It communicates gate-level
results, including safe `DISABLED` reasons, without reviewer case identifiers,
inputs, expected values, source paths, private test names, internal task names,
attachments, or raw assertion failures. Generated HTML is evidence for humans,
not input to a later verdict.

For `CONTRACT_INTEGRITY`, safe feedback contains only one of two constant
reasons: that the public executable contract or verification policy changed
after reviewer approval, or that reviewer approval evidence is missing. It does
not reveal paths, digests, policy values, AC IDs, case data, assertions, or raw
failures. The detailed run-scoped `contract-integrity.json` remains
reviewer-only.

## Delivery hygiene

`toppleCatHide` moves reviewer source into plaintext reviewer-local custody at
`~/.topplecat/projects/<sha256-project-key>/escrow/`. The state contains the
manifest, hidden blobs, approval epoch, revisions, history, audit, lock, and
recovery data. It is not encryption or a sandbox, and `./gradlew clean` does not
delete it. A legacy project-local `.topplecat/escrow/` requires the explicit
`toppleCatMigrateEscrow` task and is removed only after a successful migration.
The correct order is Hide first, then create an implementation environment that
never exposes reviewer material.

Do not commit reviewer source to Git history the implementation agent can read.
Deleting `src/hiddenTest` or creating another worktree from that history does
not remove the old objects. The external workflow must provide a trusted
reviewer/CI boundary and exclude reviewer state, hidden source, build output,
and such Git history from the public handoff. Use one of these boundaries:

- a public export without `.git`, `.topplecat/`, or `build/`;
- an isolated environment whose repository history never contained reviewer
  material; or
- a public implementation repository paired with a separate private reviewer
  repository or CI environment.

ToppleCat itself does not provide an OS sandbox, control CI identity, or decide
whether a same-user Gradle/JVM process can inspect files. Home-directory custody
alone cannot defend against malicious build scripts or production code.

`toppleCatRestore` is for an authorized reviewer who needs to inspect or edit
the exact stored source. It is not an implementation-loop command.
