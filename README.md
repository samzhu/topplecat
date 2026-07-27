# ToppleCat

<p align="center">
  <img
    src="docs/images/topplecat-readme-hero.png"
    alt="ToppleCat tipping over an AI agent's fake done claim"
    width="100%"
  >
</p>

<p align="center">
  <strong>Turn your AI agent's "done" into evidence.</strong>
</p>

<p align="center">
  <a href="README.zh-TW.md">繁體中文</a>
  ·
  <a href="https://github.com/samzhu/topplecat/actions/workflows/ci.yml">Build</a>
  ·
  <a href="LICENSE">Apache-2.0</a>
</p>

ToppleCat is a curious cat.

Whenever an AI coding agent says a Java task is **done**, ToppleCat reaches out
and gives the claim a little nudge. It reruns the contract with reviewer-only
cases, mutates production behavior to see whether public tests notice, and
checks that every declared result was actually asserted. These checks catch
invented rules, hollow or partial implementations, and code written only to
match visible examples.

A reassuring command result from the agent is not enough. ToppleCat reads the
current run's gates together and turns the done claim into evidence.

> Hidden retests, mutation gates, and executable acceptance contracts for Java.
> If it's hollow, it falls.

Robert C. Martin recently described a similar way of working with coding
agents:

> I’m significantly older than you. I started coding in the late 60s. My
> current strategy is to not read any of the code written by my agents. That’s
> the only way I can take advantage of their productivity. What I do instead is
> to surround the agents with extreme constraints. Unit tests, gherkin tests,
> QA procedures, quality metrics, mutation testing, test coverage, and a
> plethora of others. In the end, I have very high confidence in the code they
> produce because they’ve had to run the gauntlet of all of my constraints and
> tests.
>
> — [Robert C. Martin (Uncle Bob), July 23, 2026](https://x.com/unclebobmartin/status/2080257779395154409)

His point is to make agent-written code run a gauntlet before trusting it.
ToppleCat handles one Java/JUnit part of that approach: executable acceptance
contracts, reviewer-only cases, expected-value checks, and mutation testing.
It does not replace code review, QA, CI isolation, or a sandbox.

ToppleCat is a delegation verification gate for Java and JUnit. Ordinary Java
acceptance tests plus typed JSON or YAML case rows are the executable contract.
Generated JSON and HTML are evidence, never a second source of truth.

Canonical scenarios are ordinary Java written to read like business language.
[JGiven](https://github.com/TNG/JGiven) is the closest prior art for readable,
staged Java tests. ToppleCat adds a reviewer boundary around that idea: hidden
retests, mutation evidence, and safe feedback check a delegated done claim.
There is no Cucumber, Gherkin, or second executable authoring format.

## What ToppleCat catches

| A green test can still mean... | ToppleCat checks it with... |
| --- | --- |
| The implementation may be tuned to the visible example. | Reviewer-controlled **hidden retests** with independently chosen business cases. |
| The test runs but cannot detect broken behavior. | A PIT-backed **mutation gate**. |
| Expected data was read but never compared with reality. | Enforced **expected consumption**. |
| The visible contract or verification strength changed after review. | A mandatory, reviewer-sealed **contract-integrity gate**. |
| Old or partial output is mistaken for current proof. | Run-scoped gates, digests, and an explicit **evidence verdict**. |

Hidden retest and mutation answer different questions. A hidden retest exercises
business cases the implementation agent did not receive; it is not a guarantee
that every possible hard-coded shortcut will be exposed. The default PIT
producer measures **public executable contract mutation strength**: it uses
`sourceSets.test`, public test classes, and public case rows only. Reviewer rows
and reviewer-only JUnit tests never help that producer kill a mutant. If a
boundary must kill a mutant, it belongs in the public contract. For the managed
PIT producer, compiler descriptors select every public canonical `@ToppleTest`
declaring class as `targetTests`; consumer-owned `targetTests` and custom
mutation producers are left untouched. ToppleCat does not add per-case
mutation scores or infer the scope of a custom producer.

Reviewer rows can reuse the public canonical `@ToppleTest`; when `src/hiddenTest`
contains rows but no Java tests, that canonical run supplies the reviewer retest
result. Add reviewer-only Java tests only for behavior the canonical method cannot
express. Java helper sources under `src/hiddenTest` without an executable JUnit
method are compiled but do not count as hidden tests. If hidden retest is enabled
with neither hidden rows nor hidden Java tests,
ToppleCat stays fail-closed with `REVIEWER_JUNIT=INCOMPLETE`.

A gate result describes that gate, not a universal verdict about the
implementation. A reviewer retest can pass while mutation rejects a shortcut,
or the reverse can happen. Read `evidence.json` to see which gate rejected a
claim; accept it only when the current run's aggregate verdict is `PASS`.

## Watch a fake completion fall

The JUnit sample starts with a deliberate defect that passes the public case.
Its repeatable demo rejects that claim with a hidden boundary, applies the real
fix, verifies again, and restores the checked-in source.

```bash
git clone https://github.com/samzhu/topplecat.git
cd topplecat
bash samples/junit-cart-orders/demo.sh
```

The final output points to `evidence.json`, safe agent feedback, and the HTML
reports. Read the [JUnit walkthrough](samples/junit-cart-orders/TUTORIAL.md) for
the complete FAIL-to-PASS story.

## How it fits the development flow

```text
Java acceptance contract + typed public/reviewer cases
          |
          v
toppleCatCheck -> toppleCatReview -> toppleCatHide
          |
          v
AI agent implements against the public tree with ./gradlew test
          |
          v
Reviewer or CI runs toppleCatVerify
          |
          v
PASS / FAIL / INCOMPLETE evidence and human reports
```

1. **Author the executable contract.** Keep public tests and case rows under
   `src/test`; keep independently derived reviewer retests under
   `src/hiddenTest`.
2. **Check and review it.** `toppleCatCheck` validates the contract.
   `toppleCatReview` renders the complete reviewer-only review.
3. **Transfer reviewer custody.** `toppleCatHide` moves `src/hiddenTest` into
   reviewer-local plaintext custody at
   `~/.topplecat/projects/<sha256-project-key>/escrow/`. It is not a secrecy
   boundary. To evolve an existing reviewer suite, an authorized reviewer uses
   the explicit restore, review, and update workflow rather than ordinary hide.
4. **Implement normally.** Give the implementation agent a public-only
   environment and use ordinary `./gradlew test`.
5. **Verify the claim.** `toppleCatVerify` first checks the sealed public
   contract and verification policy, then restores reviewer source and executes
   enabled gates only when that approval still matches. It writes evidence and
   hides the source again in every outcome.

## Install 0.0.4

ToppleCat `0.0.4` is the release described here. A consumer project needs Java
25 and a Gradle version that supports it. Once published, add Maven Central for
both plugin and library resolution; a released consumer does not need
`mavenLocal()`.

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
```

```kotlin
// build.gradle.kts
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.4"
}

dependencies {
    testImplementation(
        "io.github.samzhu.topplecat:topplecat-junit:0.0.4"
    )
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

For an otherwise empty consumer project, `./gradlew toppleCatInit` creates a
non-destructive starter contract. It is an optional bootstrap, not a normal
workflow step. The checked-out repository demos use
`publishToMavenLocal` so they exercise the source checkout rather than the
released artifact; that is a contributor/demo workflow, not the installation
path above.

## Write an executable contract

A canonical `@ToppleTest` is a short, business-readable orchestration of
`ToppleStage` methods. The compiler keeps setup, service calls, assertions,
helpers, and control flow inside the stages rather than the scenario method.

```java
@ToppleStageField CartGiven given;
@ToppleStageField CheckoutWhen when;
@ToppleStageField ReceiptThen then;

@ToppleTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c) {
    given.a_cart(c.input("cart", Cart.class));
    when.creates_an_order();
    then.receipt_matches(c);
}
```

Each stage step calls `recorded(...)` first, performs the work, and returns
`self()`. ToppleCat compiles those calls into stable, human-readable scenario
sentences while JUnit executes the real Java methods.

Case rows preserve nested DTOs, lists, maps, and API results:

```yaml
- caseId: coupon-public-example
  acId: AC-CART-COUPON
  inputs:
    cart:
      items:
        - {sku: mug, quantity: 2, unitPrice: 250.00}
      couponCode: SAVE100
  expected:
    receipt:
      discount: 100.00
      total: 400.00
```

Every row contains exactly `caseId`, `acId`, `inputs`, and `expected`. Jackson
deserializes the data into the requested Java type. Every top-level `expected`
key is an assertion obligation: `c.verify("receipt", actual)` deep-compares and
consumes it; merely reading it does not count.

ToppleCat supports JSON and YAML case rows. It does not support CSV or introduce
a natural-language runtime.

## Run the gate

Run these commands from the consumer project:

```bash
./gradlew toppleCatCheck
./gradlew toppleCatReview
./gradlew toppleCatHide
./gradlew test
./gradlew toppleCatVerify
```

`toppleCatRestore` is a reviewer-only recovery and editing command. It is not
part of the implementation loop. After an authorized reviewer restores and
edits an existing suite, the required custody update flow is:

```text
toppleCatRestore
    -> edit src/hiddenTest
    -> toppleCatCheck
    -> toppleCatReview
    -> reviewer accepts the review
    -> toppleCatUpdateEscrow
```

`toppleCatUpdateEscrow` validates and stages the complete revised reviewer
source before activating it. It requests an atomic filesystem move where
supported and retains the same validation and recovery path otherwise.
Ordinary `toppleCatHide` still rejects a changed restored suite. A public
implementation export has neither reviewer source nor local escrow, so this
reviewer-only task fails safely there.

## Read the result

| Artifact | Audience | Purpose |
| --- | --- | --- |
| `build/topplecat/reports/review/index.html` | Reviewer only | Pre-handoff Spec context, public and hidden cases, Stage sentences, and canonical source. |
| `build/topplecat/reports/spec/index.html` | Public | Post-verify public contract rendered for humans. |
| `build/topplecat/reports/verification/index.html` | Reviewer only | Public and hidden case results, steps, failures, gates, and attachments. |
| `build/topplecat/evidence.json` | Reviewer / CI | Machine verdict and evidence digests. |
| `build/topplecat/agent-feedback.json` | Implementation agent | Safe gate-level feedback with reviewer details removed. |

Verification reports are self-contained offline bundles. Public artifacts
exclude reviewer values, case IDs, source names and paths, attachments, and raw
private failures.

The first required gate is `CONTRACT_INTEGRITY`: it compares the current public
test sources, case data, project-local Gradle logic, semantic definition, and
effective verification policy with the reviewer approval sealed by Hide or
UpdateEscrow. The final verdict is `PASS`, `FAIL`, or `INCOMPLETE`. Hidden
retest, mutation, and expected-consumption safeguards are enabled by default;
if a reviewer chooses to disable one, evidence records `DISABLED` instead of
pretending it passed. Contract integrity itself cannot be disabled.

If contract integrity is not `PASS`, ToppleCat records the other four gates as
`INCOMPLETE`, re-hides reviewer source, and removes any stale public Spec bundle.
An authorized reviewer must use Restore → Check → Review → UpdateEscrow to
approve an intentional public-contract or policy change.

`toppleCatVerify` and `toppleCatReport` fail the Gradle build when the aggregate
verdict is `FAIL` or `INCOMPLETE`, after evidence, reports, safe feedback, and
the run archive are complete. A green final gate therefore means aggregate
`PASS`. Read `evidence.json` for gate-level detail after either outcome.

The default PIT producer derives `targetTests` from the compiler-emitted
descriptors for every approved public canonical `@ToppleTest` declaring class. This keeps
mutation coverage correct even when production and test packages differ. If a
consumer explicitly sets PIT `targetTests`, ToppleCat preserves that choice;
when it excludes a canonical test, the usable PIT report makes `MUTATION=FAIL`.
If PIT produces no usable report, the gate is `INCOMPLETE` and cannot be treated
as evidence of a passing contract.

## Keep reviewer data private

Reviewer custody lives under `~/.topplecat/projects/<sha256-project-key>/escrow/`
and includes the manifest, hidden source blobs, approval epoch, revisions,
history, audit, lock, and recovery state. It is plaintext mechanical storage,
not encryption or a sandbox. `./gradlew clean` removes generated `build/` output
but does not remove reviewer state. A legacy project-local `.topplecat/escrow/`
is accepted only for explicit `toppleCatMigrateEscrow`; successful migration
removes that local escrow. A moved or cloned project never adopts another
project's state or silently creates a new approval.

ToppleCat does not control OS permissions, sandboxing, CI identity, or whether a
Gradle/JVM process can access arbitrary files. The external workflow must run
Verify in a trusted reviewer/CI boundary, hand the agent only public source and
safe feedback, and exclude reviewer state, hidden source, build artifacts, and
any Git history that ever contained reviewer material. Home-directory custody
alone cannot defend against a malicious build script or production code running
as the same OS user.

## Choose a sample

| Sample | Start here when... |
| --- | --- |
| [JUnit cart orders](samples/junit-cart-orders) | You use ordinary JUnit and service/domain DTOs. |
| [Spring Boot cart orders](samples/spring-boot-cart-orders) | You want to run ToppleCat in a Spring Boot test project. |

See the [samples guide](samples/README.md) for the differences and complete demo
commands.

## Documentation

- [Getting started](docs/guide/getting-started.md)
- [FAQ: why no Cucumber or `.feature` files?](docs/faq.md)
- [0.0.4 release notes](docs/releases/0.0.4.md)
- [Authoring contracts](docs/guide/authoring.md)
- [Verification and evidence](docs/guide/verification-and-evidence.md)
- [Troubleshooting](docs/guide/troubleshooting.md)
- [Architecture](docs/architecture.md)
- [External validation records](docs/validation/README.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

The repository also ships a
[`topplecat-verification`](.agents/skills/topplecat-verification/SKILL.md) agent
skill for authoring contracts, preserving reviewer custody, and verifying done
claims.

## Project status

ToppleCat is pre-1.0 and its API may still change. Development currently
requires Java 25 and the repository's Gradle 9.1.0 wrapper.
