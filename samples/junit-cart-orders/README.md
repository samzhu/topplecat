# JUnit cart orders sample

This plain JUnit sample has public acceptance methods for coupon and no-coupon
orders, public typed rows, hidden typed boundary rows, and a Property. The
hidden rows catch the deliberately broken order service used by the demo.

| Location | Contents |
| --- | --- |
| `src/test/java` | Public `@ToppleAcceptanceTest` methods and Properties. |
| `src/test/resources/topplecat/cases` | Public typed rows. |
| `src/hiddenTest` | Reviewer-owned hidden typed rows plus an ordinary JUnit check. Only the rows are Hidden Tests evidence. |
| `demo/` | Broken and fixed service variants used by the walkthrough. |

```bash
bash samples/junit-cart-orders/demo.sh
```

Read [the walkthrough](TUTORIAL.md) for the exact reviewer workflow and the
separate gate results.
