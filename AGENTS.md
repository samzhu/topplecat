# ToppleCat Contributor Instructions

ToppleCat is a delegation verification gate for Java/JUnit projects. Ordinary
Java acceptance tests and typed JSON/YAML case data are authoritative; generated
JSON and HTML are evidence.

## Start Here

Before changing supported behavior, read:

- `README.md`
- `docs/architecture.md`
- `docs/guide/authoring.md`
- `docs/guide/verification-and-evidence.md`

## Product Boundaries

- Keep the four-module layout: `topplecat-core`, `topplecat-junit`,
  `topplecat-report`, and `topplecat-gradle-plugin`.
- Keep public tests and case data under `src/test`; keep the complete
  reviewer-only source set under `src/hiddenTest`.
- Do not introduce a second authoring language, a command-line interface, or a
  new compatibility surface.
- Never put reviewer-only values, identifiers, paths, source names, or raw
  failures in the public report or `agent-feedback.json`.

## Verification

Develop with the narrowest relevant test, then run:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
```

`toppleCatVerify` and `build/topplecat/evidence.json` provide the final contract
verdict. A green `test` task is development feedback only.

## Git Safety

Do not commit generated `build/` output, local `.topplecat/` escrow state,
credentials, or temporary notes. Preserve unrelated worktree changes. Do not
rewrite history, force-push, or publish without explicit user approval.
