# Architecture

ToppleCat starts at the executable acceptance boundary. Java/JUnit acceptance
methods and typed JSON/YAML case rows are authoritative; generated JSON and
HTML are evidence projections. The [product definition](product.md) owns the
audience, use moments, and responsibility boundary; this document describes how
the implemented modules preserve them.

## Modules

| Module | Responsibility |
| --- | --- |
| `topplecat-core` | Case, evidence, custody, approval, Property, and safe-feedback models. |
| `topplecat-junit` | `@ToppleAcceptanceTest`, typed rows, compiler-described Scenario/Stage proxies, expected consumption, and PBT engine. |
| `topplecat-report` | Reviewer-only Spec Review and Verification Report projections. |
| `topplecat-gradle-plugin` | Commands, task wiring, scope, custody, policy, integrity, and mutation orchestration. |

## Product boundary carried by the architecture

The Reviewer owns the delivery decision. The Implementation Agent receives the
public contract and safe Gate-level feedback, while both HTML reports and
private diagnostics from an actual delivery remain reviewer-only. The separate
public project page is a human-facing explanation surface and may show clearly
labelled, fully synthetic red-team report details; it is not an agent handoff
or a projection of an actual delivery. External Workflow chooses the Spec and
execution placement. No module owns CI, PR operations, Spec lifecycle, or
organizational approval.

## Public documentation build

The repository also builds a public, current-only explanation site beside the
four Java modules. Human-authored Markdown under `site/docs/en/` and
`site/docs/zh-TW/` is rendered by MkDocs with Material into one GitHub Pages
artifact alongside the Vite project page. The artifact verifier checks the
combined output for page pairing, metadata, links, page-level Markdown,
discovery files, and denied information categories before deployment.
Fenced examples receive ToppleCat-themed Shiki token colors during the build;
the published HTML does not need client-side highlighting to remain readable.

The repository's executable learning projects are separate Maven Central
consumers. Their runners make temporary copies and temporary Reviewer Custody
for synthetic lesson deviations. The JUnit learning project copies each
completed, fully synthetic Verification Report to its ignored local
`build/topplecat/demo-reports/<lesson>/index.html` so a learner can inspect the
result. Generated evidence and reports never become checked-in source or part
of the Pages artifact.

This publication path is not a fifth ToppleCat product module. It does not
publish Javadoc, site search, versioned sites, actual delivery reports, or
reviewer-owned material.

## Execution boundary

### Java build and runtime policy

All four published Java modules use the shared Gradle convention: JDK 25 is the
primary build toolchain and every Java compile task uses `--release 21`. The
release target controls language, Java SE API, and class-file output; source or
target compatibility flags alone are not the compatibility authority. Gradle
publication metadata advertises JVM 21 for the API and runtime variants.

The release verifier scans every class entry in every module JAR, requires major
version 65 with a non-preview minor version, and checks the corresponding
Gradle module metadata. The one artifact family therefore loads on JDK 21 and
JDK 25. The Gradle plugin rejects a ToppleCat runtime below JDK 21 with an
actionable environment message; a Java 17 consumer source target remains valid
when its execution JDK is 21 or 25.

The custom contract compiler task calls the daemon's system `JavaCompiler` and
requires a full JDK. A different daemon JDK and consumer compiler/toolchain is
not a supported combination until a separate tested seam exists.

```text
ordinary ./gradlew test
    public project tests + public acceptance methods
    no ToppleCat evidence, custody, review, report, or sealing dependency

./gradlew toppleCatVerify [--spec ... | --ac AC-...]
    fresh formal public acceptance run
    + enabled Hidden Tests, PBT, Mutation Testing, expected consumption
    -> current-run evidence and reports
```

One public `@ToppleAcceptanceTest("AC-...")` binds each Acceptance Condition.
Its body is compiler-checked orchestration: it receives one `ToppleScenario`
plus concrete capability Stages and selects each direct Step. Public typed rows run only in
`PUBLIC_ONLY` mode. Hidden typed rows reuse that same method only in
`HIDDEN_ONLY` mode. Ordinary JUnit tests, including hidden helper tests, are
not ToppleCat gate evidence.

`@ToppleProperty` is separate from typed acceptance. It receives
`PropertyTrials`, has one literal existing AC, has no expected-value obligation,
and never enters mutation targeting. Properties are public declarations under
`src/test` and run in their own formal task for the selected ACs.

