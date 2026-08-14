# Learn ToppleCat with JUnit cart orders

This standalone, synthetic JUnit project shows a familiar failure mode: the
public tests pass, but the implementation still breaks the agreed rule. It
uses the locally published ToppleCat 0.2.2 artifact; Maven Central publication
is a separate maintainer action and is not assumed by this sample.

The names in this README follow the [ToppleCat glossary](../../CONTEXT.md): a
Java **Acceptance Method** runs **Typed Case Rows**. Public Typed Case Rows are
the examples visible to the implementation agent. **Reviewer-controlled Typed
Case Rows (Hidden Tests)** use the same Acceptance Method with independently
chosen examples; they do not add a private rule.

## Requirements

- JDK 21 or 25 (ToppleCat execution); the consumer source fixture may target
  Java 17, 21, or 25
- Internet access the first time Gradle downloads the wrapper and Maven Central
  dependencies

```bash
./gradlew test
./demo.sh --help
```

Choose the question you want to understand:

| Command | What it demonstrates |
| --- | --- |
| `./demo.sh public-acceptance` | Public typed examples reject a wrong result. |
| `./demo.sh hidden-tests` | Reviewer-controlled Typed Case Rows (Hidden Tests) catch the checked-in 20% shortcut. |
| `./demo.sh property-based-testing` | A bounded invariant reaches beyond example rows. |
| `./demo.sh mutation-testing` | Managed PIT finds a production change an acceptance method misses. |
| `./demo.sh contract-integrity` | A post-Seal contract change is not trusted. |
| `./demo.sh all` | Runs all five lessons. |

The checked-in service deliberately takes a synthetic 20% shortcut: the public
500-dollar cart still passes, but Reviewer-controlled Typed Case Rows (Hidden
Tests) reject the same rule at other values. The other lessons add their own
synthetic fault in a temporary copy, then clean it up.

## After a demo: read the report

Run one lesson first:

```bash
./demo.sh hidden-tests
```

The command ends by printing the path to its local synthetic HTML Verification
Report. Open `build/topplecat/demo-reports/hidden-tests/index.html`, then check
the result for the Gate this lesson targets:

| Lesson | Look for this Gate | What the report should show |
| --- | --- | --- |
| `public-acceptance` | `JUNIT=FAIL` | A public Typed Case Row disagrees with the result. |
| `hidden-tests` | `REVIEWER_JUNIT=FAIL` | Reviewer-controlled Typed Case Rows find the 20% shortcut. |
| `property-based-testing` | `PROPERTY=FAIL` | A generated input breaks the fixed-discount invariant. |
| `mutation-testing` | `MUTATION=FAIL` | A production mutation survives the weakened Acceptance Method. |
| `contract-integrity` | `CONTRACT_INTEGRITY=FAIL` | Verify refuses a Typed Case Row changed after the Mechanical Seal. |

These `FAIL` results are expected: each one is the point of its lesson. Start
with the named Gate, then read the failed AC and case details below it. One bad
synthetic delivery can affect more than one check, so other Gates may fail too.
The report records what happened in that run; it does not decide whether a
human should accept a delivery.

These examples explain evidence for the contract; they never prove that a human
selected every business rule or made a delivery decision.

All fixtures, values, and diagnostics in this project are synthetic teaching
material. Generated reports stay local and ignored. See
[README.zh-TW.md](README.zh-TW.md) for Traditional Chinese.
