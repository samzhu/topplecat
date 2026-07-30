# Release notes

Write for a Java developer deciding whether to adopt or upgrade ToppleCat.

## Narrative order

1. Open with one sentence naming the release's main developer outcome and the
   pain it removes.
2. Give each major capability an outcome-led heading. Start with what a
   developer can do, then name the API, task, or file that enables it.
3. Put breaking changes and required action near the top. State who is
   affected, what stops working, and the exact replacement.
4. State changed Java, Gradle, plugin, artifact, or operational requirements.
5. Close with a compare link or generated changelog for contributors and
   maintainers.

## Editorial filter

- Include a feature when it changes capability, adoption, compatibility,
  correctness, performance, security, or observable evidence.
- Include a fix as the user-visible failure that no longer occurs.
- Include an internal change only when it explains a meaningful constraint or
  risk reduction.
- Keep implementation class names, test counts, and raw verification logs out
  of the lead.
- Prefer one concrete before/after example over a list of internal mechanisms.
- Keep the English and Traditional-Chinese notes semantically aligned; write
  natural Traditional Chinese rather than translating identifiers.

## Minimal shape

```markdown
# ToppleCat X.Y.Z

<What developers can now accomplish and why it matters.>

## <Outcome-led capability>

<Previous pain. New behavior. Public API or task, after the value is clear.>

## Upgrade

<Who is affected and the exact action, or an explicit no-action statement.>

## Requirements

<Only changed runtime, build, or dependency requirements.>

## Full changelog

<Previous-release-to-X.Y.Z compare link.>
```

Add or remove sections to match the release. The questions a developer must be
able to answer are fixed; the headings are not.
