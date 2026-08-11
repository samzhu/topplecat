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
| `./demo.sh hidden-tests` | 獨立案例如何抓到 checked-in 的 20% 捷徑。 |
| `./demo.sh property-based-testing` | 有界 invariant 如何檢查案例列以外的輸入。 |
| `./demo.sh mutation-testing` | Managed PIT 如何發現驗收方法漏掉的 production change。 |
| `./demo.sh contract-integrity` | Seal 後的契約變動為何不再可信。 |
| `./demo.sh all` | 依序執行五項課程。 |

checked-in 的服務刻意使用合成的 20% 捷徑：一般公開案例的 500 元購物車仍會通過，但 Hidden Tests 示範會拒絕它。每個指令都會在 `build/topplecat/demo-reports/<lesson>/index.html` 留下一份本機、合成的 HTML 驗證報告。指令結束後開啟該檔案，即可查看失敗案例與對應 Gate。

其他課程需要的偏差會在暫存副本套用後清理。結果是可執行契約的證據，不代表人已選齊所有商業規則，也不替人做交付決定。

範例中的 fixtures、數值與診斷皆為合成教學資料。產生的報告只留在本機且已被忽略。
