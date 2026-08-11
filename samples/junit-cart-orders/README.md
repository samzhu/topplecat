# Learn ToppleCat with JUnit cart orders

This standalone, synthetic JUnit project shows how a Java Acceptance Method,
Typed Case Rows, a Scenario, and a Stage become an Executable Contract. It uses
released ToppleCat 0.1.0 artifacts from Maven Central; it does not build the
ToppleCat repository first.

## Requirements

- JDK 25
- Internet access the first time Gradle downloads the wrapper and Maven Central
  dependencies

```bash
./gradlew test
./demo.sh --help
```

Choose the question you want to understand:

| Command | What it demonstrates |
| --- | --- |
| `./demo.sh public-acceptance` | Public typed examples reject a wrong result. |
| `./demo.sh hidden-tests` | Independent examples catch a shortcut public rows miss. |
| `./demo.sh property-based-testing` | A bounded invariant reaches beyond example rows. |
| `./demo.sh mutation-testing` | Managed PIT finds a production change an acceptance method misses. |
| `./demo.sh contract-integrity` | A post-Seal contract change is not trusted. |
| `./demo.sh all` | Runs all five lessons. |

Every lesson starts from a passing synthetic baseline in a temporary copy,
applies one readable synthetic deviation, verifies the expected Gate, and then
cleans up. These examples explain evidence for the contract; they never prove
that a human selected every business rule or made a delivery decision.

All fixtures, values, and diagnostics in this project are synthetic teaching
material. The three root-level `*.java` and `*.yaml` teaching variants are the
readable deviations used by the commands; Gradle does not compile them. See
[README.zh-TW.md](README.zh-TW.md) for Traditional Chinese.
