# ToppleCat

<p align="center">
  <img
    src="docs/images/topplecat-readme-hero.png"
    alt="ToppleCat 推倒 AI 程式代理的假完成宣稱"
    width="100%"
  >
</p>

<p align="center">
  <strong>把 AI 程式代理的「已完成」變成證據。</strong>
</p>

<p align="center">
  <a href="README.md">English</a>
  ·
  <a href="https://github.com/samzhu/topplecat/actions/workflows/ci.yml">建置</a>
  ·
  <a href="LICENSE">Apache-2.0</a>
</p>

ToppleCat 是一隻充滿好奇心的貓。

每當 AI 程式代理宣稱某項 Java 工作**已完成**，ToppleCat 都會伸手撥一下。
它會拿代理沒看過的隱藏案例重跑合約，也會用突變測試故意改壞正式程式，
看看公開測試能不能抓到問題。每個已宣告的預期結果，也必須真的拿來和實際
行為比對。這些檢查能找出代理憑空編造的規則、漏做功能卻宣稱完成的空殼實作，
以及為了通過公開案例而把輸入與答案寫死在程式裡的做法。

ToppleCat 不會只採信代理回報的一個綠燈。完成與否，要看這次執行的所有關卡
和證據。

> 用隱藏案例重測、突變測試與可執行的 Java 驗收合約，檢查 AI 程式代理的
> 「已完成」宣稱。空洞的完成，站不住腳。

Robert C. Martin（Uncle Bob）最近也談到類似的工作方式：

