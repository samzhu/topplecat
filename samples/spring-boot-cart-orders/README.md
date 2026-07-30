# Spring Boot cart orders sample

This sample uses the same public acceptance contract, hidden typed rows, and
Property-Based Testing model as the JUnit sample, with the public acceptance
class bootstrapped by Spring Boot.

| Location | Contents |
| --- | --- |
| `src/test/java` | Public `@ToppleAcceptanceTest` methods and Properties. |
| `src/test/resources/topplecat/cases` | Public typed rows. |
| `src/hiddenTest` | Reviewer-owned hidden typed rows plus an ordinary JUnit check. Only the rows are Hidden Tests evidence. |
| `demo/` | Broken and fixed service variants used by the walkthrough. |

```bash
bash samples/spring-boot-cart-orders/demo.sh
```

Read [the walkthrough](TUTORIAL.md) for the reviewer flow and report paths.
