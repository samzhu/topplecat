# JUnit Cart Orders Tutorial

This tutorial follows one acceptance condition from authoring check to final
evidence. The checked-in service is intentionally wrong; leave it that way when
you finish so the next reader sees the same failure.

## 1. Check and Review the Contract

From the repository root, publish the local source snapshot and validate the
sample:

```bash
./gradlew publishToMavenLocal
./gradlew -p samples/junit-cart-orders toppleCatCheck
./gradlew -p samples/junit-cart-orders toppleCatReview
```

This is deliberately a source-checkout workflow. A normal consumer installs
the released artifact from Maven Central using the root
[README](../../README.md#install-003).

After a successful check, `toppleCatReview` writes
`samples/junit-cart-orders/build/topplecat/reports/review/index.html`. Open it through
its `file://` path as the authorized reviewer. It presents static Stage domain
sentences, all case rows, then collapsed canonical source, but intentionally
shows no execution status or runtime record. It includes reviewer-only data, so
never share it with an implementation agent. The executable contract remains
the Java test and typed case rows.

## 2. Hide Reviewer Source and Use the Public Loop

```bash
./gradlew -p samples/junit-cart-orders toppleCatHide
```

The hide task moves `src/hiddenTest` into local hidden storage. The normal implementation command sees
only public cases:

```bash
./gradlew -p samples/junit-cart-orders test
```

The local hidden-storage directory is plaintext reviewer state. `./gradlew clean`
does not delete it, and Git history can retain reviewer files after they leave
the working tree. Never commit reviewer source to history the implementation
agent can read. Deliver a public export without `.git`, `.topplecat/`, or
`build/`, or use an isolated environment whose history never contained it.

An authorized reviewer can later inspect or edit the stored source with:

```bash
./gradlew -p samples/junit-cart-orders toppleCatRestore
```

This is not part of the implementation loop; it restores only a source set that
was previously hidden.

## 3. Observe the Failed Verification

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

## 4. Apply the Demonstration Fix

Copy the supplied correct implementation, then rerun verification:

```bash
cp samples/junit-cart-orders/demo/OrderService.fixed.java samples/junit-cart-orders/src/main/java/sample/cartorders/OrderService.java
./gradlew -p samples/junit-cart-orders toppleCatVerify
```

The second command exits zero and records a `PASS` verdict in
`samples/junit-cart-orders/build/topplecat/evidence.json`.

## 5. Inspect Reports and Restore the Deliberate Defect

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
