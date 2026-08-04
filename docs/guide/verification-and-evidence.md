# Verification and evidence

ToppleCat verifies an executable Java/JUnit acceptance contract. It does not
select the current work, manage delivery history, grant organizational approval,
or provide an operating-system security boundary.

The Reviewer may be the developer, Spec owner, or another accountable human.
That human reads Spec Review before implementation handoff and Verification
Report after the implementation agent's done claim, then decides whether to
accept the delivery. A team may run the commands locally, in CI, or through an
external workflow; ToppleCat supplies verification and evidence but does not
own that orchestration. ToppleCat records `PASS` only when every required Gate
passes in the current run. Verification Report shows that verdict and its
evidence; it does not recommend acceptance or rejection. The human keeps the
final decision.

## Reviewer workflow

```bash
./gradlew toppleCatCheck --spec specs/023-checkout/spec.md
./gradlew toppleCatReview --spec specs/023-checkout/spec.md
./gradlew toppleCatSeal --spec specs/023-checkout/spec.md
./gradlew test
./gradlew toppleCatVerify --spec specs/023-checkout/spec.md
```

`--spec` is repeatable and selects the delivery at invocation time. Use the
same selection for Check, Review, Seal, and Verify. `--all-hidden-tests`
widens only hidden typed rows. Public acceptance and PBT follow the selected
ACs; Mutation Testing is attributed independently to each selected AC's public
Acceptance Method. An unselected AC cannot affect this run's verdict.
PIT cannot express a compiler JVM descriptor when selecting JUnit 5 tests. If
one selected AC shares its test class with an unselected public Acceptance
Method, formal Verify stops before PIT rather than run that class ambiguously.
Select every AC in that class or place the selected methods in a dedicated
class.

The Reviewer may add `--language zh-TW` to Review, Seal, Reseal, or Verify to
render ToppleCat-owned Reviewer HTML in Traditional Chinese; `en` is the
default. Seal and Reseal forward the selection to their dependent Spec Review,
so a custody operation does not overwrite a localized review with English.
The choice is made again for each invocation and does not enter the Seal or
verification policy. Only `en` and `zh-TW` are valid; a blank or unsupported
explicit value fails before formal verification begins.

`toppleCatRestore` is a reviewer-only recovery and editing command. To revise
custody, use:

```text
toppleCatRestore
    -> edit src/hiddenTest
    -> toppleCatCheck
    -> toppleCatReview
    -> any external organizational review
    -> toppleCatReseal
```

The 0.0.24 custody and approval schemas are current-only. A prior schema is not
migrated or read for verification; seal a new reviewer state instead.

## Independent formal work

`./gradlew test` is ordinary development feedback. It runs public project tests
and public acceptance methods, but produces no formal ToppleCat evidence.

`toppleCatVerify` creates a fresh formal public acceptance run and evaluates
the enabled safeguards independently:

| Capability | Evidence that can pass its gate | Cannot be supplemented by |
| --- | --- | --- |
| Hidden Tests | Executed hidden typed rows for the selected ACs | Properties or mutation reports |
| Mutation Testing | A passing Public Acceptance baseline and a current usable producer report attributed to public acceptance methods | Hidden rows or Properties |
| Property-Based Testing | Current matching Property events and JUnit XML | Hidden rows or mutation reports |

For example, if a selected delivery has five Properties but one has only a
terminal event or a stale digest, the Verification Report says four Properties
completed and `PROPERTY=INCOMPLETE`. A Property counts once only when its
current sealed AC ID, complete Java method identity, and source digest have one
matching `STARTED` event followed by one matching terminal event. Completion is
separate from outcome: a counterexample and an incomplete terminal outcome both
count as completed while the Gate remains `FAIL` or `INCOMPLETE`.

If Hidden Tests are enabled and a selected AC has no executed hidden row,
`REVIEWER_JUNIT=INCOMPLETE`. A Property may run and pass at the same time, but
it cannot change that result. A team choosing PBT without hidden rows
must explicitly disable `hiddenTests`, reseal the policy, and receives
`REVIEWER_JUNIT=DISABLED` with the actual `PROPERTY` result.

Mutation Testing has a different prerequisite: Public Acceptance must first
pass. If a public example already fails, `MUTATION=INCOMPLETE`; a temporary
PIT run cannot establish whether the unchanged public Acceptance Method would
have caught a separately changed production behavior. Any available PIT output
stays reviewer technical context and does not become a Mutation Gate verdict.

