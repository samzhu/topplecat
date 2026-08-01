# Samples

Both samples show the 0.0.11 boundary: public acceptance methods, public rows,
and Properties stay under `src/test`; reviewer-owned hidden rows stay under
`src/hiddenTest` until Seal.

| Sample | Use it when | Focus |
| --- | --- | --- |
| [junit-cart-orders](junit-cart-orders) | You use plain JUnit. | Typed acceptance rows, hidden-row rejection, PBT. |
| [spring-boot-cart-orders](spring-boot-cart-orders) | You use Spring Boot tests. | The same acceptance contract with a Spring test context. |

Run a demo from the repository root:

```bash
bash samples/junit-cart-orders/demo.sh
bash samples/spring-boot-cart-orders/demo.sh
```

Each script seals reviewer material, demonstrates a deliberate failure, applies
the correction, verifies again, restores checked-in source, and cleans its
temporary reviewer state. They use a local contributor artifact only so they
exercise this checkout.
