# Product definition

ToppleCat is a delegation verification gate for Java/JUnit projects.

**AI accelerates implementation; humans strengthen verification. The delivery
behind an AI agent's done claim earns a current-run `PASS` only when every
required Gate passes.**

ToppleCat verifies the human-selected Executable Contract under the sealed
policy. It does not decide whether the upstream Spec is complete or grant
organizational approval.

## One delivery example

A Java developer asks an AI coding agent to implement a checkout discount. The
human selects the Spec, authors the public Acceptance Method and Typed Case
Rows, prepares reviewer-owned examples and optional Properties, and reads Spec
Review to confirm what will be checked. The agent implements against only the
public handoff and uses ordinary `./gradlew test` feedback.

After the agent claims completion, the same developer may act as the Reviewer.
Formal Verify runs the sealed public acceptance and every enabled Independent
Safeguard. ToppleCat records `PASS` only when every required Gate passes in the
current run; the Reviewer reads Verification Report and decides whether that
evidence is sufficient for the next delivery decision. Neither `PASS` nor
`FAIL` proves that no business rule was omitted.

The team may run ToppleCat locally, in CI, or through another workflow. That
placement does not change the product boundary.

## Audience and roles

ToppleCat serves Java/JUnit product teams that delegate implementation to AI
coding agents while a human remains accountable for acceptance.

- The **Reviewer** reads the prepared contract and current Verification Report,
  then makes the delivery decision. The Reviewer may be a developer, Spec
  owner, tester, or another accountable team member; ToppleCat does not require
  a separate QA role or prior knowledge of a producer's technical vocabulary.
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

Verification Report serves Reviewers with different technical backgrounds. Its
first layer uses reader outcomes such as `Passed`, `Problem found`, `Comparison
completed`, and `Unable to assess`; canonical Gate verdicts and producer terms
remain unchanged in collapsed technical evidence. A completed expected-result
comparison means that the comparison ran, not that expected and actual values
matched. Failed cases therefore lead with their authored input and structured
expected/actual differences before Scenario Steps and raw failures.

Each Verification Report opens every Acceptance Condition at that key-result
layer: its ID, title, status, plain-language verification result, and the five
safeguard outcomes. Reader details start closed, including for failed or
unreported ACs. The Reviewer can expand one AC to read all of its case-level
reader content, or use the report-wide control to expand and later return the
complete list to key results; technical evidence remains a separate deliberate
disclosure. Links to an AC or safeguard reveal the required reader content
before positioning the page, while the controls preserve the Reviewer’s place.

For either reading moment, the Reviewer may select `--language en` or
`--language zh-TW` on that command. English is the default. This changes only
ToppleCat-owned HTML presentation; it does not translate authored or
producer-owned text, select a different Delivery Scope, alter the Executable
Contract, or influence the Mechanical Seal, Current-run Evidence, Gates, or
safe Implementation Agent feedback.

## Public project page

The public project page gives Java developers an adoption-oriented view of the
same product boundary: an agent's done claim earns `PASS` only when every
required Gate passes in a current run. It explains the independent checks and
the Reviewer’s final responsibility in English and Traditional Chinese, then
points developers to the Java/JUnit installation path and source repository.
It is a public introduction, not another report, contract input, verification
surface, or source of organizational approval.

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
never supplies evidence for another and Mutation Testing is never blended into
a project-wide score.

An absent result keeps its reason:

- `DISABLED`: the sealed human policy turned the safeguard off;
- `NOT_APPLICABLE`: it was enabled but no declaration applied to the scope; or
- `INCOMPLETE`: trustworthy current-run evidence was required but not produced.

PIT owns its operator and outcome meanings. ToppleCat preserves PIT's raw terms,
attributes observations to exact public Acceptance Methods, and applies a
sealed ToppleCat Gate policy. A project's other PIT workflows remain outside
ToppleCat evidence.

Managed Mutation Testing also requires a passing Public Acceptance baseline.
When a public example already finds a problem, ToppleCat records Mutation
Testing as `INCOMPLETE`: it cannot truthfully say how the unchanged Acceptance
Method reacted to a temporary production change. Any producer output remains
reviewer technical context, not a Mutation verdict for that AC.
Verification Report presents that state as `Unable to assess` and explains that
the original Public Acceptance did not supply a passing baseline; it does not
present the state as a test-strength finding.

In Verification Report, Mutation Testing starts with the Reviewer question for
each selected AC: did its unchanged public Acceptance Method detect every
temporary production-behavior change attributed to it? An attributed mutation
that still passes that acceptance fails the AC and the aggregate Gate. Public
Acceptance, Hidden Tests, Expected Result Check, Property-Based Testing, and
Mutation Testing stay visibly separate in the same AC card. Raw PIT outcomes
and attribution remain available as reviewer technical evidence; they do not
become a claim that the unmodified program is correct in every case.

For an AC that misses attributed changes, the reviewer-facing Mutation Testing
section explains the total, detected, and undetected counts and then shows only
the undetected changes. Each detail identifies what changed, where the
production source was located when that context is unambiguous, and that the
AC's unchanged public acceptance still passed. Exact before/after wording is
shown only when PIT's description and the original source line support it;
otherwise the report states the limitation. A mutation globally marked
`KILLED` may still be listed for an AC when another AC supplied the killing
method. These details remain reviewer-only and do not affect the existing Gate
policy or safe agent feedback. A compact `ⓘ` control beside Mutation Testing,
attributed changes, undetected mutation, original source line, and descriptor
bridges unfamiliar terms to short ToppleCat-owned explanations. It supplements
the visible result; it does not replace the result, add evidence, or change the
meaning of PIT's recorded terms.

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
