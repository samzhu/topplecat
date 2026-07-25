# Getting Started

ToppleCat `0.0.3` is the release described by this guide. A consumer project needs
Java 25 and a Gradle version that supports it. The current consumer example uses
JUnit Jupiter 6.1.1 and the `io.github.samzhu.topplecat` Gradle plugin.
ToppleCat brings its Jackson dependencies transitively and adds no
natural-language scenario runtime.

## Add the Released Distribution

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
[README](../../README.md#install-003). The consumer needs its own Gradle wrapper,
as a normal Gradle project does. See the
[0.0.3 release notes](../releases/0.0.3.md) for the approved-contract integrity changes in this
version.

## Configure the Consumer

From the consumer project root, author the contract and inspect it:

```bash
./gradlew toppleCatCheck
./gradlew toppleCatReview
```

`toppleCatInit` is an optional bootstrap for an otherwise empty consumer project,
not a core workflow task. It does not overwrite existing files or edit
`.gitignore`:

```bash
./gradlew toppleCatInit
```

It creates public examples and a reviewer-only example. Replace them with your
domain DTOs, production call, and independent reviewer boundary before using the
workflow for a real handoff.

`publishToMavenLocal` remains useful when developing ToppleCat from a source
checkout or running this repository's checked-out demos. It is not part of
installing the released distribution.

Author each canonical `@ToppleTest` as direct calls on `@ToppleStageField`
fields. Put production calls and assertions in the Stage methods, where
`recorded(...)` is first and `return self();` is last; use `@ToppleAc` for any
extra ordinary JUnit test.

## First Verification Cycle

Once public and reviewer contracts exist, run:

```bash
./gradlew toppleCatHide
./gradlew test
./gradlew toppleCatVerify
```

`toppleCatHide` moves the reviewer source set into local hidden storage. `test`
is the public development loop. `toppleCatVerify` runs hidden retests, PIT
mutation, and expected-consumption enforcement by default, writes evidence and
reports, and re-hides restored reviewer source. Its first mutation run can take
longer. Aggregate `FAIL` or `INCOMPLETE` makes the command exit non-zero after
those artifacts are complete; a green final task means aggregate `PASS`.
Inspect `build/topplecat/evidence.json` for gate-level detail and any explicit
`DISABLED` safeguard.

When an authorized reviewer needs to inspect or change reviewer-only source,
run `./gradlew toppleCatRestore`. It restores only an existing hidden source set
and does not hide anything first.

## Delivery Hygiene

Local hidden storage is plaintext mechanical state, not encryption.
`./gradlew clean` removes `build/`, including HTML reports, but it **does not**
remove `.topplecat/escrow/`. After `toppleCatHide`, do not give the
implementation agent the reviewer working tree merely because it was cleaned.
Never commit reviewer material to Git history the implementation agent can
read; deleting the working files or creating another worktree from that history
does not hide them. Deliver a public export without `.git`, `.topplecat/`, or
`build/`, or use an isolated environment whose history never contained reviewer
material. A separate private reviewer repository or CI environment is also an
appropriate custody boundary.

Use the [JUnit cart-orders tutorial](../../samples/junit-cart-orders/TUTORIAL.md)
to see a hidden retest reject a public-case coincidence.

External Markdown context and reviewer attachments are optional advanced
features. Add them only when the executable Java contract needs supporting
reading context or private diagnostics; see
[Authoring Contracts](authoring.md#external-spec-documents).
