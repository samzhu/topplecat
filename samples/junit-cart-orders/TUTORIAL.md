# JUnit sample walkthrough

The demo replaces the order service with a deliberately narrow implementation.
It still satisfies the visible rows but fails independently selected hidden
coupon rows. The script then installs the fixed service and verifies again.

## Run it

```bash
bash samples/junit-cart-orders/demo.sh
```

The demo uses an isolated `topplecat.stateRoot`, publishes this checkout to the
local Maven cache, runs Check and Seal, expects a failed Verify, then runs a
passing Verify after the fix. Cleanup restores the service and reviewer source.

## What to inspect

The public acceptance class uses `@ToppleAcceptanceTest` and typed rows. Its
Property runs separately and contributes only to `PROPERTY`. Reviewer rows are
the only evidence for `REVIEWER_JUNIT`. The receipt is projected and verified
once with `c.verify("receipt", receipt)`, which keeps Expected Consumption a
single complete assertion obligation.

After Verify, inspect:

```text
build/topplecat/evidence.json
build/topplecat/agent-feedback.json
build/topplecat/reports/review/index.html
build/topplecat/reports/verification/index.html
```

Spec Review is reviewer-only and shows the executable contract before handoff.
This sample supplies no `--spec` path, so it demonstrates the all-bound-AC
scope without external Markdown context. When an External Workflow supplies
`--spec`, Spec Review also shows every complete selected document. The failing
run must not leak reviewer values into safe feedback. Verification Report is
reviewer-only and shows the detailed result.
