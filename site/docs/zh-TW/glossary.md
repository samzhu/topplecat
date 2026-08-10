---
title: 名詞解釋
description: 用白話說明 ToppleCat 對規則、檢查、證據、報告與交付決定使用的正式詞彙。
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

# 名詞解釋

第一次使用 ToppleCat 不必先背這些名詞。報告、開發者或 AI 需要精確理解時，再來查
這一頁。每個項目先用白話說明；英文大寫名稱是 ToppleCat 的正式詞彙。

## Executable Contract／可執行契約 {#executable-contract}

公開驗收方法，以及公開與審閱者控制的型別案例資料列。它們定義 ToppleCat 要機械
化檢查什麼。規則和例子是否正確、完整，仍由人負責。

## Acceptance Condition／驗收條件 {#acceptance-condition}

一條由外部選定、使用穩定 `AC-...` ID 的規則。ToppleCat 把它連到可執行驗收工作，
但不會自行發明缺少的規則。

## Acceptance Method／驗收方法

一個公開的 Java/JUnit 方法，描述某條 Acceptance Condition 的 Scenario。公開案例與
審閱者控制的案例都執行同一個方法。

## Typed Case Row／型別案例資料列

一筆人工撰寫的 JSON 或 YAML 例子，包含 AC ID、輸入與預期結果。Property-Based
Testing 在執行中產生的值屬於當次證據，不是 Typed Case Row。

## Scenario、Stage 與 Step

**Scenario** 是一筆案例依序執行的 Given、When、Then 與 And。**Stage** 把相關業務
動作放在一起，並在該 Scenario 中保存一般狀態。**Step** 是其中一次被選定的動作或
觀察。

## Reviewer／審閱者與 Implementation Agent／實作 AI

**Reviewer** 是閱讀準備好的契約與當次 Verification Report，並決定如何處理交付的
人。**Implementation Agent** 是收到公開契約與安全回饋的 AI coding agent；它不會
取得審閱者私有資料。

## Independent Safeguard／獨立防線 {#independent-safeguard}

當次證據只回答自身問題的一道檢查。審閱者案例、Property-Based Testing 與
Mutation Testing 彼此分開；一項通過不能補另一項缺少的證據。

## Hidden Tests／隱藏測試

審閱者控制的型別案例資料列，使用獨立選出的例子執行既有公開 Acceptance Method。
它們檢查同一條規則，不會建立祕密的新需求。

## Property-Based Testing／性質導向測試

用有界的產生輸入，檢查一條經過人核准的不變條件。它可能找到反例，但結果仍是測試
證據，不是數學證明。

## Mutation Testing／突變測試與 Mutation Attribution／突變歸因

Mutation Testing 會暫時改動程式行為，再看原本的公開 Acceptance Method
能不能察覺。**Mutation Attribution** 把 PIT 觀察結果連到應該負責偵測它的精確
公開方法與 Acceptance Condition。

## Mechanical Seal／機械封印 {#mechanical-seal}

對完整可執行契約與驗證政策建立的 content-based integrity record。它能指出審閱後
約定是否被改動，但不等於人的核准、加密或 security sandbox。

## Current-run Evidence／本次執行證據與 Aggregate Verdict／彙總判定

**Current-run Evidence** 由目前這次正式驗證產生。**Aggregate Verdict** 是該
驗證範圍的 `PASS`、`FAIL` 或 `INCOMPLETE`。`PASS` 表示每一道必要檢查都通過，
不代表所有業務規則都已經被寫下。

## Spec Review／規格審閱與 Verification Report／驗證報告

**Spec Review** 是 AI 開始實作前使用的私人頁面，讓人確認選定的規格與準備好的檢查。
**Verification Report** 是 AI 宣稱完成後使用的私人頁面，說明本次驗證結果與需要追查的
問題。

完整正式詞彙請查
[CONTEXT.md](https://github.com/samzhu/topplecat/blob/main/CONTEXT.md)。詞彙背後的
資訊流向請讀[從規則到結果](architecture.md#information-boundary)。
