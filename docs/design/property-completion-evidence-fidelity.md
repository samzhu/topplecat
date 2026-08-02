# Property completed count evidence fidelity

**Status:** Implemented

**Date:** 2026-08-02

**Scope:** Verification Report corrective work only

## Problem Statement

### User example

一個交付範圍內有五個 public Properties。正式 Verify 執行後，四個 Property
留下完整且符合本次 sealed contract 的 `STARTED -> terminal` 證據；第五個事件雖然
使用同一個 Java method identity，卻只有 terminal，或帶著錯誤的 AC ID、舊的 source
digest，或重複 terminal。

Reviewer 應在 Verification Report 首頁看到四個已完成的 Properties，而不是五個。
第五個 Property 的證據不足以證明本次選取且 sealed 的 Property 已完整執行；若整體
evidence consistency 不成立，`PROPERTY` Gate 仍應忠實呈現 `INCOMPLETE`。

目前 completed count 只檢查 run ID、method identity 與 terminal state，然後以 method
identity 去重。因此下列不可信事件目前都會被計為一個完成的 Property：

- 只有 terminal、沒有 STARTED；
- AC ID 不符合 sealed Property；
- source digest 不符合 sealed Property；
- 同一個 Property 有重複 terminal；
- terminal 出現在 STARTED 之前。

這會造成 Evidence Fidelity 問題：Gate 可能正確顯示 `INCOMPLETE`，但同一份
Verification Report 的 Delivery Scope 卻宣稱該 Property 已完成。

## Solution

Verification Report 的 Property completed count 必須只計算具有一組可信本次執行
事件的有效 Property。每個 Property 必須同時符合以下條件，才計為一次完成：

1. Property 屬於本次有效 Delivery Scope；
2. run ID 符合 Current-run Evidence；
3. AC ID、完整 Java method identity 與 source digest 都符合 sealed Property
   definition；
4. 正好存在一個 `STARTED` 事件；
5. 正好存在一個 terminal 事件；
6. `STARTED` 在事件順序中先於 terminal；
7. terminal event 與其 `PropertyResult` 的 AC ID、method identity 及 state 一致。

terminal outcome 可以是通過、找到 counterexample，或完整結束但 evidence outcome
為 incomplete。completed count 只回答「有幾個符合本次 contract identity 的 Property
走到唯一終態」，不代表有幾個通過，也不取代 `PROPERTY` Gate verdict。

缺少 completion marker、JUnit XML 缺失或 aggregate counts 不一致，仍依既有規則使
`PROPERTY=INCOMPLETE`。只要 Property sidecar 中的事件配對本身可信，其 completed
count 仍可忠實呈現；marker 與 XML 不得被誤用為 Property identity。

## User Stories

1. As a reviewer, I want the Verification Report to count only Properties proven to belong to the current sealed delivery, so that the reported execution scope is trustworthy.
2. As a reviewer, I want a Property with a valid counterexample terminal event to count as completed, so that completion is not confused with passing.
3. As a reviewer, I want a Property with a valid incomplete terminal event to count as completed execution while the Gate remains `INCOMPLETE`, so that execution completion and evidence sufficiency stay distinct.
4. As a reviewer, I want a terminal-only event to count as zero, so that a missing start cannot be presented as a complete execution lifecycle.
5. As a reviewer, I want a STARTED-only event to count as zero, so that interrupted execution is not presented as completed.
6. As a reviewer, I want duplicate STARTED or terminal events to count as zero for that Property, so that ambiguous evidence is never collapsed into a successful completion claim.
7. As a reviewer, I want terminal-before-STARTED evidence to count as zero, so that impossible event ordering is not accepted.
8. As a reviewer, I want an event with the wrong run ID to count as zero, so that stale runs do not enter Current-run Evidence.
9. As a reviewer, I want an event with the wrong AC ID to count as zero, so that execution is not attributed to a different Acceptance Condition.
10. As a reviewer, I want an event with a method-name or overload mismatch to count as zero, so that only the exact executable Property is recognized.
11. As a reviewer, I want an event with a stale source digest to count as zero, so that execution of different Property bytes is not presented as the sealed Property.
12. As a reviewer, I want an out-of-scope Property event to remain visible only as an evidence inconsistency and not increase the selected Delivery Scope count.
13. As a reviewer, I want one valid Property to remain counted when another extra or malformed Property makes the Gate incomplete, so that the report preserves what the evidence can still prove without overstating it.
14. As a maintainer, I want completed count and Property Gate assessment to use the same identity and lifecycle rules, so that two projections cannot disagree about whether an event belongs to the current contract.
15. As a maintainer, I want the count projected through the existing Verification Report model, so that no second report-specific evidence parser is introduced.
16. As an implementation agent, I want safe feedback boundaries to remain unchanged, so that reviewer-only Property details do not leak through `agent-feedback.json`.

## Implementation Decisions

- Keep one evidence-assessment seam. The existing Property evidence assessment remains the
  single owner of event parsing, sealed-definition identity matching, lifecycle validation,
  Property results, Gate verdict, and completed count.
