---
title: 文件首頁
description: 看 ToppleCat 如何檢查 AI coding agent 的完成宣稱，並在接受 Java 交付前留下可讀的當次證據。
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

# AI 說做好了，先別急著接受 {#documentation-home}

AI coding agent 說功能已經完成，公開測試也都通過。這時候你真正知道的是：你寫下的
例子可以跑過。你還不知道它是否真的實作了規則，還是只找到一條剛好能通過範例的捷徑。

ToppleCat 讓負責交付的人能檢查這個差別。它固定雙方同意的 Java/JUnit 驗收內容，在
agent 宣稱完成後重新執行，並加入彼此獨立的檢查。最後的報告會說明這次交付為什麼取得
`PASS`，或是哪一道檢查攔下了它。

## 從這裡開始 {#start-here}

假設優惠券規則是「結帳時折 100 元」。一筆公開案例只能證明某次結帳算對了。實作如果
只認得那一組輸入，測試照樣可能是綠的。

repository 裡的可執行範例故意放進這種錯誤。ToppleCat 會重跑公開規則，再用審閱者
另外準備的案例檢查邊界，並確認原本的驗收方法能不能察覺 production code 被暫時改動。
狹隘的實作會被擋下；修正後的版本才會在新的執行中取得 `PASS`。

你可以從兩條路開始：

- **先看效果：**[執行範例，看 ToppleCat 如何拒絕一個看似完成的錯誤
  實作](getting-started.md#sample-workflow)。
- **用在自己的專案：**先[把業務規則寫成可執行檢查](authoring-contracts.md#contract-example)，
  再[驗證 agent 的交付](verification-and-evidence.md#delivery-example)。

業務規則仍要由人決定。沒有人寫下的折扣、例外或核准政策，ToppleCat 不會自行猜測。

## 選擇你的任務 {#choose-your-task}

| 你現在需要什麼 | 請讀 |
| --- | --- |
| 看它抓到一個表面正常、實際有錯的實作 | [開始使用](getting-started.md) |
| 告訴 ToppleCat「這個功能怎樣才算正確」 | [把規則寫成可執行檢查](authoring-contracts.md) |
| 判斷 `PASS`、`FAIL` 或證據不足各代表什麼 | [驗證交付並讀懂結果](verification-and-evidence.md) |
| 解決安裝、契約或驗證問題 | [排除問題](troubleshooting.md) |
| 評估 ToppleCat 適不適合目前的開發流程 | [ToppleCat 解決什麼問題](product-definition.md) |
| 了解信任邊界與資料流向 | [系統如何運作](architecture.md) |
| 查一個 ToppleCat 正式名詞 | [名詞解釋](glossary.md) |
| 確認 0.1.0 支援哪些能力 | [0.1.0 版本說明](release-notes.md) |

## 讓 AI 幫你讀

每頁都有 **Copy Markdown**。你可以把該頁的原始內容交給 AI，請它換成你的業務情境
解釋、指出工程師該執行哪些命令，或在 Java 專案完成公開設定。Markdown 和畫面上看到
的是同一份人工撰寫內容，不是自動翻譯，也不是隱藏 API。

有兩件事仍要由人決定：業務規則是否完整，以及這份證據是否足以接受交付。

## 關於這份文件

網站發布目前版本的英文與繁體中文說明。實際交付報告和審閱者控制的資料不會公開。
完整責任邊界請讀[ToppleCat 解決什麼問題](product-definition.md#responsibility-boundary)。

想看專案故事與開源原始碼，回到 [ToppleCat 專案首頁](/) 或
[GitHub](https://github.com/samzhu/topplecat)。
