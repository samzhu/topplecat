---
name: topplecat-acceptance
description: Bind selected Spec Acceptance Conditions to ToppleCat Java/JUnit executable contracts. Use when authoring Acceptance Methods, public or reviewer-owned Typed Case Rows, or optional Property-Based Testing declarations for a Java delivery.
metadata:
  topplecat-version: "0.0.24"
---

# ToppleCat acceptance

Read the repository root `CONTEXT.md` before using this skill. Build one
Executable Contract for the human-selected Java delivery. Let the active SDD
tool keep the upstream Spec structure and lifecycle. Use its stable AC IDs to
bind Acceptance Methods and Typed Case Rows—the executable source of truth.

## 1. Ground the selected contract

Read the authoritative upstream Spec. When External Workflow supplies
repository-relative Selected Spec Document paths, read every document in full
and inventory every anchored `AC-...`; a path selects the whole document. With
no `--spec` selection, inventory every bound AC and confirm that the upstream
rules cover each one.

Use upstream rules and recorded human clarifications as authoritative. Read
relevant production behavior and existing tests only to locate integration
seams.

Return a missing AC identity or ambiguous rule to the human or active SDD tool
for correction in the authoritative input before binding it. For each in-scope
AC, establish:

- one stable `AC-...` identity;
- the starting state, action, and observable result;
- applicable success, rejection, boundary, and rule-combination behavior; and
- public examples and independently derived reviewer examples.

Ask one to three concrete questions at a time when a rule can produce different
valid implementations. Continue after the distinction is recorded by its human
or SDD owner. Derive reviewer-owned rows only from those public rules.

Reviewer-owned Typed Case Rows are the default. When none are available, ask
for the missing business distinctions and propose independent examples for
confirmation. If the user declines them, record the resulting Hidden Tests gap
and the policy choice that the external workflow must make.

Complete this step only when every anchored AC is accounted for when Spec paths
are selected, or every bound AC is accounted for when no paths are selected.
In either branch, each expected distinction must trace to authoritative input
and no unresolved choice may change expected behavior.

## 2. Bind every AC

Read [the authoring reference](references/authoring.md) before editing Java or
case data.

Bind every in-scope AC to one public `@ToppleAcceptanceTest` Acceptance Method
and human-confirmed public rows. Add reviewer-owned rows for independently
chosen examples. Add a bounded `@ToppleProperty` only when a recorded invariant
deserves generated coverage. Give every Property a JUnit `@DisplayName` that
states both the generated input or repeated situation and the invariant checked
for every completed trial. Write it in the Spec's business language so a
Reviewer can understand what a runtime count such as 200/200 actually covered.
Every discarded generator input remains canonical JSON Property evidence with
a neutral explanation; it is not a Typed Case Row or a new rule.
Give every selected Scenario Step an `@As` sentence in the Spec's business
language. Describe the state, action, or observable result; never expose a Java
method fallback, type placeholder, or technical parameter as Reviewer prose.

Trace every top-level expected value to its `c.verify(...)` call inside a
compiler-described Step. Follow the reference when several independent values
need separate assertion opportunities.

Complete this step only when every in-scope AC maps one-to-one to a Java method,
every row targets an existing AC, every top-level expected value has an
assertion opportunity, every Property title identifies both its trial subject
and invariant, every selected Step has business-readable `@As` prose, and every
authored API exists in ToppleCat's current public API.

## 3. Design the safeguards

Read [the safeguards reference](references/safeguards.md) before assigning
evidence to a safeguard.

Keep every safeguard independent. State what each enabled safeguard is meant to
challenge, which authored input supplies its evidence, and which other
safeguards cannot supply missing evidence for it.

Complete this step only when every enabled or explicitly disabled safeguard is
accounted for, reviewer rows introduce new combinations of public rules, and no
safeguard receives credit from another.

## 4. Prepare a reviewable handoff

Read [the reports reference](references/reports.md) before preparing the
handoff.

Return a compact mapping of selected Spec paths—or the explicit all-bound-AC
scope—AC IDs, Java methods, public rows, reviewer rows, Properties, safeguard
policy choices, and unresolved gaps. Produce separate public and reviewer
handoffs. Reviewer-only values, identifiers, paths, and source names stay in
the reviewer handoff.

Give the Implementation Agent only the public handoff. Keep reviewer-owned
source in the reviewer handoff. External Workflow receives the exact Spec
selection and policy choices and may consume Current-run Evidence. The Reviewer
reads both human-readable reports after execution and alone decides whether to
accept the delivery.

Complete this step only when the mapping accounts for every in-scope AC and
gap, the public handoff contains only public contract material, and the
external workflow has the exact Spec selection and policy choices it needs.
