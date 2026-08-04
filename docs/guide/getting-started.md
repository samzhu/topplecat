# Getting started

ToppleCat formalizes a Java/JUnit acceptance contract after an implementation
handoff. During development, continue to use ordinary `./gradlew test`.

## Install

```kotlin
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.23"
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.0.23")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

Java 25 and a compatible Gradle version are required. `toppleCatInit` can add a
non-destructive starter to an empty consumer project.

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

Run the reviewer sequence:

```bash
./gradlew toppleCatCheck --spec specs/023-checkout/spec.md
./gradlew toppleCatReview --spec specs/023-checkout/spec.md
./gradlew toppleCatSeal --spec specs/023-checkout/spec.md
```

Reviewer HTML defaults to English. Add `--language zh-TW` to Review, Seal,
Reseal, or Verify when the Reviewer wants Traditional Chinese ToppleCat-owned
presentation. This invocation-only choice preserves authored display prose and
machine values exactly as recorded.

Seal moves the reviewer source to local custody. The implementation agent then
receives only public material and works with `./gradlew test`.

## Make the delivery earn a PASS

```bash
./gradlew toppleCatVerify --spec specs/023-checkout/spec.md
```

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