The Verification Report's completed Property count is a projection of the
Property evidence assessment, not a second report-side calculation. It counts
a selected sealed Property only when one matching `STARTED` event precedes one
matching terminal event for the current run, AC ID, complete Java method
identity, and source digest. Counterexample and incomplete terminal outcomes
still count as completed; their `PROPERTY` Gate verdict remains `FAIL` or
`INCOMPLETE`.

## Independent safeguards

| Capability | Authoritative input | Formal task | Gate |
| --- | --- | --- |
| Hidden Tests | Hidden typed rows | `toppleCatHiddenTest` | `REVIEWER_JUNIT` |
| Mutation Testing | Compiler-emitted public acceptance methods and the fixed managed PIT 1.25.5 matrix | `toppleCatManagedPit` + `toppleCatMutationGate` | `MUTATION` |
| Property-Based Testing | `@ToppleProperty` declarations, generators, current events | dedicated Property task | `PROPERTY` |

The three capabilities are Independent Safeguards: they share only scope,
integrity, reports, and the aggregate verdict. Reviewer custody is used only
for Hidden Tests. A Property result cannot supply Hidden Test coverage. A
mutation result cannot supply Property evidence. Once contract integrity
passes, Hidden Tests and Properties produce their own current-run result even
if Public Acceptance fails. Managed Mutation Testing has one additional
prerequisite: Public Acceptance must pass to establish its baseline. When that
baseline fails, `MUTATION` is `INCOMPLETE` rather than a claim about mutation
detection. `REVIEWER_JUNIT` is `PASS` only when every AC in the delivery has a
current-run hidden typed row; a missing row makes that AC's Hidden Tests and
the aggregate Reviewer JUnit result `INCOMPLETE`. An explicit policy decision
is `DISABLED`.

Formal Verify owns its PIT producer: it pins PIT 1.25.5, passes only the fixed
`topplecat-managed-v1` 12-operator profile, targets compiler-emitted public
Acceptance Methods, requests a non-timestamped XML full matrix, and reruns
without task-output or build-cache reuse. A project PIT task or report never
becomes ToppleCat evidence.
PIT's JUnit 5 producer cannot select an Acceptance Method by its compiler JVM
descriptor. If a selected delivery would target a test class that also contains
an unselected public Acceptance Method, ToppleCat stops before PIT rather than
let that unselected AC execute. Select every Acceptance Method in that class or
place the selected ACs in a dedicated class.
The managed producer is not a consumer `PitestTask`, so project-wide
`tasks.withType(PitestTask)` conventions remain confined to the project's own
PIT workflow.

Mutation attribution uses PIT's complete `coveringTests`, `killingTests`, and
`succeedingTests` selector matrix. A mutant is mapped only when its selector's
class, method, overload, and full parameter types exactly match a compiled
public Acceptance Method. `coveringTests` supplies contract-scoped execution;
`killingTests` supplies that same method's contract-scoped detection; and
`succeedingTests` remains reviewer evidence. PIT `status`, `detected`, raw
mutator identity, and description stay unmodified. The sole reviewer-only
`topplecat.mutation-results.v1` artifact keeps profile metadata, producer,
unattributed, per-mutator and per-AC outcome summaries, and raw relationships.
An AC with zero covered mutants is a nonblocking attribution gap once another
AC has exact attribution; it never receives inferred credit from Hidden Tests,
Properties, helpers, or another acceptance method.

The reviewer-only mutation evidence also retains PIT's source file, production
class, method and descriptor, line number, bytecode block and index when
provided. The Gradle projection resolves an original source line only when the
configured production sources identify one unambiguous file and line; missing
or ambiguous context remains unavailable. Java builds the per-AC list of
undetected mutations from exact covering and killing relationships. The HTML
renders that assessed list and never recomputes attribution or the Gate. A
globally `KILLED` mutation can therefore remain an undetected detail for an AC
whose own public method passed while another AC supplied the killing selector.
These reviewer-only coordinates and details do not enter safe feedback.

## Scope and custody

Check and Review accept repeatable `--spec` paths. Review requires at least
one exact path and rejects a missing selection before its dependent Check
starts. Check maps the external Spec's valid heading/marker ACs to executable
acceptance methods, reads each selected document once, hashes those bytes, and
writes a safe checked projection. Structural Markdown errors are reported by
Check after the selection gate while it reads and parses the selected bytes;
Review does not produce a report for that failed Check. Review consumes that projection rather than
re-reading Markdown, so its HTML cannot combine bytes Check did not accept.
Seal and Reseal have no selection: they custody all reviewer source and approve
the complete contract.

