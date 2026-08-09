---
title: Current release notes
description: ToppleCat 0.1.0 目前支援的行為，以及說明它的雙語文件面。
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

# Current release notes

## ToppleCat 0.1.0 {#current-release}

ToppleCat 0.1.0 接起 AI 實作 handoff 前後的工作。handoff 前，Reviewer 先讀 selected
Spec 與實際會執行的 executable contract；agent 宣稱 done 後，再讀以 AC 為中心的新鮮
evidence。交付決定仍由人做。

目前的 verification model 包含 Contract Integrity、Public Acceptance、Hidden Tests、
Expected Consumption、Property-Based Testing 與 managed Mutation Testing。每項 safeguard
保留自己的 evidence，也保留 producer 的正式 outcome。

## 文件面 {#documentation-surface}

本版本的正式技術文件提供英文與繁體中文。它是 current-only、task-oriented、沒有 search，
並以靜態 GitHub Pages artifact 建置。每頁都有同 topic 的語言 pair 與 page-level Markdown。
Markdown manifest 只是實驗性便利，不是 API、crawler guarantee 或 whole-site bundle。

## 升級提醒 {#upgrade-notes}

完整歷史說明請讀 repository 的
[0.1.0 release notes](https://github.com/samzhu/topplecat/blob/main/docs/releases/0.1.0.zh-TW.md)。
目前最重要的規則是：

- 完整 CI run 使用不帶 `--spec` 或 `--ac` 的 `toppleCatVerify`。
- Scoped Verify 是 Reviewer 的快速報告，不是完整專案驗證的替代品；`--spec` 與
  `--ac` 不能混用。
- 正式 Mutation Testing 使用 ToppleCat managed PIT profile；專案自己的 PIT task
  不進 ToppleCat evidence。
- Reviewer custody 與 Mechanical Seal 只支援目前版本。舊 suite 需先 Restore、Check、
  Review、Reseal，再做 formal Verify。
- ToppleCat `PASS` 表示本次執行中每個必要 Gate 通過；它不表示 Spec 完整，也不表示人
  已經接受交付。

目前的 command sequence 請看 [Getting started](getting-started.md#formal-verify)，結果
解讀請看 [Verification and evidence](verification-and-evidence.md#gates-and-verdicts)。
