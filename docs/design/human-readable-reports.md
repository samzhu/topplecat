# Human-readable Spec Review and Verification Report

**Status:** Implemented

**Date:** 2026-08-01

**Target:** ToppleCat 0.0.11

## User example

A reviewer selects `specs/checkout/spec.md` for one checkout delivery. The
document contains business background, acceptance conditions, a Mermaid flow
diagram, an example payload, and links to local images. Its ACs are bound to
ordinary Java Acceptance Methods, public and reviewer-owned Typed Case Rows,
and optional Property declarations.

Before implementation handoff, the reviewer runs:

```bash
./gradlew toppleCatReview --spec specs/checkout/spec.md
```

The resulting HTML should read like the selected specification, not like a
test dashboard. It presents the complete selected Markdown document, the
compiler-described Given/When/Then syntax, case data, Properties, and the
complete Java Acceptance Method that will run. It clearly says that no
verification has run yet.

After implementation, formal Verify runs public acceptance, Hidden Tests,
Property-Based Testing, and the ToppleCat-managed Mutation Testing producer.
For example, the public checkout case may pass while a reviewer case fails, a
Property finds a counterexample, and PIT records a surviving void-call mutant.
The post-run HTML should lead with the rejected delivery, link directly to all
three independent findings, and retain PIT's official raw outcome without
claiming that ToppleCat knows the business cause.

## Problem

ToppleCat 0.0.10 produces three human HTML bundles: reviewer-only Contract
Review, safe Public Spec, and reviewer-only Verification Evidence. Their names
and boundaries make the generated projection look like a third specification
surface. The implementation agent already receives the selected public source
contract and safe `agent-feedback.json`; a separate public HTML does not add an
authoritative input.

The existing review projection extracts only AC-anchored headings, paragraphs,
and lists from selected Markdown. It skips fenced code and does not preserve a
complete selected SDD document. It presents business narrative, executable
Scenario, cases, Properties, and Java mapping through one report-oriented AC
layout rather than as a readable specification.

The existing verification projection has the 0.0.10 independent safeguard and
managed PIT evidence, but it does not yet make the delivery conclusion and
actionable failures the primary reading path. Case failures also have raw
expected and actual material without a structured field-level comparison tied
to the failed Scenario Step.

This redesign must improve the two human reading experiences without changing
the authority of Java/JUnit and Typed Case Rows, the meaning or order of any
Gate, the `--spec` selection contract, Mechanical Seal semantics, the managed
PIT profile, or the information boundary.

## Decision and product boundaries

### Exactly two human HTML reports

ToppleCat keeps exactly these human-readable HTML bundles:

| Name | Stable path | Time | Audience and purpose |
| --- | --- | --- | --- |
| **Spec Review** | `build/topplecat/reports/review/index.html` | Before implementation verification | Reviewer-only reading of the selected SDD and the executable material that will be checked. |
| **Verification Report** | `build/topplecat/reports/verification/index.html` | After formal Verify | Reviewer-only conclusion and diagnostics for one current run. |

The old names Contract Review and Verification Evidence are removed from the
user interface and current-product documentation. Machine-readable
`build/topplecat/evidence.json` remains **Current-run Evidence** and is not
renamed to a report.

`build/topplecat/reports/public/` is removed. Formal report publication deletes
any stale bundle at that fixed path. The public report model, JSON codec,
renderer entry point, tests, and documentation links are removed rather than
retained as an unused compatibility surface.

`agent-feedback.json` remains the only generated execution result intended for
the implementation agent. It keeps its current safe, Gate-level information
boundary. The implementation agent may receive the original public source
contract selected by the human workflow; it does not receive either
reviewer-only HTML report.

### Existing workflow and verification semantics remain unchanged

`--spec` remains the only delivery-selection input and remains repeatable. The
same selection is used for Check, Review, Seal, and Verify. No `--spec` retains
the current full-contract behavior. 0.0.11 adds no document selector, AC
sub-selector, Gradle task, CLI, authoring DSL, report configuration API, or
report publication workflow.

