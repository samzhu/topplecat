---
title: 系統如何運作
description: 從 ToppleCat 的四個 Java 模組追蹤契約、檢查、證據與資訊邊界。
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

# 系統如何運作

想知道正式結果為什麼可信、Reviewer 私有資料去了哪裡，或某項技術行為由哪個模組
負責時，再讀這一頁。只想先看產品效果，請從[開始使用](getting-started.md#sample-workflow)
開始。

## 從規則到證據 {#execution-flow}

```text
人選定規則與例子
    -> 公開的 Java/JUnit 可執行契約
    -> Reviewer 確認並封存完整契約
    -> AI agent 使用一般測試回饋完成實作
    -> formal Verify 執行新的公開與獨立檢查
    -> 產生當次證據與私人 Verification Report
    -> 人決定如何處理交付
```

一般的 `./gradlew test` 執行公開專案測試與驗收方法。它提供開發回饋，不會產生正式的
ToppleCat 證據。

`./gradlew toppleCatVerify` 先確認完整契約與政策仍符合 Mechanical Seal，再執行公開
驗收與每道已啟用的獨立防線。最後寫出當次證據、報告與一個整體結果。

## 哪些內容是權威 {#contract-authority}

公開的 Java/JUnit Acceptance Methods 與型別化 JSON 或 YAML 案例列，就是可執行契約。
產生的 JSON 與 HTML 只負責解釋契約和觀察結果，不會變成另一個撰寫規則的地方。

每條選定規則由一個公開 `@ToppleAcceptanceTest("AC-...")` 方法負責。公開案例與
Reviewer 控制的案例分開執行，但重用相同方法。`@ToppleProperty` 是公開的不變條件，
有自己的產生輸入與證據；產生的 Property choices 不會變成隱藏案例列。

這能確保實作 agent 面對的公開契約，和正式驗證用來判斷它的公開契約相同。ToppleCat
不會在報告階段重新解釋規則。

## 四個模組 {#four-modules}

| 模組 | 負責內容 |
| --- | --- |
| `topplecat-core` | 案例、證據、custody、Property 與 safe-feedback data models |
| `topplecat-junit` | 驗收 annotations、typed rows、Scenario/Stage 執行、預期值檢查與 Properties |
| `topplecat-report` | 私人的 Spec Review 與 Verification Report projections |
| `topplecat-gradle-plugin` | Commands、task 順序、scope、custody、integrity 與 managed Mutation Testing |

公開網站與可執行 samples 用來解釋和驗證產品，不是額外的 runtime modules。

## 彼此獨立的檢查

Reviewer 案例、Property-Based Testing 與 Mutation Testing 回答不同問題。它們共享
delivery scope、integrity check 與最終報告，但不共用證據。契約完整性通過後，即使
Public Acceptance 失敗，Reviewer 案例與 Properties 仍能留下自己的結果；Mutation
Testing 則必須先有通過的公開 baseline，才能評估暫時的 production changes。

正式 Mutation Testing 使用 ToppleCat 固定的 managed PIT profile，並把 PIT
observations 對應到精確的公開 Acceptance Methods。專案自己的 PIT tasks 不會進入
ToppleCat 證據。

## Custody 與資訊邊界 {#information-boundary}

`toppleCatSeal` 把 Reviewer 控制的 source 移到本機 plaintext custody，並對完整可執行
契約與驗證政策建立 content-based seal。這可以察覺契約變更，但不是加密、
hostile-process isolation 或 operating-system security boundary。

Implementation Agent 收到公開契約與安全的 Gate-level feedback。Reviewer 保管
Spec Review、Verification Report、私人案例、counterexamples、producer diagnostics
與 raw failures。公開網站只能展示清楚標示的 synthetic demonstrations，不能使用實際
交付資料。

精確詞義請查[名詞解釋](glossary.md#executable-contract)。人的責任邊界請讀
[ToppleCat 解決什麼問題](product-definition.md#responsibility-boundary)。
