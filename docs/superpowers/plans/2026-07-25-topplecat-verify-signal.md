# ToppleCat Verify Exit-Signal Implementation Plan

> **For the downstream implementation agent:** Use the
> `test-driven-development` skill for the behavior change and the
> `verification-before-completion` skill before claiming completion. Do not
> push, tag, publish, or open a pull request.

**Goal:** Make `toppleCatReport` (and therefore `toppleCatVerify`) fail the
Gradle build whenever the aggregate verdict it just wrote is not `PASS`,
without changing what produces that verdict, without weakening the
`report -> rehide` finalizer, and without adding any new opt-out.

**Architecture:** The change is a single throw at the end of
`ToppleCatReportTask.report()`, after every output file is already written.
No other task, schema, or wiring changes.

**Tech stack:** Java 25, Gradle Java Plugin/TestKit, JUnit Jupiter.

## Global Constraints

- Read the approved design at
  `docs/superpowers/specs/2026-07-25-topplecat-verify-signal-design.md`
  before editing.
- Touch only `ToppleCatReportTask.java`, its functional/unit tests, and the
  documentation passages listed below.
- Do not add a flag, extension property, or task to make the new failure
  behavior optional.
- Do not change the `evidence.json`, `agent-feedback.json`, or report HTML
  schemas.
- Do not weaken or remove `report.configure(task -> task.finalizedBy(rehide))`
  (`ToppleCatPlugin.java:377`).
- Preserve unrelated worktree changes. Never commit `build/`, `.topplecat/`,
  credentials, or temporary notes.

## Intended Commit Sequence

1. `fix: fail toppleCatReport when the verdict is not PASS`

One commit. This is a small, self-contained behavior fix plus its required
test and documentation updates.

## File Map

- Modify
  `topplecat-gradle-plugin/src/main/java/io/github/samzhu/topplecat/gradle/ToppleCatReportTask.java`.
- Modify
  `topplecat-gradle-plugin/src/test/java/io/github/samzhu/topplecat/gradle/ToppleCatPluginFunctionalTest.java`.
- Modify `README.md`, `README.zh-TW.md`,
  `docs/guide/verification-and-evidence.md`, and
  `docs/guide/troubleshooting.md`.
- Optionally modify `docs/architecture.md` (one added clause; see Task 3).

## Task 0: Establish the Baseline

**Step 1: Confirm scope and worktree state**

Run:

```bash
git status --short --branch
git log --oneline -5
```

Expected: `main` contains the approved design commit for this plan and the
prior `0.0.2` core-correctness commits. Preserve every unrelated change; stop
and report any overlap with the files in this plan.

**Step 2: Run the current narrow suite**

Run:

```bash
./gradlew :topplecat-gradle-plugin:test
```

Expected: PASS before introducing the regression test.

## Task 1: Add the Failing Regression

**Files:**

- Modify
  `topplecat-gradle-plugin/src/test/java/io/github/samzhu/topplecat/gradle/ToppleCatPluginFunctionalTest.java`

**Step 1: Add a new test proving the gap**

Add a test near the existing
`recordsMutationIncompleteWithoutSchedulingPitWhenProductionPackagesCannotBeFound`
test (around line 567), named
`failsToppleCatVerifyWhenAggregateVerdictIsIncompleteEvenWithoutAnyFailingGate`.
Reuse that test's fixture shape (a project with no `src/main/java` production
sources, so `MUTATION` is `INCOMPLETE` while `JUNIT` and `REVIEWER_JUNIT`
would otherwise be `PASS`/`DISABLED`). Run
`runner("toppleCatVerify", "--stacktrace").buildAndFail()` and assert:

- the overall build fails;
- `result.task(":toppleCatReport").getOutcome()` is `TaskOutcome.FAILED`;
- `evidenceVerdict(project)` is `EvidenceVerdict.INCOMPLETE`;
- `build/topplecat/evidence.json`, the Spec/Verification HTML reports, and
  `agent-feedback.json` all exist and contain the same `INCOMPLETE` content
  the current passing test already checks for (reuse
  `assertMutationIncompleteWithoutPit`'s existing content assertions where
  practical, adapted for `buildAndFail()`);
- `toppleCatRehide` still ran to completion and
  `Files.exists(project.resolve("src/hiddenTest"))` is `false`.

Run:

```bash
./gradlew :topplecat-gradle-plugin:test \
  --tests '*ToppleCatPluginFunctionalTest.failsToppleCatVerifyWhenAggregateVerdictIsIncompleteEvenWithoutAnyFailingGate'
```

Expected before the fix: FAIL, because `toppleCatReport` currently succeeds
and the build does not fail.

