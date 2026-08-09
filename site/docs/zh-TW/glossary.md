---
title: Glossary
description: ToppleCat 對 executable contract、safeguard、evidence 與 delivery boundary 的正式詞彙。
page_id: glossary
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../glossary/
alternate_language: en
alternate_label: English
alternate_en: ../glossary/
alternate_zh_tw: ./glossary/
markdown_url: glossary.md
copy_label: Copy Markdown
copied_label: Copied
---

# Glossary

以下是 ToppleCat 共用的正式語言，描述 executable acceptance boundary，不是新的
authoring language。

## Executable Contract {#executable-contract}

人撰寫的 Acceptance Method 與 Typed Case Row，定義 ToppleCat 要機械化驗證什麼。規則與
案例是否正確且完整，仍由人負責。

## Acceptance Condition

由外部選定、穩定的 `AC-...` 規則，ToppleCat 將它綁到 executable acceptance work。
ToppleCat 不會自己發明遺漏的規則。

## Acceptance Method

一個公開的 Java/JUnit method，把 Acceptance Condition 綁到 executable examples，並描述
它的 Scenario。

## Scenario、Stage 與 Step

Scenario 是一個 Typed Case Row 的 Given、When、Then、And 有序執行。Stage 是可重用的
business-capability object，提供相關 Steps，並在一個 Scenario 中保存一般狀態。Step 是
Scenario 中選定的一個業務動作或觀察。

## Typed Case Row

人撰寫的 JSON 或 YAML example，包含 AC ID、inputs 與 expected results。產生的 trial 不是
Typed Case Row。

## Independent Safeguard {#independent-safeguard}

本次執行的 evidence 只回答自身問題、不能由另一個 safeguard 取代的防線。Hidden Tests、
Property-Based Testing 與 Mutation Testing 彼此分開。

## Mutation Attribution

把 Mutation Testing observation 對應到實際覆蓋它的 public Acceptance Method 與
Acceptance Condition。PIT 的正式 outcome 名稱維持不變。

## Mechanical Seal {#mechanical-seal}

對完整 executable contract 與 verification policy 的 content-based integrity record。它
確認一致性，不等於人或組織 approval。

## Current-run Evidence 與 Aggregate Verdict

Current-run Evidence 是 active formal verification run 產生的 evidence。Aggregate Verdict
是 selected Delivery Scope 的 `PASS`、`FAIL` 或 `INCOMPLETE` 結論。`PASS` 是對已檢查範圍
的證據，不是 proof 或 sign-off。

## Spec Review 與 Verification Report

Spec Review 是 handoff 前、reviewer-only 的 projection。Verification Report 是一次正式
Verify run 與其 diagnostics 的 reviewer-only projection。兩者都不是 Implementation Agent
handoff，也不是公開的 actual-delivery report。

請看 [Architecture](architecture.md#information-boundary) 了解這些詞背後的 ownership
與資訊流。
