---
title: 排除問題
description: 從眼前看到的 ToppleCat 訊息開始，弄清楚它代表什麼，以及下一步怎麼做。
page_id: troubleshooting
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../troubleshooting/
alternate_language: en
alternate_label: English
alternate_en: ../troubleshooting/
alternate_zh_tw: ./troubleshooting/
markdown_url: troubleshooting.md
copy_label: Copy Markdown
copied_label: Copied
---

# 排除問題

## 先找你看到的症狀 {#symptom-map}

先從畫面或命令輸出開始。報告應該說清楚跑了什麼、發生什麼，以及為什麼得到目前的
結果。只有一個 `FAIL` 或 `INCOMPLETE`，還不足以診斷交付。

| 你看到的情況 | 從這裡開始 |
| --- | --- |
| `toppleCatCheck` 不接受 Java 方法 | [驗收方法無法編譯](#acceptance-method-does-not-compile) |
| 規格或案例找不到對應方法 | [規則沒有公開綁定](#missing-public-binding) |
| 公開案例和實作結果不一致 | [公開驗收失敗](#public-acceptance) |
| 某道檢查顯示無法評估 | [證據不完整](#incomplete-evidence) |
| 審閱者案例或 mutation 證據不見了 | [獨立檢查沒有證據](#independent-check-missing) |
| 契約與封印不一致 | [契約完整性失敗](#contract-integrity-fails) |

## 驗收方法無法編譯 {#acceptance-method-does-not-compile}

這代表 ToppleCat 還無法把選定規則整理成可信的可執行契約。先讀第一個 Check 錯誤訊息；
它通常會指出是哪個方法、參數、Stage 或 Scenario 呼叫不符合格式。

`ToppleCase` 放第一個，後面是一個 `ToppleScenario`，再接不同的具體 Stage。Stage
不能是 final，並且要有可存取的無參數 constructor。準備工作、條件判斷、服務呼叫與
assertions 都放進 Stage 方法。

修正後重新執行 `./gradlew toppleCatCheck`。檢查命令還不能描述完整契約前，不要封存
或正式驗證。

## 規則沒有公開綁定 {#missing-public-binding}

選定規格或案例列使用的 AC ID，找不到可編譯的公開 `@ToppleAcceptanceTest` 方法。
請修正 ID，或補上缺少的方法。審閱者控制的案例只能檢查既有規則，不能建立一條實作
AI 從未看過的新規則。

## 公開驗收失敗 {#public-acceptance}

在 Verification Report 打開失敗的公開案例。先看輸入，再看預期結果與實際結果；
這會指出哪一筆人寫的例子和實作不一致。

實作錯了就修正式程式。若人寫的規則或預期結果有誤，才修改契約。任何預期中的
契約變更都要重新檢查、審閱與封存。

這次 Mutation Testing 會因為沒有通過的公開 baseline 而不完整。審閱者案例和
Properties 是獨立檢查，仍可能留下有用的當次結果。

## 證據不完整 {#incomplete-evidence}

`INCOMPLETE` 表示 ToppleCat 沒有足夠的當次可信證據，不能支持通過或失敗。可能是
工作被中斷、當次附帶資料遺失，或性質檢查事件與封存宣告對不上。

先讀該檢查旁邊的原因，修正後重新執行正式驗證。舊輸出可以協助追查歷史，但不能
補進新的執行。

## 獨立檢查沒有證據 {#independent-check-missing}

如果政策啟用了審閱者案例，每條選定規則都需要一筆實際執行的審閱者案例。補上案例、
重新審閱完整契約，再次封存。若團隊確定不使用這道檢查，應明確修改政策並重新封存；
不能拿別的檢查代替。

Mutation Testing 沒有可用結果時，先確認 Public Acceptance 是否通過，再讀 managed
外部工具的原因。ToppleCat 使用固定的 PIT 設定與當次報告；專案自己的 PIT 工作
或舊報告不能取代。

## 契約完整性失敗 {#contract-integrity-fails}

公開驗收內容、Gradle logic、semantic definition 或 verification policy 已經和
Mechanical Seal 不一致。若變更是預期的，先還原審閱者資料，執行檢查與審閱，再重新
封存完整契約；若不是預期變更，就復原契約。

正式驗證不會自行建立缺少的封印，也不會默默批准新的契約內容。

## 安全的下一步 {#safe-next-action}

訊息仍然看不懂時，可以把公開 error、本頁 Markdown 與相關公開程式交給 AI，請它說明
實際跑了什麼，並提出公開範圍內的修正。不要把私人 Verification Report 或審閱者控制
的值交給它。

審閱者應先讀白話原因，有需要再展開技術證據。無法解釋的狀態本身
就是報告問題，不要靠猜測理解 `FAIL` 或 `INCOMPLETE`。
