---
title: Getting started
description: Run a real sample that rejects a bad AI delivery, then add ToppleCat to a Java/JUnit project.
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

# See ToppleCat reject a bad delivery

Start here if you want proof before configuration. The repository contains a
consumer project with a deliberately narrow checkout implementation. Its public
tests pass. ToppleCat still rejects it because an independently chosen check
finds the shortcut. The same script installs the corrected implementation and
shows the next run passing.

To run the sample you need a shell, Git, and JDK 25. To adopt ToppleCat in
another project you also need a compatible Gradle version.

## What is being checked {#contract-example}

A checkout rule says that applying `SAVE100` subtracts 100 from the order
subtotal. A developer expresses the rule once as ordinary Java/JUnit work:

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("Apply SAVE100 to an order")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
}
```

A JSON or YAML row supplies a concrete cart and the receipt that should come
back. Together, the method and row are the public executable contract. The
implementation agent can read them and run `./gradlew test` while it works.

ToppleCat later runs that same public method with independently chosen reviewer
examples. The agent never needs those examples to implement the rule.

## Run the executable sample {#sample-workflow}

Clone the repository, then run:

```bash
bash samples/junit-cart-orders/demo.sh
```

The script performs a complete consumer workflow:

1. It publishes the current ToppleCat build to the local Maven cache.
2. It seals the prepared acceptance contract and verifies the deliberately bad
   checkout service. That run is expected to be rejected.
3. It installs the corrected service and verifies again. That run must earn
   `PASS`.

The script exits unsuccessfully if either checkpoint behaves differently. Its
source, public contract, and cleanup logic live in the
[JUnit cart-orders sample](https://github.com/samzhu/topplecat/tree/main/samples/junit-cart-orders),
so this walkthrough is exercised by the repository release gate.

## Add ToppleCat to your project

ToppleCat 0.1.0 requires Java 25. Add the plugin and JUnit dependencies:

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

Put public acceptance methods under `src/test/java` and public case rows under
`src/test/resources/topplecat/cases/`. Before giving the work to an
implementation agent, the responsible human checks the selected Spec and the
prepared contract, then seals it:

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
```

At this point, the implementation agent needs only the public project. It can
use ordinary `./gradlew test` feedback. A green test helps development, but it
is not the formal delivery verdict.

You can give this page's Markdown to a coding agent and ask it to install the
plugin and create the public contract from rules you have approved. Do not ask
the agent to invent missing business rules or inspect reviewer-controlled
material.

## Verify the delivered work {#formal-verify}

After the agent says the work is done, run:

```bash
./gradlew test
./gradlew toppleCatVerify
```

`toppleCatVerify` reruns the public contract, performs every enabled independent
check, and writes a private Verification Report for the Reviewer. The
machine-readable conclusion is `build/topplecat/evidence.json`.

For a quick report on one delivery, a Reviewer can select Spec files or AC IDs.
The report labels that limited scope. CI should run `toppleCatVerify` without
either selection so the complete contract is checked.

## Decide with the evidence {#human-decision}

`PASS` means every check required by the sealed policy passed in this run.
`FAIL` means a completed check found a blocking problem. `INCOMPLETE` means
ToppleCat could not obtain enough trustworthy evidence.

None of those results decides whether the original business rules were
complete. A human reads what ran and what happened, then decides whether to
accept the delivery.

Next, read [Turn rules into checks](authoring-contracts.md#contract-example) if
you are preparing a project, or [Verify a delivery](verification-and-evidence.md#delivery-example)
if you need to interpret the report.
