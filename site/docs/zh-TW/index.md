---
title: ToppleCat 是什麼
description: 從零認識 ToppleCat：它為什麼要重新驗證 AI 寫好的 Java 功能、會檢查什麼，以及人最後要做什麼決定。
page_id: home
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../
alternate_language: en
alternate_label: English
alternate_en: ../
alternate_zh_tw: ./
markdown_url: index.md
copy_label: Copy Markdown
copied_label: Copied
---

# ToppleCat 是什麼？ {#documentation-home}

ToppleCat 是一個給 Java/JUnit 專案使用的開源驗證工具。當 AI coding agent 說功能
已經完成，它不直接相信這句話，而是用人事先確認的業務規則重新檢查，並留下這次執行
的結果。

它不是另一個幫你寫程式的 AI。它比較像交付前的驗收員：把寫著 `PASS` 的成果推一下，
看看它是真的站得住，還是只在公開範例面前看起來正常。

## 它要解決的問題 {#problem}

假設優惠券規則是「使用 `SAVE100` 結帳時折 100 元」。AI 寫完功能，公開測試也確認
`1,000 → 900`。看起來完成了，但程式也可能只是針對這組數字寫死答案；換成另一筆合法
訂單就出錯。

一般測試仍然重要。問題在於，AI 開發時已經看過公開規則與範例。只用同一批資料驗收，
很難分辨它是理解並實作了規則，還是剛好通過已知答案。

ToppleCat 會保留原本公開的驗收內容，再從不同角度挑戰這份交付。它找到問題時，報告
會說明哪一條規則、哪一種檢查出了狀況；全部必要檢查都有可信結果並通過時，這次執行
才會得到 `PASS`。

## ToppleCat 會檢查什麼 {#checks}

第一次閱讀時，先看每項檢查回答的問題。右欄是文件與報告使用的正式名稱。

| 想確認的事 | ToppleCat 的檢查 |
| --- | --- |
| AI 看得到的公開範例現在是否真的通過？ | Public Acceptance |
| 換成 AI 事前沒看過、但仍符合相同規則的例子，結果對嗎？ | Hidden Tests |
| 測試有實際比較預期結果，還是只把資料讀進來？ | Expected Result Check |
| 同一條規則遇到許多合法輸入，能否找到反例？ | Property-Based Testing |
| 暫時改動程式行為後，原本的驗收方法能察覺嗎？ | Mutation Testing |
| 審閱後，規則或驗證設定有沒有被改過？ | Contract Integrity |

這些檢查彼此獨立。多跑一筆案例、產生許多輸入、暫時改動程式，各自會抓到不同問題；
其中一項通過，不能補上另一項缺少的證據。

## 一次交付怎麼走完 {#start-here}

```text
人寫清楚怎樣才算做對
    → 確認準備好的規則、範例與額外檢查
    → AI 只依公開內容實作，照常執行一般測試
    → ToppleCat 重新執行並加入獨立檢查
    → 人閱讀這次結果，決定是否接受交付
```

ToppleCat 把「怎樣才算做對」整理成可執行契約。這只是正式名稱，實際內容仍是一般
Java/JUnit 方法，以及寫有輸入和預期結果的 JSON 或 YAML 範例。

人要在 AI 開始實作前確認契約。AI 完成後，ToppleCat 重新執行同一份公開約定，不會
在背後換一套規則。完整流程請讀[從規則到結果](architecture.md#execution-flow)。

## 你會看到兩份報告 {#reports}

兩份報告都只留在你的專案裡，不會發布到這個網站，也不會交給實作 AI。

### 實作前：Spec Review

這份頁面讓負責驗收的人先看清楚：「我們選了哪些業務規則？準備了哪些公開與額外
範例？之後究竟會執行什麼？」它是審閱資料，還沒有測試結果。

### AI 說完成後：Verification Report

這份頁面先告訴你本次執行是通過、發現問題，還是證據不足，再列出每條規則與各項
檢查的結果。需要追查時，才展開輸入、預期與實際差異，以及更深的技術資料。

`PASS` 代表這次選定範圍內，每一道必要檢查都通過。它不代表 ToppleCat 已經替人
核准交付，也不證明沒有人漏寫業務規則。

## 誰適合使用 {#audience}

ToppleCat 適合使用 Java/JUnit、把部分實作交給 AI，並且希望由人保留驗收決定的團隊。

閱讀報告的人不一定要會寫 Java。他可以是開發者、產品負責人、測試者，或其他了解
業務預期並對交付負責的人。工程設定可以請開發者或 AI 協助；規則是否完整、證據是否
足夠，仍要由人判斷。

如果你還在評估，請讀[它適合什麼情境](product-definition.md#use-moment)。

## 先認識五個常用名詞 {#terms}

- **驗收條件（Acceptance Condition）：**一條人選定、可明確判斷結果的業務規則。
- **可執行契約（Executable Contract）：**把規則接到可以真的執行的 Java/JUnit
  檢查與案例資料。
- **審閱者（Reviewer）：**閱讀規則與報告，最後決定怎麼處理交付的人。
- **獨立防線（Independent Safeguard）：**回答一個特定驗證問題、不能被其他結果
  取代的檢查。
- **本次執行證據（Current-run Evidence）：**這一次正式驗證留下的結果，不拿舊報告
  來補缺少的資料。

遇到其他大寫英文詞，不必猜；直接查[名詞解釋](glossary.md#executable-contract)。

## 接下來怎麼做 {#choose-your-task}

| 你現在想做什麼 | 下一頁 |
| --- | --- |
| 把 ToppleCat 加進現有 Java 專案 | [開始使用](getting-started.md#ai-assisted-authoring) |
| 評估它是否適合團隊目前的開發方式 | [ToppleCat 適合什麼情境](product-definition.md) |
| 了解每項檢查與 `PASS`、`FAIL` 的意思 | [ToppleCat 如何檢查交付](verification-and-evidence.md) |
| 了解完整流程、審閱者專用內容與系統邊界 | [從規則到結果](architecture.md) |
| 把自己的業務規則接到 Java/JUnit | [把規則寫成可執行檢查](authoring-contracts.md) |
| 解決安裝或驗證問題 | [排除問題](troubleshooting.md) |
| 確認目前版本與環境需求 | [0.1.0 版本說明](release-notes.md) |

## 讓 AI 協助閱讀或安裝 {#ai-help}

每頁上方都有 **Copy Markdown**。按下後，把內容交給 AI，它可以：

- 用你的產業或業務情境重新解釋 ToppleCat；
- 告訴工程師需要準備什麼；
- 在 Java 專案安裝 plugin，並依照人已確認的公開規則建立檢查。

不要把私人報告、審閱者另外準備的案例或其他私人值交給實作 AI。也不要請 AI 猜測
沒有寫下的業務需求。

想開始使用，請讀[開始使用](getting-started.md#ai-assisted-authoring)。想看專案故事，
回到 [ToppleCat 專案首頁](/)；原始碼與貢獻資料在
[GitHub](https://github.com/samzhu/topplecat)。
