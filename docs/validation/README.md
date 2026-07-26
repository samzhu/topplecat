# External validation records

This directory is the public index for release-validation records. Release
verification is reproducible from a clean checkout with:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
bash scripts/verify-release-cleanup-test.sh
bash scripts/validate-skill.sh
python3 scripts/verify-docs.py
git diff --check
```

Records must contain only public commands, aggregate gate outcomes, and safe
diagnostics. Reviewer-only source, custody paths, hidden case values, and raw
failure details do not belong here.
