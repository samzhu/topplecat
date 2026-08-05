# ToppleCat context

ToppleCat's shared language for its executable acceptance boundary. This is a
glossary of product concepts, not an implementation guide or delivery plan.

## Executable contract

**Executable Contract／可執行契約**:
The human-authored Acceptance Methods and Typed Case Rows that define what
ToppleCat mechanically verifies. Humans remain responsible for whether its
business rules and examples are correct and complete.
_Avoid_: BDD executable file, generated report, inferred requirement

**Acceptance Condition (AC)／驗收條件**:
A stable, externally chosen `AC-...` rule that ToppleCat binds to executable
acceptance work. ToppleCat does not invent an omitted rule.
_Avoid_: requirement guessed from tests, canonical test

**Acceptance Method／驗收方法**:
The one public Java/JUnit method that binds an Acceptance Condition to its
executable examples. It describes the Scenario for that condition.
_Avoid_: `ToppleTest`, canonical test

**Scenario／情境**:
One ordered Given, When, Then, and And execution of an Acceptance Method for
one Typed Case Row. It is the whole narrative, not a name for a capability.

**Stage／階段能力**:
A reusable business-capability object that supplies related Steps and holds
ordinary state for one Scenario execution. A Stage is neither a narrative phase
nor a separate Scenario.
_Avoid_: phase field, Scenario parameter

**Step／步驟**:
One business action or observation selected within a Scenario. A Step belongs
to the executable contract even though its implementation is ordinary Java.

**Typed Case Row／型別案例資料列**:
An authored JSON or YAML example containing an AC ID, inputs, and expected
results. It is contract input; a generated trial is not a Typed Case Row.

## Verification safeguards

**Hidden Tests／隱藏測試**:
Reviewer-controlled Typed Case Rows that run an existing public Acceptance
Method with independently chosen examples. They do not create private rules.
_Avoid_: a Property presented as hidden coverage

**Mutation Testing／突變測試**:
The safeguard that changes production behavior and measures whether public
acceptance work detects the change. For a selected AC, its unchanged public
Acceptance Method must detect every mutation exactly attributed to that AC;
its evidence is independent of other safeguards.

**Mutation Attribution／突變歸因**:
ToppleCat's mapping of a Mutation Testing result to the exact public Acceptance
Method and Acceptance Condition whose execution covered it. Attribution
preserves the producer's mutation outcome and does not infer business meaning
from missing coverage.

The Verification Report may list an undetected mutation for an AC when that
AC's public method covered it but still passed, even if PIT globally reports
`KILLED` because another AC detected it. Source coordinates and original source
lines are reviewer-only diagnostics; an exact replacement is shown only when
the producer description and unique source context establish it.

**ToppleCat Managed Mutation Profile／ToppleCat 託管突變設定**:
The versioned, product-owned PIT version and exact operator set used by formal
Verify's Mutation Testing producer. Other PIT workflows remain outside
ToppleCat evidence and Gates.
_Avoid_: user-configured formal producer, universal AI mutator standard

**Contract Quality Advisory／契約品質提醒**:
A reviewer-only, non-blocking reminder of a possible expected-projection
quality risk. It does not infer business rules, affect the Seal, Verify evidence,
or any Gate, and never appears in public handoff material or
`agent-feedback.json`.

**Property-Based Testing／性質導向測試**:
The safeguard that exercises a human-approved invariant with bounded generated
inputs. It is testing evidence, not proof and not a hidden variant.
_Avoid_: a reviewer-specific generated invariant

**Property Discard Evidence／Property 捨棄證據**:
The canonical JSON choices for every generated input rejected before it became
a completed Property trial. ToppleCat retains it as current-run evidence with
a neutral explanation; it is not a new business rule or a Typed Case Row.

**Independent Safeguard／獨立防線**:
A safeguard whose current-run evidence answers only its own question and cannot
be replaced by another safeguard's evidence. After contract integrity passes,
Hidden Tests and Property-Based Testing produce their own result even when
Public Acceptance fails. Mutation Testing additionally needs a passing Public
Acceptance baseline; otherwise its result is `INCOMPLETE`.
_Avoid_: a passing gate used as coverage for a different gate

