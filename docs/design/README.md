# Product design records

This directory keeps the few product decisions that need more detail than a
standing agent rule and are not fully explained by the current architecture.

For example, suppose one delivery changes checkout while the repository also
contains older coupon and cancellation Specs. A design record can explain which
acceptance conditions ToppleCat should execute, why Hidden Tests and Mutation Testing
have different scopes, what the command should look like, and how a changed
Spec is detected. `AGENTS.md` should only tell an agent to follow that decision;
it should not repeat the whole design.

## Where each kind of information belongs

| Information | Canonical location |
| --- | --- |
| Rules every coding agent must follow | Repository or nested `AGENTS.md` |
| Repository map and commands | `DEVELOPMENT.md` |
| Current implemented product boundaries and data flow | `docs/architecture.md` |
| Current user workflow and supported configuration | `README.md` and `docs/guide/` |
| A significant decision, its example, alternatives, and consequences | `docs/design/` |
| What changed in a published version | `docs/releases/` |
| Executable behavior | Java/JUnit tests and typed JSON/YAML case rows |

## Status

Every retained design record is decision-complete before implementation. A new
decision starts as **Accepted** while its implementation is in progress. Change
it to **Implemented** only after the code, tests, architecture, guides, and
other current-product documentation have been synchronized. Do not describe an
Accepted-only record as supported current behavior.

## Required structure

Use a concrete user example before technical details, then record:

1. **Status and date**
2. **User example**
3. **Problem**
4. **Decision and product boundaries**
5. **Visible interface and behavior**
6. **Failure and integrity rules**
7. **Acceptance evidence**
8. **Consequences and alternatives**

The record should be decision-complete before implementation is delegated.
After implementation, synchronize the current-product documents in the same
change. Move the lasting conclusion into the formal record; do not retain
step-by-step delivery notes or one-time technical spikes.

This structure follows three complementary practices:

- [OpenAI's `AGENTS.md` guidance](https://learn.chatgpt.com/docs/agent-configuration/agents-md.md)
  uses repository and nested files for durable instructions close to the work.
- [GitHub's repository-instruction guidance](https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/add-custom-instructions/add-repository-instructions)
  likewise separates repository-wide and path-specific agent instructions.
- [Google Cloud's decision-record guidance](https://docs.cloud.google.com/architecture/architecture-decision-records)
  keeps the context, alternatives, decision, and consequences near the
  codebase so later contributors can understand why a choice was made.

## Current records

- [Executable acceptance boundary](executable-acceptance-boundary.md)
- [Property-Based Testing safeguard](property-based-testing.md)
- [ToppleScenario authoring](topple-scenario-authoring.md)
- [Independent safeguard results](independent-safeguard-results.md)
- [Mutation attribution and gate](mutation-attribution.md)
- [Managed mutation profile and verification evidence](managed-mutation-profile.md)
- [Contract quality advisory](contract-quality-advisory.md)

Use the root [context glossary](../../CONTEXT.md) for shared terms.
