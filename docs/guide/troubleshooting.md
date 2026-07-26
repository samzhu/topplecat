# Troubleshooting

`toppleCatCheck` is the early, static diagnostic. It does not execute tests or
write HTML. Fix its named source data or Java binding, then rerun the same check.

## A canonical test breaks the Stage DSL

```text
AC <ac-id> at <file>:<line>:<column> violates the @ToppleTest Stage DSL:
<rule>. <repair>
```

Every canonical `@ToppleTest` must be a non-empty sequence of direct calls to
fields declared on that test class with `@ToppleStageField`. Move local setup,
SUT calls, helper calls, control flow, and assertions into a Stage step. A step
must call `recorded(...)` first and end with `return self();`; put `c.verify(...)`
in a Then step. The diagnostic names the AC and source line and tells you which
move to make. `@ToppleAc` is not subject to this canonical-method rule.

## A case references an unknown acceptance condition

```text
Case <case-id> in <source> references AC <ac-id>, but javac emitted no canonical @ToppleTest descriptor. Add a compilable @ToppleTest("<ac-id>") method or correct the case acId.
```

Add one public literal `@ToppleTest("AC-...")` method, or correct the row's
`acId`. A reviewer row may target an existing public AC but may not create a new
one.

## There is no public case data

```text
No public ToppleCat JSON/YAML cases found under <public-case-root>
```

Add a `.json`, `.yaml`, or `.yml` row under
`src/test/resources/topplecat/cases/`. Public case data is required for a
canonical parameterized acceptance condition.

## A case file or row is invalid

An unsupported file is rejected with this exact diagnostic:

```text
Topple case source must be JSON or YAML: <path>
```

Each row must contain exactly `caseId`, `acId`, `inputs`, and `expected`; move
notes and unrelated files outside the configured case roots.

## An expected key was never verified

A test can return successfully and still fail with this message:

```text
Topple case <case-id> expected.<key> was declared by <ac-id> but never verified. Call c.verify("<key>", actual).
```

Use `c.verify("<key>", actual)` for every top-level expected key. Reading with
`c.expected(...)` does not fulfil the obligation.

## An external spec is not aligned with its tests

External Markdown is optional reading context, not another contract. There is no
implicit scan: configure `toppleCat.specDocs` explicitly. A missing entry or a
non-Markdown file reports one of these errors:

```text
Configured ToppleCat specDocs entry does not exist: <path>. Create the file or directory, or remove it from toppleCat.specDocs.
ToppleCat specDocs entry <path> is not a Markdown file. Use a .md file or a directory containing .md files.
```

When `specDocs` is configured, the check warns in either direction:

```text
ToppleCat check warning: external spec <path> mentions <AC>, but no Java binding exists. Add @ToppleTest("<AC>") or remove the stale AC id.
ToppleCat check warning: Java binding <AC> at <path> has no AC anchor in the configured specDocs. Add <AC> to a Markdown heading or paragraph, or remove the stale binding.
```

Add the literal `AC-...` ID to a Markdown heading or paragraph, add the named
canonical `@ToppleTest`, or remove the stale side. Leaving `specDocs` unset emits
neither warning. Supplementary `@ToppleAc` methods and reviewer source do not
participate in this alignment check.

## A safeguard is disabled or incomplete

Each evidence run records `CONTRACT_INTEGRITY`, `JUNIT`, `REVIEWER_JUNIT`,
`EXPECTED_CONSUMPTION`, and `MUTATION`. `DISABLED` means the reviewer chose not
to run one safeguard; it does not block aggregate `PASS`. These are the exact
configuration reasons for disabled adversarial safeguards:

```text
disabled by toppleCat.adversarial.enabled=false
disabled by toppleCat.adversarial.hiddenRetest.enabled=false
disabled by toppleCat.adversarial.mutation.enabled=false
disabled by toppleCat.adversarial.expectedConsumption.enabled=false
```

`INCOMPLETE` is different: it means a required stage did not complete in the
current run. For example, the reviewer retest reason is:

```text
the reviewer retest did not complete in this verification run.
```

Rerun the complete `toppleCatVerify` task; do not use an older stable report to
fill a missing current-run gate. A `FAIL` or `INCOMPLETE` aggregate verdict
makes `toppleCatVerify` and `toppleCatReport` fail after evidence, reports, safe
feedback, and the run archive are complete. Read `evidence.json` for the named
gate and safe reason. When expected consumption is disabled, the
Verification report still records consumption and displays:

