# ToppleCat Samples

The samples are independent Gradle consumers. Their checked-out demos
intentionally publish the current source checkout to Maven Local first, so they
exercise the repository under development rather than a released artifact. They
are not project dependencies or an included build. For normal use of the
released `0.0.1`, use the Maven Central setup in the root
[README](../README.md#install-001) instead.

| Sample | Choose it when | Demonstrates |
| --- | --- | --- |
| [junit-cart-orders](junit-cart-orders) | You use ordinary JUnit tests and service/domain DTOs. | Typed nested DTO injection, hidden retests, expected consumption, and local hidden storage. Mutation is deliberately disabled for demo speed. |
| [spring-boot-cart-orders](spring-boot-cart-orders) | You use a Spring Boot test project. | The same canonical Stage DSL under `@SpringBootTest`, plus a reviewer JUnit test with a Spring-managed dependency. |

Both samples deliberately begin with a public-case coincidence. Their demos run
the hidden-retest failure, apply the correct implementation for the demo, verify
the passing result, and restore the checked-in flawed source.

The sample reviewer cases are checked in so the demonstrations are reproducible;
they are teaching data, not secrets. In a real consumer project, keep equivalent
material under reviewer custody and out of the implementation tree.

Both samples explicitly disable mutation so the failure-to-pass walkthrough stays
fast. Their evidence records `MUTATION: DISABLED`; this is not a passing mutation
claim.

```bash
bash samples/junit-cart-orders/demo.sh
bash samples/spring-boot-cart-orders/demo.sh
# Run both published-consumer workflows together.
bash scripts/verify-samples.sh
```

Read the [JUnit tutorial](junit-cart-orders/TUTORIAL.md) for the complete
contract-to-evidence walkthrough and the [Spring tutorial](spring-boot-cart-orders/TUTORIAL.md)
for application-context and stage-specific guidance.
