# Property-Based Testing 對非工程師的說法

> 研究範圍：本文只採用原始 QuickCheck 論文，以及 Hypothesis、jqwik 的官方文件。目的不是定義 ToppleCat 的已實作行為，而是整理適合對非工程師解釋「性質導向測試」的準確語言。

## 可直接使用的說法

> **性質導向測試：先寫下不管輸入怎麼變都應成立的規則，再由工具產生許多不同輸入反覆檢查。**

若要接在「邊緣案例」章節後面，可寫成：

> **不只驗證一筆答案；也驗證規則在許多輸入與邊緣條件下是否仍然成立。**

這兩句保留了三個官方來源共同的核心：人定義可檢查的 property／guarantee（規則、關係或不變條件）、工具提供或產生多組輸入、每一組輸入都檢查同一條 property。QuickCheck 將 properties 描述成可自動檢查的函式，並在大量輸入上檢查它們；Hypothesis 說明測試應對指定範圍內的所有輸入成立、由工具選擇要檢查的輸入；jqwik 則區分帶參數、由 runtime 填入值的 property，與一般 example test。[Claessen & Hughes, 2000](https://www.cs.tufts.edu/~nr/cs257/archive/john-hughes/quick.pdf)；[Hypothesis, n.d.](https://hypothesis.works/)；[jqwik, n.d.](https://jqwik.net/docs/current/user-guide.html)

## 三個來源怎麼說

| 原始／官方來源 | 可轉成白話的重點 | 對簡報文字的啟示 |
| --- | --- | --- |
| QuickCheck 論文 | 測試者寫出程式應有的 properties，工具以大量自動產生的輸入檢查；失敗時回報反例。 | 「先定義規則，再用很多案例驗證規則。」 |
| Hypothesis 官方首頁 | 測試描述一個輸入範圍內都應通過的規則，工具選取不同輸入，包含人可能沒想到的 edge cases；失敗時尋找較簡單的失敗輸入。 | 「工具幫忙探索不同輸入與可能漏掉的邊界。」 |
| jqwik 官方 User Guide | property 是核心概念；相較於 example，property method 有參數，值在測試執行時產生。jqwik 也會混入 edge cases。 | 「不是只固定一組輸入；同一條規則會被多組輸入重複驗證。」 |

## 和範例式測試、邊緣案例測試的差異

### 範例式測試（example-based testing）

範例式測試是人指定一筆固定輸入與固定答案，例如「訂單 1,000 元，運費應為 60 元」。jqwik 明確說它的 example test 與一般 JUnit test case 相同，並把它視為只嘗試一次的 property。[jqwik, n.d.](https://jqwik.net/docs/current/user-guide.html)

### 性質導向測試（property-based testing）

性質導向測試改為描述多組輸入都要遵守的關係，例如：

```text
只要訂單未滿 1,000 元，運費都必須是 60 元。
只要訂單達到 1,000 元，運費都必須是 0 元。
```

工具在定義的輸入範圍內產生多筆資料，逐筆檢查這兩條規則。它不是只把固定案例「加很多筆」，而是讓每筆資料都由同一條可驗證的規則判斷。這符合 QuickCheck 的 property-over-many-cases 做法，以及 jqwik 對 property parameter generation 的定義。[Claessen & Hughes, 2000](https://www.cs.tufts.edu/~nr/cs257/archive/john-hughes/quick.pdf)；[jqwik, n.d.](https://jqwik.net/docs/current/user-guide.html)

### 邊緣案例（edge cases）

邊緣案例是特別容易暴露 bug 或規格缺口的輸入，例如 0、空字串、最小／最大值，或門檻剛好等於 1,000。jqwik 把它們列為 property-based generation 的一部分：工具會把 edge cases 混入產生的資料，甚至可先跑 edge-case 組合。[jqwik, n.d.](https://jqwik.net/docs/current/user-guide.html)

因此最準確的關係是：

```text
性質導向測試 = 規則 + 多組自動產生的輸入
邊緣案例     = 這些輸入中，特別重要的一部分
```

不要把它簡化成「性質導向測試就是找邊緣案例」；它也會驗證一般輸入，而且必須先有人寫出要驗證的規則。

## 對非工程師的投影片建議

### 章節標題

```text
邊緣案例來了，規則還站得住嗎？
```

### 第一頁定義

```text
Property-Based Testing / 性質導向測試

不只驗證一筆答案，
也讓規則接受許多不同輸入與邊緣條件的檢查。
```

### 一句口語說明

```text
範例式測試問：「這一筆對不對？」
性質導向測試問：「換很多種情況，這條規則還對不對？」
```

## 應避免的說法

- 「它會測完所有可能性」：一般 property-based testing 只對有限次產生的輸入做檢查；jqwik 只有在有限的組合空間才可改用 exhaustive generation。[jqwik, n.d.](https://jqwik.net/docs/current/user-guide.html)
- 「它自動知道商業規則」：property／輸入範圍仍由人定義；QuickCheck 指出測試者提供可自動檢查的 criterion，Hypothesis 也要求人描述輸入範圍。[Claessen & Hughes, 2000](https://www.cs.tufts.edu/~nr/cs257/archive/john-hughes/quick.pdf)；[Hypothesis, n.d.](https://hypothesis.readthedocs.io/en/latest/quickstart.html)
- 「它只是在亂數測試」：隨機或產生資料是手段；核心是有一條能判斷成敗的 property。QuickCheck 也讓測試者控制資料生成與分布。[Claessen & Hughes, 2000](https://www.cs.tufts.edu/~nr/cs257/archive/john-hughes/quick.pdf)

## APA 7 參考文獻

Claessen, K., & Hughes, J. (2000). QuickCheck: A lightweight tool for random testing of Haskell programs. *Proceedings of the Fifth ACM SIGPLAN International Conference on Functional Programming*, 268–279. https://doi.org/10.1145/351240.351266

Hypothesis. (n.d.). *Hypothesis: The property-based testing library for Python*. Retrieved August 6, 2026, from https://hypothesis.works/

Hypothesis. (n.d.). *Quickstart*. Retrieved August 6, 2026, from https://hypothesis.readthedocs.io/en/latest/quickstart.html

jqwik. (n.d.). *jqwik user guide*. Retrieved August 6, 2026, from https://jqwik.net/docs/current/user-guide.html
