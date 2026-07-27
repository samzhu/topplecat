# JUnit cart orders

This plain JUnit consumer is the smallest complete ToppleCat workflow. It
creates an order from a nested `Cart` DTO. The public test passes, but the
reviewer contract shows that the implementation is wrong.

> **Why does this sample contain `src/hiddenTest`?**
>
> A normal project handed to a developer or AI agent does not contain this
> directory. This sample includes reviewer-only cases so a fresh clone can show
> the complete demo immediately. In a real workflow, the directory exists only
> while a reviewer authors or updates those checks. `toppleCatHide` removes it
> before the project is handed off.

| Source | Role |
| --- | --- |
| `src/test/java` | Public Java acceptance tests: canonical `@ToppleTest` Stage DSL plus any extra `@ToppleAc` JUnit coverage. |
| `src/test/resources/topplecat/cases` | Public typed JSON case rows. |
| `src/hiddenTest` | Demo-only reviewer checks. This directory is absent from the implementation handoff. |

The checked-in coupon implementation is wrong on purpose. The public case
does not distinguish the wrong rule from the intended one; the reviewer retest
does. Hidden retests and expected consumption stay enabled; mutation is
disabled here to keep the tutorial fast, so its evidence shows
`MUTATION: DISABLED`.

Run the repeatable demo from the repository root:

```bash
bash samples/junit-cart-orders/demo.sh
```

For an interactive trial, run tasks directly from this directory. The local
`./gradlew` forwards to the repository wrapper, selects this sample as the
Gradle project, and resolves the published ToppleCat release:

```bash
./gradlew toppleCatCheck
./gradlew toppleCatReview
```

The script prints the stable evidence and safe feedback paths. It restores the
original `OrderService.java` bytes and the reviewer source set after success,
failure, or interruption.

For the full broken-to-fixed walkthrough, report interpretation, and manual
commands, read [TUTORIAL.md](TUTORIAL.md).
