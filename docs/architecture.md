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

## Contract boundary

An external SDD, workflow, or task system selects the current Spec or delivery,
tracks earlier deliveries, and decides who may approve or release work.
ToppleCat does not manage tasks, Spec lifecycles, delivery history, pull-request
approval, or organizational sign-off.

ToppleCat starts at the executable acceptance boundary. It connects selected
acceptance conditions to ordinary Java/JUnit tests and typed case rows, keeps
the public executable contract handed to the implementation agent identical to
the contract executed by verification, and checks the agent's done claim.
People remain responsible for making the selected rules and cases complete.
ToppleCat does not infer missing business requirements or judge behavior
outside that executable contract.

The `ReviewerContractApproval` stored with custody is named for its mechanical
role in the verification protocol. It seals exact public contract inputs and
the effective verification policy so later changes can be detected. It is not
evidence that a person or organization approved a task, Spec, pull request, or
release. `toppleCatReview` likewise produces a reading projection; it cannot
know whether anyone reviewed or accepted it.

Every generated report is a projection of that checked executable contract.
Report generation may improve presentation, but it must not add, omit, or
reinterpret a rule, case, expected value, or compiler-defined scenario step.
HTML and JSON therefore cannot become an independent authoring surface or an
input that changes a later verdict.

Public tests and case rows live under `src/test`. Reviewer-only test code and
case rows live together under `src/hiddenTest`. A hidden row targets an existing
public acceptance condition; it does not create another public contract.

For every canonical `@ToppleTest`, a small Java Stage DSL is both the execution
surface and the human projection: the method directly orchestrates only its
`@ToppleStageField` fields, while Stage steps record their sentence, perform
work, and return `self()`. `toppleCatCheck` validates that shape before review.
`@ToppleAc` stays outside that restriction because it is supplementary JUnit
coverage.

`toppleCatHide` moves the whole reviewer source set into reviewer-local custody
at `~/.topplecat/projects/<sha256-project-key>/escrow/`
before implementation work. When hidden retest is enabled, `toppleCatVerify`
restores it only long enough to run verification, then rehides it in a
finalizer. The implementation loop normally runs plain `./gradlew test` against
public cases.

## Verification runs

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

## Information boundary

Reports are split by audience:

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

PIT mutation, hidden retests, and expected-consumption checks are enabled by
default. Hidden retests ask whether the implementation works beyond the visible
examples. Mutation asks whether the public contract notices broken production
behavior.

Without consumer PIT setup, ToppleCat runs the default producer against
`sourceSets.test`, discovers production packages, and reads compiler descriptors
to find every approved canonical `@ToppleTest` class. PIT uses the full mutation
matrix and requires 100% for each acceptance condition. Reviewer rows and
reviewer-only JUnit tests are not part of that score.

Attribution matches a compiled canonical method with PIT's JUnit Unique ID.
Two AC methods can share a class without sharing a mutation score. ToppleCat
does not calculate per-case scores. Consumer-owned `targetTests` and custom
producers are left alone: excluding a canonical test produces
`MUTATION=FAIL`, while a missing or unusable report produces
`MUTATION=INCOMPLETE`. If a reviewer disables a safeguard through
`toppleCat.adversarial`, evidence records `DISABLED` and the reason.

## External spec and delivery boundaries

External Markdown is optional reading context, not another contract. A delivery
is selected explicitly with repeatable repository-relative
`--spec <path>` options on Check, Review, Hide, UpdateEscrow, and Verify.
Selected `AC-...` headings or paragraphs must each anchor a canonical public
`@ToppleTest`; the selected paths, document digests, and normalized AC set are
sealed into reviewer approval. Verification refuses a mismatched selection. It
runs selected hidden rows and `@ToppleTest`/`@ToppleAc` tagged reviewer checks
by default, deriving selected-AC reviewer coverage from current-run execution
evidence rather than reviewer source text. The public contract and mutation remain full-contract;
`--all-hidden` broadens only that hidden retest. The run writes a scope artifact
and reviewer projections display the exact scope. Plain reviewer `@Test`
methods are not ToppleCat evidence and are flagged in Review.

`toppleCat.specDocs` remains a compatibility-only reading-context setting. It
does not select or seal a delivery and retains warning-only alignment checks.
`toppleCatReview` is the pre-handoff reviewer projection. The public
`reports/spec/index.html` appears only after `toppleCatVerify`.

The surrounding workflow decides which Spec documents describe the current
delivery. ToppleCat does not discover an active Spec from a branch name, migrate
old delivery state, or force one review process across Spec Kit, Superpowers,
OpenSpec, or another SDD tool. Its portable integration point is the executable
AC identity: external context may point to an `AC-...`, and that AC must bind to
the Java/JUnit contract that ToppleCat can run. The purpose of Spec integration
is to make acceptance executable, not to turn ToppleCat into a Spec repository
or task tracker.

Reviewer custody is stored at `~/.topplecat/projects/<sha256-project-key>/escrow/`.
It is plaintext mechanical state, not encryption, sandboxing, or a security
boundary. The state contains manifest, hidden source blobs, approval epoch,
history, revisions, audit, lock, and recovery data. A legacy project-local
`.topplecat/escrow/` is handled only by explicit `toppleCatMigrateEscrow`, which
removes it after a successful migration. `./gradlew clean` does not remove
reviewer-local state, and removing `src/hiddenTest` does not erase it from Git
history.

The approval payload added delivery scope in schema v2. A valid v1 approval
created by 0.0.5 is verified with its original digest and read as the equivalent
empty selection, which retains the previous full-contract meaning. This
compatibility read does not approve the new plugin version or policy; the
authorized reviewer must use the normal Restore, Check, Review, and
UpdateEscrow path before verification can pass.

ToppleCat relies on the external workflow to provide a trusted reviewer/CI
execution boundary. That workflow must hand agents only public source and safe
feedback, excluding reviewer state, hidden source, build artifacts, and any Git
history that contained reviewer material. ToppleCat does not decide OS access,
sandboxing, CI identity, or whether same-user Gradle/JVM code can inspect files;
home-directory custody alone cannot defend against malicious build scripts or
production code.