## Task 2: Implement the Throw

**Files:**

- Modify
  `topplecat-gradle-plugin/src/main/java/io/github/samzhu/topplecat/gradle/ToppleCatReportTask.java`

**Step 1: Add the import**

Add `import org.gradle.api.GradleException;` to the existing import block
(it currently imports `org.gradle.api.DefaultTask` and other `org.gradle.api`
types but not `GradleException`).

**Step 2: Throw after every output is written**

At the end of `report()`, after the existing final log line
(`ToppleCatReportTask.java:196`), add:

```java
getLogger().lifecycle("ToppleCat verification report written: {}", verdict);
if (verdict != EvidenceVerdict.PASS) {
    throw new GradleException("ToppleCat verification verdict is " + verdict
            + "; see " + evidencePath + " for gate-level detail.");
}
```

`verdict` and `evidencePath` are both already local variables in this method
(`ToppleCatReportTask.java:176`, `:189`); do not introduce new fields or
recompute either value. Do not move this check earlier than the existing
`publishStableArtifacts`/`VerificationRunWorkspace.archive` calls
(`ToppleCatReportTask.java:194-195`) — every output must already be on disk
before the throw.

Run the Task 1 regression again. Expected: PASS.

**Step 3: Confirm the PASS path is unaffected**

Run:

```bash
./gradlew :topplecat-gradle-plugin:test \
  --tests '*ToppleCatPluginFunctionalTest.recordsDisabledExpectedConsumption*'
./gradlew :topplecat-gradle-plugin:test \
  --tests '*ToppleCatPluginFunctionalTest.livePitKeepsAcceptanceConditionsSeparateWhenTheyShareATestClass'
```

Expected: the first still passes unmodified (all-`PASS`/`DISABLED` scenario).
The second is expected to still fail overall (it already asserts
`.buildAndFail()` because `MUTATION` is `FAIL`), but do not yet change its
assertions — that happens in Task 3.

## Task 3: Reconcile Every Existing Verdict Assertion

**Files:**

- Modify
  `topplecat-gradle-plugin/src/test/java/io/github/samzhu/topplecat/gradle/ToppleCatPluginFunctionalTest.java`

**Step 1: Find every affected assertion**

Run:

```bash
grep -n '\.build()\|\.buildAndFail()\|TaskOutcome\.SUCCESS.*toppleCatReport\|assertMutationIncompleteWithoutPit' \
  topplecat-gradle-plugin/src/test/java/io/github/samzhu/topplecat/gradle/ToppleCatPluginFunctionalTest.java
```

Every test whose scenario produces a non-`PASS` aggregate verdict (`FAIL` or
`INCOMPLETE`) and currently calls `.build()` must change to
`.buildAndFail()`. Every assertion of `TaskOutcome.SUCCESS` for
`:toppleCatReport` in a non-`PASS` scenario must change to
`TaskOutcome.FAILED`. Two locations are already known and must be updated:

- `assertMutationIncompleteWithoutPit` (`:1497-1506`): change
  `assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatReport").getOutcome());`
  to `TaskOutcome.FAILED`. Its two call sites,
  `recordsMutationIncompleteWithoutSchedulingPitWhenProductionPackagesCannotBeFound`
  (around `:582` and `:591`), must call `.buildAndFail()` instead of
  `.build()`.
- The reviewer-retest/mutation-incomplete scenario around `:1269-1300`: at
  `:1278`,
  `assertEquals(TaskOutcome.SUCCESS, failed.task(":toppleCatReport").getOutcome());`
  must become `TaskOutcome.FAILED` (that scenario's verdict is `FAIL` because
  `JUNIT` fails, so `toppleCatReport` now fails too). The other `buildAndFail()`
  block in the same test, around `:1295-1299`, was not previously checking
  `toppleCatReport`'s outcome; leave its existing assertions and optionally
  add the same `TaskOutcome.FAILED` check for consistency.

Do not change any test whose scenario legitimately produces `PASS` — those
must keep `.build()` and keep asserting `TaskOutcome.SUCCESS`.

**Step 2: Run the full plugin test module**

Run:

```bash
./gradlew :topplecat-gradle-plugin:test
```

Expected: PASS, with every non-`PASS` scenario now asserting a failed build
and a `FAILED` `toppleCatReport` outcome, and every `PASS` scenario
unchanged.

## Task 4: Correct the Documentation

A prior, smaller documentation-only change added text to four files stating
that `toppleCatVerify`/`toppleCatReport` always exit `0` regardless of
verdict. That text is now inaccurate and must be replaced, not merely
supplemented.

**Files:**

