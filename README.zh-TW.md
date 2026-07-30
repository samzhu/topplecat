# ToppleCat

<p align="center"><img src="docs/images/topplecat-readme-hero.png" alt="ToppleCat 推倒 AI 程式代理的假完成宣稱" width="100%"></p>

<p align="center"><strong>把 AI 程式代理的「已完成」變成這次執行留下的證據。</strong></p>

<p align="center"><a href="README.md">English</a> · <a href="LICENSE">Apache-2.0</a></p>

ToppleCat 是 Java/JUnit 委派工作的驗證關卡。一般 Java 驗收測試與有型別的
JSON/YAML 案例資料列是可執行合約；產生的 JSON 與 HTML 只是投影，不是另一份
事實來源。

它維持三套互不代替的功能：

| 功能 | 自己的輸入與問題 | Gate |
| --- | --- | --- |
| **隱藏測試** | 審閱者保管的有型別案例：獨立設計的例子是否通過？ | `REVIEWER_JUNIT` |
| **突變測試** | 公開驗收測試與 PIT 結果：公開例子是否能發現正式程式行為被改壞？ | `MUTATION` |
| **性質導向測試（Property-Based Testing，PBT）** | 有界的 `@ToppleProperty`：核准的不變量能否通過產生的輸入？ | `PROPERTY` |

三者只共用合約完整性、範圍選擇、報告與整體判定；任何一者都不能替另一者
補證據。審閱者保管只屬於隱藏測試，性質宣告放在 `src/test`。預期值消費另有
`EXPECTED_CONSUMPTION` 保護有型別案例資料列。

ToppleCat 不管理任務、Spec 生命週期、組織簽核、CI 隔離或作業系統安全。人類負責
選定交付範圍、寫完整規則與案例，並決定簽核。

## 兩條管線

```text
./gradlew test
    一般專案測試與公開驗收測試；僅供開發回饋

./gradlew toppleCatVerify --spec path/to/spec.md
    對選定交付範圍產生新的正式驗收證據
```

一般 `test` 不會依賴 Check、Review、Seal、保管、報告或正式證據。每次
`toppleCatVerify` 都會重新執行正式的公開驗收工作，再判定每一項已啟用的獨立防線。
合約完整性通過後，一道防線的失敗會留下自己的結果，但不會阻擋後續防線；證據、報告、
安全回饋與重新隱藏完成後，才以一個整體失敗結束。

## 工作流程

```text
撰寫公開合約、性質與審閱者保管的隱藏案例
    -> toppleCatCheck -> toppleCatReview -> toppleCatSeal
    -> 實作者使用 ./gradlew test
    -> 審閱者或 CI 使用 toppleCatVerify
```

`toppleCatRestore` 只在審閱邊界中使用。修改審閱資料後，依序執行
`toppleCatRestore -> toppleCatCheck -> toppleCatReview -> toppleCatReseal`。
保管資料在 `~/.topplecat/projects/<sha256-project-key>/escrow/`，是明文的本機
狀態，不是加密或沙箱。

`--spec <repository-relative-markdown-file>` 是唯一的交付範圍輸入；可重複指定
多份文件，未指定時選取全部驗收條件。`--all-hidden-tests` 只把隱藏測試從
選定 AC 擴大到全部 AC。性質導向測試依選定 AC 執行；突變測試永遠使用完整
公開驗收合約。

## 撰寫驗收合約

