# Getting started

ToppleCat helps a team prepare Java/JUnit acceptance checks before an
implementation handoff, then verifies a delivery after the implementation agent
says it is done. During development, continue to use ordinary `./gradlew test`.

This guide is also available as the [official English technical documentation](https://topplecat.samzhu.dev/docs/getting-started/)
and [Traditional Chinese documentation](https://topplecat.samzhu.dev/docs/zh-TW/getting-started/).

## Install ToppleCat

ToppleCat 0.2.2 is the current tagged release line, but it has not yet been
published to Maven Central. The selected-Spec Review behavior described here is
not present in released 0.2.0. Use the repository's local Maven artifact for
0.2.2 until a maintainer publishes it. The released 0.2.0 artifacts remain available from
[Maven Central](https://central.sonatype.com/namespace/io.github.samzhu.topplecat),
but do not provide the selected-Spec behavior below.
Using 0.2.2 therefore requires cloning this repository and running
`./gradlew publishToMavenLocal` first.

In `settings.gradle.kts`, make the plugin marker and libraries available from
Maven Central:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

Then configure `build.gradle.kts`:

```kotlin
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.2.2"
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.2.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

ToppleCat runtime and Gradle/plugin execution require JDK 21 or 25. The
published artifacts target Java 21 and the maintainer release build uses JDK
25. Your consumer project may still compile its own source with a Java 17,
21, or 25 target when the execution JDK is 21 or 25. A JDK 17-only Gradle
environment cannot run ToppleCat. `toppleCatInit` can add a non-destructive
starter to an empty consumer project.

These are separate choices:

| Concern | Supported policy |
| --- | --- |
| ToppleCat maintainer/release build JDK | JDK 25 primary; JDK 21 is the minimum tested build line |
| ToppleCat library, plugin, report, and formal runtime | JDK 21 and JDK 25 |
| Consumer project source target | Java 17, 21, or 25 when executed by JDK 21 or 25 |

The initial support promise does not cover a JDK 21 Gradle daemon compiling
consumer contract source with a separate JDK 25 toolchain, or the reverse. The
custom contract compiler uses the daemon's system compiler until a separate
tested compiler seam expands that promise.

Support-floor changes are announced before removal: a newer LTS first enters
CI, the oldest supported runtime is marked deprecated, and removal waits for a
documented migration window and release boundary. A dependency upgrade that
raises the Java floor follows the same compatibility-change rule.

## Prepare acceptance work with an AI

ToppleCat works with any SDD workflow. That workflow continues to own the Spec,
work planning, and delivery decision. ToppleCat turns the human-selected
Acceptance Conditions into Java/JUnit checks, then independently verifies the
implementation agent's done claim.

This guide uses [Matt Pocock's skills](https://github.com/mattpocock/skills/tree/main/skills)
as an example. For Codex and other agents, first install those skills in the
consumer project:

```text
npx skills@latest add mattpocock/skills
```

Choose `setup-matt-pocock-skills`, `to-spec`, `to-tickets`, and `implement`.
Run `$setup-matt-pocock-skills` once per repository to configure its issue
tracker and domain-document locations. `implement` closes with `code-review`;
it is not an extra ToppleCat gate.

Then install the repository-local acceptance skill:

```text
npx skills@latest add samzhu/topplecat --skill topplecat-acceptance
```

The [skill source](https://github.com/samzhu/topplecat/tree/main/.agents/skills/topplecat-acceptance)
explains how it turns human-selected Acceptance Conditions (ACs) into Java/JUnit
acceptance methods, public case rows, and separately prepared reviewer examples.
It does not select a Spec, manage tickets, infer missing business rules, or
accept a delivery.

In one Codex conversation, use `$to-spec + $topplecat-acceptance` after the
rules have been discussed. `$to-spec` records the agreed rules in a Spec;
`$topplecat-acceptance` binds each selected AC to Java/JUnit acceptance work.
If a rule is ambiguous, return it to the human or the Spec owner before
creating the acceptance binding.

## Create the public acceptance contract

Put public Java acceptance methods under `src/test/java` and typed JSON/YAML
rows under `src/test/resources/topplecat/cases/`.

```java
@ToppleAcceptanceTest("AC-ORDER-CREATE")
@DisplayName("Create an accepted order")
void createsOrder(ToppleCase c, ToppleScenario scenario, OrderStage order) {
    scenario.given(order).an_order(c.input("order", Order.class));
    scenario.when(order).submits_it();
    scenario.then(order).response_matches(c);
}
```

The method orchestrates one compiler-described Scenario. Give each public Step
method a business-readable `@As` sentence. `OrderStage` is a
non-final concrete `ToppleStage` with an accessible no-argument constructor;
ToppleCat provides one fresh proxy for each row. Add Properties under `src/test`
when an invariant deserves generated coverage; they use `@ToppleProperty`, a
JUnit `@DisplayName` naming the generated situation and invariant, and
`PropertyTrials`.

## Add reviewer-owned material

Put hidden typed rows beneath `src/hiddenTest`. A hidden row reuses the existing
public acceptance method in `HIDDEN_ONLY` mode. Property declarations stay
public under `src/test`; ToppleCat does not discover or execute Properties from
reviewer custody. Ordinary reviewer Java tests and helper source are outside
ToppleCat evidence.

## Read, plan, and seal before implementation

Use `$to-tickets` only when a prepared Spec needs several independent work
items; small work can proceed directly from the Spec.

After the Spec and Java acceptance code are ready, create the private Spec
Review:

```bash
./gradlew toppleCatReview --spec specs/023-checkout/spec.md
```

Review runs its required Check. It places the complete selected Spec beside the
Java-derived Given/When/Then Scenario, public rows, and reviewer material. It
has no execution verdict: the Reviewer uses it to confirm what the Java code
will check. Run `toppleCatCheck` directly only when fast feedback on bindings or
case data is useful without opening the Review page.

The `--spec` path must be a repository-relative Markdown file. Each selected AC
uses an exact standalone marker such as
`<!-- topplecat:acceptance:AC-CHECKOUT-001 -->`; the marker is the machine
identity and insertion point. Headings are document prose and may use any level
or wording. Ordinary references do not select scope, and ToppleCat does not read
or translate `.feature` files.

After that reading, seal the complete contract before handing work to the
implementation agent:

```bash
./gradlew toppleCatSeal
```

Seal moves reviewer source to local custody and records the complete acceptance
content and verification policy. Later Verify detects an unintended contract or
policy change. Seal is not encryption, process isolation, human approval, or a
delivery verdict.

The implementation agent receives only public material. Use `$implement` with
the Spec or its tickets; it uses ordinary `./gradlew test` during development
and closes with `code-review`. Do not give it the private Spec Review or
reviewer-owned source.

Reviewer HTML defaults to English. Add `--language zh-TW` to Review or Verify
when the Reviewer wants Traditional Chinese ToppleCat-owned presentation. This
invocation-only choice preserves authored display prose and machine values
exactly as recorded.

## Make the delivery earn a PASS

```bash
./gradlew toppleCatVerify
```

Run this after the implementation agent's done claim. It is the normal
full-contract verification command for CI. When a Reviewer
wants fast evidence for just-finished work, Verify can instead take either
selected Spec files or repeated AC IDs, never both:

```bash
./gradlew toppleCatVerify --spec specs/023-checkout/spec.md
./gradlew toppleCatVerify --ac AC-CHECKOUT-THRESHOLD --ac AC-CHECKOUT-VIP
```

The scoped report identifies its selected ACs and how they were selected. Its
`PASS` does not make a claim about ACs outside that scope.

The formal run creates fresh public acceptance evidence and separately records
Hidden Tests, Mutation Testing, Property-Based Testing, and expected-value
consumption. Hidden Tests and Properties remain independent when Public
Acceptance fails. Mutation Testing needs a passing Public Acceptance baseline;
otherwise its result is `INCOMPLETE`, not a conclusion about changed production
behavior. Reports and safe feedback appear before the one aggregate Gradle
failure. Public Acceptance, Properties, and Mutation Testing follow the
selected ACs; an attributed mutation that its owning public Acceptance Method
fails to detect fails that AC. Use `--all-hidden-tests` only when a reviewer
deliberately expands hidden rows beyond the selected ACs.

ToppleCat records `PASS` only when every required Gate passes in this run. The
Reviewer reads the evidence behind that verdict and still decides whether to
accept the delivery.

Read `build/topplecat/evidence.json` for the machine verdict. Spec Review at
`build/topplecat/reports/review/index.html` and Verification Report at
`build/topplecat/reports/verification/index.html` are both reviewer-only.
There is no public HTML report; `agent-feedback.json` is the safe generated
result for the implementation agent.
