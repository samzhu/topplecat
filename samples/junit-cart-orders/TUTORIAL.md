# JUnit cart orders tutorial

This tutorial follows one acceptance condition from authoring check to final
evidence. The checked-in service is wrong on purpose. Leave it that way when
you finish, so the next run starts from the same failure.

## 1. Check and review the contract

For a normal trial of the published release, run the sample-local launcher:

```bash
cd samples/junit-cart-orders
./gradlew toppleCatCheck
./gradlew toppleCatReview
```

The walkthrough demo intentionally tests the current checkout instead. It
publishes to Maven Local and opts in with
`-Ptopplecat.useMavenLocal=true`; that opt-in is not needed for normal use of
the published artifact.

After a successful check, `toppleCatReview` writes
`samples/junit-cart-orders/build/topplecat/reports/review/index.html`. Open it through
its `file://` path as the authorized reviewer. It shows the Stage sentences,
the exact canonical `@ToppleTest` method for each AC, then public examples
followed by reviewer-only checks. It has no execution status because the tests
have not run. The page contains reviewer data, so do not share it with an
implementation agent. The Java test and typed case rows remain the executable
contract.

## 2. Hide reviewer source and use the public loop

```bash
./gradlew -p samples/junit-cart-orders toppleCatHide
```

The hide task moves `src/hiddenTest` into reviewer-local custody at
`~/.topplecat/projects/<sha256-project-key>/escrow/`. The normal implementation command sees only public cases:

```bash
./gradlew -p samples/junit-cart-orders test
```

Reviewer-local custody is plaintext mechanical state, not encryption or a
sandbox. `./gradlew clean` does not delete it, and Git history can retain
reviewer files after they leave the working tree. Never commit reviewer source
to history the implementation agent can read. Deliver a public export without
`.git`, `.topplecat/`, reviewer-local state, or `build/`, or use an isolated
public-only environment.

An authorized reviewer can later inspect or edit the stored source with:

```bash
./gradlew -p samples/junit-cart-orders toppleCatRestore
```

This is not part of the implementation loop; it restores only a source set that
was previously hidden.

## 3. Run the failing verification

Run the reviewer gate. This command is expected to exit non-zero while the
checked-in implementation is present:

```bash
./gradlew -p samples/junit-cart-orders toppleCatVerify
```

The reviewer report identifies the failed contract execution. The implementation
agent should receive only
`samples/junit-cart-orders/build/topplecat/agent-feedback.json`. It reports the
aggregate verdict and gate-level status while omitting reviewer case
identifiers, input values, expected values, test names, and raw assertion text.

## 4. Apply the demonstration fix

Copy the supplied correct implementation, then rerun verification:

```bash
cp samples/junit-cart-orders/demo/OrderService.fixed.java samples/junit-cart-orders/src/main/java/sample/cartorders/OrderService.java
./gradlew -p samples/junit-cart-orders toppleCatVerify
```

The second command exits zero and records a `PASS` verdict in
`samples/junit-cart-orders/build/topplecat/evidence.json`.

## 5. Inspect reports and restore the sample bug

```bash
cp samples/junit-cart-orders/demo/OrderService.broken.java samples/junit-cart-orders/src/main/java/sample/cartorders/OrderService.java
```

The public Spec bundle is the post-verify public contract view. The Verification
bundle is reviewer-only. Open
`samples/junit-cart-orders/build/topplecat/reports/spec/index.html` and
`samples/junit-cart-orders/build/topplecat/reports/verification/index.html`
through their `file://` paths. The command restores the checked-in demonstration
state.

For a repeatable one-command version of the complete cycle, run
`bash samples/junit-cart-orders/demo.sh`.
