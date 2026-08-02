# Product definition

ToppleCat is a delegation verification gate for Java/JUnit projects.

**AI accelerates implementation; humans strengthen verification. ToppleCat
turns an AI agent's done claim into current-run evidence that a human can
inspect.**

ToppleCat verifies the human-selected Executable Contract under the sealed
policy. It does not decide whether the upstream Spec is complete or grant
organizational approval.

## One delivery example

A Java developer asks an AI coding agent to implement a checkout discount. The
human selects the Spec, authors the public Acceptance Method and Typed Case
Rows, prepares reviewer-owned examples and optional Properties, and reads Spec
Review to confirm what will be checked. The agent implements against only the
public handoff and uses ordinary `./gradlew test` feedback.

After the agent claims completion, the same developer may act as the Reviewer:
run formal Verify, read Verification Report, and decide whether the evidence is
sufficient to submit or accept the change. A ToppleCat `PASS` can support an
accept recommendation and `FAIL` can support a reject recommendation. The
human makes the final decision, and neither result proves that no business rule
was omitted.

The team may run ToppleCat locally, in CI, or through another workflow. That
placement does not change the product boundary.

## Audience and roles

ToppleCat serves Java/JUnit product teams that delegate implementation to AI
coding agents while a human remains accountable for acceptance.

- The **Reviewer** reads the prepared contract and current Verification Report,
  then makes the delivery decision. The Reviewer may be the developer, Spec
  owner, or another team member; ToppleCat does not require a separate QA role.
- The **Implementation Agent** receives the public contract and safe Gate-level
  feedback. It never receives reviewer-owned source, values, HTML reports,
  paths, counterexamples, replay material, raw failures, or PIT details.
- The **External Workflow** selects the current Spec, decides when and where
  ToppleCat runs, manages delivery history, and applies organizational policy.

## Two core use moments

1. **Before implementation handoff:** Spec Review lets the human read the
   complete selected Spec and the executable material that will be checked. It
   contains no execution verdict.
2. **After the agent's done claim:** formal Verify produces Current-run Evidence
   and Verification Report so the human can judge the delivery before accepting
   it or submitting a PR.

Both HTML reports are human, reviewer-only reading surfaces. Java/JUnit
Acceptance Methods and Typed Case Rows remain the Executable Contract;
generated JSON and HTML are projections.

For either reading moment, the Reviewer may select `--language en` or
`--language zh-TW` on that command. English is the default. This changes only
ToppleCat-owned HTML presentation; it does not translate authored or
producer-owned text, select a different Delivery Scope, alter the Executable
Contract, or influence the Mechanical Seal, Current-run Evidence, Gates, or
safe Implementation Agent feedback.

## Responsibility boundary

| ToppleCat owns | The human, team, project, or external workflow owns |
| --- | --- |
| Binding externally selected ACs to ordinary Java/JUnit acceptance work | Selecting the current Spec and making its rules and examples complete |
| Checking and mechanically sealing contract bytes and verification policy | Organizational review, approval, delivery history, and sign-off |
| Fresh formal verification, independent Gate results, and current-run evidence | Deciding who runs ToppleCat and whether it runs locally, in CI, or elsewhere |
| Reviewer-only reports and safe implementation-agent feedback | PR creation, merge enforcement, task management, and Spec lifecycle |
| The fixed managed PIT profile, exact Acceptance Method attribution, and Mutation Gate policy | Ordinary unit/QA tests, custom PIT, performance, and security programs |
| Plaintext Reviewer Custody as a mechanical handoff safeguard | Encryption, hostile-process isolation, CI isolation, and operating-system security |

ToppleCat starts at the executable acceptance boundary. It is not an AI
development platform, task or Spec manager, CI product, general test framework,
approval system, or security boundary.

## Independent evidence

Hidden Tests, Property-Based Testing, and Mutation Testing answer different
questions. They may appear together in Verification Report, but one result
never supplies evidence for another and the results are never blended into a
quality score.

An absent result keeps its reason:

- `DISABLED`: the sealed human policy turned the safeguard off;
- `NOT_APPLICABLE`: it was enabled but no declaration applied to the scope; or
- `INCOMPLETE`: trustworthy current-run evidence was required but not produced.

PIT owns its operator and outcome meanings. ToppleCat preserves PIT's raw terms,
attributes observations to exact public Acceptance Methods, and applies a
sealed ToppleCat Gate policy. A project's other PIT workflows remain outside
ToppleCat evidence.

## Product-fit test

A proposed capability belongs in ToppleCat only when all four answers are yes:

1. Does it improve the human's pre-handoff contract review or post-done
   verification of an AI delivery?
2. Does mechanical verification require ToppleCat to own it, rather than a
   human decision, project test, or external workflow?
3. Does it preserve human authority, authoritative inputs, Evidence Fidelity,
   Independent Safeguards, and reviewer/agent information boundaries?
4. Does it fit the four-module Java/JUnit product without another authoring
   language, CLI, compatibility surface, workflow manager, or development
   platform?

When a proposal fails this test, record the correct owner instead of expanding
ToppleCat around it.

## Documentation ownership

| Document | Responsibility |
| --- | --- |
| Root README | First-time understanding, shortest useful example, and adoption-critical boundaries |
| Product definition | Audience, product promise, use moments, ownership boundary, and product-fit test |
| Architecture and guides | Current supported behavior, data flow, commands, diagnostics, and safety rules |
| Design workspace | Accepted but not-yet-implemented decisions only |
| Context glossary | One precise meaning for each ToppleCat domain term |
| Repository skills | Repeatable agent process and pointers to authoritative facts |

The README remains an entrance rather than a reference manual. Exact operators,
attribution formulas, event lifecycles, custody paths, schemas, proxy
constraints, and configuration details belong in Architecture or the relevant
guide.
