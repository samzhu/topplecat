# ToppleCat

<p align="center">
  <img src="docs/images/topplecat-readme-hero.png" alt="ToppleCat tipping over an AI agent's fake done claim" width="100%">
</p>

<p align="center"><strong>Turn an AI agent's “done” into current-run evidence.</strong></p>

<p align="center"><a href="README.zh-TW.md">繁體中文</a> · <a href="LICENSE">Apache-2.0</a></p>

ToppleCat is a Java/JUnit delegation-verification gate. Java acceptance tests
and typed JSON or YAML case rows are the executable contract; JSON evidence and
HTML reports are projections, never another source of truth.

It keeps three independent safeguards separate:

| Safeguard | Input and question | Gate |
| --- | --- | --- |
| **Hidden Tests** | Reviewer-owned typed case rows: do independently chosen examples pass? | `REVIEWER_JUNIT` |
| **Mutation Testing** | Exact public Acceptance Methods and PIT's full matrix: does each method detect the mutants it covered? | `MUTATION` |
| **Property-Based Testing (PBT)** | Bounded `@ToppleProperty` declarations: does an approved invariant survive generated inputs? | `PROPERTY` |

None supplies evidence for another. They share contract integrity, scope
selection, reporting, and the aggregate verdict only. Reviewer custody belongs
to Hidden Tests; Property declarations live under `src/test`. Expected-value
consumption is a separate `EXPECTED_CONSUMPTION` guard for typed case rows.

ToppleCat does not manage tasks, Spec lifecycle, organizational approval, CI
isolation, or OS security. People select the delivery, make its rules complete,
and decide sign-off.

## The two pipelines

```text
./gradlew test
    ordinary project tests and public acceptance tests; development feedback only

./gradlew toppleCatVerify --spec path/to/spec.md
    fresh formal acceptance evidence for the selected delivery
```

The ordinary `test` task must not depend on Check, Review, Seal, custody,
reports, or formal evidence. `toppleCatVerify` always runs the formal public
acceptance task freshly, then evaluates every enabled Independent Safeguard.
After contract integrity passes, a failed safeguard records its own result but
does not stop later safeguards; the one aggregate failure comes after evidence,
reports, safe feedback, and rehide.

## Workflow

```text
author public contract + Properties + reviewer-owned hidden rows
    -> toppleCatCheck -> toppleCatReview -> toppleCatSeal
    -> implementation uses ./gradlew test
    -> reviewer or CI uses toppleCatVerify
```

Use `toppleCatRestore` only in the reviewer boundary. After editing reviewer
material, use `toppleCatRestore -> toppleCatCheck -> toppleCatReview ->
toppleCatReseal`. Custody is plaintext local state under
`~/.topplecat/projects/<sha256-project-key>/escrow/`; it is not encryption or a
sandbox.

`--spec <repository-relative-markdown-file>` is the only delivery input. Repeat
it for multiple documents; no `--spec` selects all acceptance conditions.
`--all-hidden-tests` broadens only hidden typed rows. Public Properties follow
the selected ACs; Mutation Testing remains the full public acceptance contract.

For Mutation Testing, formal Verify always runs ToppleCat's managed PIT 1.25.5
producer with the fixed `topplecat-managed-v1` profile. It never consumes a
project `pitest` task, a consumer-selected producer, or a consumer report path.
Project-wide `tasks.withType(PitestTask)` conventions remain part of a separate
project PIT workflow and cannot alter formal Verify.
ToppleCat preserves PIT's raw `status`, `detected`, mutator, description, and
selector relationships. Its per-AC detection rate counts the mutants a
specific public Acceptance Method appears in `killingTests` for, divided by the
mutants that same method appears in `coveringTests` for; it is not PIT's global
mutation threshold. An AC with no covered managed-profile mutant is a
reviewer-visible attribution gap, not automatic passing evidence. Reviewer-only
The reviewer-only Verification Report shows the raw matrix; safe feedback stays at Gate level.
See the [managed mutation profile design](docs/design/managed-mutation-profile.md)
for the fixed 12 operators and Gate rules.

## Write an acceptance contract

