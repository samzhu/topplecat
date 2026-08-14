# ToppleCat documentation

This is the single index for ToppleCat's public documentation. Start with the
root [README](../README.md) when deciding whether the project fits your team.
Use [Getting started](guide/getting-started.md) when you are ready to run it.
The same current facts are published as the
[official bilingual technical documentation site](https://topplecat.samzhu.dev/docs/)
with a [Traditional Chinese home](https://topplecat.samzhu.dev/docs/zh-TW/).

## Choose your task

| You want to... | Read |
| --- | --- |
| Install ToppleCat and verify one delivery | [Getting started](guide/getting-started.md) |
| Write Acceptance Methods, typed rows, Stages, or Properties | [Authoring contracts](guide/authoring.md) |
| Run Check, Review, Seal, Verify, and interpret Gates or reports | [Verification and evidence](guide/verification-and-evidence.md) |
| Diagnose a failed or incomplete run | [Troubleshooting](guide/troubleshooting.md) |
| Understand the audience, product boundary, or product-fit test | [Product definition](product.md) |
| Understand modules, data flow, custody, and information boundaries | [Architecture](architecture.md) |
| Review an accepted design that has not been implemented yet | [Product design workspace](design/README.md) |
| Look up the precise meaning of a ToppleCat term | [Context glossary](../CONTEXT.md) |

## Which document is authoritative?

| Document kind | What it owns |
| --- | --- |
| Root README | First-time product understanding, shortest useful example, and adoption-critical boundaries |
| Product definition | Current audience, use moments, responsibility boundary, and product-fit test |
| Guides | Current supported user workflow, authoring, configuration, diagnostics, and safety rules |
| Architecture | Current implemented module responsibilities, execution model, evidence flow, and product boundaries |
| Context glossary | One canonical meaning for each ToppleCat domain term |
| Design workspace | Accepted decisions awaiting implementation; completed designs do not remain here |
| Release notes | What changed in one published version; they are historical and are not silently rewritten |
| Development and validation docs | Contributor routing and repository verification commands |

Current product behavior belongs in the Product definition, Architecture, and
guides. A design record describes intended work and is never evidence that a
feature is already supported.

If code, tests, current docs, and a design record disagree, stop and report the
conflict. Do not silently copy one version into more files.

## Releases and maintainers

- Latest release: [0.2.1](releases/0.2.1.md) ·
  [繁體中文](releases/0.2.1.zh-TW.md) ·
  [0.2.0 history](releases/0.2.0.md) ·
  [繁體中文歷史](releases/0.2.0.zh-TW.md)
- Contributor workflow: [CONTRIBUTING](../CONTRIBUTING.md)
- Repository task map: [DEVELOPMENT](../DEVELOPMENT.md)
- Release validation: [validation checklist](validation/README.md)
- Security policy: [SECURITY](../SECURITY.md)

## Documentation maintenance

Each fact has one owner. Link to that explanation instead of copying its full
details into README, Product definition, Architecture, guides, and agent skills.

- Add or remove user-facing documents through this index in the same change.
- Add accepted work through the [design index and lifecycle rules](design/README.md).
- Keep guides current-only; move version-specific change descriptions to
  release notes.
- When implementation finishes, merge lasting content into its current owner
  and delete the design record in the same change.
- Do not create an archive directory merely to retain searchable obsolete
  guidance; Git history is the archive for deleted process material.

Validate documentation changes with:

```bash
python3 scripts/verify-docs.py
git diff --check
```
