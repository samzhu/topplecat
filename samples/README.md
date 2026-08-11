# Samples

Both samples show the 0.1.0 boundary: public acceptance methods, public rows,
and Properties stay under `src/test`; reviewer-owned hidden rows stay under
`src/hiddenTest` until Seal.

| Sample | Use it when | Focus |
| --- | --- | --- |
| [junit-cart-orders](junit-cart-orders) | You use plain JUnit and want to learn ToppleCat. | A standalone, bilingual five-safeguard learning project. |
| [spring-boot-cart-orders](spring-boot-cart-orders) | You use Spring Boot tests. | The same acceptance contract with a Spring test context. |

Run a demo from the repository root:

```bash
bash samples/junit-cart-orders/demo.sh all
bash samples/spring-boot-cart-orders/demo.sh
```

The JUnit project uses its own Gradle Wrapper and released Maven Central 0.1.0
artifacts. Its `demo.sh --help` lists five synthetic lessons; each runs in a
temporary copy, proves a baseline, then demonstrates one independent Gate.
The Spring Boot script remains a contributor-oriented demo.
