# Samples

Both samples show the current 0.2.2 release boundary: public acceptance methods, public rows,
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

The JUnit project uses its own Gradle Wrapper and the locally published 0.2.2
artifact (Maven Central publication is a separate maintainer action). Its `demo.sh --help` lists five synthetic lessons. Each route runs
the appropriate formal verification, then keeps that route's synthetic local
Verification Report at `build/topplecat/demo-reports/<lesson>/index.html` for
the learner to inspect. The Spring Boot script remains a contributor-oriented
demo.
