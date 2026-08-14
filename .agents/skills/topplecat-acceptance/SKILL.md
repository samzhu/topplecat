---
name: topplecat-acceptance
description: Bind selected Spec Acceptance Conditions to ToppleCat Java/JUnit executable contracts. Use when authoring Acceptance Methods, public or reviewer-owned Typed Case Rows, or optional Property-Based Testing declarations for a Java delivery.
metadata:
  topplecat-version: "0.2.2"
---

# ToppleCat acceptance

Read `CONTEXT.md`. Build one Executable Contract for the human-selected Java
delivery; the active SDD tool owns upstream Spec structure and lifecycle.

## 1. Ground the selected contract

Human or upstream SDD supplies one or more exact repository-relative canonical Markdown
paths. Never guess, search, follow links, select implicitly, normalize
an absolute path, or choose a current document from repository state. A selected
path names the complete document; the product CommonMark parser derives its AC
inventory only from the exact standalone `<!-- topplecat:acceptance:AC-ID -->` marker,
the selected Spec's exact standalone ID-bearing marker. Examples include
`<!-- topplecat:acceptance:AC-CHECKOUT-001 -->`. A visible heading is not an AC declaration:
headings, ordinary mentions, and marker proximity are authored Markdown
readability only.

Pass the same exact relative path set to Check, Review, and scoped Verify. An
absolute, absent, ambiguous, missing, missing-file, structurally invalid, insufficient, or
thin-wrapper selection stops before commands, scope, narratives, or either
handoff. Route only the responsible human or upstream SDD owner and the
smallest repair. Never fall back to whole-contract material and never expose a
machine-specific path or wrapper destination.

Read every Selected Spec Document in full. A path selects the whole document;
its AC inventory is the ordered set of valid ID-bearing markers, not the first
`AC-...` mention. Each marker ID occurs exactly once across the selected
documents. The marker order is preserved; different IDs may be before/after prose or
consecutive. Duplicate, malformed, legacy generic, or container-nested
directives produce repairable product diagnostics and never fall back to heading
pairing.
With no selected paths (no `--spec` selection), inventory every bound AC and
prepare whole-contract Check/Verify/Seal only; do not claim that a Spec Review
is ready.
Whole-contract scope has no selected documents or AC IDs; bound IDs describe
the complete executable contract, never selected scope.

Keep selected failures separate from whole-contract maintenance: stop before
commands, scope, narratives, or either handoff; route only owner and repair
action. Do not guess paths, follow wrapper links, or expose diagnostics.
Use upstream rules and human clarifications as authoritative; production and
tests only locate integration seams.

Return missing AC identity or ambiguous rules for authoritative correction.
Place a marker near its related business prose when that improves readability,
but never treat proximity or heading shape as validity.
For each AC establish a stable identity, state/action/result, success/rejection/
boundary/combination behavior, public examples, and independent reviewer rows.
Ask one to three concrete questions when rules allow different implementations.

Complete this step when the selected documents and every parser-derived AC are
accounted for, or every bound AC is accounted for in the whole-contract branch.

## 2. Bind every AC

Read [the authoring reference](references/authoring.md) before editing Java or
case data. Bind each AC to one public `@ToppleAcceptanceTest` method and
confirmed public rows; add independent reviewer rows and bounded
`@ToppleProperty` only for recorded invariants. Use business-readable
`@DisplayName`, `@As` Step sentences, and `c.verify(...)`; discarded generator
inputs remain neutral evidence, not rows or new rules.

Complete this step only when every AC maps one-to-one to Java, rows and expected
values have assertion opportunities, Properties name subject/invariant, and
Steps have business-readable `@As` prose.

## 3. Design the safeguards

Read [the safeguards reference](references/safeguards.md). Keep each safeguard
independent: state its challenge, authored evidence, and missing coverage.

## 4. Prepare a reviewable handoff

Read [the reports reference](references/reports.md). Its `commands` field is
the user-facing Gradle task handoff, never a read or inspection log. Map selected Spec paths—or
the all-bound-AC scope—to AC IDs, methods, public rows, reviewer rows,
Properties, safeguard choices, and unresolved gaps. Create separate public handoff
and reviewer handoff; Reviewer-only values, identifiers, paths, and source names
stay in the reviewer handoff. Read existing handoff JSON artifacts and copy
their entries verbatim; do not reconstruct them from Java or case sources.
Successful handoffs leave failure routing empty.

Give the Implementation Agent only public material. External Workflow receives
the exact Spec selection and Current-run Evidence. The Reviewer reads both HTML
reports and alone decides whether to accept the delivery.

Preserve authored Gherkin-style Given/When/Then/And/But narratives verbatim, including keywords,
wording, order, and separate groups. Never read or translate .feature files;
never register, generate, or execute them. Return insufficient rules to the
canonical Spec owner.
