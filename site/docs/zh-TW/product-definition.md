---
title: Product definition
description: 認識 ToppleCat 的使用者、use moments、承諾與責任邊界。
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

# Product definition

## 核心使用時機 {#use-moment}

ToppleCat 服務的是把實作交給 AI coding agent、但仍由人負責 acceptance 的 Java/JUnit
團隊。交付前，Reviewer 閱讀完整的 selected Spec 與即將執行的 executable contract。
agent 宣稱 done 後，formal Verify 產生全新的 evidence，讓人做交付決定。

## 承諾

AI 加速實作；人強化驗證。ToppleCat 將人選定的 Acceptance Condition 綁到一般
Java/JUnit Acceptance Method 與型別化案例列，封存完整契約與 policy，只有本次執行每個
必要 Gate 都通過時，才記錄 `PASS`。

`PASS` 或 `FAIL` 都不證明上游 Spec 沒有遺漏規則。ToppleCat 報告的是已檢查契約與當次
證據支持的事情。

## 使用者與 ownership

Reviewer 閱讀 Spec Review 與 Verification Report。Implementation Agent 收到公開契約與
safe Gate-level feedback。External Workflow 選定目前 Spec、決定命令何時何地執行、管理
delivery history，並套用組織政策。

## Responsibility boundary {#responsibility-boundary}

| ToppleCat 擁有 | 人、團隊、專案或 External Workflow 擁有 |
| --- | --- |
| 將 selected AC 綁到一般 executable acceptance work | 選定 Spec，並讓規則與案例完整 |
| 封存契約 bytes 與 verification policy | 組織審閱、approval、delivery history 與 sign-off |
| 全新的 formal verification 與獨立 Gate evidence | 命令在哪裡執行，以及 CI/PR policy 如何套用 |
| Reviewer reports 與安全的 Implementation Agent feedback | Task management、Spec lifecycle 與 project release decision |
| Managed mutation profile 與精確 AC attribution | 一般 QA、自訂 PIT、效能與安全性計畫 |

## ToppleCat 不擁有的事 {#what-topplecat-does-not-own}

ToppleCat 不是 task manager、Spec lifecycle manager、approval system、CI product、
general test framework、Javadoc catalogue，也不是 operating-system security boundary。
公開文件是對目前產品的解釋，不是凌駕 Executable Contract 的新權威。

請看 [Architecture](architecture.md#four-modules) 了解實作模組，並看
[Glossary](glossary.md#executable-contract) 了解正式詞彙。
