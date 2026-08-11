---
title: 0.1.0 版本說明
description: 確認目前 ToppleCat 版本的 Java 要求、驗證流程、報告與限制。
page_id: release-notes
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../release-notes/
alternate_language: en
alternate_label: English
alternate_en: ../release-notes/
alternate_zh_tw: ./release-notes/
markdown_url: release-notes.md
copy_label: Copy Markdown
copied_label: Copied
---

# ToppleCat 0.1.0 版本說明

第一次聽到 ToppleCat，請先讀[ToppleCat 是什麼](index.md#documentation-home)。這一頁
留給準備導入或升級的人，回答目前版本有哪些能力、需要什麼環境，以及有哪些限制。

完整的歷史紀錄請讀
repository 的
[0.1.0 release notes](https://github.com/samzhu/topplecat/blob/main/docs/releases/0.1.0.zh-TW.md)。

## 目前版本 {#current-release}

ToppleCat 0.1.0 支援 Java 25、JUnit 6.1.1 與相容的 Gradle 版本。使用端專案
需要的 Gradle plugin 與 JUnit library 已發布到
[Maven Central](https://central.sonatype.com/namespace/io.github.samzhu.topplecat)。

這個版本可以：

- 把選定規則連到公開 Java/JUnit 驗收方法與型別化 JSON 或 YAML 案例資料；
- 在 AI 開始實作前，審閱並機械封存完整可執行契約；
- 執行新的公開驗收、審閱者控制案例、預期值檢查、Property-Based
  Testing 與 managed Mutation Testing；
- 產生以驗收條件為中心的私人 Verification Report、機器可讀證據，以及不洩漏審閱者
  資料的 AI 回饋。

CI 的正常命令是沒有指定 Spec 或 AC 的 `./gradlew toppleCatVerify`。它會驗證完整
契約，只有本次執行的每一道必要檢查都通過時才記錄 `PASS`。

## 報告與語言

ToppleCat 會在你的專案裡產生兩份私人報告。Spec Review 讓人先確認之後要檢查的規則；
Verification Report 說明 AI 完成後的本次驗證結果。兩份都是可以離線開啟的 HTML
頁面，不會把內容送到這個公開網站或外部服務。

報告的標題、按鈕與 ToppleCat 說明預設顯示英文。如果希望這些介面文字顯示繁體中文，
在產生報告的 Review 或 Verify 命令加上 `--language zh-TW`。你自己寫的業務規則、
案例 ID、輸入、預期值，以及外部測試工具記錄的結果都不會被翻譯或改寫。

若只想先檢查剛完成的功能，可以選擇一份或多份 Spec 檔案，也可以列出一個或多個
AC ID；兩種選法不能混用。報告頂端會列出這次實際檢查的規則。這種有限範圍的
`PASS` 只代表列出的規則通過，不代表整個專案都通過。CI 應使用不指定範圍的完整驗證。

## 公開文件 {#documentation-surface}

公開文件提供英文與人工撰寫的繁體中文。介紹與教學頁先說明要解決的問題，再提供
可依步驟導入；參考頁保留精確命令、名詞與邊界。

每頁都有 **Copy Markdown**，方便把目前這一頁交給 AI 解釋，或請 AI 協助公開範圍內
的安裝工作。網站不會自動翻譯，也不會把私人報告提供給 AI。

## 限制與升級提醒 {#upgrade-notes}

- ToppleCat 0.1.0 只支援目前格式。審閱者資料與 Mechanical Seal 不會從其他格式版本
  自動遷移；請還原資料、重新檢查與審閱，再為目前版本封存。
- 正式 Mutation Testing 使用 ToppleCat 固定的 managed PIT profile。專案自訂的 PIT
  工作維持獨立。
- 有限範圍的驗證可以使用 Spec 檔案或 AC ID，但不能同時使用。封存永遠涵蓋完整契約。
- 審閱者資料保存在本機的純文字儲存區，不是加密或程序隔離。
- `PASS` 是已檢查範圍的證據，不證明所有業務規則都已寫下，也不代表組織核准。

要導入目前版本，請[從 Maven Central 安裝 ToppleCat](getting-started.md#ai-assisted-authoring)。
要解讀結果，請讀[ToppleCat 如何檢查交付](verification-and-evidence.md#gates-and-verdicts)。
