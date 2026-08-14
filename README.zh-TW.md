# ToppleCat

<p align="center"><img src="docs/images/topplecat-readme-hero.png" alt="ToppleCat 推倒 AI 程式代理的假完成宣稱" width="100%"></p>

<p align="center"><strong>agent 每次交付，都得先通過考驗，才拿得到 PASS。</strong></p>

<p align="center"><a href="README.md">English</a> · <a href="LICENSE">Apache-2.0</a></p>

ToppleCat 是給 Java/JUnit 團隊使用的開源驗證工具。團隊可以把功能交給 AI coding
agent 實作，但仍由人負責驗收。AI 說完成後，ToppleCat 會用人事先確認的業務規則，
從幾個不同角度重新檢查，並說明這次執行發生了什麼。

ToppleCat 把這些可以執行的規則稱為「可執行契約」。只有本次執行的每一道必要檢查
都通過，才會記錄 `PASS`，再把結果背後的證據交給負責驗收的人。

一般 Java 驗收測試與有型別的 JSON/YAML 案例資料列是事實來源。產生的 JSON
與 HTML 只解釋檢查了什麼，不會變成第二份規格。

## 一個具體例子

結帳 Spec 規定訂單滿 1,000 元折扣 100 元。公開案例檢查
`1,000 -> 900`，但實作也可能把 `900` 寫死而照樣通過。

Reviewer 可以在把工作交給 AI 前準備更多檢查：

- 審閱者控制的型別案例資料列（Hidden Tests），例如 `2,000 -> 1,900` 或
  `999 -> 不折扣`；
- 「應付金額永遠不為負數」這類 Property；以及
- Managed Mutation Testing，檢查公開 Acceptance Method 能否發現邊界或
  算術被改壞。

實作完成後，ToppleCat 會獨立執行每一個啟用的 safeguard，並把結果放在同一份
報告。若選定 AC 的公開驗收讓一個精確歸因的改動程式通過，該 AC 的 Mutation
Testing 與整體執行會失敗；只有每個必要 Gate 都通過，ToppleCat 才會為本次
執行記錄 `PASS`。最後仍由人類閱讀證據並決定。若 Spec 從未寫出 VIP 折扣，
ToppleCat 也不會自行猜出這條規則。

## 放在交付流程中的位置

```text
人類選定 Spec 並準備可執行契約
    -> Spec Review：確認將用什麼驗收
    -> AI 使用一般 ./gradlew test 回饋完成實作
    -> toppleCatVerify：產生新的正式證據
    -> Verification Report：呈現本次 verdict 與各 AC 證據
    -> 人類決定如何處理交付
```

兩份 HTML 都是給人類 Reviewer 閱讀。Implementation Agent 取得公開契約與安全的
Gate 層級回饋，不會取得 reviewer-owned 案例或任何一份 HTML。

公開專案頁是給人類訪客看的產品說明，不是 Implementation Agent handoff。頁面上清楚標示的
red-team 示範可為了解釋某項 safeguard 而呈現完整的合成報告細節；它們不是實際交付的
evidence、feedback 或核准。

ToppleCat 提供命令、證據與報告；團隊自行決定由誰執行，以及放在本機、CI 或
其他 workflow。

## ToppleCat 檢查什麼

| Safeguard | 問題 | Gate |
| --- | --- | --- |
| **Hidden Tests** | 審閱者控制的型別案例資料列，能否通過同一個公開 Acceptance Method？ | `REVIEWER_JUNIT` |
| **Property-Based Testing** | 人類核准的不變量，能否通過有界的產生輸入？ | `PROPERTY` |
| **Mutation Testing** | 每個精確的公開 Acceptance Method，能否發現自己執行到的 managed-profile mutants？ | `MUTATION` |

三者彼此獨立：一種結果不能替另一種補證據，也不會混成一個品質分數。
Contract Integrity 確認完整契約與驗證政策仍符合 Mechanical Seal；
Expected Consumption 另外確認作者寫下的預期值真的有被斷言。

