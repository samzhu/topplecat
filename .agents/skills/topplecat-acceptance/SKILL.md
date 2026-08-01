---
name: topplecat-acceptance
description: Author ToppleCat executable Java/JUnit acceptance contracts from SDD Specs. Use when a Java delivery needs Acceptance Conditions, public or reviewer-owned Typed Case Rows, or Property-Based Testing declarations.
metadata:
  topplecat-version: "0.0.12"
---

# ToppleCat acceptance

Read the repository root `CONTEXT.md` before using this skill. Build one
executable contract for the selected Java delivery. Let the active SDD tool
keep its own Spec structure. Use stable AC IDs to bind that reading context to
Acceptance Methods and Typed Case Rows—the executable source of truth.

## 1. Grill the delivery

Read the selected Spec, relevant production behavior, and existing tests. For
rules, treat the selected Spec and confirmed human answers as authoritative;
use production behavior and tests only to locate integration seams. For each
rule, establish:

- one stable `AC-...` identity;
- the starting state, action, and observable result;
- success, rejection, boundary, and rule-combination behavior; and
- public examples and independently derived reviewer examples.

Ask one to three concrete questions at a time when a rule can produce different
valid implementations. Keep questioning until the user confirms the business
distinction. Derive reviewer-owned rows only from confirmed public rules.

Reviewer-owned Typed Case Rows are the default. When none are available, ask
for the missing business distinctions and propose independent examples for
confirmation. If the user declines them, record the resulting Hidden Tests gap
and the policy choice that the external workflow must make.

Complete this step only when every selected rule has an AC and no unresolved
choice would change expected behavior.

## 2. Bind every AC

Read [the authoring reference](references/authoring.md) before editing Java or
case data.

Bind every selected AC to one public `@ToppleAcceptanceTest` Acceptance Method
and approved public rows. Add reviewer-owned rows for independently chosen
examples. Add a bounded `@ToppleProperty` only when a confirmed invariant
deserves generated coverage.

Prefer one `c.verify("receipt", projection)` for a complete observable receipt.
When a contract genuinely has several independent top-level expected values,
place the `verify` calls in JUnit `assertAll` so a mismatch in one still gives
the others a verification attempt. A failed `verify` remains `ASSERTED`; a
later call never reached remains `UNTOUCHED`. Do not invent a `verifyAll` API.

Complete this step only when every selected AC maps one-to-one to a Java method,
every row targets an existing AC, and every expected output has an assertion
opportunity.

## 3. Design the safeguards

Read [the safeguards reference](references/safeguards.md) before assigning
evidence to a safeguard.

Keep every safeguard independent. State what each enabled safeguard is meant to
challenge and which authored input supplies its evidence.

Complete this step only when every safeguard has a stated purpose and reviewer
rows introduce new combinations without introducing private rules.

## 4. Prepare a reviewable handoff

Read [the reports reference](references/reports.md) before preparing the
handoff.

Return a compact mapping of selected Spec paths, AC IDs, Java methods, public
rows, reviewer rows, Properties, safeguard policy choices, and unresolved gaps.
Produce separate public and reviewer handoffs. Reviewer-only values,
identifiers, paths, and source names stay in the reviewer handoff.

External workflow automation and humans execute ToppleCat tasks, inspect the
Spec Review, Verification Report, and machine verdict, then
decide whether to accept the final result.

Complete this step only when the mapping accounts for every selected AC and
gap, the public handoff contains only public contract material, and the
external workflow has the exact Spec selection and policy choices it needs.