Every `AC-...` anchored in a selected Markdown document belongs to the selected
Delivery Scope and must have its existing public Acceptance Method binding.
Spec Review does not create a second selected/unselected AC state inside that
document.

The selected Markdown remains human reading context. Acceptance Methods, Typed
Case Rows, and Property declarations remain the executable source of truth.
Rendering a Markdown sentence, diagram, or image cannot turn it into an
additional executable rule.

Formal Verify retains the fixed Gate names and order:

```text
CONTRACT_INTEGRITY
JUNIT
REVIEWER_JUNIT
EXPECTED_CONSUMPTION
PROPERTY
MUTATION
```

Mechanical Seal and `CONTRACT_INTEGRITY` remain an integrity concern. Hidden
Tests, Property-Based Testing, and Mutation Testing remain three independent
functional-testing aspects. They are never combined with integrity or with one
another into a score. After integrity passes, every enabled Independent
Safeguard still runs to a current result even when another fails.

The ToppleCat-managed PIT 1.25.5 producer, `topplecat-managed-v1` operators,
exact Acceptance Method attribution, threshold policy, raw PIT fields,
attribution-gap semantics, and `topplecat.mutation-results.v1` remain unchanged.
0.0.11 only improves how their existing reviewer evidence is organized and
explained.

## Visible interface and behavior

### Spec Review: specification first

The page title is **Spec Review**. Its first screen states **Specification
prepared — not executed** and contains no PASS, FAIL, success color, aggregate
verdict, run result, or wording that implies the implementation was checked.

The report follows this reading order:

1. selected SDD document;
2. each document AC's executable acceptance material;
3. reviewer-only Contract Quality Advisories; and
4. collapsed technical and policy metadata.

The page renders the complete contents of every selected Markdown document in
deterministic selected-scope order. It does not repeat the same extracted SDD
narrative inside every AC card. When no external Spec was selected, it says so
plainly and presents the existing full executable contract without inventing a
Markdown document.

The Markdown projection supports the document forms needed for an SDD:

- headings, paragraphs, ordered and unordered lists, task lists, block quotes,
  horizontal rules, links, emphasis, inline code, fenced code, and tables;
- repository-local images with preserved alternative text and optional title;
- fenced `mermaid` diagrams rendered for human reading; and
- a visible, escaped fallback for any content that cannot be rendered safely.

Repository-local report assets are copied or embedded into the offline bundle
without escaping the repository or report directory. Remote images are not
downloaded; their alternative text and safe link remain visible. Raw Markdown
HTML, scripts, iframes, event handlers, and unsafe URLs never execute. SVG and
Mermaid output are sanitized before entering the report DOM.

Mermaid rendering uses a pinned, report-owned offline runtime and a restrictive
security mode. A valid diagram is shown by default. Its original Mermaid source
is available in a collapsed, syntax-highlighted detail. If rendering fails, the
report shows **Diagram could not be rendered** and the escaped original source;
it never silently drops the block.

For each AC, the executable portion appears in this order:

1. AC ID, business title, and its position in the selected SDD;
2. compiler-described Given/When/Then/And Scenario;
3. public and reviewer-owned Typed Case Rows, clearly labelled by visibility;
4. optional Property declarations and their configured bounds;
5. the complete public Acceptance Method source; and
6. collapsed source paths, method identities, digests, and verification policy.

Given, When, Then, and And are BDD syntax keywords, not ordinary paragraph
labels. They are rendered in execution order with keyword, Step sentence, and
typed argument syntax visually distinguished. The report uses the
compiler-described Step sentence and retained typed values; it never substitutes
a Java object's arbitrary `toString()`.

The complete Acceptance Method includes its annotations, declaration, and
method body, and is syntax-highlighted as Java. It is shown after the
human-readable Scenario and may be collapsed by default. Stage bodies, helper
methods, production source, and transitively called implementation code are not
included. Spec Review is not a Java source browser.

Property source is presented with its human title, AC binding, tries, discard
and shrink bounds, followed by syntax-highlighted Java in a secondary detail.
Case inputs and expected values use readable key/value or table projections;
syntax-highlighted JSON or YAML source may appear as secondary data when the
structured projection would otherwise lose authored shape. The report provides
no Copy button.

