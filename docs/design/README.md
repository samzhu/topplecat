# Product design workspace

This directory is for accepted product work that has not been implemented yet.
It is a handoff workspace, not a permanent archive.

Current product facts belong in the [Product definition](../product.md),
[Architecture](../architecture.md), [guides](../README.md), and
[context glossary](../../CONTEXT.md).

## Active designs

There are no active designs. Completed accepted decisions are merged into the
current Product, Architecture, guides, glossary, skills, and release-facing
documentation, then removed from this workspace.

## Decide by content

Do not classify a file from its name or directory alone. Read it and move each
lasting part to the document that owns that kind of information:

| Content | Owner |
| --- | --- |
| Audience, product promise, use moments, responsibility boundary, product-fit test | `docs/product.md` |
| Current modules, execution model, data flow, integrity, custody, information boundary | `docs/architecture.md` |
| Current commands, authoring, configuration, result interpretation, troubleshooting | `docs/guide/` |
| Precise product terms | `CONTEXT.md` |
| Published version changes | `docs/releases/` |
| Accepted behavior not implemented yet, with alternatives and acceptance evidence | `docs/design/` |
| Completed task plan, technical spike, temporary research, duplicated explanation | Delete after its conclusion reaches the owner above |

If one file contains several kinds of content, split by responsibility. Do not
keep the mixed file merely because part of it remains useful.

## Lifecycle

1. Discuss and research a proposal without creating a permanent record.
2. After the human accepts a cross-cutting direction, create one design record
   with `Status: Accepted` before delegating implementation.
3. During implementation, keep the record focused on unresolved behavior,
   boundaries, failure rules, and acceptance evidence. Do not add a daily log
   or completed task checklist.
4. When code and tests pass, merge each lasting conclusion into Product,
   Architecture, the affected guides, glossary, skills, and release-facing
   documentation as applicable.
5. Delete the completed design record in that same change. Git history keeps
   the development trail.

Do not mark a retained record `Implemented`. An implemented design remaining in
this directory is unfinished documentation cleanup.

Do not create `archive/`, `completed/`, or `history/` folders. They keep stale
instructions searchable and make a new session guess which answer is current.

## Required shape for an active design

Use one concrete user example before implementation details, then include:

1. `Status: Accepted` and acceptance date;
2. affected current documents;
3. **User example**;
4. **Problem**;
5. **Decision and product boundaries**;
6. **Visible interface and behavior**;
7. **Failure and integrity rules**;
8. **Acceptance evidence**; and
9. **Consequences and alternatives**.

The record must say which Product, Architecture, guide, glossary, skill, and
user-facing sections will change when implementation finishes. README and
guides must not describe the design as available while the record remains here.

## Completion check

Before deleting a completed record, verify all of these from current files:

- Code and tests implement the accepted behavior.
- Product definition still states the correct ownership boundary.
- Architecture explains the implemented structure and information flow.
- Guides explain every supported command, option, result, and diagnostic users
  need.
- Glossary, skills, README, and release notes are synchronized where affected.
- No remaining document links to the record being removed.
- Documentation checks and the relevant repository tests pass.

If a conclusion has no current owner, the design is not ready to delete. Add it
to the correct current document first; do not retain the whole work package as
a shortcut.
