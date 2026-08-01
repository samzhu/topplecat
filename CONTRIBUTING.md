# Contributing to ToppleCat

Small, focused changes are easiest to review. Bug fixes, tests, and
documentation should support ToppleCat's Java/JUnit delegation gate.

For the repository map, task-specific design documents, and implementation
workflows, see the [development guide](DEVELOPMENT.md).

## Local setup

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

## Release versioning

ToppleCat uses three-part `X.Y.Z` release versions. Keep the same value in all
release surfaces. For example, a 0.0.3 release uses:

```text
Gradle/Maven version: 0.0.3
Git tag:              0.0.3
GitHub Release:       ToppleCat 0.0.3
```

The release tag has no `v` prefix. `scripts/publish-central.sh` reads the
Gradle project version, requires the matching `X.Y.Z` tag on `HEAD`, and
publishes artifacts with that same value. Keep published tags, GitHub Releases,
and the matching historical notes under `docs/releases/` unchanged.

Use the
[`topplecat-release` skill](.agents/skills/topplecat-release/SKILL.md) to
organize and validate the repository for release. After explicit authorization,
it prepares the release notes and creates and pushes the verified annotated
tag; the maintainer publishes Maven Central and the GitHub Release.

## Change rules

1. Read the public architecture and relevant guide before changing behavior.
2. Add or update Java/JUnit tests and typed JSON/YAML case rows for behavior.
3. Keep generated output, local escrow state, credentials, and temporary notes
   out of commits.
4. Update public documentation when commands, artifact coordinates, reports, or
   authoring behavior changes.

## Project boundaries

- Do not replace Java/JUnit with another test framework or authoring syntax.
- Do not add a natural-language scenario runtime or a second executable
  specification format.
- Keep `src/hiddenTest` reviewer-only. Public handoff material and agent
  feedback must not reveal private values, IDs, source names, paths, or failure
  details.
- Keep mutation attribution automatic from PIT coverage data; users must not
  hand-maintain a second AC-to-mutation map.

## Before a pull request

```bash
GRADLE_CMD=./gradlew scripts/verify-release.sh
```

Describe the behavior change, verification command, and any compatibility
impact. Do not include generated reports or real reviewer data.
