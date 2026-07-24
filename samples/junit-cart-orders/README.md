# JUnit Cart Orders

This plain JUnit consumer is the smallest complete ToppleCat workflow. It
creates an order from a nested `Cart` DTO and shows why a green public test is a
claim rather than proof.

| Source | Role |
| --- | --- |
| `src/test/java` | Public Java acceptance tests: canonical `@ToppleTest` Stage DSL plus any extra `@ToppleAc` JUnit coverage. |
| `src/test/resources/topplecat/cases` | Public typed JSON case rows. |
| `src/hiddenTest` | Reviewer-only boundary tests and YAML retests. |

The checked-in coupon implementation is deliberately wrong. The public case
does not distinguish the wrong rule from the intended one; the reviewer retest
does. Hidden retests and expected consumption stay enabled; mutation is
explicitly disabled here to keep the tutorial fast, so its evidence honestly
shows `MUTATION: DISABLED`.

Run the safe, repeatable demo from the repository root:

```bash
bash samples/junit-cart-orders/demo.sh
```

The script prints the stable evidence and safe feedback paths. It restores the
original `OrderService.java` bytes and the reviewer source set after success,
failure, or interruption.

For the full broken-to-fixed walkthrough, report interpretation, and manual
commands, read [TUTORIAL.md](TUTORIAL.md).