每個 AC 有一個公開 `@ToppleAcceptanceTest`。預設寫法會接收一個
`ToppleScenario` 與一個或多個具體的 capability Stage；方法只負責選取編譯過的
Given/When/Then 順序，前置設定、服務呼叫、斷言與流程控制都放在 Stage 內。

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_cart(c.input("cart", Cart.class));
    scenario.when(coupon).creates_an_order();
    scenario.then(coupon).receipt_matches(c);
}
```

`CouponStage` 是可代理的非 `final` 具體 `ToppleStage`，且必須有可存取的無參數
建構子。同一個 proxy 會帶著該案例的狀態走過三個呼叫。這是唯一支援的驗收撰寫方式。

公開案例在 `src/test/resources/topplecat/cases/`；審閱者案例以同一 schema 放在
`src/hiddenTest/resources/topplecat/cases/`。一列只有 `caseId`、`acId`、`inputs`
與 `expected` 四欄。每個頂層預期值都是斷言義務：`c.verify(...)` 才算消費，
只讀取不算。

## 例子不夠時加入不變量

`@ToppleProperty` 是繫結既有 AC 的有界 JUnit 檢查。它不是案例資料列，也不會
參與預期值消費或突變歸因。

```java
@ToppleProperty("AC-CART-COUPON")
void payableTotalIsNeverNegative(PropertyTrials trials) {
    trials.forAll(Generators.integers(0, 10_000))
        .check(subtotal -> assertTrue(checkout.payable(subtotal) >= 0));
}
```

性質宣告放在 `src/test`，但一般 `./gradlew test` 不會執行。
`toppleCatVerify` 會在獨立的 `PROPERTY` gate 中執行選定 AC 的性質導向測試。
可重現的失敗會在僅供審閱者查看的 Verification Evidence 顯示產生器選擇、
縮小後的反例與重播記號；安全回饋絕不包含產生的輸入、識別字、重播記號、
路徑或原始失敗訊息。

## 設定功能

每個功能都有自己的開關。關閉時記錄為 `DISABLED`，不會假裝成
`NOT_APPLICABLE` 或 `PASS`。

```kotlin
toppleCat {
    hiddenTests { enabled.set(false) }
    mutationTesting { enabled.set(false) }
    propertyBasedTesting { enabled.set(false) }
    expectedConsumption { enabled.set(false) }
}
```

若隱藏測試仍啟用卻沒有執行隱藏案例，`REVIEWER_JUNIT=INCOMPLETE`，即使性質
通過也一樣。只使用性質的團隊必須明確關閉隱藏測試並重新 Seal；這時證據會顯示
`REVIEWER_JUNIT=DISABLED` 與實際的 `PROPERTY` 結果。

## 閱讀結果

| 產物 | 對象 | 用途 |
| --- | --- | --- |
| `build/topplecat/reports/review/index.html` | 審閱者 | 交付前的 Contract Review。 |
| `build/topplecat/reports/public/index.html` | 公開 | Verify 後安全的公開驗收投影。 |
| `build/topplecat/reports/verification/index.html` | 審閱者 | 含私有結果的 Verification Evidence。 |
| `build/topplecat/evidence.json` | 審閱者 / CI | machine verdict 與 gate 摘要值。 |
| `build/topplecat/agent-feedback.json` | 實作代理 | 只有 gate 層級的安全回饋。 |

每次正式執行都記錄 `CONTRACT_INTEGRITY`、`JUNIT`、`REVIEWER_JUNIT`、
`EXPECTED_CONSUMPTION`、`PROPERTY` 與 `MUTATION`。整體結果為 `PASS`、`FAIL`
或 `INCOMPLETE`；只有本次執行的 `PASS` 才能接受完成宣稱。

## 安裝 0.0.8

ToppleCat 需要 Java 25 與相容的 Gradle。

```kotlin
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.8"
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.0.8")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

## 延伸閱讀

- [快速開始](docs/guide/getting-started.md)
- [撰寫合約](docs/guide/authoring.md)
- [驗證與證據](docs/guide/verification-and-evidence.md)
- [文件索引](docs/README.md)
- [共同語言](CONTEXT.md)
- [架構](docs/architecture.md)
- [0.0.8 release notes](docs/releases/0.0.8.md)
- [JUnit 範例](samples/junit-cart-orders)
- [Spring Boot 範例](samples/spring-boot-cart-orders)

Repository 也提供
[`topplecat-acceptance`](.agents/skills/topplecat-acceptance/SKILL.md)
skill，協助 SDD agent 把選定的 AC 寫成可執行 Java 驗收方法、公開與審閱者
案例，以及選用的性質導向測試。ToppleCat 的執行與最終判定仍由人類或外部
工作流程負責。
