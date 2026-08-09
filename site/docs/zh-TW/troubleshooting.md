---
title: Troubleshooting
description: 依照可見症狀分開 what ran、what happened，以及證據支持的 Gate 結論。
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

# Troubleshooting

## 症狀地圖 {#symptom-map}

先從看得到的症狀開始，再分清 external observation、ToppleCat attribution、Gate
consequence 與安全的下一步。單獨一個 status word 不是診斷。

## Acceptance Method 無法編譯

**Observation：** `toppleCatCheck` 報告 binding、parameter、Stage 或直接 Scenario
authoring 有問題。

**Attribution：** 選定的 AC 不符合要求的一般 Java/JUnit Acceptance Method 形狀，還
不能信任完整 formal contract。

**Gate consequence：** Contract Integrity 無法建立 downstream evidence。

**Next action：** 保持 `ToppleCase` 第一個、非 generic `ToppleScenario` 第二個，之後放
不同的非 final concrete Stage，並提供可存取的無參數 constructor。把 setup 與 assertions
移到 Stage method。

## 案例列或選定 AC 沒有 binding

**Observation：** Typed row 或選定的 Spec AC 沒有可編譯的公開
`@ToppleAcceptanceTest` method。

**Attribution：** row 不能建立新規則，必須指向已存在的 public AC binding。

**Gate consequence：** Check 在產生可信的 formal evidence 前就停止。

**Next action：** 修正 literal AC ID、Spec 或 `--ac` selection，或補上缺少的公開 method。
規則本身是否完整仍是人的責任。

## Public Acceptance 失敗 {#public-acceptance}

**Observation：** JUnit Acceptance Method 將 authored expected value 與 actual result
比較後發現不一致。

**Attribution：** mismatch 屬於那個 public case 與 Acceptance Method；它不說明意圖，
也不涵蓋未寫出的案例。

**Gate consequence：** `JUNIT` Gate 如實記錄已完成但發現問題。Mutation Testing 因為
沒有通過的 baseline 而是 `INCOMPLETE`；獨立的 Hidden Tests 與 Properties 仍可能各自
產生結果。

**Next action：** 先看 public input 與 expected/actual comparison，再視情況修改實作或
人寫的契約。不要拿更早的 artifact 取代這次執行。

## Evidence 不完整 {#incomplete-evidence}

**Observation：** Safeguard 沒有產生可信的本次執行證據，例如 task 被中斷、current
sidecar 不見，或 Property lifecycle 和 sealed declaration 對不上。

**Attribution：** ToppleCat 無法誠實地把完整 observation 歸因到本次執行。先前 archive
只能用來診斷。

**Gate consequence：** 該 safeguard 是 `INCOMPLETE`；aggregate `PASS` 不成立。

**Next action：** 依文件 workflow 從乾淨的 current run 重新執行。檢查本次 task output 與
新產生的 evidence，不要讀 archived run 來填洞。

## Hidden coverage 或 mutation evidence 不見了

**Observation：** 選定 AC 沒有執行 hidden typed row，或 managed mutation producer
沒有可用的 full matrix。

**Attribution：** Hidden Tests 與 Mutation Testing 回答不同問題，一邊不能借證據給另一邊。
Mutation Testing 還需要 Public Acceptance baseline 通過。

**Gate consequence：** 相應 safeguard 維持 `INCOMPLETE`（或明確 sealed 的 `DISABLED` /
`NOT_APPLICABLE`），並保留原因。

**Next action：** 加入獨立選出的 hidden row 並重新 seal，或修復支援的 formal workflow。
不要把 reviewer-owned source 或值交給 Implementation Agent。

## 安全的下一步 {#safe-next-action}

症狀不清楚時，先讀 Verification Report 的白話原因，再讀 canonical technical evidence。
維持三個層次：external producer 看到了什麼、ToppleCat 怎麼歸因，以及本次證據支持哪個
Gate 結論。
