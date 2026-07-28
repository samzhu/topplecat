# Getting started

ToppleCat `0.0.6` is the release described by this guide. A consumer project needs
Java 25 and a Gradle version that supports it. The current consumer example uses
JUnit Jupiter 6.1.1 and the `io.github.samzhu.topplecat` Gradle plugin.
ToppleCat brings its Jackson dependencies transitively and adds no
natural-language scenario runtime.

## Add the released distribution

Use Maven Central for both plugin and library resolution. Do not add
`mavenLocal()` to a release consumer: it can silently select an unrelated stale
development build.

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
```

Then apply the plugin and add the JUnit integration shown in the root
[README](../../README.md#install-006). The consumer needs its own Gradle wrapper,
as a normal Gradle project does. See the
[0.0.6 release notes](../releases/0.0.6.md) for delivery-scoped verification
and upgrade guidance.

## Configure the consumer

From the consumer project root, author the contract and inspect it:

```bash
./gradlew toppleCatCheck --spec specs/023-checkout/spec.md
./gradlew toppleCatReview --spec specs/023-checkout/spec.md
```

Use the same repository-relative Spec selection throughout one delivery.
Projects that intentionally verify their complete contract can omit `--spec`.

`toppleCatInit` can bootstrap an empty consumer project. It does not overwrite
files or edit `.gitignore`:

```bash
./gradlew toppleCatInit
```

It creates public examples and a reviewer-only example. Replace them with your
domain DTOs, production call, and independent reviewer boundary before using the
workflow for a real handoff.

Use `publishToMavenLocal` only while developing ToppleCat from source or running
the checked-out demos. Released consumers install from Maven Central.

Author each canonical `@ToppleTest` as direct calls on `@ToppleStageField`
fields. Put production calls and assertions in the Stage methods, where
`recorded(...)` is first and `return self();` is last; use `@ToppleAc` for any
extra ordinary JUnit test.

## First verification cycle

Once public and reviewer contracts exist, run:

```bash
./gradlew toppleCatHide --spec specs/023-checkout/spec.md
./gradlew test
./gradlew toppleCatVerify --spec specs/023-checkout/spec.md
```

`toppleCatHide` moves the reviewer source set into
`~/.topplecat/projects/<sha256-project-key>/escrow/`, the reviewer-local
plaintext custody store. `test` is the public development loop. `toppleCatVerify` runs hidden retests, PIT
mutation, and expected-consumption enforcement by default, writes evidence and
reports, and re-hides restored reviewer source. Its first mutation run can take
longer. Aggregate `FAIL` or `INCOMPLETE` makes the command exit non-zero after
those artifacts are complete; a green final task means aggregate `PASS`.
Inspect `build/topplecat/evidence.json` for gate-level detail and any explicit
`DISABLED` safeguard.

For the managed PIT producer, ToppleCat derives `targetTests` from compiler
descriptors for all approved public canonical `@ToppleTest` declaring classes. This also
covers projects whose production and test packages differ. An explicit consumer
`targetTests` or custom mutation producer remains authoritative; excluding a
canonical test produces `MUTATION=FAIL` when the PIT report is usable, while a
missing or unusable report produces `MUTATION=INCOMPLETE`.

When an authorized reviewer needs to inspect or change reviewer-only source,
run `./gradlew toppleCatRestore`. It restores only an existing hidden source set
and does not hide anything first.

## Delivery hygiene

Reviewer-local storage is plaintext mechanical state, not encryption or a
sandbox. It contains the manifest, hidden blobs, approval epoch, revisions,
history, audit, lock, and recovery state below
`~/.topplecat/projects/<sha256-project-key>/escrow/`; `./gradlew clean` removes
`build/` but does not remove it. A legacy `.topplecat/escrow/` can only be moved
by `toppleCatMigrateEscrow`, which removes the project-local copy after success.
After Hide, do not give the implementation agent reviewer state, hidden source,
build artifacts, or Git history that contained reviewer material. Use a public
export without `.git`, `.topplecat/`, and `build/`, or an isolated public-only
environment.

ToppleCat does not provide an OS sandbox, enforce CI identity, or restrict
same-user Gradle/JVM code. The external workflow must run Verify in a trusted
reviewer/CI boundary; home-directory custody alone cannot defend against a
malicious build script or production code.

Use the [JUnit cart-orders tutorial](../../samples/junit-cart-orders/TUTORIAL.md)
to see a hidden retest reject a public-case coincidence.

External Markdown context and reviewer attachments are optional advanced
features. Add them only when the executable Java contract needs supporting
reading context or private diagnostics; see
[Authoring Contracts](authoring.md#external-spec-documents).