Verify has no selection by default and then runs every AC. For a quick formal
report, it accepts either repeatable `--spec` paths or repeatable `--ac AC-...`
IDs, never both. Either form selects the ACs for public acceptance, PBT, Hidden
Tests, and Mutation Testing; the persisted scope distinguishes document-based
selection from direct AC selection. `--all-hidden-tests` broadens hidden typed
rows only. An unselected AC cannot affect a scoped run's verdict.
For a scoped `PASS`, the Reviewer HTML summary repeats that the result covers
only the selected ACs rather than the complete executable contract.

`--language` is a separate invocation-only presentation input for
`toppleCatReview` and `toppleCatVerify`.
It accepts exactly `en` and `zh-TW`, defaults to `en`, and reaches only the
Reviewer HTML-writing tasks. It is not Gradle DSL configuration, a browser or
operating-system locale, a scope input, or an approval input. Invalid values
fail during command configuration, before a formal Verify run starts.

`toppleCatSeal` stores reviewer-only material under
`~/.topplecat/projects/<sha256-project-key>/escrow/`, along with a mechanical
approval. `toppleCatRestore` exposes it only in a reviewer boundary;
`toppleCatReseal` replaces a restored, rechecked suite. The 0.2.0 format is the
only supported format. Custody is plaintext mechanical storage, not encryption
or a sandbox.

Approval seals compiler-derived acceptance source closure (including resolved
Stage owners), public typed rows,
Property declarations, effective policy, compiler definition,
and project Gradle logic. The source closure includes only sources javac
resolves from acceptance methods and Stage owners; unrelated ordinary tests do
not invalidate it.

## Evidence and information boundary

Every formal run starts below `build/topplecat/runs/current/`, discards any
unarchived active workspace, gets a new UUID, and is archived after reports are
written. Stable copies are diagnostic only; they cannot supply missing current
evidence. Gates are recorded in this order:

```text
CONTRACT_INTEGRITY
JUNIT
REVIEWER_JUNIT
EXPECTED_CONSUMPTION
PROPERTY
MUTATION
```

`CONTRACT_INTEGRITY` cannot be disabled. It first runs the current Check to
rebuild the compiler definition. A mismatch prevents downstream work and marks
it `FAIL`; absent or incomplete current proof is `INCOMPLETE`.
When it passes, formal Verify runs public acceptance, hidden typed rows,
Properties, and Mutation Testing in that order, then aggregates expected-value
consumption and writes reports before its one aggregate Gradle failure exit.
If Public Acceptance fails, the managed PIT task may still leave producer
diagnostics, but the Mutation Gate is `INCOMPLETE` because no trustworthy
passing baseline exists.
Verify reuses an existing Mechanical Seal through an internal custody check; it
does not run Review, Seal, or update approval. The integrity comparison always
uses the complete contract, even when Verify later runs a selected AC scope.

- Spec Review is reviewer-only, precedes handoff, and presents complete selected Markdown documents with each bound executable projection inline at its exact marker.
- Verification Report is reviewer-only and contains results, counterexamples,
  classifications, replay tokens, and private failures when applicable.
- `agent-feedback.json` has gate-level safe reasons only—no hidden values,
  IDs, paths, source names, tokens, attachments, or raw failures.

Spec Review is a document reader, not an execution dashboard: it renders every
selected Markdown document once and replaces each exact acceptance marker with
the AC-bound Scenario, public and reviewer Typed Case Rows, Properties, and the
one public Acceptance Method. The reviewer projection is v8. Heading level
does not affect the load point; ordinary AC references never create scope or a
second projection.
Raw Markdown HTML and unsafe URLs are escaped, repository-local image assets
stay inside the offline bundle, and Mermaid has an escaped source fallback.
Both report bundles are offline, self-contained, and CSP-safe. They load no
external UI, font, syntax highlighter, Mermaid runtime, analytics, or CDN
resource. Missing or unsafe local assets remain visible as escaped text or a
placeholder rather than disappearing or executing content.

The report-writing seam selects one complete English or Traditional Chinese
catalog and writes matching `lang="en"` or `lang="zh-TW"` metadata. Catalog
entries cover ToppleCat-owned headings, controls, accessibility prose,
explanations, empty states, and fallbacks. Compiler-owned Scenario phases have
one-to-one localized labels, while authored `@DisplayName`, `@As`, Property
titles, selected Markdown, case data, raw failures, AC IDs, Gate names,
verdicts, and PIT producer values remain the original bytes. Presentation
metadata and catalogs do not enter report `data.json`, evidence, approvals, or
safe agent feedback.