## Delivery and evidence

**Spec Review／規格審閱**:
A reviewer-only, human-readable projection shown before implementation
verification. It presents each complete Selected Spec Document together with
the public and reviewer-owned Executable Contract material bound to its
Acceptance Conditions. It contains no execution result and does not judge
whether the upstream Spec is complete.
_Avoid_: Contract Review, Public Spec, Living Documentation

**Selected Spec Document／已選規格文件**:
A repository-relative Markdown document selected by the human or external
workflow for Spec Review. Its complete contents belong in that Review. Verify
may also derive a scoped Delivery Scope from every `AC-...` identifier in one
or more selected documents. ToppleCat does not manage the document's lifecycle.
_Avoid_: partially selected Spec, ToppleCat-managed requirement

**Verification Report／驗證報告**:
The reviewer-only, human-readable projection of one formal Verify run and its
diagnostics. It is distinct from machine-readable Current-run Evidence and safe
agent feedback.
_Avoid_: Verification Evidence, blended quality report

**Evidence Fidelity／證據忠實性**:
The requirement that ToppleCat projections preserve observed contract and
producer outcomes without adding, omitting, renaming, or reinterpreting them.
A failing or incomplete result remains valid evidence when it truthfully
records what happened.

**Delivery Scope／交付範圍**:
The Acceptance Conditions covered by one verification run. A normal Verify
covers the complete contract; a scoped Verify gets its ACs from selected Spec
documents or explicit AC IDs, but never both. It names what is being verified,
not a task lifecycle.

**Mechanical Seal／機械封印**:
The content-based integrity record over the complete executable contract and
verification policy. It confirms consistency, not human or organizational
approval.
_Avoid_: sign-off, approval decision

**Reviewer Custody／審閱者保管**:
The reviewer-controlled local holding area for Hidden Tests and their
Mechanical Seal. It is custody, not encryption or a security boundary.

**Current-run Evidence／本次執行證據**:
The evidence produced by the active formal verification run. Archived output
is diagnostic material and cannot replace it.

**Aggregate Verdict／彙總判定**:
The current formal run's `PASS`, `FAIL`, or `INCOMPLETE` conclusion for the
selected Delivery Scope. `PASS` means every required Gate passed under the
sealed policy in this run; it is evidence, not proof that the business rules
are complete, an acceptance recommendation, or organizational approval.
For a non-empty Delivery Scope, Verification Report states this restriction
beside its `PASS` conclusion.
_Avoid_: delivery recommendation, sign-off, proof of correctness

## People and orchestration

**Reviewer／審閱者**:
The human accountable for reading the prepared Executable Contract and the
current Verification Report, then deciding whether to accept the delivery. The
Reviewer may be the developer, Spec owner, or another team member; ToppleCat
does not define a separate organizational role or grant approval.
_Avoid_: mandatory QA department, automated approver

**Implementation Agent／實作代理**:
The AI coding agent that implements against the public handoff and may receive
safe Gate-level feedback. It does not receive reviewer-owned contract material
or either reviewer-only HTML report.
_Avoid_: Reviewer, approval authority

**Public Product Demonstration／公開產品示範**:
A clearly labelled, fully synthetic red-team example for human visitors to the
project page. It explains what ToppleCat can observe and which Gate rejects the
synthetic delivery; it is neither an Implementation Agent handoff nor a
Verification Report, Current-run Evidence, or approval for an actual delivery.
Synthetic report details may be shown only for that explanatory purpose.
_Avoid_: public export of a real delivery, safe agent feedback

**External Workflow／外部工作流程**:
The human or automation that chooses the current Spec, decides when and where
ToppleCat runs, manages delivery history, and applies organizational policy.
ToppleCat supplies verification mechanisms and evidence, not workflow
orchestration.
_Avoid_: ToppleCat-managed task or Spec lifecycle
