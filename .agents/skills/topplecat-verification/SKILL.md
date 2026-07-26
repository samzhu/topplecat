---
name: topplecat-verification
description: Gate Java delegation with ToppleCat. Use when authoring a Java/JUnit executable contract, transferring reviewer custody, implementing a public handoff, verifying a done claim, or assessing adoption. Don't use for ordinary unit tests or non-Java work.
---

# ToppleCat Verification

Treat ToppleCat as a gate state machine: complete the first unmet gate while
preserving reviewer custody.

## Gate Invariants

- Java acceptance tests and typed JSON/YAML rows are authoritative. Markdown is
  context; generated JSON and HTML are projections.
- Each approved AC has a literal Java binding; each data-driven AC has one
  canonical `@ToppleTest` with `@ToppleStageField`.
- A Markdown-only AC is incomplete.
- Public material lives under `src/test`; reviewer-owned material lives under
  `src/hiddenTest`.
- `./gradlew test` is an implementation signal. Only current-run
  `build/topplecat/evidence.json` is the final contract verdict.
- The managed PIT producer targets every compiled public canonical
  `@ToppleTest` declaring class through compiler descriptors, not package-name
  guesses. An explicit consumer PIT `targetTests` or custom mutation producer
  remains authoritative; a usable report that excludes a canonical test is
  `MUTATION=FAIL`, while a missing or unusable report is `INCOMPLETE`.

## Establish The Custody Boundary

1. Identify the environment from the handoff:
   - **Public implementation or adoption** uses its supplied public checkout
     and selects **Implement** or **Assess adoption**.
   - **Authorized author, reviewer, or CI** may access reviewer source,
     reviewer-local escrow, and private evidence.
2. If that role is unknown, ask the user before accessing more material.

Proceed only when the custody boundary is known and the selected environment
has accessed only appropriate material.

## Select The Gate

1. An authorized author, reviewer, or CI inspects build, task, Java bindings,
   case roots, hidden source, escrow manifest, and current evidence as needed.
2. Choose exactly one branch allowed by that boundary:
   - **Author** for an authorized author or reviewer changing an AC, case, DTO
     expectation, or Stage.
   - **Review and hide** when static checks are green but reviewer sign-off or
     custody transfer is incomplete.
   - **Implement** when a public contract handoff is ready for implementation.
   - **Verify** when an implementation agent has made a done claim.
   - **Restore** when an authorized reviewer must inspect or edit hidden source.
   - **Assess adoption** when a Java/JUnit project has not adopted ToppleCat.
3. State the selected branch, visible source boundary, and first unmet gate
   before editing or running a downstream task.

**Completion criterion:** one allowed branch is selected from the handoff or
reviewer state before any downstream gate runs.

## Execute The Branch

### Author

1. Read `references/authoring.md` before changing an AC, binding, case row, DTO,
   Stage, or attachment.
2. Obtain the immutable task or Spec ID and assign literal AC IDs.
3. Author public bindings, representative rows, and independent reviewer
   boundaries in one change.
4. Run `./gradlew toppleCatCheck`; resolve errors and configured warnings.
5. Run the narrow public test; new behavior shows a meaningful red result before
   implementation, or record why existing behavior is green.

**Completion criterion:** affected ACs have literal bindings, valid rows,
consumed expected keys, reviewer rows, and a passing Check.

### Review And Hide

1. Read `references/reviewer-custody.md` before opening reviewer material or
   changing custody state.
2. Run `./gradlew toppleCatReview`, inspect its report AC by AC, then correct
   source and repeat Review until accepted.
3. If the current user is not the authorized reviewer, report the review path
   and stop for explicit sign-off.
4. After sign-off, run `./gradlew toppleCatHide`; it seals public contract and
   policy. Ordinary Hide never refreshes a seal.
5. Prepare a public export without `.git`, `.topplecat/`, `build/`,
   reviewer-local state, or `src/hiddenTest`; or use an isolated environment
   whose Git history never contained reviewer material.

**Completion criterion:** reviewer accepts every AC, Hide completes, and the
implementation tree contains only public contract and production material.

### Implement

1. Work from public production source, Spec context, Java tests, and case rows.
2. Use `./gradlew test` until the public contract is green.
3. Return a done claim without opening escrow or reviewer artifacts.

**Completion criterion:** public tests pass and no reviewer-owned file or value
entered the implementation context.

### Verify

1. Read `references/evidence.md` before running or interpreting final evidence.
2. Run `./gradlew toppleCatVerify` as reviewer or CI; it checks
   `CONTRACT_INTEGRITY`, reports, and re-hides source.
3. Diagnose with reviewer-only Verification artifacts; on failure, give the
   implementation agent public source changes and
   `build/topplecat/agent-feedback.json`.

**Completion criterion:** only current-run `PASS` with every required gate and
explicit `DISABLED` decision accepts the claim; otherwise return safe feedback
and re-hide reviewer source.

### Restore

1. Read `references/reviewer-custody.md` before touching escrow.
2. Run `./gradlew toppleCatRestore` as the authorized reviewer.
3. Inspect only, or make the authorized change and return to **Author**.
4. Re-hide unchanged source, or after edits complete Check, Review, and
   `toppleCatUpdateEscrow` for source, contract, or policy reapproval.

**Completion criterion:** restored files match the manifest; every edit returns
through Check and Review; reviewer source is hidden before handoff.

### Assess Adoption

1. Read `references/authoring.md` for the target contract shape.
2. Map acceptance tests, stable IDs, DTO/API expectations, case data, reviewer
   boundaries, and PIT configuration.
3. Separate blockers from optional improvements and identify one pilot AC.

**Completion criterion:** every acceptance surface is accounted for, custody and
feedback boundaries are explicit, and one pilot AC has a conversion plan.

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
