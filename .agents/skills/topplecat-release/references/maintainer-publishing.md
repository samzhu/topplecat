# Tag-push reference

Read this reference in step 5, immediately before preparing an authorized
ToppleCat tag. It supplies the mechanical checks for the sequence in
`SKILL.md`; the skill owns the release narrative and ordering.

## Resolve the next remote tag

Fetch `main` and tags. Consider only remote tags matching exactly `X.Y.Z`, sort
them as semantic versions, and increment the highest patch number. Do not infer
the next tag from a local tag or a GitHub Release. Stop when the resulting value
does not equal the version sealed into the candidate commit.

Before tagging, confirm the branch is `main`, the worktree is clean, the
candidate commit has the expected version, and neither the local nor remote
`X.Y.Z` tag exists.

## Use the committed release note for the local tag

Use the exact contents of the committed English release note—do not write a
second summary. Create the tag, then verify its target and publish candidate
artifacts only to the local Maven repository:

```bash
git tag -a X.Y.Z <candidate-sha> -F docs/releases/X.Y.Z.md
git show --no-patch --format=fuller X.Y.Z
git rev-list -n 1 X.Y.Z
./gradlew publishToMavenLocal
```

The annotation is therefore the committed release note itself. Local
publication confirms the tagged coordinates can be consumed without sending
artifacts to Maven Central.

## Push and resolve the remote tag

Push the candidate branch first, then push and resolve the tag:

```bash
git push origin main
git ls-remote --heads origin main
git push origin X.Y.Z
git ls-remote --tags origin refs/tags/X.Y.Z refs/tags/X.Y.Z^{}
```

Stop when either remote ref differs from the candidate commit.

## Maintainer follow-up

Maven Central publication and GitHub Release drafting or publication are manual
maintainer actions. This skill leaves their release title and body in the
committed English release note and makes no external artifact or GitHub Release
change.
