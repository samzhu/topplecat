# Release-gate hygiene

Run this procedure immediately before every complete release gate, including a
rerun. It isolates stale Gradle TestKit output before the gate's own initial
`clean` proves the candidate rebuilds from source.

## 1. Stop the local TestKit daemon

Use the same Gradle executable that will run the gate:

```bash
./gradlew --stop
```

Complete this step when the command has returned. It ends local Gradle daemons
that can retain the preceding functional test's temporary cache.

## 2. Quarantine the regenerable cache

Move the sole affected generated directory to system temporary storage if it
exists:

```bash
if [[ -d topplecat-gradle-plugin/build ]]; then
  testkit_cache_quarantine="$(mktemp -d)"
  mv topplecat-gradle-plugin/build "$testkit_cache_quarantine/"
fi
```

The move is recoverable and leaves source, every other module's `build/`, and
repository state untouched. The complete gate recreates this plugin output.

## 3. Run the unchanged complete gate

Rerun the exact complete-gate command; for the repository wrapper, that is:

```bash
GRADLE_CMD=./gradlew scripts/verify-release.sh
```

If the rerun fails at `clean` for a new or continuing process lock, report a
local-environment blocker with its path and owner. If it reaches compilation,
tests, or verification and fails there, report that outcome as the candidate's
release-gate result.
