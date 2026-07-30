# Spring Boot sample walkthrough

This demo follows the JUnit cart-orders flow with a Spring Boot test context.
The public acceptance class is bootstrapped by `@SpringBootTest`, while typed
rows, hidden rows, and Properties keep the same ToppleCat boundaries.

## Run it

```bash
bash samples/spring-boot-cart-orders/demo.sh
```

The script uses an isolated reviewer state, seals reviewer material, expects
the deliberately broken service to fail Verify through hidden typed rows, then
applies the fixed service and verifies again.

## What to inspect

Public acceptance and PBT run separately from reviewer-owned hidden rows.
Inspect `evidence.json`, safe feedback, the public report at
`reports/public/index.html`, and the reviewer-only Verification Evidence report
under `reports/verification/index.html`.