Contract Quality Advisories remain reviewer-only and non-blocking. They appear
next to the affected AC and in one concise advisory summary. They do not become
execution results, business conclusions, or Gate findings.

### Spec Review layout and visual language

Spec Review is a document layout, not a card dashboard. Desktop uses a compact
document outline beside a primary reading column. Narrow viewports use one
column and a disclosure for the outline. Major SDD and AC content stays visible;
details are reserved for secondary code, raw data, and technical metadata.
There are no nested accordions or tabs that hide the main specification.

Long-form text uses a readable measure and line height. Tables and code may use
the wider content area without forcing the whole page into long lines. The
heading hierarchy, source-document boundaries, AC anchors, and focus order
remain semantic and navigable by keyboard.

The pre-execution palette is neutral. Blue may identify navigation and binding,
amber may identify an advisory, and grey may identify secondary technical
material. Large green or red result surfaces are reserved for Verification
Report. Color never carries meaning without text, structure, or another visual
cue. The bundle supports system light and dark modes, visible focus, a skip
link, reduced motion, and single-column reflow without ordinary horizontal page
scrolling.

All code forms use an offline, report-owned syntax highlighter with matched
light and dark themes. At minimum it recognizes Java, JSON, YAML, Markdown, and
Mermaid. BDD Scenario rendering has its own semantic token classes for
Given/When/Then/And, Step text, and typed arguments. Unsupported code languages
fall back to readable escaped monospace text without losing the source.

These layout and color rules are report usability requirements, not executable
business conditions and not new ToppleCat Gates. Implementation acceptance
uses rendered-browser inspection and accessibility checks; the report never
claims that the delivery passed because its presentation is attractive.

### Verification Report: conclusion and problems first

The page title is **Verification Report**. Its first screen states one plain
conclusion such as **Delivery rejected — verification failed**, **Verification
incomplete**, or **Delivery accepted — verification passed**. It shows:

- aggregate verdict;
- number of failed and incomplete Gates;
- number of failed ACs and cases when known;
- run ID, start and finish time; and
- actual selected and executed scope.

A Problems Summary follows the conclusion. FAIL findings precede INCOMPLETE
findings. Every item names what ran, what happened, and why that evidence
supports or cannot support the Gate result, then links to the relevant Gate,
AC, case, Property, mutation assessment, or integrity detail. A bare status is
never its own explanation.

The report separates these areas without a blended score:

1. **Contract Integrity** — Mechanical Seal comparison and
   `CONTRACT_INTEGRITY` only;
2. **Public Acceptance** — `JUNIT` case execution and the separate
   `EXPECTED_CONSUMPTION` obligation;
3. **Hidden Tests** — reviewer-owned typed-row execution and
   `REVIEWER_JUNIT`;
4. **Property-Based Testing** — `PROPERTY`, classifications, coverage,
   counterexample, shrinking, and replay diagnostics; and
5. **Mutation Testing** — `MUTATION`, managed profile, threshold, exact
   attribution, and PIT's raw findings.

The three Independent Safeguard sections—Hidden Tests, Property-Based Testing,
and Mutation Testing—are peer functional-testing sections. A PASS, FAIL,
INCOMPLETE, DISABLED, or NOT_APPLICABLE result in one never changes how another
is labelled. Mutation data never appears in Contract Integrity, and Mechanical
Seal data never appears as a mutation finding.

When contract integrity fails or is incomplete, the report highlights one
integrity root-cause area. Downstream work is described as not executed because
contract integrity did not establish a trusted contract; it is not repeated as
several independent business failures. When integrity passes, the report
expects every enabled safeguard to have either completed or recorded an
explicit current-run incomplete reason before the HTML is complete.

By default, a failing report expands and selects the first real failed AC and
case, not the first passing public row. Search and status filters support
navigation but never change the evidence or default conclusion.

One case detail follows this reading order:

