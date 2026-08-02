---
name: topplecat-release
description: Prepare ToppleCat releases as an open-source Java framework. Use when preparing a release, committing its candidate, selecting and creating the next tag, or pushing the release to GitHub.
---

# ToppleCat release

Treat a release as a developer handoff. Lead with what Java developers can now
do and which problem disappears; use commits, tasks, and internal classes only
as evidence. This skill's release boundary is a verified GitHub tag.
`publishToMavenLocal` validates local artifacts only. Maven Central publication
and GitHub Release creation remain maintainer actions outside this skill.

Read `DEVELOPMENT.md`, `CONTEXT.md`, and `CONTRIBUTING.md` before starting.
Invoking `$topplecat-release` authorizes the complete release flow in steps
1–6 for the current candidate: prepare it, commit it, create its next tag,
validate local Maven artifacts, and push `main` and the tag to GitHub.

## 1. Establish the release delta

Identify the previous *published GitHub Release*, not merely the highest tag. If
the remote lookup is unavailable, record the baseline as unknown. Compare that
release with the working candidate—`HEAD` plus scoped worktree changes—and
inventory every user-visible change:

- developer capability;
- problem or failure mode it removes;
- adoption or compatibility action;
- executable evidence; and
- affected documentation or site surface.

Classify internal-only work explicitly. Complete this step when every visible or
breaking change has one inventory row.

## 2. Curate the repository

Use the inventory to update current-product docs, guides, samples, and agent
skills. Keep one authoritative explanation for each behavior. The repository
keeps the current release-note pair; published tags and GitHub Releases retain
history. Leave existing authorized version and note work intact while preparing.

Make an explicit site decision from the current site. Update it when its public
API example, product promise, report UI, screenshots, requirements, or links
are stale. If it changes, follow `site/README.md`, build it, and inspect the
rendered page.

Complete this step when each inventory row is current everywhere it appears and
the site decision has evidence.

## 3. Validate preparation and report readiness

Read `docs/validation/README.md` and run every listed command. Immediately
before each complete release gate, apply
[release-gate hygiene](references/release-gate-hygiene.md): stop the local
Gradle TestKit daemon and move only the regenerable
`topplecat-gradle-plugin/build/` cache to system temporary storage. Then rerun
the same complete release gate. Run the site build and rendered-page checks when
the site changed. Confirm generated output, credentials, and local custody state
remain outside the change.

Report the previous release or unknown baseline, candidate commit and worktree
state, inventory, repository cleanup, site decision, checks, and blockers. Do
not create release metadata in this branch.

Complete this step when the repository is internally current, every complete
release gate ran after release-gate hygiene, all required checks pass, and every
unresolved item is named.

## 4. Finalize the authorized candidate

Confirm the authorized `X.Y.Z` value. Update every version coordinate,
including the site install command, and replace the current English and
Traditional-Chinese release-note pair. Read
[the release-note reference](references/release-notes.md) before writing it.

Before committing, make the candidate worktree exclusive to intended release
changes and run:

```bash
java-format format
```

Inspect the formatter diff, then **Commit the formatted release candidate**.
The candidate commit must contain synchronized coordinates, the note pair, and
formatted Java source; no generated output or local custody state belongs in it.

Complete this step when the release candidate is one clean commit with a
developer-readable note pair and all coordinates agreeing on `X.Y.Z`.

## 5. Verify, select, and describe the tag

Rerun every command in `docs/validation/README.md`, plus site verification when
applicable. Inspect candidate JARs, version surfaces, note parity, and the
clean candidate commit.

Fetch `origin` tags and select the **next remote tag**: take the highest remote
three-part `X.Y.Z` tag and increment its patch value. The result must equal the
committed release version. A different semantic-version step needs the user's
explicit `X.Y.Z` authorization; otherwise stop and prepare a corrected
candidate.

Read [the tag-push reference](references/maintainer-publishing.md), then set
the **Tag annotation** to the exact committed English release note
`docs/releases/X.Y.Z.md`. Do not replace it with a summary, commit history, or
validation log.

Complete this step when the candidate commit is clean and verified, the next
remote tag matches its version, and the committed English release note exists
for use as the tag annotation.

## 6. Tag locally, validate locally, then push GitHub

Create and verify the annotated `X.Y.Z` tag without a `v` prefix on the
candidate commit, using `docs/releases/X.Y.Z.md` verbatim as its content.
Do not require a GPG signature. Immediately run:

```bash
./gradlew publishToMavenLocal
```

Confirm the local artifacts use `X.Y.Z`. This is local Maven validation only;
Maven Central is maintainer-owned.

Push the candidate with `git push origin main`, confirm `origin/main` resolves
to that commit, then push the verified tag with `git push origin X.Y.Z` and
resolve its dereferenced remote ref to the same commit. GitHub Release
publication is maintainer-owned.

Complete this step when the remote tag resolves to the verified candidate commit
and the handoff identifies the manual Maven Central and GitHub Release actions
without performing either one.