Formal Verify uses only ToppleCat's managed PIT 1.25.5 producer. It targets
compiler-emitted public Acceptance Methods, fixes the
`topplecat-managed-v1` 12-operator profile, and writes a non-timestamped XML
full matrix containing PIT's `coveringTests`, `killingTests`, and
`succeedingTests` groups. A project `pitest` task, custom producer task,
consumer `targetTests`, and consumer report path are not configuration inputs
for ToppleCat evidence. Before every formal run, ToppleCat clears its internal
PIT XML and prior current workspace, then disables task-output and build-cache
reuse for both the producer and Gate. The XML, v1 result, completion marker,
evidence, and report therefore belong to the same run.
Project-wide `tasks.withType(PitestTask)` conventions apply only to the
project's own PIT tasks; they cannot rewrite the formal managed producer.

The fixed profile contains these exact PIT operator IDs:

```text
TRUE_RETURNS
FALSE_RETURNS
PRIMITIVE_RETURNS
EMPTY_RETURNS
NULL_RETURNS
REMOVE_CONDITIONALS_EQUAL_IF
REMOVE_CONDITIONALS_EQUAL_ELSE
REMOVE_CONDITIONALS_ORDER_IF
REMOVE_CONDITIONALS_ORDER_ELSE
CONDITIONALS_BOUNDARY
VOID_METHOD_CALLS
MATH
```

ToppleCat attributes a result only to the exact public Acceptance Method whose
class, method, overload, and parameter types match the PIT selector.
`coveringTests` says the method ran against that mutant; `killingTests` says
that same method detected it. If PIT produced mutants but none can be exactly
attributed to any public Acceptance Method, the Mutation Gate fails
(`MUTATION=FAIL`). Once at least one mutant is exactly attributed, any remaining
unattributed mutants stay in reviewer evidence and do not directly affect the
Gate. Each selected AC with covered mutants must have its own public Acceptance
Method detect every mutation attributed to that method. One AC cannot supply
detection credit to another, and there is no percentage threshold or
project-wide mutation score. An AC with no covered mutant is an attribution
gap: once another AC has exact attribution it is nonblocking and the reviewer
report says `此 AC 沒有取得本次 managed mutation profile 的歸因證據，需要 reviewer 判斷。`
Missing, malformed, interrupted, stale, profile-mismatched, non-full-matrix,
or zero-mutant producer evidence makes Mutation Testing incomplete. PIT's own
status, `detected`, raw mutator, and description remain visible without being
turned into a ToppleCat score.

Contract integrity is the only precondition. Verify first runs the current Check
to rebuild the compiler definition it compares with the Mechanical Seal. If it
is `PASS`, Verify runs public acceptance, hidden typed-row retest,
Property-Based Testing, and Mutation
Testing in that fixed order. Hidden Tests and Properties remain independent
when Public Acceptance finds a problem; Mutation Testing instead records its
baseline as unavailable. JUnit-like tasks retain XML and sidecars after
assertion failures. The report waits for all enabled safeguards or their
explicit interruption, aggregates expected consumption, writes evidence,
reports, and safe feedback, re-hides reviewer source, and only then fails
Gradle for an aggregate `FAIL` or `INCOMPLETE`.

An interrupted task is `INCOMPLETE`; a completed task with missing or malformed
current-run evidence (including JUnit runtime sidecars) is also `INCOMPLETE`.
Each run discards an unarchived active workspace before it starts. Stable copies
and archives are never inputs to a later verdict. Property feedback distinguishes a discovered
counterexample from an incomplete task or damaged evidence. Mutation feedback
likewise distinguishes surviving mutants from an incomplete task or missing /
damaged producer evidence.

## Gates and verdicts

Every formal run records this fixed gate order:

```text
CONTRACT_INTEGRITY
JUNIT
REVIEWER_JUNIT
EXPECTED_CONSUMPTION
PROPERTY
MUTATION
```

Each gate is `PASS`, `FAIL`, `INCOMPLETE`, `DISABLED`, or `NOT_APPLICABLE`.
Contract integrity is mandatory and cannot be disabled. `DISABLED` is an
explicit sealed policy decision; `NOT_APPLICABLE` is an enabled safeguard with
no applicable declaration. Neither is presented as a pass.

The aggregate evidence verdict is only `PASS`, `FAIL`, or `INCOMPLETE`. `PASS`
means every required Gate passed under the sealed policy in this run; it does
not mean that the Spec contains every business rule or that the delivery has
organizational approval.
`toppleCatVerify` and `toppleCatReport` finish evidence, reports, safe feedback,
and rehide before failing Gradle for an aggregate `FAIL` or `INCOMPLETE`.

