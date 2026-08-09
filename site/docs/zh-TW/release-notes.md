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

這一頁回答實際導入時最常見的問題：目前版本有哪些能力可以依賴？完整的歷史紀錄請讀
repository 的
[0.1.0 release notes](https://github.com/samzhu/topplecat/blob/main/docs/releases/0.1.0.zh-TW.md)。

## 目前版本 {#current-release}

ToppleCat 0.1.0 支援 Java 25、JUnit 6.1.1 與相容的 Gradle 版本。使用端專案
使用一個 Gradle plugin 與一個 JUnit library。

這個版本可以：

- 把選定規則綁到公開 Java/JUnit Acceptance Methods 與型別化 JSON 或 YAML 案例列；
- 在 AI implementation handoff 前，審閱並機械封存完整可執行契約；
- 執行新的 Public Acceptance、Reviewer 控制案例、預期值檢查、Property-Based
  Testing 與 managed Mutation Testing；
- 產生以 AC 為中心的私人 Verification Report、機器可讀證據，以及安全的 agent
  feedback。

CI 的正常命令是沒有指定 Spec 或 AC 的 `./gradlew toppleCatVerify`。它會驗證完整
契約，只有本次執行的每個必要 Gate 都通過時才記錄 `PASS`。

## 報告與語言

Spec Review 與 Verification Report 是私人、self-contained 的 HTML bundles。預設
介面是英文；Reviewer 想讀繁體中文的 ToppleCat 介面文字時，可以在 Review 或 Verify
命令加入 `--language zh-TW`。人寫的業務文字、IDs、values 與 producer outcomes 都
維持原樣。

Reviewer 可以用選定的 Spec 檔案或重複的 AC IDs 產生較快的正式報告。報告會標示
範圍，有限範圍的 `PASS` 不代表完整專案通過。

## 公開文件 {#documentation-surface}

公開文件提供英文與人工撰寫的繁體中文。教學頁先說明交付問題與可執行範例；reference
頁保留精確命令、名詞與邊界。

每頁都有同主題 Markdown，方便人把內容交給 AI 閱讀。這些小型頁面資源與語言
manifests 只是閱讀便利，不是 docs API 或整站 `llms-full` bundle。

## 限制與升級提醒 {#upgrade-notes}

- ToppleCat 0.1.0 只支援目前格式。Reviewer custody 與 Mechanical Seal 不會從其他
  schema version 自動遷移；請 Restore、Check、Review，再為目前版本 Reseal。
- 正式 Mutation Testing 使用 ToppleCat 固定的 managed PIT profile。專案自訂的 PIT
  task 維持獨立。
- Selected Verify 可以使用 Spec files 或 AC IDs，但不能同時使用。Sealing 永遠涵蓋
  完整契約。
- Reviewer custody 是 plaintext mechanical storage，不是加密或 process isolation。
- `PASS` 是已檢查範圍的證據，不證明所有業務規則都已寫下，也不代表組織核准。

要試用目前支援的路徑，請[執行可重現範例](getting-started.md#sample-workflow)。要解讀
結果，請讀[驗證交付並讀懂結果](verification-and-evidence.md#gates-and-verdicts)。
