# Architecture

ToppleCat starts at the executable acceptance boundary. Java/JUnit acceptance
methods and typed JSON/YAML case rows are authoritative; generated JSON and
HTML are evidence projections.

## Modules

| Module | Responsibility |
| --- | --- |
| `topplecat-core` | Case, evidence, custody, approval, Property, and safe-feedback models. |
| `topplecat-junit` | `@ToppleAcceptanceTest`, typed rows, compiler-described Scenario/Stage proxies, expected consumption, and PBT engine. |
| `topplecat-report` | Reviewer-only Spec Review and Verification Report projections. |
| `topplecat-gradle-plugin` | Commands, task wiring, scope, custody, policy, integrity, and mutation orchestration. |

## Execution boundary

```text
ordinary ./gradlew test
    public project tests + public acceptance methods
    no ToppleCat evidence, custody, review, report, or sealing dependency

./gradlew toppleCatVerify [--spec ...]
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
passes, each enabled safeguard produces its own current-run result even if an
earlier safeguard fails. `REVIEWER_JUNIT` is `PASS` only when current-run
hidden typed rows executed. When enabled but those rows are missing it is
`INCOMPLETE`; an explicit policy decision is `DISABLED`.

Formal Verify owns its PIT producer: it pins PIT 1.25.5, passes only the fixed
`topplecat-managed-v1` 12-operator profile, targets compiler-emitted public
Acceptance Methods, requests a non-timestamped XML full matrix, and reruns
without task-output or build-cache reuse. A project PIT task or report never
becomes ToppleCat evidence.
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

## Scope and custody

`--spec` is the sole selection input. It maps external Spec ACs to executable
acceptance methods; no input means every AC. `--all-hidden-tests` broadens
hidden typed rows only. Public acceptance and PBT follow the selected ACs.
Mutation Testing remains full-contract.

`toppleCatSeal` stores reviewer-only material under
`~/.topplecat/projects/<sha256-project-key>/escrow/`, along with a mechanical
approval. `toppleCatRestore` exposes it only in a reviewer boundary;
`toppleCatReseal` replaces a restored, rechecked suite. The 0.0.12 format is the
only supported format. Custody is plaintext mechanical storage, not encryption
or a sandbox.

Approval seals compiler-derived acceptance source closure (including resolved
Stage owners), public typed rows,
Property declarations, selected scope, effective policy, compiler definition,
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
Verify reuses an existing Mechanical Seal through an internal custody check; it
does not run Review, Seal, or update approval.

- Spec Review is reviewer-only, precedes handoff, and presents complete selected Markdown documents with their bound executable material.
- Verification Report is reviewer-only and contains results, counterexamples,
  classifications, replay tokens, and private failures when applicable.
- `agent-feedback.json` has gate-level safe reasons only—no hidden values,
  IDs, paths, source names, tokens, attachments, or raw failures.

Spec Review is a document reader, not an execution dashboard: it renders every
selected Markdown document in order before the AC-bound Scenario, public and
reviewer Typed Case Rows, Properties, and the one public Acceptance Method.
Raw Markdown HTML and unsafe URLs are escaped, repository-local image assets
stay inside the offline bundle, and Mermaid has an escaped source fallback.

Verification Report leads with the aggregate conclusion and a Problems Summary.
It keeps Contract Integrity, Public Acceptance (including Expected Consumption),
Hidden Tests, Property-Based Testing, and Mutation Testing in separate sections.
When a `ToppleCase.verify(...)` comparison differs, the active compiler Step
receives reviewer-only structured expected/actual field differences; that data
never enters evidence feedback for the implementation agent.

Direct `toppleCatCheck` logs reviewer-only, non-blocking Contract Quality
Advisories about hidden expected-output shapes and opaque identifier literals,
and Spec Review displays them in its reviewer report. The Check that runs
inside formal `toppleCatVerify` suppresses advisory output. Advisories do not
enter the ContractDefinition, Mechanical Seal, Verify evidence, Verification Report,
`agent-feedback.json`, or any Gate.

The aggregate verdict is `PASS`, `FAIL`, or `INCOMPLETE`. Individual gates may
also be `DISABLED` or `NOT_APPLICABLE`; neither is a passing result.
