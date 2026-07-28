# Verification evidence

Read this reference before verifying a done claim or interpreting ToppleCat
outputs.

## Contents

- Verification command
- Safeguard roles
- Verdicts
- Artifact boundary
- Failure loop

## Verification command

Run Verify with the delivery scope pinned by `SKILL.md`:

```bash
./gradlew toppleCatVerify --spec specs/SPEC-42/spec.md
```

By default the task:

1. acquires reviewer-source custody;
2. freshly compares the current public contract and effective policy with the
   reviewer approval (`CONTRACT_INTEGRITY`);
3. hides and restores reviewer source as needed, then runs public and reviewer
   JUnit verification only when integrity passed;
4. enforces expected-value consumption;
5. runs the configured PIT mutation producer and per-AC gate;
6. writes current-run reports and evidence;
7. re-hides reviewer source.

For the managed PIT producer, `targetTests` comes from compiler-emitted
descriptors for every public canonical `@ToppleTest` declaring class. This
keeps coverage valid when production and test packages differ. Consumer-owned
`targetTests` and custom mutation producers are preserved. A usable report that
does not cover a canonical test is a mutation `FAIL`; a missing, malformed, or
interrupted report is `INCOMPLETE`.

Use `./gradlew test` only for the public implementation loop. A green public
test task is a development signal, not the final ToppleCat verdict.

When reviewer custody contains only hidden rows, the public canonical
`@ToppleTest` execution is also the reviewer-row retest evidence. A passing row
set can therefore make `REVIEWER_JUNIT` pass without a placeholder hidden Java
class; any hidden Java tests still require their own passing `hiddenTest` result.
For a selected delivery, every selected AC must have current-run evidence from
an executed hidden row or an executed AC-bound reviewer Java test. Source text,
plain JUnit, and disabled tests do not satisfy this gate.

## Safeguard roles

- `CONTRACT_INTEGRITY` rejects public contract or verification-policy changes
  made after reviewer approval.
- Hidden retests exercise independently chosen business cases. They improve
  functional coverage but cannot expose every hard-coded shortcut.
- Expected consumption proves that each declared top-level expected value was
  asserted, not merely read.
- Mutation asks whether the public executable contract detects changed
  production behavior. It does not use reviewer rows to improve the public
  mutation score.

Report the gate that rejected the claim. Do not describe a mutation failure as
a hidden-retest failure, or a passing hidden retest as proof that no hard-coding
exists.

## Verdicts

- `PASS`: every enabled required gate completed and passed.
- `FAIL`: at least one acceptance case or required gate failed. In particular,
  a completed approval mismatch is `CONTRACT_INTEGRITY: FAIL`.
- `INCOMPLETE`: a required producer or evidence stage did not complete.
- `DISABLED`: a reviewer explicitly disabled one safeguard. It is recorded as a
  decision, not represented as a passing gate.

Read the verdict from `build/topplecat/evidence.json` written by the
just-completed verification. Archived runs below `build/topplecat/runs/` are
diagnostic history; never use an older run to fill a gap in current evidence.
The archived current run also contains `verification-scope.json`, which records
the selected Spec paths and digests, AC set, hidden mode, and full mutation
scope.

The gate order is `CONTRACT_INTEGRITY`, `JUNIT`, `REVIEWER_JUNIT`,
`EXPECTED_CONSUMPTION`, then `MUTATION`. Contract integrity cannot be disabled.
If it is not `PASS`, the four downstream gates must be current-run
`INCOMPLETE`; their old results and a stale public Spec bundle are never proof.

## Artifact boundary

Public or safe-to-share artifacts:

```text
build/topplecat/reports/spec/index.html
build/topplecat/reports/spec/data.json
build/topplecat/agent-feedback.json
```

Reviewer-only artifacts:

```text
build/topplecat/reports/review/index.html
build/topplecat/reports/verification/index.html
build/topplecat/reports/verification/data.json
build/topplecat/evidence.json
mutation results and raw test failures
```

The Spec bundle contains only public ACs, public rows, and public narrative
sentences. The Verification bundle contains reviewer cases, expected values,
private outcomes, and failure details.

`agent-feedback.json` contains gate-level safe summaries. It must not contain
hidden case IDs or values, reviewer test names, source paths, internal task
names, or raw failures.

For integrity failure it may say only that the public executable contract or
verification policy changed after reviewer approval. For missing approval it may
say only that an authorized reviewer must review and reseal. Detailed digests,
changed paths, and policy fields stay in reviewer-only run artifacts.

## Failure loop

1. Diagnose the failed AC or gate with reviewer-only artifacts.
2. Decide whether the public contract is incomplete or production behavior is
   wrong.
3. Change public contract material only when the approved AC was underspecified.
4. Give the implementation agent public changes and safe agent feedback.
5. Keep hidden values and reviewer reports private.
6. Rerun public tests during implementation, then run a fresh
   `toppleCatVerify`.

Accept the done claim only when current-run evidence reports `PASS`, every
disabled safeguard is an explicit reviewer decision, and reviewer source is
hidden again. For `FAIL` or `INCOMPLETE`, reject the claim, return only safe
feedback, and keep reviewer source hidden.
