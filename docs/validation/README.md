# External validation records

This directory indexes public release-validation records. From a clean
checkout, run:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
bash scripts/verify-release-cleanup-test.sh
bash scripts/validate-skill.sh
python3 scripts/verify-docs.py
git diff --check
```

Keep these records limited to public commands, aggregate gate outcomes, and
safe diagnostics. Do not include reviewer source, custody paths, hidden values,
or raw failures.
