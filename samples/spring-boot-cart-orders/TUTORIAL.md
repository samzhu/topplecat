# Spring Boot cart orders tutorial

This sample uses the same acceptance-case model as the plain JUnit sample, but
the canonical test is bootstrapped with `@SpringBootTest`. ToppleCat injects a
typed `Cart` case into the Stage scenario. The reviewer-only JUnit test
separately uses an injected Spring-managed `OrderService`; neither test
turns the controller into its subject.

## Run the demonstration

From the repository root, run:

```bash
bash samples/spring-boot-cart-orders/demo.sh
```

The script publishes the local snapshot, hides the reviewer source, and shows
the expected verification failure. It then applies the demonstration fix,
verifies the passing result, and restores the original source. Mutation is
disabled to keep the walkthrough fast, so the evidence records
`MUTATION: DISABLED` rather than a mutation pass:

```text
evidence: .../build/topplecat/evidence.json
agent feedback: .../build/topplecat/agent-feedback.json
```

## Review before hiding reviewer source

Before using the demo's hide step, an authorized reviewer can inspect the
complete static contract without executing the Spring tests:

```bash
./gradlew publishToMavenLocal
./gradlew -p samples/spring-boot-cart-orders toppleCatCheck
./gradlew -p samples/spring-boot-cart-orders toppleCatReview
```

These commands publish the source checkout so the sample tests the code in this
repository. Once `0.0.4` is available in Maven Central, regular consumers should
install it using the root [README](../../README.md#install-004).

Open `samples/spring-boot-cart-orders/build/topplecat/reports/review/index.html` through
its `file://` path. It shows direct Given/When/Then Stage sentences, public plus
reviewer rows, then collapsed canonical source. It contains hidden data and must
never be given to an implementation agent.

Then move the reviewer source into local hidden storage before implementation
work:

```bash
./gradlew -p samples/spring-boot-cart-orders toppleCatHide
```

Reviewer source is stored as plaintext mechanical custody at
`~/.topplecat/projects/<sha256-project-key>/escrow/`, not encryption or a
sandbox. `./gradlew clean` does not remove it, and removing a working file does
not erase it from Git history. Never commit reviewer source to history the
implementation agent can read. Give the agent a public export without `.git`,
`.topplecat/`, reviewer-local state, or `build/`, or use an isolated public-only
environment. Use `toppleCatRestore` only when an authorized reviewer needs to
inspect or edit the hidden source again.

## Read the narrative

The coupon test declares `@ToppleStageField` fields for Given, When, and Then
helpers. Each helper records a report sentence before performing work, and
`@ProvidedState` passes the cart and receipt between stages. The final Then
stage calls `c.verify(...)`, so a mismatch is attributed to the contract-check
sentence in the reviewer report.

After the demo's verify step, open
`samples/spring-boot-cart-orders/build/topplecat/reports/spec/index.html` and
`samples/spring-boot-cart-orders/build/topplecat/reports/verification/index.html` in a
browser using their `file://` paths. The public page shows only public contract
data. The reviewer page has the full verification detail.
