---
title: Verification and evidence
description: 執行 ToppleCat 正式驗證，分開 observation、契約 attribution 與 Gate verdict。
page_id: verification-and-evidence
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../verification-and-evidence/
alternate_language: en
alternate_label: English
alternate_en: ../verification-and-evidence/
alternate_zh_tw: ./verification-and-evidence/
markdown_url: verification-and-evidence.md
copy_label: Copy Markdown
copied_label: Copied
---

# Verification and evidence

## 一個交付案例 {#delivery-example}

假設 checkout 契約規定訂單滿 1,000 元折 100 元。正式 Verify 時，public Acceptance
Method 會執行這筆人寫的案例，啟用的 safeguards 也各自執行。如果 managed mutation
暫時改變折扣門檻，而同一個 public method 仍然通過，Mutation Gate 取得的證據是：
這個 AC 沒有分辨出那次暫時變更。這和宣稱原本 production program 已經有這個錯，是兩
件不同的事。

## 三層證據 {#three-evidence-layers}

每個結果都要分三層讀：

1. **External observation：** JUnit task、Property engine 或 managed PIT producer
   記錄它看見的事情，並保留 producer 自己的正式 outcome 名稱。
2. **Contract attribution：** ToppleCat 把 observation 連到擁有這個問題的 public
   Acceptance Method、Typed Case Row、Property declaration 或 sealed policy。
3. **ToppleCat Gate verdict：** sealed policy 決定 safeguard 是 `PASS`、`FAIL`、
   `INCOMPLETE`、`DISABLED` 或 `NOT_APPLICABLE`，最後 aggregate run 記錄 `PASS`、
   `FAIL` 或 `INCOMPLETE`。

產生的 JSON 與 HTML 只 projection 已檢查的契約內容與 producer outcome；不會加入新的
規則、案例、expected value 或 Scenario step。

## 執行正式 workflow

開發回饋仍是一般的 `./gradlew test`。CI 的正常命令是：

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
./gradlew test
./gradlew toppleCatVerify
```

Verify 預設涵蓋完整 Executable Contract。Reviewer 若想快速查看，可以重複傳入
`--spec` 或重複傳入 `--ac AC-...`，但不能混用。Seal 與 integrity 永遠涵蓋完整契約。

## Gates 與 verdicts {#gates-and-verdicts}

正式 Gate 順序是：

```text
CONTRACT_INTEGRITY
JUNIT
REVIEWER_JUNIT
EXPECTED_CONSUMPTION
PROPERTY
MUTATION
```

Hidden Tests、Property-Based Testing 與 Mutation Testing 是 Independent Safeguards。
Hidden row 不能代替 Property；Property 不能提供 mutation detection。Mutation Testing
還需要 Public Acceptance 通過作為 baseline，否則它的結果是 `INCOMPLETE`。

`PASS` 代表這次執行中 sealed policy 要求的每個 Gate 都通過。這是對已檢查契約的證據，
不是規則完整性的 proof，也不是 organizational approval。Scoped `PASS` 只限它所選的
Delivery Scope。

## Reviewer 邊界 {#reviewer-boundary}

Spec Review 與 Verification Report 是 reviewer-only 的 HTML surface。給
Implementation Agent 的 safe feedback 只有 Gate-level 原因，不含 reviewer 值、source
name、path、token、counterexample 或 raw private failure。公開網站可以用清楚標示的
synthetic demonstration 做教育，但這份文件不發布任何實際交付資料。

遇到結果不完整或出乎預期，先讀 [Troubleshooting](troubleshooting.md#symptom-map)。
[Architecture](architecture.md#execution-flow) 說明各種 evidence 在哪裡產生與保留。
