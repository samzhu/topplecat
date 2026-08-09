---
title: Architecture
description: 認識 ToppleCat 的四個模組、執行流程、evidence、custody 與資訊邊界。
page_id: architecture
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../architecture/
alternate_language: en
alternate_label: English
alternate_en: ../architecture/
alternate_zh_tw: ./architecture/
markdown_url: architecture.md
copy_label: Copy Markdown
copied_label: Copied
---

# Architecture

## 四個模組 {#four-modules}

| Module | Responsibility |
| --- | --- |
| `topplecat-core` | Case、evidence、custody、Property 與 safe-feedback model |
| `topplecat-junit` | Acceptance annotation、typed row、compiler-described Scenario/Stage proxy、expected consumption 與 Property |
| `topplecat-report` | Reviewer-only Spec Review 與 Verification Report projection |
| `topplecat-gradle-plugin` | Command、task wiring、scope、custody、integrity 與 mutation orchestration |

repository 維持這四個 product module。Samples 與 maintainer validation infrastructure
不是額外的 ToppleCat product module。

## Contract authority {#contract-authority}

一般 Java/JUnit Acceptance Method 與型別化 JSON/YAML 案例列是 authoritative。每個 AC 由
一個公開 `@ToppleAcceptanceTest("AC-...")` 綁定；compiler 決定 Scenario phase、Stage
selection、overload identity 與 rendered Step。公開案例在 `PUBLIC_ONLY` mode 執行，hidden
案例在 `HIDDEN_ONLY` mode 重用同一個 method。產生的 JSON 與 HTML 是 projection。

`@ToppleProperty` 是一個獨立的公開 declaration，用來表達人核准的 invariant。產生的
choices 屬於本次執行 evidence，不會變成案例列或 hidden contract input。

## Execution flow {#execution-flow}

```text
ordinary ./gradlew test
    -> public project tests and public acceptance methods

./gradlew toppleCatVerify
    -> current public acceptance
    -> enabled Hidden Tests, Properties, Expected Consumption, Mutation Testing
    -> Current-run Evidence and reviewer reports
```

Contract Integrity 先把完整契約與 verification policy 和 Mechanical Seal 比對。通過後，
獨立 safeguards 依固定順序執行。Public Acceptance 失敗不會消除獨立的 Hidden 或 Property
evidence；Mutation Testing 則因為沒有可信 baseline 而無法完成。

## Custody 與 integrity

`toppleCatSeal` 把 reviewer-owned source 放在本機 plaintext mechanical custody，並對完整
契約與 policy 建立 integrity seal。Custody 不是 encryption、hostile-process isolation、
CI isolation 或 operating-system security boundary。Verify 重用既有 seal，不建立 approval
也不更新它。

## Information boundary {#information-boundary}

Spec Review 與 Verification Report 是 reviewer-only、給人閱讀的 projection。安全的
Implementation Agent feedback 只有 Gate-level remediation，不含 reviewer 值、identifier、
path、source name、token、raw private failure 或 Property trial material。公開專案頁可以
用標示清楚的 synthetic demonstration 做教育，但不能放實際交付。

產品 owner 與 use moment 請看 [Product definition](product-definition.md#responsibility-boundary)。
正式詞彙請看 [Glossary](glossary.md#independent-safeguard)。
