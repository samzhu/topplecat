# Verification Evidence

Read this reference before verifying a done claim or interpreting ToppleCat
outputs.

## Verification Command

Run:

```bash
./gradlew toppleCatVerify
```

By default the task:

1. acquires reviewer-source custody;
2. hides and restores reviewer source as needed;
3. runs public and reviewer JUnit verification;
4. enforces expected-value consumption;
5. runs the configured PIT mutation producer and per-AC gate;
6. writes current-run reports and evidence;
7. re-hides reviewer source.

Use `./gradlew test` only for the public implementation loop. A green public
test task is a done claim, not the final ToppleCat verdict.

## Verdicts

- `PASS`: every enabled required gate completed and passed.
- `FAIL`: at least one acceptance case or required gate failed.
- `INCOMPLETE`: a required producer or evidence stage did not complete.
- `DISABLED`: a reviewer explicitly disabled one safeguard. It is recorded as a
  decision, not represented as a passing gate.

Read the verdict from `build/topplecat/evidence.json` written by the
just-completed verification. Archived runs below `build/topplecat/runs/` are
diagnostic history; never use an older run to fill a gap in current evidence.

## Artifact Boundary

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
sentences. The Verification bundle contains reviewer cases, expected values, private
outcomes, and failure details.

`agent-feedback.json` contains gate-level safe summaries. It must not contain
hidden case IDs or values, reviewer test names, source paths, internal task
names, or raw failures.

## Failure Loop

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