> I’m significantly older than you. I started coding in the late 60s. My
> current strategy is to not read any of the code written by my agents. That’s
> the only way I can take advantage of their productivity. What I do instead is
> to surround the agents with extreme constraints. Unit tests, gherkin tests,
> QA procedures, quality metrics, mutation testing, test coverage, and a
> plethora of others. In the end, I have very high confidence in the code they
> produce because they’ve had to run the gauntlet of all of my constraints and
> tests.
>
> — [Robert C. Martin，2026 年 7 月 23 日](https://x.com/unclebobmartin/status/2080257779395154409)

他的做法是先讓代理寫出的程式通過層層限制與測試，再決定能不能相信。
ToppleCat 處理的是其中 Java/JUnit 委派驗證這一段：可執行的驗收合約、
審閱者專用案例、預期結果檢查與突變測試。它不能取代程式碼審查、QA、
CI 隔離或沙箱。

ToppleCat 是 Java/JUnit 委派工作的驗證關卡。一般 Java 驗收測試與有型別的
JSON/YAML 案例資料列才是可執行合約；產生的 JSON 與 HTML 是證據，不是
另一份事實來源。

主要驗收情境是讀起來像業務流程的一般 Java 程式碼。
[JGiven](https://github.com/TNG/JGiven) 是最接近的先行專案。ToppleCat
在這個做法外再加一層審閱邊界，用隱藏案例重測、突變測試證據與安全回饋，
檢查代理的完成宣稱。它不用 Cucumber 或
Gherkin，也沒有第二套可執行規格格式。

## ToppleCat 會檢查什麼

| 綠燈仍可能代表…… | ToppleCat 如何檢查 |
| --- | --- |
| 實作可能只針對看得見的範例調整。 | 由審閱者控制、依不同業務情境設計的**隱藏案例重測**。 |
| 測試有執行，卻無法察覺壞掉的行為。 | 由 PIT 執行的**突變測試關卡**。 |
| 預期結果已讀取，卻沒有和實際結果比較。 | 強制檢查**預期結果是否真的被驗證**。 |
| 審閱後公開合約或驗證強度被改變。 | 強制執行、由審閱者封存的**合約完整性關卡**。 |
| 舊的或不完整的輸出被誤認為本次證明。 | 每次執行各自保存關卡結果、摘要值與明確判定。 |

隱藏案例重測與突變測試回答的是不同問題。隱藏案例重測會用實作代理沒看過的
業務案例檢查行為，但不能保證抓到每一種硬編碼捷徑。預設的 PIT 執行工作衡量
的是**公開可執行合約的突變測試強度**，只使用 `sourceSets.test`、公開測試類別
與公開案例資料列。

審閱者案例資料列與審閱者專用 JUnit 測試，不會幫預設的 PIT 執行工作殺死
突變版本。若某個邊界條件必須由突變測試保護，就應把它寫進公開合約。
ToppleCat 管理 PIT 執行工作時，會從編譯器產生的描述檔找出每個公開的主要
`@ToppleTest` 類別，並把它們設成 `targetTests`。使用端自行設定的
`targetTests` 與自訂突變測試執行方式都會保留。ToppleCat 不會另外計算每個
案例的突變分數，也不會猜測自訂執行方式涵蓋了哪些範圍。

審閱者案例資料列可以重用公開的主要 `@ToppleTest`。當 `src/hiddenTest` 只有
案例資料列、沒有 Java 測試時，就由這個主要測試執行隱藏案例。只有主要測試
無法表達的額外行為，才需要審閱者專用的 Java 測試。若已啟用隱藏案例重測，
`src/hiddenTest` 裡只有輔助用 Java 原始碼、沒有可執行的 JUnit 方法，
這些原始碼只會被編譯，不會被當成隱藏測試。若隱藏案例資料列與隱藏 Java
測試都不存在，ToppleCat 會安全地維持未完成狀態，將 `REVIEWER_JUNIT`
記為 `INCOMPLETE`。

單一關卡只能說明自己檢查的事情，不能代表整份實作一定正確。隱藏案例可能
通過，但突變測試擋下同一份偷懶實作；反過來也可能發生。請查看
`evidence.json`，確認是哪一關拒絕完成宣稱。只有這次執行的整體判定為 `PASS`
時，才能接受代理的完成宣稱。

## 看一次假完成怎麼被抓到

JUnit 範例一開始刻意保留一個能通過公開案例的缺陷。可重複執行的示範腳本
會用隱藏邊界案例拒絕這項宣稱、套用真正的修正、再次驗證，最後還原儲存庫中
原有的原始碼。

```bash
git clone https://github.com/samzhu/topplecat.git
cd topplecat
bash samples/junit-cart-orders/demo.sh
```

最後輸出會指出 `evidence.json`、給代理看的安全回饋與 HTML 報告。
完整 FAIL → PASS 故事請閱讀
[JUnit 操作教學](samples/junit-cart-orders/TUTORIAL.md)。

## 開發流程

```text
Java 驗收合約 + 有型別的公開／審閱案例
          |
          v
toppleCatCheck -> toppleCatReview -> toppleCatHide
          |
          v
AI 程式代理只看公開工作目錄並使用 ./gradlew test
          |
          v
審閱者或 CI 執行 toppleCatVerify
          |
          v
PASS / FAIL / INCOMPLETE 證據與人類可讀報告
```

1. **撰寫可執行合約。** 公開測試與案例資料列放在 `src/test`；獨立設計
   的審閱案例放在 `src/hiddenTest`。
2. **檢查並審閱。** `toppleCatCheck` 驗證合約；`toppleCatReview`
   產生包含完整審閱資料的審閱頁。
3. **交由審閱者保管。** `toppleCatHide` 把 `src/hiddenTest` 移到審閱者本機
   的明文保管區：`~/.topplecat/projects/<sha256-project-key>/escrow/`。
   這不是保密邊界。若要修改既有的審閱案例組，授權審閱者必須走明確的還原、
   審閱與更新流程，不能只執行一般的隱藏工作。
4. **正常實作。** 只把公開工作環境交給實作代理，平常使用 `./gradlew test`。
5. **驗證完成宣稱。** `toppleCatVerify` 先確認封存的公開合約與驗證政策仍
   完全相符；只有相符時，才暫時還原審閱者原始碼並執行已啟用的關卡。無論
   結果如何，都會寫出證據並重新隱藏原始碼。

## 安裝 0.0.5

ToppleCat `0.0.5` 是本文件說明的版本。使用端專案需要 Java 25，以及支援它的
Gradle 版本。正式發佈後，Gradle 外掛與函式庫都能從 Maven Central 取得；
使用正式版本的專案不需要 `mavenLocal()`。

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
```

```kotlin
// build.gradle.kts
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.5"
}

dependencies {
    testImplementation(
        "io.github.samzhu.topplecat:topplecat-junit:0.0.5"
    )
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

空白的使用端專案可用 `./gradlew toppleCatInit` 建立起始合約，而且不會覆寫
現有檔案。這只是選用的起始範本，不是一般流程。專案裡的示範腳本使用
`publishToMavenLocal`，是為了測試目前工作副本的原始碼，而不是正式發佈的套件。
這是開發與示範流程，不是一般使用者的安裝方式。

## 寫一份可執行合約

主要的 `@ToppleTest` 是由 `ToppleStage` 方法組成的簡短業務流程。
編譯器會限制情境方法，只允許依序呼叫 Stage。前置設定、服務呼叫、斷言、
輔助方法與流程控制都放在 Stage 裡。

```java
@ToppleStageField CartGiven given;
@ToppleStageField CheckoutWhen when;
@ToppleStageField ReceiptThen then;

@ToppleTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c) {
    given.a_cart(c.input("cart", Cart.class));
    when.creates_an_order();
    then.receipt_matches(c);
}
```

每個 Stage 步驟先呼叫 `recorded(...)`，執行工作，最後回傳 `self()`。
ToppleCat 會把這些呼叫編譯成穩定、可讀的情境句子，同時仍由 JUnit
執行真正的 Java 方法。

案例資料列可以保留巢狀 DTO、List、Map 與 API 結果：

```yaml
- caseId: coupon-public-example
  acId: AC-CART-COUPON
  inputs:
    cart:
      items:
        - {sku: mug, quantity: 2, unitPrice: 250.00}
      couponCode: SAVE100
  expected:
    receipt:
      discount: 100.00
      total: 400.00
```

每筆資料恰好包含 `caseId`、`acId`、`inputs`、`expected`。Jackson 會
把資料反序列化成指定的 Java 型別。每個最外層的 `expected` 欄位，都是一項
必須完成的驗證：`c.verify("receipt", actual)` 會深入比較實際結果與預期結果；
只把預期結果讀出來，不算驗證。

ToppleCat 支援 JSON 與 YAML，不支援 CSV，也不需要自然語言執行環境。

## 執行驗證

請在使用端專案執行以下指令：

```bash
./gradlew toppleCatCheck
./gradlew toppleCatReview
./gradlew toppleCatHide
./gradlew test
./gradlew toppleCatVerify
```

`toppleCatRestore` 是審閱者檢查或修改隱藏原始碼時使用的還原指令，
不屬於平常的實作流程。授權審閱者還原並修改既有案例組後，必須依照下面的
保管資料更新流程操作：

```text
toppleCatRestore
    -> 修改 src/hiddenTest
    -> toppleCatCheck
    -> toppleCatReview
    -> 審閱者確認審閱內容
    -> toppleCatUpdateEscrow
```

`toppleCatUpdateEscrow` 會先驗證並暫存完整的新版審閱者原始碼，確認無誤後
才啟用。檔案系統支援時會要求原子搬移；不支援時，仍會保留相同的驗證與
復原機制。一般的 `toppleCatHide` 仍會拒絕已修改、但尚未重新核准的案例組。
公開交付內容不含審閱者原始碼或本機保管區，因此這項只供審閱者使用的
Gradle 工作會安全地失敗。

## 看懂驗證結果

| 產物 | 受眾 | 作用 |
| --- | --- | --- |
| `build/topplecat/reports/review/index.html` | 僅審閱者 | 交付前的規格脈絡、公開與隱藏案例、Stage 句子及主要驗收測試原始碼。 |
| `build/topplecat/reports/spec/index.html` | 公開 | 驗證後產生、供人類閱讀的公開合約。 |
| `build/topplecat/reports/verification/index.html` | 僅審閱者 | 公開與隱藏案例的結果、步驟、失敗內容、關卡與附件。 |
| `build/topplecat/evidence.json` | 審閱者／CI | 給機器讀取的判定與證據摘要值。 |
| `build/topplecat/agent-feedback.json` | 實作代理 | 已移除審閱細節的安全關卡回饋。 |

驗證報告是一份可離線開啟的完整報告。公開產物不包含審閱者專用的值、
案例編號、原始碼名稱或路徑、附件，以及未整理的私密失敗內容。

第一個必要關卡是 `CONTRACT_INTEGRITY`。它會把目前的公開測試原始碼、案例資料、
專案內的 Gradle 建置邏輯、語意定義與有效的驗證政策，和執行 Hide 或
UpdateEscrow 時由審閱者封存的核准內容逐一比對。最終判定是 `PASS`、`FAIL`
或 `INCOMPLETE`。隱藏案例重測、突變測試與預期結果使用檢查預設都會啟用。
若審閱者明確停用其中一項，證據會記錄 `DISABLED`，不會假裝它已經通過。
合約完整性本身不能停用。

若合約完整性不是 `PASS`，ToppleCat 會把其餘四個關卡記為 `INCOMPLETE`，
重新隱藏審閱者原始碼，並移除任何過期的公開規格報告。授權審閱者必須走完
Restore → Check → Review → UpdateEscrow，才能核准刻意修改過的公開合約或
驗證政策。

整體判定為 `FAIL` 或 `INCOMPLETE` 時，`toppleCatVerify` 與
`toppleCatReport` 會先完整產生證據、報告、安全回饋與這次執行的封存資料，
再讓 Gradle 建置失敗。因此，最後看到綠燈就代表整體判定是 `PASS`。
無論成功或失敗，都能從 `evidence.json` 查看每一關的細節。

預設的 PIT 執行工作會從編譯器產生的描述檔，取得每個已核准的公開主要
`@ToppleTest` 類別，並設定 PIT 的 `targetTests`。即使正式程式與測試位在
不同的 Java 套件，突變測試仍會對準真正的公開合約。若使用端明確設定 PIT
`targetTests`，ToppleCat 會保留該設定；若設定排除了主要驗收測試，而 PIT
報告仍可讀取，就會得到 `MUTATION=FAIL`。若 PIT 沒有產生可用報告，該關卡
會是 `INCOMPLETE`，不能拿來證明合約已通過。

## 審閱資料放在哪裡

審閱資料的保管區位於
`~/.topplecat/projects/<sha256-project-key>/escrow/`，裡面包含清單、隱藏原始碼
內容、核准版本、修訂紀錄、歷程、稽核資料、鎖定與復原狀態。這些資料以明文
存放，沒有加密，也不是沙箱（sandbox）。`./gradlew clean` 會移除產生的
`build/`，但不會刪除審閱資料。舊版留在專案內的 `.topplecat/escrow/`，
只能透過 `toppleCatMigrateEscrow` 明確遷移；遷移成功後才會移除舊保管區。
專案被移動或重新複製時，不會誤用其他專案的資料，也不會默默建立新的核准。

ToppleCat 不控制 OS 權限、沙箱、CI 身分，也不限制同一個 Gradle/JVM 程序
能讀取哪些檔案。外部流程必須在可信任的審閱者或 CI 環境執行 Verify，只把
公開原始碼與安全回饋交給代理，並排除審閱資料、隱藏原始碼、建置產物，以及
曾經包含審閱資料的 Git 歷史。只把保管區放在家目錄，無法阻止同一位 OS
使用者執行惡意的建置腳本或正式程式。

## 選擇範例

| 範例 | 適合從這裡開始的情境 |
| --- | --- |
| [JUnit cart orders](samples/junit-cart-orders) | 使用一般 JUnit 與領域／服務 DTO。 |
| [Spring Boot cart orders](samples/spring-boot-cart-orders) | 想在 Spring Boot 測試專案中使用 ToppleCat。 |

其他差異與完整示範指令請參考[範例導覽](samples/README.md)。

## 文件

- [開始使用](docs/guide/getting-started.md)
- [常見問題：為什麼不用 Cucumber 或 `.feature`？](docs/faq.zh-TW.md)
- [0.0.5 發佈說明](docs/releases/0.0.5.zh-TW.md)
- [撰寫合約](docs/guide/authoring.md)
- [驗證與證據](docs/guide/verification-and-evidence.md)
- [疑難排解](docs/guide/troubleshooting.md)
- [架構](docs/architecture.md)
- [外部驗證紀錄](docs/validation/README.md)
- [貢獻指南](CONTRIBUTING.md)
- [安全政策](SECURITY.md)

專案也附帶
[`topplecat-verification`](.agents/skills/topplecat-verification/SKILL.md)
代理技能，協助撰寫合約、保管審閱資料並驗證完成宣稱。

## 目前狀態

ToppleCat 目前仍是 1.0 前的版本，API 可能繼續調整。開發環境需要 Java 25，
並使用專案隨附的 Gradle 9.1.0 Wrapper。
