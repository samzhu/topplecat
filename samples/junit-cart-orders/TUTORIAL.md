# How the cart-orders lessons work

`junit-cart-orders` is one self-contained teaching project. Start with its
README, then choose one lesson with `./demo.sh <name>`. The checked-in service
contains a synthetic 20% shortcut that Hidden Tests reject; other lessons use a
temporary copy for their specific synthetic delivery.

Every lesson follows the same shape:

1. Prepare a sealed synthetic contract.
2. Run the selected synthetic delivery through formal Verify.
3. Confirm the intended Gate outcome.
4. Open `build/topplecat/demo-reports/<lesson>/index.html` to read its local
   Verification Report.
5. Remove the temporary project and Reviewer Custody.

The public [CouponAcceptanceTest](src/test/java/sample/cartorders/CouponAcceptanceTest.java)
is the SDK reference. An Acceptance Method binds an AC to `ToppleCase`, writes
a Given/When/Then `ToppleScenario`, and delegates business vocabulary to a
reusable `ToppleStage`. Its final `verify()` call compares the complete expected
receipt. The report is the learning surface: it shows the failure-first result
and the corresponding Gate for that synthetic delivery.

## What each lesson proves

- **Public Acceptance** changes the service so visible Typed Case Rows fail.
- **Hidden Tests** uses a shortcut that fits visible rows but fails independent
  synthetic rows for the same public rule.
- **Property-Based Testing** keeps the example rows compatible but violates the
  fixed-discount invariant for generated inputs.
- **Mutation Testing** deliberately makes the Acceptance Method consume the
  expected receipt without observing the service receipt. Managed PIT then
  finds attributed production mutations that survive.
- **Contract Integrity** changes a public expected value after the Mechanical
  Seal. Verify refuses to trust the changed contract.

These are synthetic lessons, not a claim about a real delivery. A passing Gate
is evidence for the sealed contract in that run; it does not prove the business
rules are complete or make a human delivery decision.
