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
The receipt is projected and verified once with `c.verify("receipt", receipt)`
so one complete receipt is one Expected Consumption obligation. When a contract
must verify independent top-level values, use JUnit `assertAll` so each
`verify` gets an attempt.
Inspect `evidence.json`, safe feedback, the reviewer-only Spec Review under
`reports/review/index.html`, and the reviewer-only Verification Report under
`reports/verification/index.html`.
