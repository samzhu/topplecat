---
title: ToppleCat 適合什麼情境
description: 評估 ToppleCat 是否適合讓 AI 實作 Java 功能、但仍由人負責驗收的團隊。
page_id: product-definition
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../product-definition/
alternate_language: en
alternate_label: English
alternate_en: ../product-definition/
alternate_zh_tw: ./product-definition/
markdown_url: product-definition.md
copy_label: Copy Markdown
copied_label: Copied
---

# ToppleCat 適合什麼情境

如果團隊讓 AI 寫 Java 功能，但不想只靠 AI 已經看過的測試來驗收，ToppleCat 就是為
這個空缺設計的。

它把人事先確認的規則固定下來。AI 說完成後，ToppleCat 重新執行公開範例，加入其他
獨立檢查，再把本次結果交給負責驗收的人。工具負責留下證據，人負責做決定。

## 什麼時候適合使用 {#use-moment}

假設產品負責人定義優惠券規則，工程師或 AI 把規則接到 Java/JUnit 檢查。AI 開始實作
前，團隊先看過規則、範例，以及之後會加入哪些檢查。AI 說完成後，ToppleCat 用同一份
約定重新驗證。

符合以下情況時，ToppleCat 最有幫助：

- 專案使用 Java 與 JUnit；
- 團隊把選定功能交給 AI coding agent 實作；
- 人可以在實作前寫下可觀察的規則與範例；
- 接受或合併程式前，希望看到這次執行留下的證據。

負責驗收的人不一定要會寫 Java。他可以是開發者、產品負責人、測試者，或其他理解
預期業務結果並對交付負責的人。工程接線可以請開發者或 AI 完成。

## 開發流程會多出什麼

### AI 開始前

人先選定這次要做的規則，並把預期行為寫成可執行檢查。Spec Review 讓負責驗收的人
確認之後到底會檢查什麼。確認後，ToppleCat 會記住完整契約與驗證設定的內容。

### AI 實作時

AI 只會看到公開規則、公開範例與一般專案測試。它仍可用 `./gradlew test` 快速取得
開發回饋；審閱者另外準備的資料不會交給它。

### AI 說完成後

ToppleCat 先確認審閱過的內容沒有改變，再執行公開與額外檢查。Verification Report
會說明每條規則發生什麼。只有本次執行的每一道必要檢查都通過，才會記錄 `PASS`。

`PASS` 不是自動核准。負責驗收的人仍要判斷規則是否完整，以及這些證據是否足夠。

## 責任邊界 {#responsibility-boundary}

| ToppleCat 負責 | 人、團隊或既有開發流程負責 |
| --- | --- |
| 把選定規則連到可執行的 Java/JUnit 驗收內容 | 選定要做的功能，並把規則與範例寫完整 |
| 察覺審閱後的契約或驗證設定被改動 | 決定由誰審閱、核准或簽署交付 |
| 重新驗證，讓不同檢查各自留下結果 | 決定在本機、CI 或其他環境執行 |
| 產生私人報告，以及不洩漏審閱資料的 AI 回饋 | 管理任務、交付歷史、pull request 與 release |
| 執行 ToppleCat 固定的 mutation 檢查方式 | 一般單元／QA 測試、效能與安全檢查 |

## ToppleCat 不會替你做什麼 {#what-topplecat-does-not-own}

ToppleCat 不會管理任務或規格版本，也不會替組織核准交付。它不是 CI 服務、通用測試
框架或安全沙箱。本機保管審閱資料的機制用來避免交付時混入私人內容，不是加密。

它最無法代替人的地方，是回答：「我們有沒有漏掉重要業務規則？」契約裡沒有寫的 VIP
折扣、退款例外或法規要求，ToppleCat 不會自己猜出來。

如果這正是你的使用情境，可以先[執行可重現範例](getting-started.md#sample-workflow)。
想看每一項檢查，請讀[ToppleCat 如何檢查交付](verification-and-evidence.md#delivery-example)。
