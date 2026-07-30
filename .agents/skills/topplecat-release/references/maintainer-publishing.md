# Maintainer publication handoff

The agent prepares this checklist after release verification. The maintainer
performs every remote and Maven action.

## Preflight

1. Fetch `main` and tags, then record the candidate commit.
2. Confirm the branch is `main`, the working tree is clean, `HEAD` equals
   `origin/main`, CI is green, and version coordinates agree.
3. Confirm the `X.Y.Z` tag and GitHub Release do not exist remotely.
4. Confirm Maven Central does not already serve the candidate version.

ToppleCat tags have no `v` prefix. After the authorized local tag has been
created, verify it again before any push:

```bash
git tag -v X.Y.Z
git push origin X.Y.Z
```

Stop if verification fails and resolve the local tag before pushing.

## Stage the public release

After pushing the tag, create a GitHub Release draft bound to that existing
remote tag. With GitHub CLI:

```bash
gh release create X.Y.Z \
  --verify-tag \
  --draft \
  --fail-on-no-commits \
  --title "ToppleCat X.Y.Z" \
  --notes-file docs/releases/X.Y.Z.md
```

When GitHub CLI is unavailable, use the Releases page with the same tag, title,
and body, and save a draft. Draft-first keeps assets and notes editable before
release immutability applies.

## Publish artifacts before announcing

Run `scripts/publish-central.sh`. It reruns the release gate, signs Maven
artifacts, uploads a user-managed Central deployment, and stops for Portal
review. Publish that deployment in the Central Portal only after validation.

Wait until the public Maven Central coordinates resolve. If the site changed,
also wait for the Pages deployment and inspect the public page. Then publish
the GitHub draft and mark it Latest:

```bash
gh release edit X.Y.Z --draft=false --latest
gh release view X.Y.Z
```

Verify the release title, tag, target commit, body, latest status, source
archives, and public installation coordinates.

## Repository settings

Prefer release immutability and a tag ruleset that restricts deletion or
rewrites of version tags. With immutability enabled, assemble the draft before
publishing because the tag and assets lock at publication.

Primary references:

- [Managing releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
- [Automatically generated release notes](https://docs.github.com/en/repositories/releasing-projects-on-github/automatically-generated-release-notes)
- [Signing tags](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-tags)
- [Immutable releases](https://docs.github.com/en/code-security/concepts/supply-chain-security/immutable-releases)
- [`gh release create`](https://cli.github.com/manual/gh_release_create)
- [Semantic Versioning](https://semver.org/)
