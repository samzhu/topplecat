# Release validation

This maintainer document lists the current public release checks. It does not
store generated validation output or historical run logs. From a clean checkout,
run:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
bash scripts/publish-central-preflight-test.sh
scripts/verify-consumer-compatibility.sh
bash scripts/verify-release-cleanup-test.sh
bash scripts/validate-skill.sh
python3 scripts/verify-docs.py
git diff --check
```

Keep these records limited to public commands, aggregate gate outcomes, and
safe diagnostics. Do not include reviewer source, custody paths, hidden values,
or raw failures.
