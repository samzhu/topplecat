# 用 JUnit cart orders 學習 ToppleCat

這是一個可獨立執行、完全合成的 JUnit 專案。它示範一個很常見的情況：公開測試都過了，實作卻還是沒有遵守原本同意的規則。範例使用本機發布的 ToppleCat 0.2.2 artifact；Maven Central 發布是另外的維護者工作，本範例不預設它已完成。

本頁沿用 [ToppleCat 名詞解釋](../../CONTEXT.md) 的詞彙。**驗收方法（Acceptance Method）** 是執行案例的 Java 方法；**型別案例資料列（Typed Case Rows）** 是人寫下的 JSON 或 YAML 例子。公開型別案例資料列會交給實作 AI；**審閱者控制的型別案例資料列（Hidden Tests）** 則用同一個驗收方法跑不同例子，不會偷偷加一條新規則。

## 需求

- JDK 21 或 25（ToppleCat 執行環境）；使用端 source fixture 可以 target Java 17、21 或 25
- 第一次執行時可連到網路，讓 Gradle 下載 wrapper 與 Maven Central 相依套件

```bash
./gradlew test
./demo.sh --help
```

你想了解哪一項？

| 指令 | 說明 |
| --- | --- |
| `./demo.sh public-acceptance` | 公開案例如何拒絕錯誤結果。 |
| `./demo.sh hidden-tests` | 審閱者控制的型別案例資料列（Hidden Tests）如何抓到 checked-in 的 20% 捷徑。 |
| `./demo.sh property-based-testing` | 有界 invariant 如何檢查案例列以外的輸入。 |
| `./demo.sh mutation-testing` | Managed PIT 如何發現驗收方法漏掉的 production change。 |
| `./demo.sh contract-integrity` | Seal 後的契約變動為何不再可信。 |
| `./demo.sh all` | 依序執行五項課程。 |

專案裡的服務刻意放了一個合成的 20% 捷徑。公開的 500 元購物車還是會通過；同一條規則交給審閱者控制的型別案例資料列（Hidden Tests）驗證時，就會被抓出來。其他課程會在暫存副本加入自己的合成錯誤，結束後自動清理。

## 執行完，報告要看什麼？

先跑一條就好：

```bash
./demo.sh hidden-tests
```

指令最後會印出本機合成 HTML 驗證報告的位置。開啟
`build/topplecat/demo-reports/hidden-tests/index.html`，先找這條課程要驗證的 Gate：

| 課程 | 先看哪個 Gate | 報告裡應該看到什麼 |
| --- | --- | --- |
| `public-acceptance` | `JUNIT=FAIL` | 公開型別案例資料列的預期結果和實際結果不同。 |
| `hidden-tests` | `REVIEWER_JUNIT=FAIL` | 審閱者控制的型別案例資料列抓到 20% 捷徑。 |
| `property-based-testing` | `PROPERTY=FAIL` | 一筆產生的輸入違反固定折扣這條不變條件。 |
| `mutation-testing` | `MUTATION=FAIL` | 被削弱的驗收方法沒有抓到產品程式的突變。 |
| `contract-integrity` | `CONTRACT_INTEGRITY=FAIL` | 機械封印後改過型別案例資料列，Verify 因此拒絕執行。 |

這裡的 `FAIL` 是預期結果，因為每條課程就是要示範那一道 Gate 會抓到什麼。先看表中指定的 Gate，再往下讀失敗的 AC 和案例細節。一個合成錯誤可能同時影響別的檢查，所以其他 Gate 也可能失敗。報告只記錄這次執行看到的結果；是否接受交付，仍由人決定。

範例中的 fixtures、數值與診斷皆為合成教學資料。產生的報告只留在本機且已被忽略。
