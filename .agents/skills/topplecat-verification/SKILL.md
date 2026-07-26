---
name: topplecat-verification
description: Gate Java delegation with ToppleCat. Use when turning an approved Spec or AC into an executable @ToppleTest contract, preparing or hiding reviewer retests, implementing against an approved public contract, independently verifying a done claim, or assessing ToppleCat adoption for Java/JUnit. Don't use for ordinary unit tests or non-Java work.
---

# ToppleCat Verification

Treat ToppleCat as a gate state machine. Enter at the first unmet gate, complete
that branch, and preserve reviewer custody throughout the run.

## Gate Invariants

- Java acceptance tests plus typed JSON/YAML rows are the executable authority.
  Markdown is optional reading context; generated JSON and HTML are projections.
- Every approved AC has a literal Java binding. Every data-driven AC has one
  canonical `@ToppleTest`; a Markdown-only AC is incomplete.
- A canonical method is ordered `@ToppleStageField` orchestration. Stages own
  setup, production calls, assertions, attachments, and control flow.
- Public material lives under `src/test`; reviewer-owned material lives under
  `src/hiddenTest`. Agents receive neither reviewer source nor
  `~/.topplecat/projects/<sha256-project-key>/escrow/` or private reports.
- `./gradlew test` is an implementation signal. Only current-run
  `build/topplecat/evidence.json` is the final contract verdict.

## Select The Gate

1. Inspect the Gradle build, approved task, Java bindings, case roots,
   `src/hiddenTest`,
   `~/.topplecat/projects/<sha256-project-key>/escrow/manifest.json`, legacy
   `.topplecat/escrow/manifest.json`, and current evidence.
2. Choose exactly one branch:
   - **Author** for a new or changed AC, case, DTO expectation, or Stage.
   - **Review and hide** when static checks are green but reviewer sign-off or
     custody transfer is incomplete.
   - **Implement** when the public contract is approved and reviewer source is
     already hidden.
   - **Verify** when an implementation agent has made a done claim.
   - **Restore** when an authorized reviewer must inspect or edit hidden source.
   - **Assess adoption** when a Java/JUnit project has not adopted ToppleCat.
3. State the selected branch, visible source boundary, and first unmet gate
   before editing or running a downstream task.

**Completion criterion:** one branch is selected from observed repository state,
and no later gate runs before that branch's preconditions are satisfied.

## Execute The Branch

### Author

1. Read `references/authoring.md` before changing an AC, binding, case row, DTO
   expectation, Stage, or attachment.
2. Obtain the repository's immutable task or Spec ID and assign literal AC IDs.
3. In one change, author the public Java binding, representative public rows,
   and independently derived reviewer boundaries.
4. Run `./gradlew toppleCatCheck`; resolve every source error and every
   configured Spec-alignment warning.
5. Run the narrow public test. New behavior must produce a meaningful red result
   before implementation; record why an existing behavior is already green.

**Completion criterion:** affected ACs have literal bindings, valid public rows,
consumed expected keys, meaningful reviewer rows, and a passing Check.

### Review And Hide

1. Read `references/reviewer-custody.md` before opening reviewer material or
   changing custody state.
2. Run `./gradlew toppleCatReview` and inspect
   `build/topplecat/reports/review/index.html` AC by AC. Correct source and repeat
   Review until the contract is accepted.
3. If the current user is not the authorized reviewer, report the review path
   and stop for explicit sign-off.
4. After sign-off, run `./gradlew toppleCatHide`; it seals public contract and
   policy. Ordinary Hide never refreshes a seal.
5. Prepare a public export without `.git`, `.topplecat/`, `build/`, reviewer-local
   state, or `src/hiddenTest`, or an isolated implementation environment whose
   Git history never contained reviewer material.

**Completion criterion:** reviewer accepts every AC; Hide completes; the
implementation tree contains only public contract and production material.

### Implement

1. Work from production source, public Spec context, public Java acceptance
   tests, and public case rows.
2. Use `./gradlew test` as the development loop until the public contract is
   green.
3. Return a done claim without opening escrow or reviewer artifacts.

**Completion criterion:** the public test task passes in the implementation tree
and no reviewer-owned file or value entered the implementation context.

### Verify

1. Read `references/evidence.md` before running or interpreting final evidence.
2. Run `./gradlew toppleCatVerify` as reviewer or CI; it checks
   `CONTRACT_INTEGRITY` before downstream work, then reports and re-hides source.
3. Diagnose with reviewer-only Verification artifacts. On failure, give the
   implementation agent only public source changes and
   `build/topplecat/agent-feedback.json`.

**Completion criterion:** the current run is interpreted without borrowing old
artifacts; a `PASS` claim is made only when all required gates pass and each
`DISABLED` gate is an explicit reviewer decision; otherwise the done claim is
rejected with safe feedback; reviewer source is re-hidden in either outcome.

### Restore

1. Read `references/reviewer-custody.md` before touching escrow.
2. Run `./gradlew toppleCatRestore` as the authorized reviewer.
3. Inspect only, or make the authorized change and return to **Author**.
4. Re-hide unchanged source, or after edits complete Check, Review, and
   `toppleCatUpdateEscrow`, including public-contract or policy-only reapproval.

**Completion criterion:** restored files match the escrow manifest; every edit
returns through Check and Review; and reviewer source is hidden again before any
implementation handoff.

### Assess Adoption

1. Read `references/authoring.md` for the target contract shape.
2. Map existing acceptance tests, stable requirement IDs, DTO/API expectations,
   case data, reviewer boundaries, and PIT configuration.
3. Separate adoption blockers from optional improvements and identify the
   smallest representative AC to convert first.

**Completion criterion:** every existing acceptance surface is accounted for;
custody and feedback boundaries are explicit; blockers are distinguished from
optional work; and one bounded pilot AC has a concrete conversion plan.

## Failure Routing

- A `toppleCatCheck` source or alignment failure returns to **Author**.
- A custody lock, hash mismatch, or missing escrow asset stays in **Restore** or
  **Review and hide** with source and escrow preserved.
- A `toppleCatVerify` `FAIL` returns safe feedback to **Implement**.
- A `CONTRACT_INTEGRITY` `FAIL` or `INCOMPLETE` returns to the authorized
  reviewer: use Restore → Check → Review → UpdateEscrow; never reseal via
  Verify or ordinary Hide.
- An `INCOMPLETE` verdict stays in **Verify** until its required producer or
  configuration completes; it is never relabelled as `PASS` or `DISABLED`.

ToppleCat is not an OS sandbox and does not control CI identity or same-user
Gradle/JVM access; the external workflow must provide the trusted reviewer/CI
boundary and give agents only public source plus safe feedback.
