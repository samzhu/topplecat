# ToppleCat Verify Exit-Signal Design

## Status

Approved for implementation.

## Product Goal

ToppleCat exists to help a human reject an AI agent's false done claim. That
promise fails if the reviewer or CI pipeline that runs the final gate cannot
trust the gate's own exit code. A reviewer must not have to separately parse
`evidence.json` after every run just to learn that verification did not
actually pass.

This change makes `toppleCatReport` (and therefore `toppleCatVerify`, which
wraps it) fail the Gradle build whenever the aggregate verdict it just wrote
is not `PASS`. It does not change what produces `PASS`, `FAIL`, or
`INCOMPLETE`, and it does not add any new task, flag, or schema.

## Problem

`ToppleCatReportTask.report()` computes the aggregate verdict
(`ToppleCatReportTask.java:176-180`), writes `evidence.json`, the HTML
reports, and `agent-feedback.json` (`ToppleCatReportTask.java:181-195`), then
only logs the verdict (`ToppleCatReportTask.java:196`). It never throws.
`toppleCatVerify` itself is an empty lifecycle task
(`ToppleCatPlugin.java:257-262`) that adds no assertion of its own.

A genuine `FAIL` usually already fails the Gradle build today, because the
gate tasks (`toppleCatVerificationTest`, `hiddenTest`, `toppleCatMutationGate`)
are wired to `report` with `finalizedBy`, not `dependsOn`
(`ToppleCatPlugin.java:248-250`), and each of those gate tasks throws on its
own when it genuinely fails: a `Test` task's default `ignoreFailures=false`
fails it on any failing JUnit test, and `ToppleCatMutationGateTask` throws
explicitly when the mutation threshold is not met
(`ToppleCatMutationGateTask.java:82`). This is confirmed by existing
functional tests that already call `.buildAndFail()` for `MUTATION` and
`JUNIT` failures (`ToppleCatPluginFunctionalTest.java:478`, `:550`, `:684`,
`:1276`).

The gap is `INCOMPLETE`, and any `FAIL` that `toppleCatReport`'s own
aggregation might someday compute without an upstream gate task having
thrown. When a required stage never runs at all — no `src/hiddenTest`, no
usable PIT producer, an interrupted required stage — no task throws, so
Gradle exits `0` while `evidence.json` says `INCOMPLETE`. The existing test
`assertMutationIncompleteWithoutPit` (`ToppleCatPluginFunctionalTest.java:1497-1506`)
pins exactly this: it asserts `TaskOutcome.SUCCESS` for `:toppleCatReport`
while `evidenceVerdict(project) == INCOMPLETE`, and its two call sites
(`:582`, `:591`) both use `.build()`, not `.buildAndFail()`.

A reviewer or CI pipeline that only checks `./gradlew toppleCatVerify`'s exit
code cannot currently distinguish "everything genuinely passed" from "a
required stage silently never ran." Relying on every consumer to remember to
additionally parse `evidence.json` defeats the point of a terminal CI gate.

## Required Semantics

After `toppleCatReport` has completely written `evidence.json`, the Spec and
Verification HTML reports, and `agent-feedback.json` — that is, strictly
after the existing `publishStableArtifacts`/archive/log sequence at
`ToppleCatReportTask.java:194-196` — the task must fail
(`throw new org.gradle.api.GradleException(...)`) whenever the aggregate
verdict is not `PASS`.

This applies uniformly to `FAIL` and `INCOMPLETE`. Do not special-case only
`INCOMPLETE`: the task's own exit code must be a complete, self-sufficient
signal, not one that happens to work today only because certain `FAIL` paths
coincidentally already fail via a separate upstream task.

- The exception message must state the aggregate verdict and point to the
  `evidence.json` path for gate-level detail. It must not add reviewer
  values, case identifiers, expected values, or source paths beyond the
  evidence file's own path — the same safe-feedback boundary this task
  already enforces for `agent-feedback.json`.
- Evidence, reports, and `agent-feedback.json` must already be complete and
  correct before the exception is thrown. The exception must never cause any
  of those outputs to be skipped, truncated, or partially written.