Each AC has one public `@ToppleAcceptanceTest`. The default form receives one
`ToppleScenario` and one or more concrete capability Stages. Its small
orchestration method selects the compiled Given/When/Then order; setup,
service calls, assertions, and control flow belong in those Stages.

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_cart(c.input("cart", Cart.class));
    scenario.when(coupon).creates_an_order();
    scenario.then(coupon).receipt_matches(c);
}
```

`CouponStage` is a non-final concrete `ToppleStage` with an accessible
no-argument constructor. The same proxy carries its per-case state across the
three calls. This is the only supported acceptance-authoring form.

Public rows live under `src/test/resources/topplecat/cases/`; reviewer-owned
rows use the same schema under `src/hiddenTest/resources/topplecat/cases/`.
One row has exactly `caseId`, `acId`, `inputs`, and `expected`. A top-level
expected value is an assertion obligation: `c.verify(...)` consumes it, while
merely reading it does not.

## Add an invariant when examples are not enough

`@ToppleProperty` is a bounded JUnit check tied to an existing AC. It is not a
case row and does not enter expected consumption or mutation attribution.

```java
@ToppleProperty("AC-CART-COUPON")
void payableTotalIsNeverNegative(PropertyTrials trials) {
    trials.forAll(Generators.integers(0, 10_000))
        .classify("free-shipping-boundary", subtotal -> subtotal >= 1_000)
        .requireCoverage("free-shipping-boundary", 5.0)
        .check(subtotal -> assertTrue(checkout.payable(subtotal) >= 0));
}
```

Properties live under `src/test`, but ordinary `./gradlew test` excludes them.
`toppleCatVerify` runs Properties for the selected ACs in the independent
`PROPERTY` gate. A reproducible failure records generator choices, a shrunk
counterexample, and a replay token in the reviewer-only Verification Report
report. Safe feedback never contains generated inputs, identifiers, tokens,
paths, or raw failures.

## Configure safeguards

Each safeguard has its own switch. Disabling one records `DISABLED`; it never
pretends to be `NOT_APPLICABLE` or `PASS`.

```kotlin
toppleCat {
    hiddenTests { enabled.set(false) }
    mutationTesting { enabled.set(false) }
    propertyBasedTesting { enabled.set(false) }
    expectedConsumption { enabled.set(false) }
}
```

If Hidden Tests remain enabled, missing executed hidden rows leave
`REVIEWER_JUNIT=INCOMPLETE`, even if a Property passes. A Property-only team
must explicitly disable Hidden Tests and reseal that policy; then evidence shows
`REVIEWER_JUNIT=DISABLED` and the actual `PROPERTY` result.

## Read the result

| Artifact | Audience | Purpose |
| --- | --- | --- |
| `build/topplecat/reports/review/index.html` | Reviewer | Spec Review before handoff: the complete selected Markdown document and its bound executable material. |
| `build/topplecat/reports/verification/index.html` | Reviewer | Failure-first Verification Report for one formal run, including private diagnostics. |
| `build/topplecat/evidence.json` | Reviewer / CI | Machine verdict and gate digests. |
| `build/topplecat/agent-feedback.json` | Implementation agent | Gate-level safe feedback only. |

Spec Review can also show non-blocking reviewer advisories about hidden
expected-output shapes and likely opaque identifier literals. They are prompts
to examine the selected examples, not inferred business rules: they never
change the executable contract, Seal, evidence, public handoff, or a Gate.

Every formal run records `CONTRACT_INTEGRITY`, `JUNIT`, `REVIEWER_JUNIT`,
`EXPECTED_CONSUMPTION`, `PROPERTY`, and `MUTATION`. The aggregate verdict is
`PASS`, `FAIL`, or `INCOMPLETE`; accept a done claim only when the current run
is `PASS`.

## Install 0.0.12

ToppleCat requires Java 25 and a compatible Gradle version.

```kotlin
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.12"
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.0.12")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

## Learn more

- [Getting started](docs/guide/getting-started.md)
- [Authoring contracts](docs/guide/authoring.md)
- [Verification and evidence](docs/guide/verification-and-evidence.md)
- [Documentation index](docs/README.md)
- [Context glossary](CONTEXT.md)
- [Architecture](docs/architecture.md)
- [0.0.12 release notes](docs/releases/0.0.12.md)
- [JUnit sample](samples/junit-cart-orders)
- [Spring Boot sample](samples/spring-boot-cart-orders)

The repository also provides the
[`topplecat-acceptance`](.agents/skills/topplecat-acceptance/SKILL.md) skill.
It helps SDD agents turn selected ACs into executable Java acceptance methods,
public and reviewer case rows, and optional Properties. Humans or external
workflow automation remain responsible for running ToppleCat and accepting its
verdict.
