---
name: topplecat-verification
description: Gate Java delegation with ToppleCat. Use when authoring a Java/JUnit executable contract, sealing or restoring reviewer custody, implementing a public handoff, verifying an agent done claim for omitted, hard-coded, or contract-tampered behavior, or assessing adoption. Don't use for ordinary unit tests or non-Java work.
---

# ToppleCat verification

Treat ToppleCat as a gate state machine. Complete the first unmet gate visible
to the current custody role.

## Gate Invariants

- Java acceptance tests and typed JSON/YAML rows are authoritative. Markdown is
  context; generated JSON and HTML are projections.
- Bind each approved AC to a literal Java annotation. Give each data-driven AC
  one canonical `@ToppleTest` built from `@ToppleStageField`.
- A Markdown-only AC is incomplete.
- Public material lives under `src/test`; reviewer-owned material lives under
  `src/hiddenTest`.
- Reviewer approval seals the public contract and verification policy. Only
  `toppleCatUpdateEscrow` refreshes that approval.
- `./gradlew test` is an implementation signal. Only current-run
  `build/topplecat/evidence.json` is the final contract verdict.
- A mutation `PASS` requires current evidence that every required public
  canonical test was exercised. Consumer-owned PIT targeting remains
  authoritative.
- Give implementation agents public source and
  `build/topplecat/agent-feedback.json`; keep reviewer evidence within reviewer
  custody.

## Select The Gate

1. Identify the custody role before opening files:
   - **Public implementation or adoption** sees only its supplied public
     checkout.
   - **Authorized author, reviewer, or CI** may inspect hidden source,
     reviewer-local escrow, and private evidence.
   Ask the user when the role is unknown.
2. Choose one branch:
   - **Author** changes an AC, case, DTO expectation, or Stage.
   - **Review and hide** when static checks are green but reviewer sign-off or
     custody transfer is incomplete.
   - **Implement** works from a ready public handoff.
   - **Verify** checks an implementation agent's done claim.
   - **Restore** lets an authorized reviewer inspect or edit hidden source.
   - **Assess adoption** maps an existing Java/JUnit project.
3. State the selected branch, visible source boundary, and first unmet gate
   before acting.

**Completion criterion:** the role, visible boundary, branch, and first unmet
gate are explicit before a downstream task runs.

## Execute the branch

### Author

1. Read `references/authoring.md` before changing any contract surface.
2. Assign literal AC IDs from the immutable task or Spec ID.
3. Add public bindings and representative rows plus independently derived
   reviewer boundaries.
4. Run `./gradlew toppleCatCheck`; resolve errors and configured warnings.
5. Run the narrow public test. New behavior reaches a meaningful red result
   before implementation; record why pre-existing behavior is already green.

**Completion criterion:** affected ACs have literal bindings, valid rows,
consumed expected keys, reviewer rows, and a passing Check.

### Review And Hide

1. Read `references/reviewer-custody.md` before opening reviewer material or
   changing custody state.
2. Run `./gradlew toppleCatReview`; inspect every AC and revise until accepted.
3. Obtain explicit sign-off from the authorized reviewer.
4. Run `./gradlew toppleCatHide` to seal the contract and policy.
5. Prepare a public export without `.git`, `.topplecat/`, `build/`,
   reviewer-local state, or `src/hiddenTest`; or use an isolated environment
   whose Git history never contained reviewer material.

**Completion criterion:** reviewer accepts every AC, Hide completes, and the
public handoff contains no reviewer material, state, or history.

### Implement

1. Use only public production source, Spec context, Java tests, and case rows.
2. Use `./gradlew test` until the public contract is green.
3. Return the production changes and done claim to the reviewer.

**Completion criterion:** public tests pass and no reviewer-owned file or value
entered the implementation context.

### Verify

1. Read `references/evidence.md` before running or interpreting final evidence.
2. Run `./gradlew toppleCatVerify` as reviewer or CI.
3. Read the just-written `build/topplecat/evidence.json`; diagnose with
   reviewer-only artifacts. On failure, return public contract changes and
   `build/topplecat/agent-feedback.json`.

**Completion criterion:** accept only current-run aggregate `PASS`, with every
`DISABLED` safeguard an explicit reviewer decision and reviewer source rehidden.
Otherwise reject the claim and return safe feedback.

### Restore

1. Read `references/reviewer-custody.md` before touching escrow.
2. Run `./gradlew toppleCatRestore` as the authorized reviewer.
3. Re-hide unchanged source. For an approved edit, return to **Author**, then
   Check, Review, and run `toppleCatUpdateEscrow`.

**Completion criterion:** restored files match the manifest; every edit returns
through Check and Review; reviewer source is hidden before handoff.

### Assess Adoption

1. Read `references/authoring.md`; for an existing configuration, also read
   `references/evidence.md`.
2. Map acceptance tests, stable IDs, typed case data, reviewer boundaries, and
   PIT targeting.
3. Separate blockers from optional improvements and identify one pilot AC.

**Completion criterion:** every acceptance surface is accounted for, custody and
feedback boundaries are explicit, and one pilot AC has a conversion plan.

## Failure routing

- A `toppleCatCheck` source or alignment failure returns to **Author**.
- A custody lock, hash mismatch, or missing escrow asset stays in **Restore** or
  **Review and hide** while preserving source and escrow.
- A `toppleCatVerify` `FAIL` returns safe feedback to **Implement**.
- A `CONTRACT_INTEGRITY` `FAIL` or `INCOMPLETE` returns to the authorized
  reviewer for Restore → Check → Review → UpdateEscrow.
- An `INCOMPLETE` verdict stays in **Verify** until the missing producer or
  configuration completes.

Use an external trusted reviewer/CI boundary for OS access and identity.
ToppleCat provides plaintext custody and verification gates, not a sandbox.
