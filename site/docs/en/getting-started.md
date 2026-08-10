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

## Add ToppleCat and its authoring skill {#ai-assisted-authoring}

To use ToppleCat with an implementation agent, install two things:

- The Gradle plugin runs ToppleCat inside your Java/JUnit project.
- The `topplecat-acceptance` skill teaches an AI how to turn rules you have
  chosen into Java/JUnit acceptance code and case data.

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

Then install the authoring skill in the project where the AI will work:

```text
npx skills@latest add samzhu/topplecat --skill topplecat-acceptance
```

Read the [skill source](https://github.com/samzhu/topplecat/tree/main/.agents/skills/topplecat-acceptance)
before granting an agent its normal project permissions. The skill does not
invent a missing business rule. It helps the AI ask about unclear behaviour,
then binds the rule you chose to Java/JUnit work that ToppleCat can run.

## Prepare one delivery with an AI {#prepare-with-an-ai}

The checkout sample has one simple rule: a payable cart using `SAVE100` gets
100 off. The public example names one qualifying cart and the receipt it should
receive. A Reviewer can choose another cart from that same published rule to
check that the code did not merely recognize the visible example. That extra
case is not a secret requirement, and the implementation agent does not see
it.

### In the Codex conversation

Use these two skills in the same conversation:

```text
$to-spec + $topplecat-acceptance
```

`$to-spec` writes the rules already agreed in the conversation into a Spec.
`$topplecat-acceptance` turns every Acceptance Condition (AC) in that Spec into
one Java/JUnit acceptance method and its examples. It prepares a public handoff
for the implementation agent and a separate Reviewer handoff for the additional
checks.

If the rule could mean two different things, stop there and answer the
question. For example, say whether a coupon applies to a cart containing an
excluded item. The skill must not choose that behaviour for you.

You now have a written rule, public Java acceptance code, public case data, and
reviewer-owned examples derived from the same rule. The next step is to read
what the Java code will actually check.

## Use Matt Pocock skills with ToppleCat {#matt-pocock-workflow}

[Matt Pocock's skills](https://github.com/mattpocock/skills/tree/main/skills)
can keep the wider work moving: they record the Spec, split larger work into
tickets, and implement the approved work. ToppleCat joins that workflow to
prepare and protect the acceptance checks. It does not replace the workflow or
decide what the product should do.

If this is the first time a repository uses Matt Pocock's engineering skills,
install the skills and run `$setup-matt-pocock-skills` once. That records where
the repository keeps its work and domain notes. After that setup, use the
following path for a delivery.

### In the terminal: read the prepared checks

Run this after the Spec and Java acceptance code are prepared:

```bash
./gradlew toppleCatReview --spec specs/checkout/spec.md
```

`toppleCatReview` is a Gradle command, not an agent skill. It writes a private
Spec Review page. Read the selected Spec beside the Given/When/Then presentation
compiled from the Java acceptance method and its case data. This page has no
test result. It is where the Reviewer checks that the Java code says what the
rule says.

Review runs the Check it needs. You do not need to put `toppleCatCheck` in the
main path. If you only want quick feedback on acceptance bindings and case
data, you can run `./gradlew toppleCatCheck --spec specs/checkout/spec.md`
directly.

### In the agent conversation: split larger work when needed

Use `$to-tickets` when the approved Spec is large enough to need several
independent pieces of work. It creates small, end-to-end tickets and records
which ones must finish first. For a small change, skip this step and work from
the Spec directly.

### In the terminal: protect the checks before implementation

After reading the Spec Review, run:

```bash
./gradlew toppleCatSeal
```

Seal moves Reviewer-only source into local custody and records the complete
acceptance content and verification settings. The implementation agent now gets
the public project only. When formal verification runs later, ToppleCat can tell
whether those prepared checks or their policy changed after Seal.

Seal is an integrity record. It is not encryption, process isolation, or a
human decision to accept the finished delivery.

### In the agent conversation: implement the approved work

Use `$implement` with the Spec or its tickets. The implementation agent works
against the public acceptance code and ordinary `./gradlew test` feedback. Do
not give it the private Spec Review, reviewer examples, or reviewer-owned
source.

When the agent says the work is done, move back to the terminal for formal
verification.

## Verify the delivered work {#formal-verify}

After the implementation agent says the work is done, run:

```bash
./gradlew toppleCatVerify
```

`toppleCatVerify` reruns the public contract, performs every enabled independent
check, and writes a private Verification Report for the Reviewer. The
machine-readable conclusion is `build/topplecat/evidence.json`. This is a
Gradle command, not an agent skill.

For a quick report on one delivery, a Reviewer can select Spec files or AC IDs.
The report labels that limited scope. CI should run `toppleCatVerify` without
either selection so the complete contract is checked.

## Decide with the evidence {#human-decision}

`PASS` means every check required by the sealed policy passed in this run.
`FAIL` means a completed check found a blocking problem. `INCOMPLETE` means
ToppleCat could not obtain enough trustworthy evidence. A scoped `PASS` covers
only the Spec or ACs named for that run.

None of those results decides whether the original business rules were
complete. A human reads what ran and what happened, then decides whether to
accept the delivery.

Next, read [Turn rules into checks](authoring-contracts.md#contract-example) if
you are preparing a project, or [How ToppleCat verifies a delivery](verification-and-evidence.md#delivery-example)
if you need to interpret the report.
