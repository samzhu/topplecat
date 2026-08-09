---
title: Getting started
description: Adopt ToppleCat, author one executable acceptance contract, and run a sample-backed verification workflow.
page_id: getting-started
language_code: en
language_name: English
language_label: Current language
alternate_url: zh-TW/getting-started/
alternate_language: zh-TW
alternate_label: 繁體中文
alternate_en: getting-started/
alternate_zh_tw: zh-TW/getting-started/
markdown_url: getting-started.md
copy_label: Copy Markdown
copied_label: Copied
---

# Getting started

## The first delivery example {#contract-example}

Suppose a checkout Spec says: an order at or above 1,000 receives a 100-dollar
discount. The human chooses that rule, writes one public example, and decides
which additional safeguards are required. The implementation agent sees the
public contract, not reviewer-owned examples. After its done claim, the
Reviewer runs the same contract and reads the evidence before deciding what to
do with the delivery.

The [JUnit cart-orders sample](https://github.com/samzhu/topplecat/tree/main/samples/junit-cart-orders)
is the executable reference for this path. Run its `demo.sh` when you want to
exercise the complete consumer setup; the code and case rows are kept in the
repository so the tutorial does not promise an imaginary API.

## Install the plugin

ToppleCat 0.1.0 requires Java 25 and a compatible Gradle version. Add the
plugin and JUnit dependencies to a consumer project:

```kotlin
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.1.0"
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

## Author the public contract

Put public Acceptance Methods under `src/test/java` and typed JSON/YAML rows
under `src/test/resources/topplecat/cases/`. One literal AC ID binds one
public method. The method describes the Scenario; ordinary Stage methods hold
business calls and assertions.

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_cart(c.input("cart", Cart.class));
    scenario.when(coupon).creates_an_order();
    scenario.then(coupon).receipt_matches(c);
}
```

Keep the rule and case complete yourself. ToppleCat checks the selected
Executable Contract; it does not infer omitted business requirements.
[Authoring contracts](authoring-contracts.md#typed-case-rows) explains the
compiler-defined Scenario and expected-value rules in detail.

## Follow the sample workflow {#sample-workflow}

The repository sample runs the supported Gradle flow:

```bash
cd samples/junit-cart-orders
bash demo.sh
```

For a consumer project, the human or external workflow first selects and
reviews the Spec, then seals the complete contract:

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
```

The Implementation Agent works with ordinary `./gradlew test` feedback. That
green task is useful development feedback, but it is not the formal verdict.

## Run formal Verify {#formal-verify}

After the agent claims the checkout is done, run the full contract:

```bash
./gradlew test
./gradlew toppleCatVerify
```

Verify produces fresh Current-run Evidence, independently evaluates enabled
safeguards, writes the reviewer-only Verification Report, and re-hides reviewer
source before returning an aggregate result. A quick Reviewer report may use
either selected Spec files or repeated AC IDs, never both; its `PASS` names
only the selected scope.

Read `build/topplecat/evidence.json` for the machine verdict. The human reads
the Verification Report and decides whether the delivery is acceptable.

## What the result means {#human-decision}

`PASS` means every required Gate passed under the sealed policy in this current
run. It does not prove that the checkout Spec was complete, that the program is
correct for unstated inputs, or that an organization should approve the
delivery. See [Verification and evidence](verification-and-evidence.md#delivery-example)
for the three layers of observation, attribution, and Gate verdict.
