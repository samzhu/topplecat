---
name: topplecat-product-design
description: Frame ToppleCat product behavior. Use when reviewing a change proposal, designing a feature or optimization, or preparing an implementation handoff; also use when another skill needs a Product Frame.
---

# ToppleCat product design

Build a **Product Frame** before discussing solutions. Treat the repository's
current-product documents, code, and tests as the source of truth; this skill
defines the design process, not product facts.

## 1. Reconstruct the current product

Read `DEVELOPMENT.md`, `CONTEXT.md`, and
`docs/product.md` completely, then follow the relevant task-map rows. The
Product definition is canonical for product fit, audience, use moments, and
responsibility boundaries.

For a supported-behavior change, also read `README.md`, `docs/architecture.md`,
`docs/guide/authoring.md`, and `docs/guide/verification-and-evidence.md`
completely.

Read `docs/design/README.md`, every relevant design record, and the affected
implementation seams and tests. Treat architecture and guides as implemented
behavior. Every retained design record is `Accepted` and describes intended
behavior only. Surface any conflict among documents, code, and tests before
continuing.

Complete this step only when the current behavior, authoritative input, human
responsibility, ToppleCat boundary, affected artifact or safeguard, information
audience, and existing implementation seam are all identified from evidence.

## 2. Present the Product Frame

Present this frame before proposing a solution or asking the user to repeat a
product decision. Keep each field to one or two sentences unless a source
conflict needs more detail:

- **Human problem and visible outcome**
- **Primary user and use moment**
- **Current implemented behavior**
- **Authoritative inputs and their human or external owner**
- **What ToppleCat owns**
- **What remains outside ToppleCat**
- **Affected safeguard, Gate, evidence, report, and audience**
- **Existing implementation seam**
- **Sources read**
- **Non-goals**
- **Product-fit verdict**
- **Open decisions that genuinely need the human**

Explain one concrete delivery example in plain language before technical
terms. Separate what an external tool observes, how ToppleCat attributes that
observation, and the resulting Gate verdict.

Complete this step only when every field has an evidence-backed statement or
is explicitly marked not applicable, and no question is already answered by
the repository.

## 3. Resolve the design branch

For an explanation or review, answer from the Product Frame without changing
the repository.

For a proposal, apply the canonical record's Product-fit gate first. State
which core use moment improves, why mechanical verification needs ToppleCat to
own the behavior instead of a human, project test, or external workflow, and
which authority or information boundary it affects. When the proposal belongs
elsewhere, name the correct owner and close the ToppleCat design branch.

For a product-fit proposal, compare the smallest viable designs with the same
concrete example. Preserve independent product dimensions and information
audiences. Use official primary sources only when an external tool's semantics
or another current fact is necessary, and preserve that source's official
terms.

Ask one to three concrete questions at a time only when the answer changes a
valid design. Continue until no unresolved choice would change visible
behavior, evidence meaning, policy ownership, or information exposure.

Complete this step only when the product-fit verdict is explicit and the
selected direction and rejected alternatives have explicit boundary and
evidence consequences.

## 4. Record and hand off the decision

When the human accepts a cross-cutting decision or requests implementation
delegation, use `docs/design/README.md` to find the owning topic record before
creating a file. Merge a narrow follow-up into that record; do not retain a
completed task plan, duplicate current guide, or searchable archive copy. Mark
the decision `Accepted`. After implementation, merge its lasting content into
Product, Architecture, guides, glossary, skills, and user-facing documentation,
then delete the record.

Give the implementation agent the purpose, visible behavior, existing seams,
failure and integrity rules, audience boundaries, acceptance evidence,
validation commands, and explicit non-goals.
Keep explanation-only and review-only requests read-only.

Complete this step only when an active design is decision-complete, the handoff
traces every requirement to an acceptance check, and no proposed behavior is
presented as already implemented.
