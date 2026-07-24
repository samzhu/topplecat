# Contributing to ToppleCat

ToppleCat welcomes focused fixes, tests, documentation, and improvements that
strengthen delegation verification for Java/JUnit work.

## Local Setup

- JDK 25
- Gradle 9.1.0 through the repository wrapper
- A Unix-like shell for the release gate

```bash
./gradlew check
```

The published modules are `topplecat-core`, `topplecat-junit`,
`topplecat-report`, and `topplecat-gradle-plugin`.
`integration-tests/mutation-gate` is release-gate test infrastructure, not a
published module or user sample.

## Change Rules

1. Read the public architecture and relevant guide before changing behavior.
2. Add or update Java/JUnit tests and typed JSON/YAML case rows for behavior.
3. Keep generated output, local escrow state, credentials, and temporary notes
   out of commits.
4. Update public documentation when commands, artifact coordinates, reports, or
   authoring behavior changes.

## Positioning Red Lines

- Do not replace Java/JUnit with another test framework or authoring syntax.
- Do not add a natural-language scenario runtime or a second executable
  specification format.
- Keep `src/hiddenTest` reviewer-only. Public reports and agent feedback must
  not reveal private values, IDs, source names, paths, or failure details.
- Keep mutation attribution automatic from PIT coverage data; users must not
  hand-maintain a second AC-to-mutation map.

## Before a Pull Request

```bash
GRADLE_CMD=./gradlew scripts/verify-release.sh
```

Describe the behavior change, verification command, and any compatibility
impact. Do not include generated reports or real reviewer data.
