---
title: ToppleCat 如何檢查交付
description: 認識 ToppleCat 用哪些方法重新檢查 AI 寫好的功能，以及 PASS、FAIL 和證據不足各代表什麼。
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

# ToppleCat 如何檢查交付

ToppleCat 不會只給一個模糊分數。它把驗收拆成幾個不同問題：公開範例是否通過、
換一批合法案例會不會失敗、預期結果有沒有真的比較，以及原本的驗收能不能察覺暫時
改壞的程式。

每個問題各自留下結果。所有必要檢查都有可信的當次證據並通過，這次交付才會取得
`PASS`。

## 結帳功能會經過什麼 {#delivery-example}

公開優惠券案例通過，AI 寫的功能看起來沒有問題。ToppleCat 接著把審閱者另外選出的
例子送進同一個公開驗收方法，也會暫時改動程式行為，看看原本的方法能不能察覺。

如果折扣門檻暫時改變後，那個方法仍然通過，ToppleCat 找到的是這條規則的驗收弱點。
它沒有宣稱原始程式本來就含有這個暫時變更。驗證報告會寫清楚發生什麼、
觀察屬於哪條規則，以及這次執行為什麼不能取得 `PASS`。

## 報告回答哪些問題

每一道檢查回答不同問題，所以報告不會把它們混成一個分數：

| 這次交付要回答的問題 | 發現問題時代表什麼 | Gate 名稱 |
| --- | --- | --- |
| 現在執行的還是審閱者封存的契約嗎？ | 公開驗收內容或驗證政策在審閱後改變 | `CONTRACT_INTEGRITY` |
| 公開案例有通過嗎？ | 實作不符合 AI 原本看得到的例子 | `JUNIT` |
| 審閱者另外選出的案例也通過同一個方法嗎？ | 實作沒有處理某個獨立選出的邊界 | `REVIEWER_JUNIT` |
| 人寫下的預期結果真的有被斷言嗎？ | 測試只讀取或跳過結果，沒有實際比較 | `EXPECTED_CONSUMPTION` |
| 核准過的不變條件能通過多組產生輸入嗎？ | 找到反例，或 Property 沒有留下完整可信證據 | `PROPERTY` |
| 每個公開方法能察覺歸屬於它的暫時程式變更嗎？ | 驗收方法對相關變更沒有反應，或缺少可信 baseline | `MUTATION` |

其中一項通過，不能補另一項的洞。審閱者案例通過，不會抵銷性質導向測試找到的問題；
產生輸入都通過，也不代表公開方法一定能察覺暫時的程式變更。

## 執行流程

AI 開始實作前，審閱者先確認真正會執行的內容，再封存完整契約：

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
```

AI 使用一般的 `./gradlew test` 開發。它宣稱完成後，再執行：

```bash
./gradlew test
./gradlew toppleCatVerify
```

審閱者閱讀 `build/topplecat/reports/verification/index.html`，自動化流程讀取
`build/topplecat/evidence.json`。兩者都只描述這次執行；舊報告不能拿來補本次缺少的
證據。

## 從觀察走到判定 {#three-evidence-layers}

需要深入診斷時，把一個結果拆成三層：

1. 外部工具先記錄它觀察到的事情。JUnit、性質檢查引擎與 PIT 都保留自己的正式
   結果名稱。
2. ToppleCat 把觀察連到負責這個問題的驗收方法、案例、Property 或封存政策。
3. 封存政策根據這份歸屬明確的證據，產生各項檢查結果與整體判定。

這個區分可以避免把工具訊息誤讀成業務結論。產生的 JSON 與 HTML 只報告已檢查的
契約和觀察結果，不會自行加入規則。

## 各項檢查與整體結果 {#gates-and-verdicts}

整體結果刻意只有三種：

- `PASS`：這次執行中，每一道必要檢查都通過。
- `FAIL`：某道完成的檢查找到阻擋問題。
- `INCOMPLETE`：ToppleCat 沒有取得足夠、可信的當次證據。

單一道檢查也可能被政策明確停用，或不適用於這次範圍；兩者都不會偷偷當成通過。

CI 的正常做法是驗證完整契約。審閱者若只想快速看某次交付，可以指定 Spec 或 AC
ID，但兩種方式不能混用。有限範圍的 `PASS` 只表示列出的範圍通過，不代表整個專案
都通過。

## 審閱者的資訊邊界 {#reviewer-boundary}

Spec Review 與 Verification Report 只給審閱者閱讀。實作 AI 收到的是不洩漏私人答案
的檢查層級回饋：它會知道哪一類工作需要處理，但不會取得審閱者案例、值、路徑、
反例或原始私有 failure。

AI 可以摘要公開文件，也可以協助修改公開實作。私人報告由人保管，是否接受交付仍由
審閱者決定。

如果結果不符合預期，先看[排除問題](troubleshooting.md#symptom-map)。信任邊界與
資訊流向請讀[從規則到結果](architecture.md#execution-flow)。