1. public rule being checked;
2. Scenario with syntax-highlighted BDD keywords;
3. failed or last reached Step;
4. structured field-level expected/actual comparison;
5. inputs, complete expected result, and attachments; and
6. collapsed raw failure, source, and technical metadata.

The report states only what the evidence supports. It may say that a reviewer
case failed, a Property found a reproducible counterexample, PIT recorded a
`SURVIVED` mutant, or an AC has no managed-profile attribution. It cannot infer
that an implementation is hard-coded, that a test is fake, that a mutant is
business-relevant, or that a hidden value is the uniquely correct answer.

### 0.0.10 Mutation Testing evidence in the new report

Mutation Testing retains the full 0.0.10 reviewer evidence:

- PIT 1.25.5 and `topplecat-managed-v1` identity;
- all 12 configured operator IDs and human-readable signal-family grouping;
- producer, uniquely attributed, and unattributed totals;
- each AC's covered, detected, threshold, detection rate, and attribution gap;
- summaries grouped by raw mutator identity; and
- expandable raw `status`, `detected`, mutator, description, and exact
  covering/killing/succeeding selector relationships.

The report uses PIT's official outcome names and does not create a
status-to-score mapping. A reviewer-lowered threshold may allow the Mutation
Gate to pass while raw survivors remain visible. Partial unattributed mutants
remain reviewer evidence and do not directly alter the Gate once exact public
attribution exists. These are presentation requirements for existing 0.0.10
behavior, not new mutation rules.

## Structured diagnostics and report models

`ToppleCase.verify(...)` records a reviewer-only structured comparison when
expected and actual values differ. A comparison contains deterministic field
paths and distinguishes missing expected fields, unexpected actual fields, and
changed values. Arrays retain indexed paths. Existing mathematical JSON numeric
equality remains authoritative, including `200`, `200.0`, and `200.00`.

The comparison is bound to the active compiler-described Scenario Step and
written into the current-run reviewer sidecar. Verification projection uses
that structure for the field-level diff while retaining the complete expected,
actual, and raw failure as secondary reviewer diagnostics. This adds no public
matcher DSL and does not change whether JUnit or Expected Consumption passes.

Spec Review receives a document-level model rather than duplicating selected
Markdown blocks per AC. The model preserves selected document identity, source
order, safe parsed blocks, local asset references, and AC anchors. Executable
AC projections link to those anchors without changing Delivery Scope.

The public `SpecView`, its codec, and its renderer are removed. Review and
Verification view schemas advance once to their 0.0.11 current forms, with no
predecessor reader, migration, dual writer, or compatibility adapter. Existing
machine evidence, six Gate names, gate order, and aggregate rules are not
changed by the view-schema update.

Both report bundles remain offline, self-contained, and CSP-safe. They use no
external UI, font, highlighter, Mermaid, analytics, or JGiven runtime. Any
third-party rendering code is pinned, bundled, attributed, and covered by the
repository's dependency and license checks.

## Failure, security, and integrity rules

- Spec Review contains reviewer-owned rows, paths, Java source, and advisories;
  it is always reviewer-only.
- Verification Report may contain hidden cases, Property counterexamples,
  replay tokens, attachments, and raw PIT details; it is always reviewer-only.
- `agent-feedback.json` must still exclude hidden IDs, values, paths, source
  names, raw failures, attachments, counterexamples, replay tokens, PIT
  versions, profiles, operators, counts, descriptions, selectors, classes, and
  methods.
- Removing Public Spec must not cause reviewer fields to be copied into any new
  public artifact. There is no replacement public HTML.
- Report rendering cannot add, omit, rename, or reinterpret contract content or
  external producer outcomes. A missing value is labelled unavailable rather
  than inferred.
- Markdown and code are escaped before syntax tokenization. Raw HTML and unsafe
  URLs never execute. Asset resolution rejects absolute paths, parent traversal,
  symlink escapes, and destinations outside the report bundle.
- A missing or unreadable local image produces a visible placeholder with its
  authored alternative text and reference. It does not silently disappear.
- A Mermaid parse failure preserves the original source and does not alter
  Check, Seal, Verify, or any Gate.
