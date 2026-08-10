---
title: 從規則到結果
description: 看一條業務規則如何成為可執行檢查，經過 AI 實作與重新驗證，最後變成人可以閱讀的結果。
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

# 從規則到結果

這一頁把完整流程串起來：人先寫下怎樣才算做對，AI 依公開內容實作，ToppleCat 再用
同一份約定重新驗證。後半段才說明技術模組與審閱者專用內容如何保管。

若只想先看效果，可以直接[執行範例](getting-started.md#sample-workflow)。

## 一次交付的完整流程 {#execution-flow}

```text
人選定規則與範例
    → 寫成公開的 Java/JUnit 可執行契約
    → 審閱者確認內容，封存完整契約
    → AI 使用一般測試完成實作
    → ToppleCat 執行新的公開與獨立檢查
    → 產生本次證據與私人驗證報告
    → 人決定如何處理交付
```

一般的 `./gradlew test` 會執行公開專案測試與驗收方法。它提供開發時的快速回饋，
不會產生正式的 ToppleCat 證據。

`./gradlew toppleCatVerify` 先確認完整契約與驗證設定仍符合機械封印
（Mechanical Seal），再執行公開驗收和每一道已啟用的獨立檢查。最後寫出本次證據、
私人報告與一個整體結果。

## 哪些內容決定「做對了」 {#contract-authority}

公開的 Java/JUnit 驗收方法，以及 JSON 或 YAML 案例資料，就是可執行契約。它們是
ToppleCat 判斷結果的依據。產生的 JSON 與 HTML 只說明檢查內容和觀察結果，不會變成
另一份規格。

每條規則由一個公開的 `@ToppleAcceptanceTest("AC-...")` 方法負責。公開案例和審閱者
另外準備的案例會分開執行，但使用同一個方法。`@ToppleProperty` 則描述一條公開的
不變條件，使用產生的輸入尋找反例。

這個安排讓 AI 開發時看見的公開契約，和 ToppleCat 正式驗證時使用的公開契約保持
一致。報告只呈現已經執行的內容，不會重新解讀業務規則。

## 四個 Java 模組 {#four-modules}

第一次使用時不必理解模組。需要追查程式責任時，再回來看這張表。

| 模組 | 負責內容 |
| --- | --- |
| `topplecat-core` | 案例、證據、審閱資料保管、性質檢查與安全回饋的資料模型 |
| `topplecat-junit` | 驗收標記、案例資料、情境執行、預期值檢查與性質導向測試 |
| `topplecat-report` | 私人的 Spec Review 與 Verification Report |
| `topplecat-gradle-plugin` | Gradle 命令、執行順序、驗證範圍、完整性與 mutation 檢查 |

公開網站和可執行範例用來說明、驗收產品本身，不是額外的執行模組。

## 為什麼要分開檢查

審閱者案例、性質導向測試與 mutation 檢查回答不同問題。它們可以出現在同一份報告，
但不共用結果。額外案例通過，不能代替性質檢查；性質檢查通過，也不能證明公開驗收
一定會察覺暫時改壞的程式。

契約完整性通過後，即使公開案例失敗，額外案例和性質檢查仍會留下自己的結果。
Mutation Testing 比較特殊：公開驗收要先通過，才有可信的原始基準可供比較。

正式的 Mutation Testing 使用 ToppleCat 固定的 PIT 設定，並把 PIT 的觀察結果連回
精確的公開驗收方法。專案自己設定的 PIT 工作不會混入 ToppleCat 證據。

## 審閱者專用內容如何保管 {#information-boundary}

`toppleCatSeal` 會把審閱者另外準備的原始碼移到本機保管區，並記錄完整契約與驗證設定
的內容。之後若有人更動這些內容，ToppleCat 可以察覺。

這個保管區是本機純文字儲存，不是加密，也不能隔離使用同一作業系統帳號的其他程序。
它的用途是讓實作 AI 只收到公開契約和不洩漏私人答案的回饋。

Spec Review、Verification Report、私人案例、反例、外部工具診斷與原始錯誤，都由
審閱者保管。公開網站只能使用清楚標示的合成示範，不能放入實際交付資料。

精確詞義請查[名詞解釋](glossary.md#executable-contract)。人的責任邊界請讀
[ToppleCat 適合什麼情境](product-definition.md#responsibility-boundary)。
