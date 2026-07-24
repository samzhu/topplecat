# Spring Boot Cart Orders

This consumer shows ToppleCat running in a Spring Boot test project.
`SpringCouponAcceptanceTest` loads the application context with
`@SpringBootTest`; the reviewer-only boundary test demonstrates an injected
`OrderService`.

Both canonical acceptance conditions use the required `ToppleStage` DSL, so the
reports tell the reviewer how the cart was prepared, when the order was created,
and where the contract was checked. Production calls and assertions stay in the
Stage methods; the canonical methods only arrange business-readable steps.

The checked-in service has a deliberate public-case coincidence. A reviewer
retest exposes it during `toppleCatVerify`.

Hidden retest and expected consumption stay enabled. Mutation is explicitly
disabled to keep the demonstration fast, so evidence honestly records
`MUTATION: DISABLED`.

```bash
bash samples/spring-boot-cart-orders/demo.sh
```

The demo runs the failure-to-pass cycle, prints the evidence and safe feedback
paths, and restores the original source and reviewer source set on all exits.
Read [TUTORIAL.md](TUTORIAL.md) for the Spring-context and narrative details.
