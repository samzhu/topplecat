# Getting started

ToppleCat formalizes a Java/JUnit acceptance contract after an implementation
handoff. During development, continue to use ordinary `./gradlew test`.

## Install

```kotlin
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.8"
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.0.8")
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
void createsOrder(ToppleCase c, ToppleScenario scenario, OrderStage order) {
    scenario.given(order).an_order(c.input("order", Order.class));
    scenario.when(order).submits_it();
    scenario.then(order).response_matches(c);
}
```

The method orchestrates one compiler-described Scenario. `OrderStage` is a
non-final concrete `ToppleStage` with an accessible no-argument constructor;
ToppleCat provides one fresh proxy for each row. Add Properties under `src/test`
when an invariant deserves generated coverage; they use `@ToppleProperty` and
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

Seal moves the reviewer source to local custody. The implementation agent then
receives only public material and works with `./gradlew test`.

## Verify a completion claim

```bash
./gradlew toppleCatVerify --spec specs/023-checkout/spec.md
```

The formal run creates fresh public acceptance evidence and separately records
Hidden Tests, Mutation Testing, Property-Based Testing, and expected-value
consumption. Once contract integrity passes, each enabled safeguard runs even
if an earlier one fails; reports and safe feedback appear before the one
aggregate Gradle failure. Public Properties follow the selected ACs. Use
`--all-hidden-tests` only when a reviewer deliberately expands hidden rows
beyond the selected ACs.

Read `build/topplecat/evidence.json` for the machine verdict. The public report
is at `build/topplecat/reports/public/index.html`; the review and verification
reports are reviewer-only.