Contract integrity seals the compiler-derived acceptance source closure, public
typed rows, project Gradle logic, semantic definition, selected scope, and
effective verification policy. It excludes production source and unrelated
ordinary tests. An approval mismatch is `FAIL`; missing current approval is
`INCOMPLETE`. Verify uses only an existing Mechanical Seal and records that its
approval was not updated. If none exists, run `toppleCatSeal` before Verify;
Verify never creates approval. Downstream gates then do not run and are
recorded `INCOMPLETE`; no earlier artifact fills a gap.

## Reports and information boundary

| Artifact | Purpose | Audience |
| --- | --- | --- |
| Spec Review | Complete selected Markdown document and bound executable material before handoff | Reviewer only |
| Verification Report | Current formal-run conclusion, failure-first diagnostics, and private evidence | Reviewer only |

Their stable paths are respectively:

```text
build/topplecat/reports/review/index.html
build/topplecat/reports/verification/index.html
```

Verification Report can show Property classifications, generator choices,
shrunk counterexamples, replay tokens, and the reviewer-only PIT attribution
matrix. It also shows disabled safeguards as `DISABLED`. Spec Review may show
non-blocking expected-output quality advisories. There is no public HTML report
for an actual delivery; the separate project page may instead use clearly
labelled, fully synthetic red-team demonstrations for human product education.
`agent-feedback.json` never exposes reviewer case IDs, values, source names or
paths, Property trial material, tokens, attachments, raw private failures, or
quality-advisory output.

### Reading one AC

Each AC first shows a five-part summary: Public Acceptance, Hidden Tests,
Expected Result Check, Property-Based Testing, and Mutation Testing. The
reader-facing labels mean:

- **Passed:** the available result satisfied this safeguard.
- **Problem found:** a completed check found a mismatch or weakness.
- **Comparison completed:** authored expected values were compared with actual
  values. This does not say they matched; Public Acceptance and the difference
  table answer that question.
- **Unable to assess:** trustworthy current-run evidence was not sufficient for
  this safeguard. The explanation states why.
- **Disabled / Not applicable:** sealed policy disabled the safeguard, or no
  declaration/evidence applied to this AC.

For a failed case, read `Input` first and then `Expected compared with actual`.
The table preserves the authored field path and shows expected and actual side
by side. Complete expected data, Scenario execution Steps, values passed to
Steps, raw failures, canonical Gate verdicts, and producer details remain
available in collapsed sections.

Every AC starts in key-result-only state on each report load, including `FAIL`
and `NOT_REPORTED` ACs. Its ID, title, status, plain-language result, and all
five safeguard outcomes remain visible. A safeguard that found a problem or
could not be fully assessed is larger and states its recorded reader-safe reason
in the key-result layer; this is a reading aid, not a second Gate result. Use
**Expand this AC** to open that AC’s reader details and every public and hidden
case reader layer at once; the case reader still leads with input and
expected/actual comparison, while complete expected data, execution details,
raw failures, Gate evidence, and producer details remain independently
collapsed. **This AC: key result only** closes just that AC.

The AC list has a sticky reading toolbar. **Expand all ACs** performs the same
reader-level expansion in bounded browser-scheduled batches and announces
completed ACs as `Expanding X of N`. While it runs, **Stop and show key
results** cancels pending work and returns every AC to key-result-only state.
After completion, **All ACs: key results only** collapses the complete list.
Bulk progress counts fully expanded ACs, not scheduled cases; an interrupted
operation never changes verification evidence or claims that all ACs opened.

Needs Attention links, safeguard links, and URL fragments reveal the AC and
any required nested disclosure before positioning the target. A fragment that
targets technical evidence opens only that disclosure path; unrelated
technical evidence remains closed. The global toolbar and active AC identity
row preserve the reading context, and the controls are native keyboard
buttons with localized English and Traditional Chinese labels. Disclosure
state is not persisted across reloads.

### Reading Mutation results

For each AC, Verification Report first answers a plain question: while
verifying this delivery, did the unchanged public Acceptance Method detect
every temporary production change that was exactly attributed to that AC? A
surviving attributed change fails that AC and the aggregate Gate. This reports
what the Acceptance Method distinguished during the temporary verification
changes; it does not prove that the original program is correct in every
situation.

That question is only meaningful after Public Acceptance passes. If it did not,
the AC's Mutation Testing section says evidence is unavailable because the
baseline failed. It does not show attributed counts or call PIT output a
detected or undetected change for that AC.