```text
Expected consumption enforcement disabled
```

## Contract integrity is FAIL or INCOMPLETE

`CONTRACT_INTEGRITY` is a mandatory precondition. `FAIL` means ToppleCat
successfully found that the public executable contract or resolved verification
policy no longer matches the reviewer-approved epoch. `INCOMPLETE` normally
means the escrow predates approval sealing or its approval cannot be read.

Do not rerun Hide to accept the change: ordinary Hide does not replace the
existing approval. An authorized reviewer must run Restore, inspect the intended
change, run Check and Review, then run `toppleCatUpdateEscrow`. For a legacy v1
escrow this sequence performs the migration to v2. Until then, JUnit, reviewer
JUnit, expected consumption, and mutation are recorded as `INCOMPLETE`; no stale
public Spec bundle is retained.

## Reviewer state is missing after a move or clone

The project key is the SHA-256 of the canonical project root. A moved or cloned
checkout therefore does not match the original reviewer-local state and must
not create a new approval from public files. Restore the original checkout, or
have an authorized reviewer recover the matching
`~/.topplecat/projects/<project-key>/escrow/` state. If the tree still contains
legacy `.topplecat/escrow/`, run `./gradlew toppleCatMigrateEscrow` explicitly;
the task preserves v1/v2 manifest data and removes the project-local escrow only
after migration succeeds.

## PIT is not producing mutation results

With the default `pitest` producer, ToppleCat configures PIT automatically and
derives `targetTests` from compiler descriptors for approved public canonical
`@ToppleTest` declaring classes. This is independent of the production package
name. If a consumer explicitly sets `targetTests`, ToppleCat preserves it; a
usable report that excludes a canonical test is a mutation `FAIL`, not a silent
pass. If PIT cannot produce a usable report, the gate is `INCOMPLETE`.

If a custom producer task is unavailable, the mutation gate reports:

```text
ToppleCat mutation is enabled, but producer task '<producer>' was not found. ToppleCat configures PIT automatically for the default 'pitest' task; otherwise set toppleCat.adversarial.mutation.producerTask to a task that writes mutations.xml.
```

For a custom PIT output path, use the reported producer task and configure
`toppleCat.adversarial.mutation.reportFile`. The producer must write a full
mutation matrix whose `coveringTests` retain PIT's JUnit Unique IDs. ToppleCat
matches each compiled canonical method signature to those IDs; class names
alone do not identify the right method when one class contains several ACs. A
surviving or unattributed mutant fails its acceptance condition while evidence
and safe feedback are still produced.

When a usable report contains no covering test for a canonical AC, the safe
feedback may say:

```text
Mutation verification did not exercise the required public acceptance contract. Check PIT test targeting and public acceptance coverage.
```

The message omits reviewer test names, source paths, and raw PIT details. A
missing, malformed, or interrupted report is `INCOMPLETE`; an older report
cannot fill the gap.

## Reviewer rows without hidden Java

Hidden JSON/YAML rows do not require a placeholder Java test. `toppleCatVerificationTest`
runs those rows through the public canonical `@ToppleTest`, and the report aggregates
that result as `REVIEWER_JUNIT`. A failing row makes the reviewer gate `FAIL`; an
unasserted expected key still makes expected consumption `FAIL` or `INCOMPLETE`.
Java helper sources with no executable JUnit method do not turn a rows-only source
set into a hidden-test requirement. If hidden Java tests also exist, `hiddenTest`
must pass independently. If neither
hidden rows nor hidden Java tests exist while retest is enabled, the gate stays
`INCOMPLETE` rather than becoming an unconditional pass.

## Delivery hygiene

Reviewer state is plaintext mechanical custody at
`~/.topplecat/projects/<sha256-project-key>/escrow/`, not encryption, sandboxing,
or a secrecy boundary. `./gradlew clean` does not remove it. A legacy
`.topplecat/escrow/` requires `toppleCatMigrateEscrow`; do not copy it into a
public handoff. Git history can retain reviewer files after they leave the
working tree, so the external workflow must exclude reviewer state, hidden
source, build output, and any history that contained them. ToppleCat does not
control OS access or CI identity and cannot defend against same-user malicious
build scripts or production code.

## The public report lacks reviewer detail

This is expected for the Spec bundle and `agent-feedback.json`. Use
`reports/verification/index.html` only in a reviewer-controlled environment when
private diagnostics are needed. The pre-handoff `reports/review/index.html` is
also reviewer-only.
