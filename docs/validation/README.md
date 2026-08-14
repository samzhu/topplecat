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

`bash scripts/validate-skill.sh` is the deterministic release gate for the
repository-owned skill. It reads the installed `SKILL.md`, interface, retained
references, and synthetic canonical Markdown fixture, then exercises exact
selected, whole-contract, and selected-failure output assertions. The test
also feeds forged scope, narrative, custody, and fallback outputs to those
assertions. It does not implement a Markdown parser, maintain a selection
registry, invoke a model, or claim that deterministic source checks are a real
skill invocation. The product CommonMark parser remains the only authority for
AC structure and inventory.

The synthetic fixture covers multiple selected documents with multiple ACs, an
unselected old document, authored Given/When/Then/And/But groups, public and
reviewer handoffs, whole-contract no-Review guidance, absolute/ambiguous/
missing-file/missing/invalid/insufficient/thin-path failure routing, and the
`.feature` boundary. Real-model forward runs and external SDD integrations are optional
maintainer work outside the repository completion gate; their environment
status must never be reported as a product or release PASS.

Keep these records limited to public commands, aggregate gate outcomes, and
safe diagnostics. Do not include reviewer source, custody paths, hidden values,
or raw failures.