- Report-generation failure cannot be presented as a successful or complete
  report. Formal Verify retains its existing aggregate failure and rehide
  guarantees.
- `reports/public/` cleanup targets only the exact known generated path. It
  never broadens into arbitrary project directories.

## Implementation task plan

### Task 1 — Consolidate names, artifacts, and current schemas

- Introduce the final Spec Review and Verification Report labels in report
  models and the shared HTML shell.
- Remove `SpecView`, its JSON methods, renderer entry point, public projection
  construction, stable publication, tests, and user-facing references.
- Delete stale `build/topplecat/reports/public/` during formal publication.
- Advance Review and Verification view schemas once, without compatibility
  readers.
- Keep Gradle task names, report paths, `evidence.json`, and
  `agent-feedback.json` unchanged.

### Task 2 — Build the complete selected-SDD projection

- Replace AC-fragment Markdown extraction with a safe document-order model.
- Preserve the complete selected Markdown source and AC anchors.
- Support SDD Markdown blocks, local images, safe links, fenced code, and
  Mermaid with escaped fallback.
- Resolve report assets within repository and bundle boundaries.
- Keep `--spec`, selected AC calculation, Delivery Scope, and Mechanical Seal
  behavior unchanged.

### Task 3 — Rebuild Spec Review as a document reader

- Render the neutral pre-execution header and selected SDD first.
- Add semantic document navigation, readable typography, responsive single
  column, light/dark modes, and accessible focus.
- Render Given/When/Then/And as highlighted BDD syntax.
- Present public/reviewer cases, Properties, and advisories per AC.
- Show complete syntax-highlighted Acceptance Method and Property source as
  secondary details; exclude Stage/helper/production source and Copy buttons.

### Task 4 — Add structured reviewer-only comparison evidence

- Define deterministic comparison path and difference kinds in core.
- Record mismatches from `ToppleCase.verify(...)` without changing assertion or
  expected-consumption semantics.
- Bind the comparison to the active Step and current-run reviewer sidecar.
- Project it only into Verification Report and prove it cannot enter safe
  feedback or another public artifact.

### Task 5 — Rebuild Verification Report around the conclusion

- Add current-run metadata, plain aggregate conclusion, and Problems Summary.
- Separate integrity, public acceptance/expected consumption, Hidden Tests,
  Property-Based Testing, and Mutation Testing.
- Default to the first actual failure and link Problems Summary entries to
  concrete details.
- Present field-level comparison before raw failure details.
- Preserve complete 0.0.10 managed PIT evidence and official PIT terminology.
- Keep all Gate and aggregate calculations outside the renderer.

### Task 6 — Synchronize product documentation and acceptance guidance

- Update architecture, README, verification guide, troubleshooting, docs index,
  and repository-owned acceptance skill to the two-report model.
- Remove Public Spec links and old Contract Review/Verification Evidence names.
- Keep this record Accepted while implementation is incomplete; mark it
  Implemented only after code, tests, guides, architecture, and user-facing
  documentation agree.

## Acceptance evidence

### Model and parser tests

- Preserve complete selected documents in deterministic order, including
  headings, paragraphs, lists, task lists, block quotes, tables, links, inline
  code, fenced Java/JSON/YAML, images, and Mermaid.
- Preserve AC anchors without creating a second AC selection layer.
- Escape raw HTML, scripts, event handlers, unsafe URLs, and unsupported source
  without dropping its text.
- Reject asset traversal and show deterministic missing-asset and Mermaid-error
  fallbacks.
- Retain complete Acceptance Method and Property source while excluding Stage,
  helper, and production source.
- Cover nested objects, arrays, missing fields, unexpected fields, changed
  values, multiple differences, and BigDecimal-equivalent numbers in structured
  comparison.

### Report DOM and browser tests

- Spec Review says **Specification prepared — not executed**, contains no
  verdict, renders complete SDD content, and exposes all selected executable
  material to the reviewer.
- BDD keywords, Java, JSON, YAML, Markdown, and Mermaid receive semantic syntax
  token classes; unsupported code remains readable.
