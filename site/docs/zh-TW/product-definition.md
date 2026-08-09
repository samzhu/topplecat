---
title: ToppleCat 解決什麼問題
description: 評估 ToppleCat 是否適合把實作交給 AI、但仍由人負責驗收的 Java 團隊。
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

# ToppleCat 解決什麼問題

ToppleCat 處理一個很具體的時刻：團隊把選定功能交給 AI coding agent，agent 說已經
完成，接下來仍要由人決定是否接受交付。

如果沒有另一道驗證，這個決定往往只依賴 agent 開發時就看過的公開例子。ToppleCat
保留那些例子，再加入彼此獨立的檢查，讓負責決定的人拿到本次執行的新證據。

## 什麼時候適合使用 {#use-moment}

假設產品負責人定義優惠券規則，開發者把它寫成公開的 Java/JUnit 驗收內容。handoff
前，負責的人先確認規則、例子，以及之後會執行哪些額外檢查。agent 宣稱完成後，
ToppleCat 驗證這份封存過的約定。

Reviewer 不一定要懂 Java。公開整合可以由開發者或 coding agent 準備。Reviewer 可以
是開發者、產品負責人、測試者，或任何理解預期業務結果、能閱讀白話報告並對交付負責
的人。

以下情況很適合 ToppleCat：

- 專案使用 Java 與 JUnit；
- 選定工作會交給 AI agent 實作；
- 團隊能在 handoff 前寫下可觀察的規則與例子；
- 接受或 merge 前，希望取得這次執行的證據。

## 開發流程會多出什麼

實作前，人選定的規則會變成可執行契約。Reviewer 確認真正會被檢查的內容，再封存
完整契約與驗證政策。

agent 只使用公開專案與一般測試回饋。它說完成後，formal Verify 執行封存契約與每道
已啟用的獨立防線。Reviewer 閱讀結果，再決定下一步。

ToppleCat 只有在本次執行的每個必要 Gate 都通過時，才記錄 `PASS`。它不會把
`PASS` 自動變成核准。

## 責任邊界 {#responsibility-boundary}

| ToppleCat 負責 | 人、團隊、專案或外部流程負責 |
| --- | --- |
| 把選定規則綁到一般的可執行驗收工作 | 選定 Spec，並確認規則與例子完整 |
| 察覺完整契約或驗證政策在審閱後被改動 | 決定由誰審閱、核准或 sign-off |
| 執行新的正式驗證，並讓各項檢查彼此獨立 | 決定命令在本機、CI 或其他地方執行 |
| 產生私人 Reviewer reports 與安全的 agent feedback | 管理任務、交付歷史、pull request 與 release |
| 使用固定的 managed mutation profile 與精確方法歸屬 | 一般 unit/QA tests、自訂 PIT、效能與安全工作 |

這個分工是刻意的。ToppleCat 只報告已檢查契約與當次證據能支持的結論，不會從缺少的
需求自行推論人的意圖。

## ToppleCat 不負責什麼 {#what-topplecat-does-not-own}

ToppleCat 不是 task manager、Spec manager、approval system、CI service、通用測試
框架或 security sandbox。本機的 reviewer custody 是機械式 handoff 防線，不是加密。

它也無法回答最上游、最重要的問題：「我們是否已經寫下所有必要業務規則？」這件事
仍由人負責。

如果這符合你的使用情境，先[執行可重現範例](getting-started.md#sample-workflow)。
需要技術信任邊界時，再讀[系統如何運作](architecture.md#information-boundary)。