- Do not add another counter in the report renderer, report model mapper, Gradle task, or
  JavaScript. The Verification Report receives the completed count produced by the same
  assessment that produced the `PROPERTY` Gate result.
- Model a valid completed Property as exactly one ordered pair: one matching `STARTED`
  followed by one matching terminal event.
- Match the complete sealed identity: current run ID, AC ID, complete Java method identity,
  and source digest. Class-only, method-name-only, display-name, or AC-only matching is not
  allowed.
- Validate the terminal `PropertyResult` against its containing terminal event. A mismatch
  makes that Property evidence inconsistent and prevents it from increasing completed count.
- Count each valid sealed Property at most once.
- An invalid selected Property contributes zero to completed count. Other valid selected
  Properties may still contribute one each even when the total Gate becomes `INCOMPLETE`.
- Preserve existing Property verdict semantics. A valid counterexample remains `FAIL`; a
  valid pass remains eligible for `PASS`; incomplete, missing, duplicate, mismatched,
  malformed, extra, or aggregate-inconsistent evidence retains the existing
  `INCOMPLETE` behavior.
- Preserve the distinction between event identity and JUnit aggregate validation. JUnit XML
  supplies only test/executed/failure/skipped consistency and is not used to discover Java
  method identity.
- Keep the current schemas, Gradle tasks, Property authoring API, Delivery Scope model,
  Verification Report layout, safe feedback format, and information boundary unchanged.
- Do not change the wording or meaning of Hidden Tests, Mutation Testing, Mechanical Seal,
  or any other Gate.

## Testing Decisions

- Use the existing Property evidence assessment as the primary and only behavioral test seam.
  Tests provide sealed `PropertyDefinition` values plus run-sidecar events and assert both the
  Gate verdict and completed count returned from the same assessment.
- Test observable evidence behavior, not private helper methods. The completed-count helper
  must not become a new testing API.
- Use a table or parameterized test covering at least:
  - valid `STARTED -> COMPLETED_PASS`: count 1;
  - valid `STARTED -> COMPLETED_COUNTEREXAMPLE`: count 1 and Gate `FAIL`;
  - valid `STARTED -> COMPLETED_INCOMPLETE`: count 1 and Gate `INCOMPLETE`;
  - STARTED only: count 0;
  - terminal only: count 0;
  - duplicate STARTED: count 0;
  - duplicate terminal: count 0;
  - terminal before STARTED: count 0;
  - wrong run ID: count 0;
  - wrong AC ID: count 0;
  - wrong complete method identity or overload: count 0;
  - wrong source digest: count 0;
  - terminal event/result identity or state mismatch: count 0;
  - one valid selected Property plus an extra out-of-scope event: count 1 and Gate
    `INCOMPLETE`.
- Extend the existing functional Verification Report coverage only to prove that the count
  returned by assessment reaches the report unchanged. Do not reproduce the complete event
  validator in a report DOM test.
- Keep the existing five-Property happy-path functional case and assert both five structured
  Property results and a visible completed count of five.
- Add one inconsistent-evidence functional case that demonstrates the report does not show
  an invalid Property as completed while the Gate is `INCOMPLETE`.
- Run the narrow Property assessment and report functional tests first, followed by:

  ```bash
  ./gradlew check
  GRADLE_CMD=./gradlew scripts/verify-release.sh
  python3 scripts/verify-docs.py
  git diff --check
  ```

## Out of Scope

- Redesigning Spec Review or Verification Report layout, colors, navigation, wording, or
  responsive behavior.
- Renaming the existing Property count or adding new counters, charts, dashboards, or scores.
- Changing `PROPERTY` Gate semantics beyond making completed count use the same trusted event
  identity and lifecycle evidence.
- Changing Property generators, trials, shrinking, replay, counterexample reporting, or JUnit
  execution.
- Changing Property event or results schemas.
- Changing Mechanical Seal, Hidden Tests, Mutation Testing, PIT attribution, thresholds, or
  aggregate verdict rules.
- Adding a Gradle DSL, matcher API, CLI, compatibility reader, migration, or new module.
- Re-running or rewriting historical `topplecat-vif` evidence.
- Performing a release, version change, commit, tag, push, Maven publication, or GitHub Issue
  update as part of this corrective implementation.

## Further Notes

- This correction follows the existing **Evidence Fidelity** rule: generated JSON and HTML
  must project what Current-run Evidence can prove and must not reinterpret partial or stale
  evidence as a completed selected Property.
- A simpler terminal-only count was rejected because it cannot distinguish a valid current
  Property lifecycle from wrong-AC, stale-digest, duplicate, or terminal-only evidence.
- Counting only `COMPLETED_PASS` was also rejected because completed execution and successful
  outcome are different facts. Counterexamples and incomplete terminal outcomes must remain
  faithfully visible without being renamed as passes.
- Code, tests, and current-product explanations now agree; this record therefore documents
  implemented behavior.