- Local images and Mermaid diagrams render offline; no report asset requests an
  external runtime or CDN.
- Main specification content is not hidden inside nested disclosures. Heading
  order, landmarks, skip link, focus, keyboard navigation, and status text are
  semantic.
- Verification Report shows aggregate failure first, lists every failed or
  incomplete problem, and opens the first actual failed AC/case.
- Contract Integrity, Public Acceptance, Hidden Tests, Property-Based Testing,
  and Mutation Testing remain separate; there is no blended score.
- Mutation DOM coverage includes profile, operator IDs, raw PIT fields,
  threshold, per-AC attribution, and nonblocking attribution gaps.
- Implementation QA inspects representative reports at 1600px and 390px in
  system light and dark modes for readable measure, reflow, contrast, focus, and
  absence of ordinary page-level horizontal overflow. These are presentation
  checks, not business Gates.

### Functional and information-boundary tests

- `toppleCatReview --spec ...` renders the complete selected SDD and all bound
  reviewer material without running verification.
- No-spec full-contract review retains its current behavior and clearly states
  that no external Spec document was selected.
- Formal Verify produces Verification Report, Current-run Evidence, safe
  feedback, attachments, and rehide output on PASS, FAIL, and INCOMPLETE.
- A combined run proves public acceptance can pass while Hidden Tests,
  Property-Based Testing, and Mutation Testing each finish with their own
  current result and the aggregate fails.
- Integrity failure shows one root cause and records downstream non-execution
  without pretending those safeguards ran.
- Formal publication no longer creates `reports/public` and safely removes a
  stale fixed-path bundle.
- Negative scans cover hidden IDs, values, paths, raw failures, attachments,
  counterexamples, replay tokens, and all managed PIT raw detail in
  `agent-feedback.json` and every remaining non-reviewer artifact.
- Existing 100 AC / 1,000 case / 5,000 Step scale coverage remains. Detailed
  case content is rendered on demand so the initial DOM remains usable.

Implementation completes with the narrowest affected module and functional
tests followed by:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
python3 scripts/verify-docs.py
git diff --check
```

## Consequences and alternatives

The reviewer receives one faithful pre-run reading surface and one complete
post-run diagnostic surface. Removing Public Spec reduces artifact ambiguity
and keeps the implementation-agent result channel explicit. The cost is a more
capable safe Markdown renderer, bundled syntax and Mermaid assets, a larger
review schema, and browser-level presentation testing.

Keeping Public Spec is rejected because it duplicates public source contract
material and suggests that generated HTML is authoritative. Renaming it is
also rejected because the implementation agent already has source and safe
feedback.

Keeping only AC excerpts is rejected because the reviewer asked to understand
the selected SDD as a complete document, including diagrams and explanatory
context. Executing raw Markdown HTML is rejected because it grants selected
content code execution inside a reviewer report.

Using JGiven, Cucumber, an external Markdown site generator, an online syntax
highlighter, or a Mermaid CDN is rejected because ToppleCat keeps Java/JUnit as
its authoring boundary and its reports offline and CSP-safe. Their terminology
and presentation may inform the design without becoming a runtime dependency.

A dense dashboard, pie charts, trends, cross-run comparison, history, task
status, Spec lifecycle, approval workflow, and organizational sign-off remain
outside 0.0.11.

The document-first layout follows the general guidance to use clear headings
and anchor navigation for long content rather than hiding primary material in
nested accordions. The failure-first Verification Report borrows navigation
principles from test reporters without copying their framework-specific test
tree. Readability and accessibility guidance comes from:

- [Cucumber executable specifications](https://cucumber.io/docs/);
- [GOV.UK accordion guidance](https://design-system.service.gov.uk/components/accordion/);
- [GOV.UK check-answers layout guidance](https://design-system.service.gov.uk/patterns/check-answers/);
- [USWDS typography guidance](https://designsystem.digital.gov/components/typography/);
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/); and
- [Playwright HTML reporter](https://playwright.dev/docs/test-reporters).
