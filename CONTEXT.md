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
acceptance work detects the change. Its evidence is independent of other
safeguards.

**Mutation Attribution／突變歸因**:
ToppleCat's mapping of a Mutation Testing result to the exact public Acceptance
Method and Acceptance Condition whose execution covered it. Attribution
preserves the producer's mutation outcome and does not infer business meaning
from missing coverage.

**ToppleCat Managed Mutation Profile／ToppleCat 託管突變設定**:
The versioned, product-owned PIT version and exact operator set used by formal
Verify's Mutation Testing producer. Other PIT workflows remain outside
ToppleCat evidence and Gates.
_Avoid_: user-configured formal producer, universal AI mutator standard

**Contract Quality Advisory／契約品質提醒**:
A reviewer-only, non-blocking reminder of a possible expected-projection
quality risk. It does not infer business rules, affect the Seal, Verify evidence,
or any Gate, and never appears in the public report or `agent-feedback.json`.

**Property-Based Testing／性質導向測試**:
The safeguard that exercises a human-approved invariant with bounded generated
inputs. It is testing evidence, not proof and not a hidden variant.
_Avoid_: a reviewer-specific generated invariant

**Independent Safeguard／獨立防線**:
A safeguard whose current-run evidence answers only its own question and cannot
be replaced by another safeguard's evidence. After contract integrity passes,
every enabled Independent Safeguard produces its own result even when an
earlier safeguard fails.
_Avoid_: a passing gate used as coverage for a different gate

## Delivery and evidence

**Evidence Fidelity／證據忠實性**:
The requirement that ToppleCat projections preserve observed contract and
producer outcomes without adding, omitting, renaming, or reinterpreting them.
A failing or incomplete result remains valid evidence when it truthfully
records what happened.

**Delivery Scope／交付範圍**:
The Acceptance Conditions selected by the human or external workflow for one
verification run. It names what is being verified, not a task lifecycle.

**Mechanical Seal／機械封印**:
The content-based integrity record over the selected executable contract and
verification policy. It confirms consistency, not human or organizational
approval.
_Avoid_: sign-off, approval decision

**Reviewer Custody／審閱者保管**:
The reviewer-controlled local holding area for Hidden Tests and their
Mechanical Seal. It is custody, not encryption or a security boundary.

**Current-run Evidence／本次執行證據**:
The evidence produced by the active formal verification run. Archived output
is diagnostic material and cannot replace it.