Verification Report leads with the aggregate conclusion, selected-AC counts,
and Needs Attention list. Its main summary presents Contract Integrity once:
on pass, it says the complete executable contract matches its Mechanical Seal;
on a mismatch or missing integrity proof, it says downstream AC work did not
run. Each AC card keeps that global result out of per-AC failure presentation
and presents Public Acceptance, Hidden Tests, Expected Result Check,
Property-Based Testing, and Mutation Testing in that order.
The Java report projection assigns each safeguard both its canonical Gate
verdict and a stable reader outcome and reason. The offline renderer localizes
those reader fields; it does not derive their meaning from a verdict or parse a
producer reason. In the key-result grid, a failed or incomplete safeguard spans
the visual focus and repeats that reader-safe reason, while the fixed five-item
order and all evidence values remain unchanged. Canonical reasons remain
available in collapsed technical evidence.
The AC workspace uses the available main report column on wider screens while
Spec Review keeps its prose-oriented measure; narrow screens remain one column.
When a `ToppleCase.verify(...)` comparison differs, the active compiler Step
receives reviewer-only structured expected/actual field differences; that data
never enters evidence feedback for the implementation agent.
The failed case presents input and those structured differences before its
collapsed complete expected value, Scenario execution details, Step values,
and raw failure. Authored JSON paths and values are preserved rather than
translated into inferred business meaning.

The Verification Report renderer keeps disclosure state in memory for the
current page only. Each AC has an always-visible key-result layer and a
controlled reader-detail region; the latter reuses the existing lazy case
materialization boundary and opens every public and hidden case reader layer
only when the AC is expanded. Complete expected data, execution details, raw
failures, canonical Gate evidence, producer details, and PIT evidence remain
independent nested disclosures. A report-wide expansion schedules bounded
browser batches, reports completed ACs, and can cancel back to key results; it
does not change the report model or any evidence artifact.

The report’s reading toolbar and active AC identity row use platform sticky
positioning when available and remain ordinary document-flow controls when it
is not. Local, global, and fragment-driven actions share the same disclosure
state so labels, `aria-expanded`, focus, and target visibility stay aligned.
Ancestor expansion happens before linked navigation, and scroll anchoring
keeps the operated AC in view. These are presentation behaviors only: they do
not alter `data.json`, Current-run Evidence, the Mechanical Seal, Gates,
reviewer custody, or safe Implementation Agent feedback.

Within Mutation Testing, the primary reading order is AC-first. For each
selected AC, a mutation that still passes its unchanged public Acceptance Method
is `FAIL`; detecting every attributed mutation is `PASS`; and no exact
attribution remains a neutral gap or the recorded incomplete/disabled state.
When Public Acceptance itself fails, Mutation Testing remains unavailable for
every AC in that run even if PIT wrote producer diagnostics; the report never
uses those diagnostics as a detection verdict.
There is no percentage threshold or project-wide mutation score. The managed
profile, PIT-wide outcomes, operator IDs, attribution counts, selectors, mutator
descriptions, and raw PIT outcomes stay unchanged inside collapsed reviewer
technical details. This presentation does not change Current-run Evidence, the
Mechanical Seal, a Gate, the aggregate verdict, or safe Implementation Agent
feedback.

The Verification Report bundle uses one static, reusable information-control
pattern for the initial Mutation Testing explanations. Each native button is
paired with a localized explanatory region through `aria-expanded`,
`aria-controls`, and `aria-describedby`; hover and focus show a transient
explanation, while click or tap pins it until Escape or outside interaction
closes it. Only one popover is open at once, and placement is constrained to
the viewport. This is reviewer-only presentation: it does not eagerly
materialize lazy cases or alter `data.json`, Current-run Evidence, the
Mechanical Seal, Gates, Reviewer Custody, or safe Implementation Agent
feedback.

Direct `toppleCatCheck` logs reviewer-only, non-blocking Contract Quality
Advisories about hidden expected-output shapes and opaque identifier literals,
and Spec Review displays them in its reviewer report. The Check that runs
inside formal `toppleCatVerify` suppresses advisory output. Advisories do not
enter the ContractDefinition, Mechanical Seal, Verify evidence, Verification Report,
`agent-feedback.json`, or any Gate.

The aggregate verdict is `PASS`, `FAIL`, or `INCOMPLETE`. ToppleCat records
`PASS` only when every required Gate passes in the current run. That verdict is
contract evidence, not an acceptance recommendation, proof that the selected
business rules are complete, or organizational approval. Individual gates may
also be `DISABLED` or `NOT_APPLICABLE`; neither is a passing result.
