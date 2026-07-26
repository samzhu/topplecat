# Architecture

ToppleCat is a delegation verification gate. Java/JUnit code and typed JSON or
YAML rows are the source of truth; JSON evidence and HTML reports are derived
artifacts.

## Modules

| Module | Responsibility |
| --- | --- |
| `topplecat-core` | Case schema, evidence, escrow metadata, and safe feedback model. |
| `topplecat-junit` | `@ToppleTest` Stage DSL, `@ToppleAc`, typed case injection, expected consumption, and runtime stage records. |
| `topplecat-report` | Safe public and reviewer-only report projections plus static HTML rendering. |
| `topplecat-gradle-plugin` | Gradle tasks, source-set lifecycle, adversarial verification runs, report publication, and PIT mutation gate. |

## Contract Boundary

Public tests and case rows live under `src/test`. Reviewer-only test code and
case rows live together under `src/hiddenTest`. A hidden row targets an existing
public acceptance condition; it does not create another public contract.

For every canonical `@ToppleTest`, a small Java Stage DSL is both the execution
surface and the human projection: the method directly orchestrates only its
`@ToppleStageField` fields, while Stage steps record their sentence, perform
work, and return `self()`. `toppleCatCheck` validates that shape before review.
`@ToppleAc` is deliberately outside that restriction for supplementary JUnit
coverage.

`toppleCatHide` moves the whole reviewer source set into reviewer-local custody
at `~/.topplecat/projects/<sha256-project-key>/escrow/`
before implementation work. When hidden retest is enabled, `toppleCatVerify`
restores it only long enough to run verification, then rehides it in a
finalizer. The implementation loop normally runs plain `./gradlew test` against
public cases.

## Verification Runs

Every verification execution uses `build/topplecat/runs/current/` until the
report assigns it a fresh execution-time UUID and archives it below
`build/topplecat/runs/`. Public JUnit, reviewer JUnit, mutation, narrative,
and expected-consumption artifacts are collected from that one run. ToppleCat keeps
the latest three archives. Every run records `CONTRACT_INTEGRITY`, `JUNIT`,
`REVIEWER_JUNIT`, `EXPECTED_CONSUMPTION`, and `MUTATION` gates. A failed or
missing required stage does not borrow an older successful artifact: the final
evidence verdict becomes `INCOMPLETE` when ToppleCat cannot prove a required
stage completed. A reviewer may explicitly disable a safeguard; its gate is then
`DISABLED` with the configuration reason and does not block an aggregate `PASS`
unless another required gate is `FAIL` or `INCOMPLETE`.

After a run, stable copies are published below `build/topplecat/` for a user to
inspect. Those copies are convenience outputs, not inputs to a later verdict.
Public and reviewer verification tests execute freshly for each run rather
than treating Gradle up-to-date or cached output as current proof. After all
artifacts are published and the run is archived, aggregate `FAIL` or
`INCOMPLETE` fails the Gradle task; the reviewer-source rehide finalizer still
runs.

## Information Boundary

The report and review projection is intentionally split:

- `toppleCatCheck` is a static diagnostic. `toppleCatReview` depends on it and
  writes the reviewer-only bundle at `build/topplecat/reports/review/`. It
  presents AC/title and external context, Stage domain sentences, case rows,
  then collapsed canonical source, without execution data.
- `reports/spec/` contains public acceptance conditions and public case data
  only.
- `reports/verification/` contains reviewer case data, expected values, private
  test outcomes, and failure details.
- `agent-feedback.json` contains only a gate-level verdict and safe reasons,
  never reviewer data, source names, or internal task names.

PIT mutation is one of three adversarial safeguards enabled by default alongside
hidden retest and expected-consumption enforcement. Hidden retest asks whether
the implementation generalized beyond visible examples; mutation asks whether
the public executable contract detects broken production behavior. Without
consumer PIT setup, ToppleCat applies its default producer to `sourceSets.test`
only, discovers production package targets, and derives PIT `targetTests` from
compiler-emitted descriptors for every approved public canonical `@ToppleTest`
declaring class. This target is configured before PIT executes, enables the full mutation
matrix, and requires a 100% score for each acceptance condition. Reviewer rows
and reviewer-only JUnit tests are excluded from that producer. Attribution
matches the compiled canonical method identity against PIT's JUnit Unique ID
rather than assigning every mutant covered by the same test class; multiple AC
methods may therefore share a class without sharing a score. No per-case
mutation score is produced. An explicit consumer `targetTests` and a custom
mutation producer remain authoritative and are never overwritten. A usable
report that excludes a canonical test is `MUTATION=FAIL`; a missing or unusable
report is `MUTATION=INCOMPLETE`. A reviewer can explicitly disable any safeguard
through `toppleCat.adversarial`; evidence then records `DISABLED` with the
configuration reason instead of treating it as a pass.

## External Spec and Delivery Boundaries

External Markdown is optional reading context, not another contract. A consumer
explicitly configures `toppleCat.specDocs`; `AC-...` headings or paragraphs
anchor reading context to canonical `@ToppleTest` contracts, and
`toppleCatCheck` warns in either direction only when that configuration is
present. `toppleCatReview` is the pre-handoff reviewer projection. The public
`reports/spec/index.html` appears only after `toppleCatVerify`.

Reviewer custody is stored at `~/.topplecat/projects/<sha256-project-key>/escrow/`.
It is plaintext mechanical state, not encryption, sandboxing, or a security
boundary. The state contains manifest, hidden source blobs, approval epoch,
history, revisions, audit, lock, and recovery data. A legacy project-local
`.topplecat/escrow/` is handled only by explicit `toppleCatMigrateEscrow`, which
removes it after a successful migration. `./gradlew clean` does not remove
reviewer-local state, and removing `src/hiddenTest` does not erase it from Git
history.

ToppleCat relies on the external workflow to provide a trusted reviewer/CI
execution boundary. That workflow must hand agents only public source and safe
feedback, excluding reviewer state, hidden source, build artifacts, and any Git
history that contained reviewer material. ToppleCat does not decide OS access,
sandboxing, CI identity, or whether same-user Gradle/JVM code can inspect files;
home-directory custody alone cannot defend against malicious build scripts or
production code.
