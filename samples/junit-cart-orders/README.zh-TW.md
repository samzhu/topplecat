# 用 JUnit cart orders 學習 ToppleCat

這是一個可獨立執行、完全合成的 JUnit 範例。它示範 Java 的 Acceptance Method、型別案例資料列、Scenario 與 Stage 如何組成可執行契約。範例直接使用 Maven Central 的 ToppleCat 0.1.0，不需要先建置 ToppleCat 原始碼。

## 需求

- JDK 25
- 第一次執行時可連到網路，讓 Gradle 下載 wrapper 與 Maven Central 相依套件

```bash
./gradlew test
./demo.sh --help
```

你想了解哪一項？

| 指令 | 說明 |
| --- | --- |
| `./demo.sh public-acceptance` | 公開案例如何拒絕錯誤結果。 |
| `./demo.sh hidden-tests` | 獨立案例如何抓到公開案例漏掉的捷徑。 |
| `./demo.sh property-based-testing` | 有界 invariant 如何檢查案例列以外的輸入。 |
| `./demo.sh mutation-testing` | Managed PIT 如何發現驗收方法漏掉的 production change。 |
| `./demo.sh contract-integrity` | Seal 後的契約變動為何不再可信。 |
| `./demo.sh all` | 依序執行五項課程。 |

每一課都會在暫存副本先證明合成 baseline 通過，再套用一個可閱讀的合成偏差、驗證預期 Gate，最後清理。結果是可執行契約的證據，不代表人已選齊所有商業規則，也不替人做交付決定。

範例中的 fixtures、數值與診斷皆為合成教學資料。專案根目錄的三個 `*.java` 與 `*.yaml` 檔是每堂課可閱讀的偏差版本；Gradle 不會編譯它們。
