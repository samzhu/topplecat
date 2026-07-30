# ToppleCat Contributor Instructions

ToppleCat is a delegation verification gate for Java/JUnit projects. Ordinary
Java acceptance tests and typed JSON/YAML case data are authoritative; generated
JSON and HTML are evidence.

## Start Here

Read `DEVELOPMENT.md` and `CONTEXT.md` first. Use the task map to find the
relevant implementation documents and verification commands, and use the
context glossary's formal terms consistently.

Before changing supported behavior, read:

- `README.md`
- `docs/architecture.md`
- `docs/guide/authoring.md`
- `docs/guide/verification-and-evidence.md`

## Design Records And Documentation

- Do not leave an accepted product design only in a chat, prompt, issue, or
  agent handoff. Before delegating implementation, record it under
  `docs/design/` using the structure in `docs/design/README.md`.
- Keep `AGENTS.md` short and durable: put mandatory boundaries, work rules, and
  document routing here. Put feature examples, alternatives, detailed behavior,
  failure semantics, and acceptance cases in the relevant design record.
- Treat `docs/architecture.md` and the guides as the current implemented
  product. A design record marked `Accepted` but not `Implemented` describes
  intended work and must not be presented in the README as an available feature.
- When implementation changes supported behavior, update the design status,
  architecture, affected guide, tests, agent skill, and user-facing
  documentation in the same change. Link to one canonical explanation instead
  of copying details into several files.
- If code, tests, design records, and current-product documentation disagree,
  stop and identify the conflict. Do not silently choose the version that makes
  the task easiest.

## Product Boundaries

- Humans and their external SDD, workflow, or task system choose the current
  Spec, manage delivery history, and decide any organizational sign-off.
  ToppleCat is not a task manager, Spec lifecycle manager, or approval system.
- Humans remain responsible for making the selected rules and cases complete.
  Do not make ToppleCat infer missing business requirements or judge behavior
  outside the executable contract.
- ToppleCat starts at the executable acceptance boundary: bind selected ACs to
  ordinary Java/JUnit tests and typed case rows, keep the public contract handed
  to the implementation agent identical to the contract run by verification,
  and test the agent's done claim.
- Treat a ToppleCat reviewer approval as a mechanical integrity seal over
  contract bytes and verification policy, not proof of human or organizational
  sign-off.
- Treat generated JSON and HTML only as projections of the checked contract.
  Rendering must not add, omit, or reinterpret rules, cases, expected values,
  or compiler-defined scenario steps.
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

## Human Communication

- Explain product behavior with a concrete example before introducing the
  implementation term. For example, first describe which checkout and coupon
  cases run for one delivery, then name the mechanism "Spec-scoped hidden
  retest."
- Lead with the human problem and visible outcome. Introduce annotations,
  Gradle task wiring, digests, schemas, gates, and class names only after the
  reader understands what they solve.
- When presenting alternatives, reuse one simple domain example across all
  options so the trade-off is easy to compare.
- Translate jargon into plain language the first time it appears. Do not assume
  a maintainer asking a product question wants an implementation-first answer.

## Git Safety

Do not commit generated `build/` output, local `.topplecat/` escrow state,
credentials, or temporary notes. Preserve unrelated worktree changes. Do not
rewrite history, force-push, or publish without explicit user approval.
