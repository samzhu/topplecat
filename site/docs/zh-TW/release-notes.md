---
title: ToppleCat 0.2.2 版本說明
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

# ToppleCat 0.2.2 版本說明

第一次聽到 ToppleCat，請先讀[ToppleCat 是什麼](index.md#documentation-home)。這一頁
留給準備導入或升級的人，回答目前版本有哪些能力、需要什麼環境，以及有哪些限制。

完整的歷史紀錄請讀 repository 的
[0.2.1 release notes](https://github.com/samzhu/topplecat/blob/main/docs/releases/0.2.1.zh-TW.md)。

## 目前的 release line {#current-release}

## 在業務 Spec 指定的位置放入每個驗收投影

以前選定的 Markdown 會把可見標題和 generic marker 綁在一起。現在只用一個精確、獨立一行的
marker，例如 `<!-- topplecat:acceptance:AC-CHECKOUT-001 -->`，同時作為 AC identity 與
Spec Review 插入點。標題可用任何文字與層級，一般 AC 提及仍是作者文字。團隊調整業務文件
結構時，不會再意外改變範圍。

每個選定 AC marker 在 supplied repository-relative `--spec` paths 中只能出現一次。Check
會列出所有重複或格式錯誤的 selected directive；Review 依 source order 為每個有效 marker
插入一份完整、已檢查的 projection。marker 可連續出現，也可放在相關說明前後。

## 升級 {#upgrade-notes}

使用 0.2.1 selected-Spec contract 的專案，必須把所有 generic
`<!-- topplecat:acceptance -->` marker 換成精確的帶 ID marker。不要依賴標題、marker
鄰近性、一般引用或 `.feature` 檔案選取範圍。Check、Review 與 scoped Verify 必須收到同一組
確切的 relative `--spec` paths。

## 要求與限制

ToppleCat 執行需要 JDK 21 或 25；使用端 source 可以 target Java 17、21 或 25，但 Gradle
執行環境必須是支援的 JDK。這個 release line 尚未發布到 Maven Central，請先用
`./gradlew publishToMavenLocal` 在本機建置，並把 `mavenLocal()` 放在 `mavenCentral()` 前面，
直到維護者完成另外的發布。

Spec Review 與 Verification Report 仍是 reviewer-only HTML。有限範圍的 `PASS` 只代表選定
AC 已通過，不證明業務 Spec 完整，也不代表組織核准。

## 公開文件 {#documentation-surface}

英文與繁體中文文件、samples 和 acceptance skill 都說明相同的 marker contract 與 local Maven
導入邊界。

要導入目前版本，請先[在本機建置並使用 0.2.2](getting-started.md#ai-assisted-authoring)。
