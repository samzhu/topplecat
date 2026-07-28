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
- The external delivery workflow chooses the Spec and sign-off. Humans own rule
  completeness; ToppleCat judges only the approved executable contract.
- Preserve continuity between the public contract handed to the implementer,
  the reviewer-approved contract, and the contract Verify executes.
- Bind every selected AC literally. Each data-driven AC has one canonical
  `@ToppleTest` using `@ToppleStageField`. A Markdown-only AC is incomplete.
- Keep public material under `src/test` and reviewer material under
  `src/hiddenTest`.
- Reviewer approval mechanically seals contract and policy. Only
  `toppleCatUpdateEscrow` refreshes it.
- `./gradlew test` is an implementation signal. Current-run
  `build/topplecat/evidence.json` is the final verdict.
- Mutation `PASS` requires current canonical-test evidence. Consumer-owned PIT
  targeting remains authoritative.
- Share public source and `build/topplecat/agent-feedback.json` with the
  implementer; keep reviewer evidence in reviewer custody.

## Select The Gate

1. Identify the custody role before opening files:
   - **Public implementation or adoption** sees only the public checkout.
   - **Authorized author, reviewer, or CI** may inspect reviewer custody.
   Ask when the role is unknown.
2. Choose one branch:
   **Author**, **Review and hide**, **Implement**, **Verify**, **Restore**, or
   **Assess adoption**.
3. Pin the **delivery scope**:
   - Supplied Markdown Specs: record repository-relative paths and repeat
     `--spec <path>` for each. Reuse that exact set through Check, Review, Hide
     or UpdateEscrow, and Verify.
   - No supplied Spec: use full-contract commands without `--spec`.
   Delivery input is the scope authority; never infer scope from Git or an SDD
   product.
4. State role, visible boundary, branch, scope, and first unmet gate.

**Completion criterion:** the role, visible boundary, branch, pinned delivery
scope, and first unmet gate are explicit before a downstream task runs.

## Execute the branch

### Author

1. Read `references/authoring.md` before changing any contract surface.
2. Add literal AC bindings, public rows, and independent reviewer boundaries.
3. Run `./gradlew toppleCatCheck` with the pinned scope; resolve errors and
   configured warnings.
4. Run the narrow public test and record the meaningful red result.

**Completion criterion:** affected ACs have literal bindings, valid rows,
consumed expected keys, reviewer rows or AC-bound reviewer Java tests, and a
passing scoped Check.

### Review And Hide

1. Read `references/reviewer-custody.md` before opening reviewer material.
2. Run `./gradlew toppleCatReview` with the pinned scope; compare every selected
   AC with its executable contract and rows.
3. Let the external workflow perform organizational sign-off.
4. Run `./gradlew toppleCatHide` with the pinned scope.
5. Export only public source, without history, build output, or reviewer state.

**Completion criterion:** the report faithfully maps selected ACs to executable
contracts, Hide completes, and the public handoff contains no reviewer
material, state, or history. External sign-off is outside this skill.

### Implement

1. Use only public production source, Spec context, Java tests, and case rows.
2. Use `./gradlew test` until the public contract is green.
3. Return the production changes and done claim to the reviewer.

**Completion criterion:** public tests pass and no reviewer-owned file or value
entered the implementation context.

### Verify

1. Read `references/evidence.md` before running or interpreting final evidence.
2. Run `./gradlew toppleCatVerify` with the sealed scope. Append `--all-hidden`
   only to broaden hidden retests; mutation remains full-contract.
3. Read the new `build/topplecat/evidence.json`. Diagnose privately; return
   only public changes and `build/topplecat/agent-feedback.json`.

**Completion criterion:** accept only current-run aggregate `PASS`, current-run
reviewer coverage for every selected AC, explicit reviewer decisions for every
`DISABLED` gate, and rehidden reviewer source.

### Restore

1. Read `references/reviewer-custody.md` before touching escrow.
2. Run `./gradlew toppleCatRestore` as the authorized reviewer.
3. Re-hide unchanged source. For an approved edit, return to **Author**, then
   run Check, Review, and `toppleCatUpdateEscrow` with the pinned scope.

**Completion criterion:** restored files match the manifest; every edit returns
through Check and Review; reviewer source is hidden before handoff.

### Assess Adoption

1. Read `references/authoring.md`; also read `references/evidence.md` for an
   existing configuration.
2. Map acceptance tests, stable IDs, typed case data, reviewer boundaries, and
   PIT targeting.
3. Identify blockers and one pilot AC.

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

Use an external trusted reviewer/CI boundary for OS access and identity;
ToppleCat custody is plaintext, not a sandbox.
