---
title: 文件首頁
description: 從可執行驗收契約與本次執行證據開始認識 ToppleCat。
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

# 文件首頁 {#documentation-home}

ToppleCat 是給 Java/JUnit 專案使用的 delegation-verification gate。這些頁面說明
從人選定的 Spec 到全新的 Verification Report 之間，產品目前支援的工作流程。
這裡是公開的技術解釋面；真正的可執行契約仍是一般 Java/JUnit
Acceptance Method 與型別化 JSON 或 YAML 案例列。

## 從這裡開始 {#start-here}

想像一條結帳規則：訂單滿 1,000 元就折 100 元。公開案例可以檢查
`1,000 -> 900`，但單一案例不能證明每種合法結帳都遵守規則。ToppleCat 會維持
人寫下的規則不變，再於 agent 宣稱完成後，以彼此獨立的檢查重新驗證。

1. 先讀 [Getting started](getting-started.md#contract-example)，走完以 sample
   為基礎的完整路徑。
2. 用 [Authoring contracts](authoring-contracts.md#acceptance-method) 把
   Acceptance Condition 綁到可執行 Java 與型別化案例列。
3. 用 [Verification and evidence](verification-and-evidence.md#gates-and-verdicts)
   讀懂本次執行的 Gate 判定。

只有本次執行的每個必要 Gate 都通過，ToppleCat 才會記錄 `PASS`。開發時的綠色
測試，或 ToppleCat 的 `PASS`，都只是對已檢查契約的證據；它不證明上游 Spec 沒有
漏掉業務規則，也不替人決定是否接受交付。

## 選擇你的任務 {#choose-your-task}

| 你想要... | 閱讀 |
| --- | --- |
| 安裝 ToppleCat 並驗證一次交付 | [Getting started](getting-started.md) |
| 撰寫 Acceptance Method 與型別化案例列 | [Authoring contracts](authoring-contracts.md) |
| 理解 Gate 與證據 | [Verification and evidence](verification-and-evidence.md) |
| 根據看得到的症狀診斷問題 | [Troubleshooting](troubleshooting.md) |
| 了解 ownership 與使用時機 | [Product definition](product-definition.md) |
| 了解模組與資訊流 | [Architecture](architecture.md) |
| 查詢正式的 ToppleCat 詞彙 | [Glossary](glossary.md) |
| 閱讀目前支援的版本 | [Current release notes](release-notes.md) |

## 公開邊界

網站只發布目前、人工撰寫的英文與繁體中文技術頁面。它不發布 Javadoc、實際
交付報告、reviewer-owned 值、私有 repository workspace、受門檻保護的執行筆記或
任務協調記錄，也不提供 `llms-full` bundle。兩份小型、按語言分開的 Markdown
manifest 只是實驗性便利；一般 HTML 導覽與 sitemap 才是主要 discovery path。

想看專案故事與開源原始碼，回到 [ToppleCat 專案首頁](/) 或
[GitHub](https://github.com/samzhu/topplecat)。
