# ToppleCat samples

These samples are independent Gradle consumers. The demos publish the current
checkout to Maven Local before running, so they test the code in this repository
without using project dependencies or an included build. Once `0.0.6` is
available in Maven Central, regular consumers should use the setup in the root
[README](../README.md#install-006).

> **Demo-only reviewer files:** the samples check in `src/hiddenTest` so a
> fresh clone can reproduce the complete reviewer flow. A normal project given
> to a developer or AI agent does not contain that directory. It exists only
> while a reviewer authors or updates checks, then `toppleCatHide` removes it
> before handoff.

| Sample | Choose it when | Demonstrates |
| --- | --- | --- |
| [junit-cart-orders](junit-cart-orders) | You use ordinary JUnit tests and service/domain DTOs. | Typed nested DTO injection, hidden retests, expected consumption, and reviewer-local custody. Mutation is disabled to keep the demo short. |
| [spring-boot-cart-orders](spring-boot-cart-orders) | You use a Spring Boot test project. | The same canonical Stage DSL under `@SpringBootTest`, plus a reviewer JUnit test with a Spring-managed dependency. |

Each sample starts with a bug that happens to satisfy the public case. The demos
show the hidden-retest failure, apply the correct implementation, verify the
result, and restore the checked-in bug.

For interactive use inside a checked-out repository, each sample has a small
`./gradlew` launcher. It delegates to the repository wrapper while selecting the
sample project, so you can run, for example,
`cd samples/junit-cart-orders && ./gradlew toppleCatReview`. It resolves the
published ToppleCat release by default; the demos explicitly opt into Maven
Local when they need to exercise the checkout itself.

The reviewer cases are checked in to make the demos reproducible. They are
teaching data, not secrets. In a real project, keep equivalent material under
reviewer custody and out of the implementation tree.

Mutation is disabled in both samples so the walkthrough stays fast. Their
evidence records `MUTATION: DISABLED`; this does not count as a mutation pass.

```bash
bash samples/junit-cart-orders/demo.sh
bash samples/spring-boot-cart-orders/demo.sh
# Run both sample workflows together.
bash scripts/verify-samples.sh
```

Read the [JUnit tutorial](junit-cart-orders/TUTORIAL.md) for the complete
contract-to-evidence walkthrough and the [Spring tutorial](spring-boot-cart-orders/TUTORIAL.md)
for application-context and stage-specific guidance.
