# Issue tracker: Local Markdown

Specs and issues for this repository live as Markdown files under `.scratch/`.

## Conventions

- Use one directory per feature: `.scratch/<feature-slug>/`.
- Put the feature spec at `.scratch/<feature-slug>/spec.md`.
- Put implementation tickets under
  `.scratch/<feature-slug>/issues/<NN>-<slug>.md`, with blockers numbered first.
- Give every published spec or ticket a `Status:` line near the top.
- Use `ready-for-agent` when the work is ready to hand to an implementation
  agent.
- Append later discussion under a `## Comments` heading rather than changing
  the original request without explanation.

## Publishing and reading

When an engineering skill says to publish a spec or issue, create the
corresponding Markdown file under `.scratch/<feature-slug>/`. When a skill is
given a local issue or spec reference, read that file in full before acting on
it.

Do not create or update a GitHub Issue unless the user explicitly asks to move
the work to GitHub.
