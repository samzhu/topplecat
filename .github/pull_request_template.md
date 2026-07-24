## What changed

Describe the behavior change and why it belongs in ToppleCat.

## Verification

List the exact commands you ran.

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
```

## Boundaries

- [ ] Public reports and `agent-feedback.json` contain no reviewer-only data.
- [ ] Generated `build/` output and `.topplecat/` state are not included.
- [ ] User-facing commands, paths, and examples are documented.