正式 Mutation Testing 使用 ToppleCat 固定、版本化的 PIT profile，並保留 PIT
官方結果。專案自己的 PIT task 維持獨立，不會進入 ToppleCat evidence。詳細規則見
[驗證與證據指南](docs/guide/verification-and-evidence.md#independent-formal-work)。

如果某個 AC 未改變的公開驗收漏掉了歸因到它的突變，報告會先顯示已偵測與未偵測的數量，
再只列出未偵測的改動卡片。卡片說明改變、正式原始碼位置，以及該驗收仍然通過；只有 PIT
描述與唯一的原始碼行足以支持時，才會顯示確切替換。這些細節只給審閱者，不會進入安全的
agent feedback。報告也會在 Mutation Testing 與容易陌生的卡片術語旁放置精簡的 `ⓘ` 控制，
並用所選報告語言提供短說明。滑鼠移入或鍵盤聚焦會顯示說明；點擊或點按會讓說明保持開啟，
直到審閱者關閉它。即使不打開說明，原本可見的結果與技術揭露仍然可讀。

## 快速開始

ToppleCat 0.2.2 是目前的 tagged release line，但這次 release 尚未發布到 Maven Central。
本 checkout 的 selected-Spec Review 行為不在已發布的 0.2.0 中。若要現在使用
0.2.2，請先 clone 本 repository 並執行 `./gradlew publishToMavenLocal`，
再在使用端的 plugin 與 dependency repository 中把 `mavenLocal()` 放在
`mavenCentral()` 前面。已發布的 0.2.0 artifact 仍可從
[Maven Central](https://central.sonatype.com/namespace/io.github.samzhu.topplecat)
取得，但不提供以下 selected-Spec 行為。使用端專案可以 target Java 17、21 或 25；
Gradle/plugin 的執行環境必須是 JDK 21 或 25，只使用 JDK 17 執行 ToppleCat 不受支援。

`settings.gradle.kts`：

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

`build.gradle.kts`：

```kotlin
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.2.2"
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.2.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

實作前先準備並檢視功能 Spec，再封印完整契約。AI 宣稱完成後，正常流程會驗證整個專案：

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal

./gradlew test
./gradlew toppleCatVerify
```

若 AI 剛完成一段工作，Reviewer 想先快速確認那次範圍，可以用一份或多份 Spec，或重複
指定 AC ID 來執行 Verify；兩種選法不能混用：

```bash
./gradlew toppleCatVerify --spec specs/checkout/spec.md
./gradlew toppleCatVerify --ac AC-CHECKOUT-THRESHOLD --ac AC-CHECKOUT-VIP
```

`toppleCatCheck` 與 `toppleCatReview` 的每個 `--spec` 都必須是 repository-relative 的
Markdown 檔案。完整選定文件使用精確、獨立一行的帶 ID marker，例如
`<!-- topplecat:acceptance:AC-CHECKOUT-001 -->`，它是唯一的 AC identity 與 Review 插入點。
標題可以是任意層級與文字；一般文字、清單、表格、連結、inline code 或 fenced code 中的
引用不會選取 AC。重複、格式錯誤、舊 generic 或容器內 directive 會以可修復診斷失敗；
`.feature` 檔案不會被讀取或翻譯。

範圍驗證報告會明說這種 `PASS` 只代表列出的 AC 通過，不代表完整可執行契約通過。CI
應使用不帶選項的 `toppleCatVerify`。

Reviewer HTML 預設使用英文。要以繁體中文閱讀 ToppleCat 自己的報告介面，請在
`toppleCatReview` 或 `toppleCatVerify` 加上
`--language zh-TW`。這只影響該次執行的 HTML；作者寫下的文字與 AC ID、Gate 名稱、
verdict、PIT 結果等 canonical 值都會維持原樣。

從[快速開始指南](docs/guide/getting-started.md)開始。若想實際了解每項保障會抓到什麼
問題，可選擇執行 [JUnit cart-orders 學習專案](samples/junit-cart-orders)：它有 Public
Acceptance、Hidden Tests、Property-Based Testing、Mutation Testing 與 Contract
Integrity 五條完全合成的路徑。每次 `./demo.sh <lesson>` 後，都能在
`build/topplecat/demo-reports/<lesson>/index.html` 開啟該次的本機合成 HTML 驗證報告。
範例 README 會說明每條路徑要先看哪一道 Gate，以及預期的失敗代表什麼。

## 最小驗收契約

每個 Acceptance Condition 有一個公開 Java/JUnit Acceptance Method。方法描述一個
有順序的 Scenario；一般 Java Stage 方法負責業務呼叫與斷言。

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_cart(c.input("cart", Cart.class));
    scenario.when(coupon).creates_an_order();
    scenario.then(coupon).receipt_matches(c);
}
```

Typed Case Rows 提供輸入與預期結果：

```yaml
- caseId: coupon-at-threshold
  acId: AC-CART-COUPON
  inputs:
    cart: {subtotal: 1000}
  expected:
    receipt: {total: 900}
```

公開案例放在 `src/test`；reviewer-owned 案例以相同 schema 放在
`src/hiddenTest`。完整 Java、Stage、案例、預期值與 Property 規則請見
[撰寫驗收合約](docs/guide/authoring.md)。

## 閱讀結果

| 產物 | 對象 | 用途 |
| --- | --- | --- |
| `build/topplecat/reports/review/index.html` | Reviewer | 交付前的 Spec Review：完整已選 Spec 與其綁定的可執行材料，不是執行結果。 |
| `build/topplecat/reports/verification/index.html` | Reviewer | 本次正式執行的 Verification Report，先呈現各 AC 結果與私有診斷。 |
| `build/topplecat/evidence.json` | Reviewer／automation | machine-readable 本次 verdict 與 Gate 摘要值。 |
| `build/topplecat/agent-feedback.json` | Implementation Agent | 不含 reviewer 答案的安全 Gate 層級修正方向。 |

兩份 Reviewer HTML 都支援在各自的報告命令選擇 `--language en` 或
`--language zh-TW`。這會改變標題、無障礙文字、控制項、說明與 HTML 語言中繼資料，
但不會改變報告 JSON、evidence、安全 agent feedback、Mechanical Seal 或契約本身。
Verification Report 會先顯示白話的閱讀結果，再提供 canonical 技術證據。案例失敗時，
會先列出輸入以及預期值與實際值的差異；Scenario Steps、原始失敗、Gate 判定與 PIT
細節仍可展開深入查閱。每個 AC 一開始都會顯示關鍵結果；可用 AC 閱讀控制深入閱讀
單一 AC，或用報告範圍的控制逐步展開所有 AC。技術證據仍各自收合，連結到 AC 或防護
結果時，報告會先顯示所需內容再移動到目標。

整體 verdict 有三種：

- `PASS`：本次執行的每個必要 Gate 都通過；若有指定範圍，僅代表該範圍的 AC；
- `FAIL`：完整執行發現阻擋性的 AC 或 Gate 問題；以及
- `INCOMPLETE`：沒有取得足夠可信的本次執行證據。

無論哪一種結果，最後都由人類決定。

## 產品邊界

ToppleCat 從可執行驗收邊界開始：

- 人類或 External Workflow 選擇 Spec，並負責讓規則與案例完整。
- 團隊負責任務狀態、Spec 生命週期、執行位置、PR 政策、組織核准與簽核。
- 一般 unit／QA tests、專案自己的 PIT、效能計畫與安全計畫仍是專案責任。
- Reviewer Custody 是明文的機械式保管，不是加密、sandbox、CI 隔離或
  作業系統安全。

提出新的 ToppleCat 責任前，先讀 canonical
[產品定義](docs/product.md)。

## 延伸閱讀

- [快速開始](docs/guide/getting-started.md)
- [撰寫合約](docs/guide/authoring.md)
- [驗證與證據](docs/guide/verification-and-evidence.md)
- [產品定義](docs/product.md)
- [架構](docs/architecture.md)
- [共同語言](CONTEXT.md)
- [文件索引](docs/README.md)
- [0.2.2 release notes](docs/releases/0.2.2.zh-TW.md)
- [0.2.0 release notes](docs/releases/0.2.0.zh-TW.md)
- [JUnit 範例](samples/junit-cart-orders)
- [Spring Boot 範例](samples/spring-boot-cart-orders)

Repository 也提供
[`topplecat-acceptance`](.agents/skills/topplecat-acceptance/SKILL.md)
skill，協助把已選 AC 寫成可執行 Java 驗收方法、公開與 reviewer 案例，以及
選用的 Properties。人類或 External Workflow 仍負責執行 ToppleCat，並決定如何
處理它的證據。
