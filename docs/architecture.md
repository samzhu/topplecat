# Architecture

ToppleCat starts at the executable acceptance boundary. Java/JUnit acceptance
methods and typed JSON/YAML case rows are authoritative; generated JSON and
HTML are evidence projections.

## Modules

| Module | Responsibility |
| --- | --- |
| `topplecat-core` | Case, evidence, custody, approval, Property, and safe-feedback models. |
| `topplecat-junit` | `@ToppleAcceptanceTest`, typed rows, compiler-described Scenario/Stage proxies, expected consumption, and PBT engine. |
| `topplecat-report` | Contract Review, safe Public Spec, and reviewer-only Verification Evidence projections. |
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
| Mutation Testing | Public acceptance methods and producer report | mutation producer + `toppleCatMutationGate` | `MUTATION` |
| Property-Based Testing | `@ToppleProperty` declarations, generators, current events | dedicated Property task | `PROPERTY` |

The three capabilities share only scope, integrity, reports, and the aggregate
verdict. Reviewer custody is used only for Hidden Tests. A Property result
cannot supply Hidden Test coverage. A mutation result cannot supply Property
evidence. `REVIEWER_JUNIT` is `PASS` only when current-run hidden typed rows
executed. When enabled but those rows are missing it is `INCOMPLETE`; an
explicit policy decision is `DISABLED`.

## Scope and custody

`--spec` is the sole selection input. It maps external Spec ACs to executable
acceptance methods; no input means every AC. `--all-hidden-tests` broadens
hidden typed rows only. Public acceptance and PBT follow the selected ACs.
Mutation Testing remains full-contract.

`toppleCatSeal` stores reviewer-only material under
`~/.topplecat/projects/<sha256-project-key>/escrow/`, along with a mechanical
approval. `toppleCatRestore` exposes it only in a reviewer boundary;
`toppleCatReseal` replaces a restored, rechecked suite. The 0.0.7 format is the
only supported format. Custody is plaintext mechanical storage, not encryption
or a sandbox.

Approval seals compiler-derived acceptance source closure (including resolved
Stage owners), public typed rows,
Property declarations, selected scope, effective policy, compiler definition,
and project Gradle logic. The source closure includes only sources javac
resolves from acceptance methods and Stage owners; unrelated ordinary tests do
not invalidate it.

## Evidence and information boundary

Every formal run starts below `build/topplecat/runs/current/`, gets a new UUID,
and is archived after reports are written. Stable copies are diagnostic only;
they cannot supply missing current evidence. Gates are recorded in this order:

```text
CONTRACT_INTEGRITY
JUNIT
REVIEWER_JUNIT
EXPECTED_CONSUMPTION
PROPERTY
MUTATION
```

`CONTRACT_INTEGRITY` cannot be disabled. A mismatch prevents downstream work
and marks it `FAIL`; absent or incomplete current proof is `INCOMPLETE`.

- Contract Review is reviewer-only and precedes handoff.
- Public Spec is a safe post-Verify projection under `reports/public/`.
- Verification Evidence is reviewer-only and contains results, counterexamples,
  classifications, replay tokens, and private failures when applicable.
- `agent-feedback.json` has gate-level safe reasons only—no hidden values,
  IDs, paths, source names, tokens, attachments, or raw failures.

The aggregate verdict is `PASS`, `FAIL`, or `INCOMPLETE`. Individual gates may
also be `DISABLED` or `NOT_APPLICABLE`; neither is a passing result.