- Modify `README.md`
- Modify `README.zh-TW.md`
- Modify `docs/guide/verification-and-evidence.md`
- Modify `docs/guide/troubleshooting.md`

**Step 1: `README.md`**

In the "Read the Result" section, replace the paragraph that begins
`` `toppleCatVerify` and `toppleCatReport` exit `0` once evidence has been
written`` with:

```text
`toppleCatVerify` and `toppleCatReport` fail the Gradle build whenever the
aggregate verdict is not `PASS`, after evidence, reports, and
`agent-feedback.json` have already been completely written. A green
`toppleCatVerify` run is proof of a `PASS` verdict; read `evidence.json` only
when you need gate-level detail behind a failure.
```

**Step 2: `README.zh-TW.md`**

In the "閱讀結果" section, replace the paragraph that begins
`` `toppleCatVerify` 與 `toppleCatReport` 只要成功寫出 evidence`` with:

```text
`toppleCatVerify` 與 `toppleCatReport` 只要最終 verdict 不是 `PASS`，就會讓
Gradle build 失敗——此時 evidence、報表與 `agent-feedback.json` 都已經完整
寫出。`toppleCatVerify` 跑成功（綠燈）就代表 verdict 是 `PASS`；只有在失敗時
才需要去讀 `evidence.json` 取得 gate 層級的細節。
```

**Step 3: `docs/guide/verification-and-evidence.md`**

In the "Verdicts" section, replace the paragraph that begins
`` `toppleCatVerify` and `toppleCatReport` exit `0` as soon as evidence has
been written`` with:

```text
`toppleCatVerify` and `toppleCatReport` fail the Gradle build whenever the
aggregate verdict is not `PASS`, after evidence, reports, and
`agent-feedback.json` have already been completely written. A green
`toppleCatVerify` run is therefore proof of a `PASS` verdict on its own; a
reviewer or CI pipeline only needs `evidence.json` to see gate-level detail
behind a failure, not to learn whether one occurred.
```

**Step 4: `docs/guide/troubleshooting.md`**

In the "A Safeguard Is Disabled or Incomplete" section, replace the sentence
that begins `` `toppleCatVerify` and `toppleCatReport` still exit `0` when
the verdict is `FAIL` or `INCOMPLETE` `` (and the two sentences that follow
it, ending `...do not rely on the task's exit code alone.`) with:

```text
`toppleCatVerify` and `toppleCatReport` now fail the Gradle build itself when
the verdict is `FAIL` or `INCOMPLETE`, once evidence and reports have already
been completely written. Read `evidence.json` for gate-level detail once you
know a failure occurred; you no longer need to read it just to detect one.
```

**Step 5 (optional): `docs/architecture.md`**

In the "Verification Runs" section, the existing sentence "The final or
missing required stage does not borrow an older successful artifact: the
final evidence verdict becomes `INCOMPLETE` when ToppleCat cannot prove a
required stage completed" may gain one trailing clause: "...and
`toppleCatReport`/`toppleCatVerify` fail the Gradle build in that case." Keep
this to a single added clause; do not restructure the surrounding paragraph.

**Step 6: Verify docs**

Run:

```bash
python3 scripts/verify-docs.py
git diff --check
```

Expected: PASS.

## Task 5: Full Verification and Handoff

**Step 1: Run the repository contract**

Run exactly:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
```

Expected: both commands exit `0`. `scripts/verify-release.sh` still passes
unmodified — its own `grep -Fq '"PASS"' evidence.json` check remains valid
and is now a redundant but harmless second confirmation of the same signal
`toppleCatVerify`'s exit code already carries.

**Step 2: Inspect hygiene and scope**

Run:

```bash
git diff --check
git status --short --branch
git log --oneline --decorate -3
git ls-files | rg '(^|/)(build|\.topplecat)/'
```

Expected:

- no tracked generated `build/` or local `.topplecat/` files;
- no credentials or temporary reviewer notes;
- one implementation commit after the design/plan commits;
- no unrelated files changed;
- no push, tag, release, or publication.

**Step 3: Return an evidence-based done claim**

Report:

- the commit hash and subject;
- the exact results of both full verification commands;
- confirmation that every previously-`.build()` non-`PASS` scenario in
  `ToppleCatPluginFunctionalTest.java` now uses `.buildAndFail()` with a
  `FAILED` `toppleCatReport` outcome;
- confirmation that every `PASS`/`DISABLED` scenario is unchanged;
- confirmation that `toppleCatRehide` still completes in every failing
  scenario exercised by the test suite;
- confirmation that no public evidence or feedback schema changed;
- confirmation that the work remains local and unpushed.

Stop there. The upstream reviewer will inspect every acceptance criterion in
the design, review the diff, and rerun the full commands independently.
