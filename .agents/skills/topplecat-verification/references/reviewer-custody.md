# Reviewer Custody

Read this reference before reviewing, hiding, restoring, or delivering a
ToppleCat contract.

## Source Boundary

```text
src/test/             implementation-visible contract
src/hiddenTest/       complete reviewer-only source set
.topplecat/escrow/    reviewer-local plaintext hidden storage
build/topplecat/      generated review and verification artifacts
```

Treat `.topplecat/escrow/` as local mechanical storage, not encryption or secure
delivery. `./gradlew clean` removes generated build output but leaves escrow
state intact. Removing `src/hiddenTest` from the working tree does not erase it
from Git history.

## Reviewer Sequence

Run:

```text
./gradlew toppleCatCheck
./gradlew toppleCatReview
./gradlew toppleCatHide
```

Inspect `build/topplecat/reports/review/index.html` before Hide. It contains public and
reviewer cases and remains reviewer-only.

`toppleCatHide` validates and moves the complete `src/hiddenTest` source set into
local plaintext storage. On its first hide it also seals the reviewed public
contract and effective verification policy into the active escrow epoch. It is
safe to rerun in an already hidden state, but it refuses mismatched source rather
than deleting it and never refreshes an existing approval.

## Delivery

Never commit reviewer source to Git history the implementation agent can read.
A checkout or worktree derived from history that contains reviewer material is
not a privacy boundary. Use a public export without `.git`, `.topplecat/`, or
`build/`; an isolated environment whose history never contained reviewer
material; or a public implementation repository paired with a separate private
reviewer repository or CI environment.

Give the implementation agent only:

- production source;
- public Spec context;
- public Java tests and case rows;
- ordinary public test output;
- safe `agent-feedback.json` after reviewer verification, when needed.

## Restore

Run `./gradlew toppleCatRestore` only as an authorized reviewer who needs to
inspect or edit hidden source. Restore requires an existing valid manifest,
verifies stored hashes, and does not call Hide first.

After editing reviewer source, or when intentionally changing public contract
material or verification policy, repeat Check and Review and then run
`toppleCatUpdateEscrow`. That task is the explicit, atomic approval update path;
ordinary Hide, Restore, Rehide, and Verify must preserve the existing approval.

Legacy version-1 escrow is readable for Restore and Rehide, but cannot yield a
verification PASS. An authorized reviewer migrates it through Restore → Check →
Review → UpdateEscrow.

## Multiple Specs

Store reviewer rows in per-Spec subdirectories when useful:

```text
src/hiddenTest/resources/topplecat/cases/SPEC-42/
src/hiddenTest/resources/topplecat/cases/SPEC-43/
```

Hide and Restore always operate on the complete reviewer source set atomically.
Keep per-Spec selection out of custody operations. Serialize work when multiple
Specs modify the same DTO, production path, or reviewer source set.

## Recovery Rules

- Preserve source and escrow whenever a custody operation reports a mismatch.
- Wait for an active Hide, Restore, or Verify operation to release the project
  lock before retrying.
- Keep new, missing, or modified reviewer files in place for diagnosis.
- Restore known source through `toppleCatRestore`; never reconstruct it from a
  report.
- Keep `.topplecat/` while `src/hiddenTest` is absent. Deleting escrow in that
  state can destroy the only local reviewer copy.
