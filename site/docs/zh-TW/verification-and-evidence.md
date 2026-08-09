---
title: 驗證交付並讀懂結果
description: AI agent 說完成後執行 ToppleCat，弄清楚哪些檢查通過、哪裡有問題，以及還需要人做什麼決定。
page_id: verification-and-evidence
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../verification-and-evidence/
alternate_language: en
alternate_label: English
alternate_en: ../verification-and-evidence/
alternate_zh_tw: ./verification-and-evidence/
markdown_url: verification-and-evidence.md
copy_label: Copy Markdown
copied_label: Copied
---

# 驗證交付並讀懂結果

agent 開發時，綠色測試很有用，但它還不是最後的交付結論。正式 Verify 會對封存過的
約定開始一次全新執行。好幾個彼此不同的問題都得到可信結果後，這次交付才可能取得
`PASS`。

## 結帳功能會經過什麼 {#delivery-example}

公開優惠券案例通過，agent 的實作看起來沒有問題。ToppleCat 接著把審閱者另外選出的
例子送進同一個公開驗收方法，也會暫時改動 production code 的行為，看看原本的方法
能不能察覺。

如果折扣門檻暫時改變後，那個方法仍然通過，ToppleCat 找到的是這條規則的驗收弱點。
它沒有宣稱原始程式本來就含有這個暫時變更。Verification Report 會寫清楚發生什麼、
觀察屬於哪條規則，以及這次執行為什麼不能取得 `PASS`。

## 把報告當成幾個問題來讀

每一道檢查回答不同問題，所以報告不會把它們混成一個分數：

| 這次交付要回答的問題 | 發現問題時代表什麼 | Gate 名稱 |
| --- | --- | --- |
| 現在執行的還是 Reviewer 封存的契約嗎？ | 公開驗收內容或驗證政策在審閱後改變 | `CONTRACT_INTEGRITY` |
| 公開案例有通過嗎？ | 實作不符合 agent 原本看得到的例子 | `JUNIT` |
| 審閱者另外選出的案例也通過同一個方法嗎？ | 實作沒有處理某個獨立選出的邊界 | `REVIEWER_JUNIT` |
| 人寫下的預期結果真的有被斷言嗎？ | 測試只讀取或跳過結果，沒有實際比較 | `EXPECTED_CONSUMPTION` |
| 核准過的不變條件能通過多組產生輸入嗎？ | 找到反例，或 Property 沒有留下完整可信證據 | `PROPERTY` |
| 每個公開方法能察覺歸屬於它的暫時程式變更嗎？ | 驗收方法對相關變更沒有反應，或缺少可信 baseline | `MUTATION` |

其中一項通過，不能補另一項的洞。審閱者案例通過，不會修好 Property failure；
Property 通過，也不代表公開方法一定能察覺暫時的程式變更。

## 執行流程

handoff 前，Reviewer 先確認真正會執行的內容，再封存完整契約：

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
```

agent 使用一般的 `./gradlew test` 開發。它宣稱完成後，再執行：

```bash
./gradlew test
./gradlew toppleCatVerify
```

Reviewer 閱讀 `build/topplecat/reports/verification/index.html`，自動化流程讀取
`build/topplecat/evidence.json`。兩者都只描述這次執行；舊報告不能拿來補本次缺少的
證據。

## 從觀察走到判定 {#three-evidence-layers}

需要深入診斷時，把一個結果拆成三層：

1. 外部工具先記錄它觀察到的事情。JUnit、Property engine 與 PIT 都保留自己的正式
   outcome 名稱。
2. ToppleCat 把觀察連到負責這個問題的驗收方法、案例、Property 或封存政策。
3. 封存政策根據這份歸屬明確的證據，產生 Gate 結果與整體判定。

這個區分可以避免把工具訊息誤讀成業務結論。產生的 JSON 與 HTML 只報告已檢查的
契約和觀察結果，不會自行加入規則。

## Gate 與整體結果 {#gates-and-verdicts}

整體結果刻意只有三種：

- `PASS`：這次執行中，每個必要 Gate 都通過。
- `FAIL`：某道完成的檢查找到阻擋問題。
- `INCOMPLETE`：ToppleCat 沒有取得足夠、可信的當次證據。

單一道檢查也可能被政策明確停用，或不適用於這次範圍；兩者都不會偷偷當成通過。

CI 的正常做法是驗證完整契約。Reviewer 若只想快速看某次交付，可以指定 Spec 或 AC
ID，但兩種方式不能混用。有限範圍的 `PASS` 只表示列出的範圍通過，不代表整個專案
都通過。

## Reviewer 的資訊邊界 {#reviewer-boundary}

Spec Review 與 Verification Report 只給 Reviewer 閱讀。實作 agent 收到的是安全的
Gate-level feedback：它會知道哪一類工作需要處理，但不會取得審閱者案例、值、路徑、
反例或原始私有 failure。

AI 可以摘要公開文件，也可以協助修改公開實作。私人報告由人保管，是否接受交付仍由
Reviewer 決定。

如果結果不符合預期，先看[排除問題](troubleshooting.md#symptom-map)。信任邊界與
資訊流向請讀[系統如何運作](architecture.md#execution-flow)。
