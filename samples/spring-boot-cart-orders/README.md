# Spring Boot cart orders

This consumer shows ToppleCat running in a Spring Boot test project.
`SpringCouponAcceptanceTest` loads the application context with
`@SpringBootTest`; the reviewer-only boundary test uses an injected
`OrderService`.

> **Why is `src/hiddenTest` in this repository?** It is demo data so a fresh
> clone can reproduce the reviewer workflow. In a normal implementation
> handoff, the directory is absent. A reviewer uses it only while creating or
> updating checks, then runs `toppleCatHide` before the project goes to a
> developer or AI agent.

Both canonical acceptance conditions use the required `ToppleStage` DSL, so the
reports tell the reviewer how the cart was prepared, when the order was created,
and where the contract was checked. Production calls and assertions stay in the
Stage methods; the canonical methods only arrange business-readable steps.

The checked-in service has a bug that happens to satisfy the public case. A
reviewer retest exposes it during `toppleCatVerify`.

Hidden retest and expected consumption stay enabled. Mutation is explicitly
disabled to keep the demonstration fast, so evidence records
`MUTATION: DISABLED`.

```bash
bash samples/spring-boot-cart-orders/demo.sh
```

For an interactive trial from this directory, use the local `./gradlew`
launcher. It resolves the published ToppleCat release:

```bash
./gradlew toppleCatCheck
./gradlew toppleCatReview
```

The demo runs the failure-to-pass cycle, prints the evidence and safe feedback
paths, and restores the original source and reviewer source set on all exits.
Read [TUTORIAL.md](TUTORIAL.md) for the Spring-context and narrative details.