- `toppleCatRehide` must keep running whether or not `toppleCatReport`
  throws. Its existing wiring
  (`report.configure(task -> task.finalizedBy(rehide))`,
  `ToppleCatPlugin.java:377`) already guarantees this, because a `finalizedBy`
  task runs once the finalized task has started, regardless of that task's
  outcome. This design requires no change to that wiring and must not weaken
  it.
- `toppleCatVerify` needs no code change. It already only wraps `report` via
  `finalizedBy`, so once `toppleCatReport` fails, `toppleCatVerify` and the
  overall build already fail too.
- A `DISABLED` gate must continue to never cause a throw by itself. This is
  already true — `DISABLED` is treated as satisfying the `PASS` branch of the
  verdict computation (`ToppleCatReportTask.java:178-179`) — and must remain
  true as a non-regression, not be reintroduced as a new special case.
- `toppleCatCheck`, `toppleCatReview`, `toppleCatHide`, `toppleCatRestore`,
  and `toppleCatUpdateEscrow` are unaffected. This design touches only
  `ToppleCatReportTask`.

## Non-Goals

- No new Gradle task and no `toppleCat.adversarial`-style opt-out for this
  behavior. A silent-on-non-PASS terminal gate is the defect being fixed, not
  a feature some consumers should be able to keep; there is no supported way
  to run `toppleCatVerify`/`toppleCatReport` and have it stay green on a
  non-`PASS` verdict.
- No change to the `evidence.json`, `agent-feedback.json`, or HTML report
  schemas.
- No change to which stage produces `PASS`, `FAIL`, `INCOMPLETE`, or
  `DISABLED` — only whether the already-computed verdict now fails the
  Gradle task that computed it.
- Does not address daemon interruption between `toppleCatRestore` and
  `toppleCatRehide` leaving `src/hiddenTest` visible. That risk is already
  covered by the existing "local hidden storage is plaintext mechanical
  state, not a secrecy boundary" language in `docs/architecture.md` and is
  unrelated to this change.
- Does not touch `scripts/verify-release.sh`. Its existing `grep -Fq '"PASS"'
  evidence.json` check remains valid and does not need to change; it becomes
  a redundant but harmless second confirmation once this fix lands.

## Acceptance Criteria

- A run whose required gates all reach `PASS` (or explicit `DISABLED`) still
  leaves `toppleCatReport` and `toppleCatVerify` at `TaskOutcome.SUCCESS`.
  Every existing all-`PASS` functional test that calls `.build()` keeps
  passing unmodified.
- A run with an `INCOMPLETE` gate (no PIT producer configured, no reviewer
  JUnit present, an interrupted required stage) now fails
  `toppleCatReport`/`toppleCatVerify` (`.buildAndFail()`), while
  `evidence.json`, the HTML reports, and `agent-feedback.json` are still
  completely written with the correct `INCOMPLETE` content.
- A run with a `FAIL` gate continues to fail the build, and
  `toppleCatReport`'s own task outcome is now also `FAILED` in that case
  (currently asserted `SUCCESS` at
  `ToppleCatPluginFunctionalTest.java:1278`; that assertion must be updated,
  not preserved).
- In every failing scenario above, `toppleCatRehide` still completes and
  `src/hiddenTest` is not left visible in the working tree.
- `agent-feedback.json` still contains only gate-level results; the new
  exception message introduces no reviewer detail.
- A run where every adversarial safeguard is deliberately disabled remains
  `PASS`/`SUCCESS`, unchanged.

## Compatibility

This is a deliberate breaking behavior change for any automation that
currently treats `toppleCatVerify`'s exit code as always `0`. That prior
behavior is the exact defect this change repairs — a reviewer or CI pipeline
could not trust a green build — so it is treated as a bug fix, not an
opt-in feature. Gate names, evidence schemas, task names, plugin ID, and
default thresholds are unchanged.

## Downstream Implementation Boundary

The implementing agent may change `ToppleCatReportTask.java`, its functional
and unit tests, and the documentation passages listed in the companion plan.
It must not:

- Add a flag, extension property, or task to make this behavior optional.
- Change the `evidence.json`, `agent-feedback.json`, or report schemas.
- Weaken or remove the `report -> rehide` `finalizedBy` wiring.
- Broaden this change beyond `ToppleCatReportTask` and the documentation that
  describes its exit behavior.

When implementation is reported done, an independent review must compare the
result against every acceptance criterion above and rerun the full repository
verification commands before accepting the claim.
