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

Whenever an AI coding agent says a Java task is **done**, ToppleCat gives the
claim a small push. It reruns the contract with reviewer-only cases, checks
whether the tests notice broken production behavior, and proves that every
declared result was actually asserted.

If the implementation only memorized the public example, or the test merely
looked busy without proving behavior, the claim tips over. If it stays standing,
ToppleCat leaves evidence.

> Hidden retests, mutation gates, and executable acceptance contracts for Java.
> If it's hollow, it falls.

ToppleCat is a delegation verification gate for Java and JUnit. Ordinary Java
acceptance tests plus typed JSON or YAML case rows are the executable contract.
Generated JSON and HTML are evidence, never a second source of truth.

Canonical scenarios are ordinary Java written to read like business language.
[JGiven](https://github.com/TNG/JGiven) is important prior art for readable,
staged Java tests. ToppleCat focuses on a different boundary: independently
checking a delegated done claim with hidden retests, mutation evidence, and safe
feedback. It does not use Cucumber or Gherkin, and it does not introduce a
second authoring language beside the executable Java contract.

## What ToppleCat Catches

| A green test can still mean... | ToppleCat checks it with... |
| --- | --- |
| The implementation was tuned to the visible example. | Reviewer-controlled **hidden retests**. |
| The test runs but cannot detect broken behavior. | A PIT-backed **mutation gate**. |
| Expected data was read but never compared with reality. | Enforced **expected consumption**. |
| Old or partial output is mistaken for current proof. | Run-scoped gates, digests, and an explicit **evidence verdict**. |

Hidden retest and mutation answer different questions. Hidden retest asks whether
an implementation generalizes beyond visible examples. The default PIT producer
measures **public executable contract mutation strength**: it uses
`sourceSets.test`, public test classes, and public case rows only. Reviewer rows
and reviewer-only JUnit tests never help that producer kill a mutant. If a
boundary must kill a mutant, it belongs in the public contract; ToppleCat does
not add per-case mutation scores or infer the scope of a custom producer.

## Watch a Fake Completion Fall

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

## How It Fits the Development Flow

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
   local plaintext custody storage. It is not a secrecy boundary. To evolve an
   existing reviewer suite, an authorized reviewer uses the explicit restore,
   review, and update workflow rather than ordinary hide.
4. **Implement normally.** Give the implementation agent a public-only
   environment and use ordinary `./gradlew test`.
5. **Verify the claim.** `toppleCatVerify` restores reviewer source for the run,
   executes all enabled gates, writes evidence, and hides the source again.

## Install 0.0.2

ToppleCat `0.0.2` is the current Maven Central release. A consumer project needs
Java 25 and a Gradle version that supports it. Add Maven Central for both plugin
and library resolution; a released consumer does not need `mavenLocal()`.

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
    id("io.github.samzhu.topplecat") version "0.0.2"
}

dependencies {
    testImplementation(
        "io.github.samzhu.topplecat:topplecat-junit:0.0.2"
    )
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

For an otherwise empty consumer project, `./gradlew toppleCatInit` creates a
non-destructive starter contract. It is an optional bootstrap, not a normal
workflow step. The checked-out repository demos intentionally use
`publishToMavenLocal` so they exercise the source checkout rather than the
released artifact; that is a contributor/demo workflow, not the installation
path above.

## Write an Executable Contract

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

## Run the Gate

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
source before atomically activating it. Ordinary `toppleCatHide` still rejects a
changed restored suite. A public implementation export has neither reviewer
source nor local escrow, so this reviewer-only task fails safely there.

## Read the Result

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

The final verdict is `PASS`, `FAIL`, or `INCOMPLETE`. Hidden retest, mutation,
and expected-consumption safeguards are enabled by default. If a reviewer
deliberately disables one, evidence records `DISABLED` instead of pretending it
passed.

## Keep Reviewer Data Private

Local `.topplecat/escrow/` is mechanical plaintext storage, not encryption.
`./gradlew clean` removes generated `build/` output but does not remove escrow.
Removing `src/hiddenTest` from a working tree also does not erase it from Git
history: a worktree created from history that contains reviewer source is not a
privacy boundary. Never commit reviewer material to history the implementation
agent can read. Hand off either a public export without `.git`, `.topplecat/`,
or `build/`; an isolated environment whose history never contained reviewer
material; or a public repository paired with a separate private reviewer
repository or CI environment.

## Choose a Sample

| Sample | Start here when... |
| --- | --- |
| [JUnit cart orders](samples/junit-cart-orders) | You use ordinary JUnit and service/domain DTOs. |
| [Spring Boot cart orders](samples/spring-boot-cart-orders) | You want to run ToppleCat in a Spring Boot test project. |

See the [samples guide](samples/README.md) for the differences and complete demo
commands.

## Documentation

- [Getting started](docs/guide/getting-started.md)
- [Authoring contracts](docs/guide/authoring.md)
- [Verification and evidence](docs/guide/verification-and-evidence.md)
- [Troubleshooting](docs/guide/troubleshooting.md)
- [Architecture](docs/architecture.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

The repository also ships a
[`topplecat-verification`](.agents/skills/topplecat-verification/SKILL.md) agent
skill for authoring contracts, preserving reviewer custody, and verifying done
claims.

## Project Status

ToppleCat is pre-1.0 and its API may still change. Development currently
requires Java 25 and the repository's Gradle 9.1.0 wrapper.
