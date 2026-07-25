# Troubleshooting

`toppleCatCheck` is the early, static diagnostic. It does not execute tests or
write HTML. Fix its named source data or Java binding, then rerun the same check.

## A Canonical Test Breaks the Stage DSL

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

## A Case References an Unknown Acceptance Condition

```text
Case <case-id> in <source> references AC <ac-id>, but javac emitted no canonical @ToppleTest descriptor. Add a compilable @ToppleTest("<ac-id>") method or correct the case acId.
```

Add one public literal `@ToppleTest("AC-...")` method, or correct the row's
`acId`. A reviewer row may target an existing public AC but may not create a new
one.

## There Is No Public Case Data

```text
No public ToppleCat JSON/YAML cases found under <public-case-root>
```

Add a `.json`, `.yaml`, or `.yml` row under
`src/test/resources/topplecat/cases/`. Public case data is required for a
canonical parameterized acceptance condition.

## A Case File or Row Is Invalid

An unsupported file is rejected with this exact diagnostic:

```text
Topple case source must be JSON or YAML: <path>
```

Each row must contain exactly `caseId`, `acId`, `inputs`, and `expected`; move
notes and unrelated files outside the configured case roots.

## An Expected Key Was Never Verified

A test can return successfully and still fail with this message:

```text
Topple case <case-id> expected.<key> was declared by <ac-id> but never verified. Call c.verify("<key>", actual).
```

Use `c.verify("<key>", actual)` for every top-level expected key. Reading with
`c.expected(...)` does not fulfil the obligation.

## An External Spec Is Not Aligned With Its Tests

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

## A Safeguard Is Disabled or Incomplete

Each evidence run records `CONTRACT_INTEGRITY`, `JUNIT`, `REVIEWER_JUNIT`,
`EXPECTED_CONSUMPTION`, and `MUTATION`. `DISABLED` means the reviewer deliberately selected not to run one
safeguard; it does not block aggregate `PASS`. These are the exact configuration
reasons for disabled adversarial safeguards:

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
gate and safe reason. When expected consumption is deliberately disabled, the
Verification report still records consumption and displays:

```text
Expected consumption enforcement disabled
```

## Contract Integrity Is FAIL or INCOMPLETE

`CONTRACT_INTEGRITY` is a mandatory precondition. `FAIL` means ToppleCat
successfully found that the public executable contract or resolved verification
policy no longer matches the reviewer-approved epoch. `INCOMPLETE` normally
means the escrow predates approval sealing or its approval cannot be read.

Do not rerun Hide to accept the change: ordinary Hide deliberately preserves the
existing approval. An authorized reviewer must run Restore, inspect the intended
change, run Check and Review, then run `toppleCatUpdateEscrow`. For a legacy v1
escrow this explicit sequence performs the migration to v2. Until then, JUnit,
reviewer JUnit, expected consumption, and mutation are deliberately recorded as
`INCOMPLETE`; no stale public Spec bundle is retained.

## PIT Is Not Producing Mutation Results

With the default `pitest` producer, ToppleCat configures PIT automatically. If a
custom producer task is unavailable, the mutation gate reports:

```text
ToppleCat mutation is enabled, but producer task '<producer>' was not found. ToppleCat configures PIT automatically for the default 'pitest' task; otherwise set toppleCat.adversarial.mutation.producerTask to a task that writes mutations.xml.
```

For a custom PIT output path, use the reported producer task and configure
`toppleCat.adversarial.mutation.reportFile`. The producer must write a full
mutation matrix whose `coveringTests` retain PIT's JUnit Unique IDs. ToppleCat
matches each compiled canonical method signature to those IDs; class names
alone are intentionally insufficient when one class contains several ACs. A
surviving or unattributed mutant fails its acceptance condition while evidence
and safe feedback are still produced.

## Delivery Hygiene

`.topplecat/escrow/` is plaintext mechanical state, not a secrecy boundary.
`./gradlew clean` does not remove it, and Git history can retain reviewer files
after they leave the working tree. Never commit reviewer source to history the
implementation agent can read. Use a public export without `.git`,
`.topplecat/`, or `build/`; an isolated environment whose history never
contained reviewer material; or separate public implementation and private
reviewer repositories or CI environments.

## The Public Report Lacks Reviewer Detail

This is expected for the Spec bundle and `agent-feedback.json`. Use
`reports/verification/index.html` only in a reviewer-controlled environment when
private diagnostics are needed. The pre-handoff `reports/review/index.html` is
also reviewer-only.