Public Acceptance, Hidden Tests, and Mutation Testing appear separately for the
same AC. A Mutation result without trustworthy evidence says `Unable to
assess`; it is never a pass. If the current evidence says `DISABLED`,
`NOT_APPLICABLE`, or `INCOMPLETE`, the report keeps the actual reason. Readers
who need producer detail can expand the
collapsed technical details, which preserve PIT's official outcomes, managed
profile, operator IDs, attribution counts, selectors, and descriptions exactly
as recorded. The English and Traditional Chinese reports use the same reading
order and evidence values.

When an AC did not detect attributed changes, its Mutation Testing section also
shows the assessed total, detected, and undetected counts, followed only by
undetected-change cards. Each card answers `What changed?`, `Where?`, and `What
happened?`: the last statement is that this AC's unchanged public acceptance
still passed. A PIT `KILLED` outcome is not by itself AC detection; if another
Acceptance Method killed the mutation, the current AC still receives an
undetected card when its own method covered and passed it. The card shows the
original source line only when current production sources resolve one unique
location. It states the producer description and an explicit limitation when
the available evidence cannot establish an exact before/after replacement.

The technical details remain collapsed and retain the raw PIT status,
`detected` flag, mutator, description, source coordinates, original source
line, and covering/killing/succeeding selector relationships. The assessed
mutation details are built in the Java report model; browser rendering is only
a projection. None of these reviewer-only diagnostics enters
`agent-feedback.json` or public implementation handoff material.

The report places a compact `ⓘ` button beside Mutation Testing and the initial
card terms **attributed changes**, **undetected mutation**, **original source
line**, and **descriptor**. Hover or keyboard focus shows a short explanation;
click or touch activation pins it, opening another control replaces the prior
one, and Escape or outside interaction closes the pinned explanation. These
controls are localized with the report language, stay within the viewport on a
narrow screen, and supplement rather than replace the visible result. Opening
or closing one does not expand cases, open technical evidence, change fragment
navigation, or change any evidence artifact. If scripting is unavailable, the
existing result and long-form explanation remain readable.

`build/topplecat/evidence.json` is the machine verdict for the current run.
Each run starts in `build/topplecat/runs/current/`, receives a fresh UUID when
archived, and retains only a small recent archive set. Stable copies are for
inspection, never inputs to a later verdict.

Spec Review reads the complete selected SDD before executable material. It says
that the specification is prepared but not executed. Verification Report starts
with pass, fail, or unavailable evidence, then lists the selected ACs needing
attention before the complete AC list. Each AC keeps the fixed order Contract
rule and result, Public Acceptance, Hidden Tests, Expected Result Check,
Property-Based Testing, and Mutation Testing. A collapsed technical section
preserves the canonical Gate names and evidence. A field-level expected/actual
comparison is the first diagnostic content inside a failed case, before
collapsed execution details. It is reviewer-only context for the failed
compiler Step and does not alter the JUnit assertion or Expected Consumption
result.
On a wide display, the AC list uses the available report workspace so a Reviewer
can compare its five sections without horizontal page overflow; the same view
collapses to one column on narrow screens. Spec Review keeps its separate,
readable prose width.

Contract Integrity is shown once in the main summary, not as a failure of every
AC. When the checked contract matches its Mechanical Seal, the summary says so.
When it does not—or ToppleCat lacks trustworthy current integrity evidence—the
summary says downstream AC work did not run; unexecuted ACs are not presented as
functional failures.

Both HTML bundles are offline, self-contained, and CSP-safe. Spec Review
supports headings, paragraphs, lists, task lists, block quotes, tables, links,
inline and fenced code, repository-local images, and Mermaid diagrams. Raw HTML
and unsafe URLs never execute; missing assets and diagram failures keep visible
fallback text. Reports fetch no external fonts, UI code, syntax highlighter,
Mermaid runtime, analytics, or CDN assets.

Each bundle declares the selected `lang` value and localizes every
ToppleCat-owned heading, control, accessibility label, explanation, empty
state, and fallback. The selected Markdown, `@DisplayName`, `@As`, Property
title, Typed Case Row values, Gate identifiers and verdicts, PIT status,
mutator, selector, producer description, and raw failure remain verbatim.
The presentation choice is absent from the report JSON, Current-run Evidence,
and `agent-feedback.json`; it cannot change a Gate verdict or seal integrity.

## Custody boundary

Seal stores reviewer-only source below
`~/.topplecat/projects/<sha256-project-key>/escrow/`. This is plaintext
mechanical custody—not encryption, a sandbox, or protection from a process
running as the same OS user. A public handoff must exclude reviewer source,
custody state, build artifacts, and any Git history containing reviewer
material. `./gradlew clean` does not delete reviewer custody.
