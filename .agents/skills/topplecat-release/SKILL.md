---
name: topplecat-release
description: Prepare ToppleCat releases as an open-source Java framework. Use when organizing the repository for release, pruning release-facing docs, deciding whether the site must change, drafting developer-focused release notes after approval, validating the release commit, creating and pushing an authorized tag, or preparing the maintainer's GitHub and Maven publication handoff.
---

# ToppleCat release

Treat a release as a developer handoff. Lead with what Java developers can now
do and which problem disappears; use commits, PRs, tasks, and internal classes
only as evidence.

Read `DEVELOPMENT.md`, `CONTEXT.md`, and `CONTRIBUTING.md` before starting.
A request to *prepare a release* means complete steps 1–3: organize and verify
the repository, then report readiness. It does not author release notes, change
the release version, or create a tag. Continue with steps 4–6 only when the user
explicitly authorizes the release.

## 1. Establish the release delta

Identify the previous *published GitHub Release*, not merely the highest local
tag. If the remote lookup is unavailable, record the baseline as unknown
rather than inferring that no release exists. Compare the resolved release
with the working candidate—`HEAD` plus scoped worktree changes—then inspect
changed public behavior, documentation, examples, compatibility, requirements,
and safeguards.

Build a private inventory with one row per user-visible change:

- developer capability;
- problem or failure mode it removes;
- adoption or compatibility action;
- executable evidence; and
- affected documentation or site surface.

Complete this step only when every user-visible and breaking change is
accounted for, and internal-only changes are explicitly classified as such.

## 2. Curate the repository

Use the inventory to update current-product docs, guides, samples, and agent
skills. Prune duplicated, superseded, and proposed wording from active
surfaces. Follow `CONTRIBUTING.md`: the repository keeps the current note pair,
while published tags and remote GitHub Releases preserve history. Leave
existing authorized version and note work intact, but do not introduce or
rewrite it during preparation.

Prefer technique names, such as Property-Based Testing, to project names. Name
an external project only when ToppleCat depends on it or attribution is needed
to understand a design decision. State the concept ToppleCat learned from and
the ToppleCat-specific need that led to a different choice; do not treat another
project as design authority or turn current-product docs into a comparison.

Make an explicit site decision from the current site, not from the changed-file
list alone. Update it when the public API example, product promise, report UI,
screenshots, requirements, or linked destinations remain stale. Record whether
its install version must change during release. If the site changes, follow
`site/README.md`, build it, and inspect the rendered result.

Complete this step only when each inventory row has one authoritative
explanation, every other active surface is accurate, and the site decision has
evidence.

## 3. Validate preparation and report readiness

Read `docs/validation/README.md` and run every current command it lists. Run
the site build and rendered-page checks when the site changed. Inspect
clean-tree exclusions and confirm generated output, credentials, and local
custody state remain outside the change.

Report the previous release or unknown baseline, the working candidate's base
commit and worktree status, developer-capability inventory, repository cleanup,
site decision, commands and outcomes, and blockers. Do not include draft
release notes or create release metadata.

Complete this step only when the repository is internally current, every
required check passes, and the readiness report names every unresolved item.

## 4. Finalize the authorized release

Confirm the authorized `X.Y.Z` version. Update every version coordinate,
including the site's install command, and replace the repository's current
release-note pair.

Read [the release-note reference](references/release-notes.md) before drafting.
Write `docs/releases/<version>.md` and its Traditional-Chinese counterpart with
the same capabilities, compatibility facts, and upgrade actions. Use
`.github/release.yml` and GitHub's generated notes to reconcile every merged
PR as developer-facing or deliberately internal.

Complete this step only when a developer can decide why to adopt, whether the
release affects them, and exactly what to change without reading the commit
list.

## 5. Verify and present the exact handoff

Rerun every command in `docs/validation/README.md`, plus site verification when
applicable. Inspect candidate JARs, version surfaces, English/Traditional-
Chinese parity, and the clean release commit.

If the release changes are not committed or the release commit is not yet on
`origin/main`, stop before tagging and hand that action to the maintainer.
Present the GitHub Release title and body, exact commit SHA, and proposed local
tag. Create no tag until the user's authorization clearly covers that exact
version and commit.

Complete this step only when all checks pass and the user has received the
release notes and confirmed the release identity.

## 6. Create and push the authorized tag

Read [the maintainer handoff](references/maintainer-publishing.md) immediately
before acting. Create and verify the annotated `X.Y.Z` tag without a `v` prefix
on the confirmed release commit that matches `origin/main`. Do not require a
GPG signature. Push the verified tag to `origin`.

Stop at the remote-tag boundary. Give the maintainer the verified remote tag,
GitHub Release title/body, and publication checklist. The maintainer publishes
Maven Central and the GitHub Release.

Complete this step only when the remote tag resolves to the confirmed release
commit and the handoff identifies every remaining external action without
performing it.
