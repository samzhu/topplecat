# Verification and evidence

ToppleCat verifies an executable Java/JUnit acceptance contract. It does not
select the current work, manage delivery history, grant organizational approval,
or provide an operating-system security boundary.

The Reviewer may be the developer, Spec owner, or another accountable human.
That human reads Spec Review before implementation handoff and Verification
Report after the implementation agent's done claim, then decides whether to
accept the delivery. A team may run the commands locally, in CI, or through an
external workflow; ToppleCat supplies verification and evidence but does not
own that orchestration. Verification Report may recommend acceptance or
rejection from the Gate results, while the human keeps the final decision.

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
ACs; Mutation Testing remains full-contract.

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

The 0.0.17 custody and approval schemas are current-only. A prior schema is not
migrated or read for verification; seal a new reviewer state instead.

## Independent formal work

`./gradlew test` is ordinary development feedback. It runs public project tests
and public acceptance methods, but produces no formal ToppleCat evidence.

`toppleCatVerify` creates a fresh formal public acceptance run and evaluates
the enabled safeguards independently:

| Capability | Evidence that can pass its gate | Cannot be supplemented by |
| --- | --- | --- |
| Hidden Tests | Executed hidden typed rows for the selected ACs | Properties or mutation reports |
| Mutation Testing | A current usable producer report attributed to public acceptance methods | Hidden rows or Properties |
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
Gate. Each AC with covered mutants independently meets or misses its sealed
threshold from exact `killingTests`. An AC with no covered mutant is an
attribution gap: once another AC has exact attribution it is nonblocking and
the reviewer report says `此 AC 沒有取得本次 managed mutation profile 的歸因證據，需要 reviewer 判斷。`
Missing, malformed, interrupted, stale, profile-mismatched, non-full-matrix,
or zero-mutant producer evidence makes Mutation Testing incomplete. PIT's own
status, `detected`, raw mutator, and description remain visible without being
turned into a ToppleCat score.

Contract integrity is the only precondition. Verify first runs the current Check
to rebuild the compiler definition it compares with the Mechanical Seal. If it
is `PASS`, Verify runs public acceptance, hidden typed-row retest,
Property-Based Testing, and Mutation
Testing in that fixed order. A completed safeguard that finds a problem is
`FAIL`, not a reason to skip later safeguards. JUnit-like tasks retain XML,
sidecars, and completion markers after assertion failures; the mutation task
writes usable findings before the aggregate failure exit. The report waits for
all enabled safeguards or their explicit interruption, aggregates expected
consumption, writes evidence, reports, and safe feedback, re-hides reviewer
source, and only then fails Gradle for an aggregate `FAIL` or `INCOMPLETE`.

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

The aggregate evidence verdict is only `PASS`, `FAIL`, or `INCOMPLETE`.
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
non-blocking expected-output quality advisories. There is no public HTML report;
`agent-feedback.json` never exposes reviewer case IDs, values, source names or
paths, Property trial material, tokens, attachments, raw private failures, or
quality-advisory output.

### Reading Mutation results

For each AC, Verification Report first answers a plain question: while
verifying this delivery, did the public Acceptance Method notice enough
temporary changes to production behavior to meet its sealed requirement? For
example, “10 relevant changes; public acceptance noticed 8; meets the sealed
80% requirement” means that AC met this safeguard's recorded rule for this
run. “Below requirement” uses the same three values. This reports what the
Acceptance Method distinguished during the temporary verification changes; it
does not prove that the original program is correct in every situation.

Public Acceptance, Hidden Tests, and Mutation Testing appear separately for the
same AC. A missing Mutation result says “No data”; it is never a pass. If the
current evidence says `DISABLED`, `NOT_APPLICABLE`, or `INCOMPLETE`, the report
keeps the actual reason. Readers who need producer detail can expand the
collapsed technical details, which preserve PIT's official outcomes, managed
profile, operator IDs, attribution counts, selectors, and descriptions exactly
as recorded. The English and Traditional Chinese reports use the same reading
order and evidence values.

`build/topplecat/evidence.json` is the machine verdict for the current run.
Each run starts in `build/topplecat/runs/current/`, receives a fresh UUID when
archived, and retains only a small recent archive set. Stable copies are for
inspection, never inputs to a later verdict.

Spec Review reads the complete selected SDD before executable material. It says
that the specification is prepared but not executed. Verification Report starts
with accepted, rejected, or incomplete, then lists concrete failed findings
before incomplete findings. Its five separate sections preserve the meaning of
Contract Integrity, Public Acceptance and Expected Consumption, Hidden Tests,
Property-Based Testing, and Mutation Testing. A field-level expected/actual
comparison is reviewer-only diagnostic context for the failed compiler Step; it
does not alter the JUnit assertion or Expected Consumption result.

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
