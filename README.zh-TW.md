# ToppleCat

<p align="center">
  <img
    src="docs/images/topplecat-readme-hero.png"
    alt="ToppleCat 推倒 AI agent 的假完成宣稱"
    width="100%"
  >
</p>

<p align="center">
  <strong>把 AI agent 的「已完成」變成證據。</strong>
</p>

<p align="center">
  <a href="README.md">English</a>
  ·
  <a href="https://github.com/samzhu/topplecat/actions/workflows/ci.yml">Build</a>
  ·
  <a href="LICENSE">Apache-2.0</a>
</p>

ToppleCat 是一隻充滿好奇心的貓。

每當 AI coding agent 宣稱某項 Java 工作**已完成**，ToppleCat 都會伸手
輕輕撥一下。牠會用實作看不到的案例重測、故意破壞 production behavior
確認測試能否察覺，並證明每一個宣告的結果都真的被驗證。

如果實作只記住公開範例，或測試看似忙碌卻沒有證明行為，這項完成宣稱就會
倒下。若它站得住，ToppleCat 才會留下證據。

> 用 hidden retests、mutation gates 與可執行的 Java 驗收合約，推倒
> AI agent 的「已完成」宣稱。空洞的完成，站不住腳。

ToppleCat 是 Java/JUnit 工作的委派驗證閘門。一般 Java 驗收測試與具型別
的 JSON/YAML case rows 是可執行合約；產生的 JSON 與 HTML 是證據，不是
另一份真相來源。

Canonical scenario 是刻意寫得像商業語言的一般 Java 程式碼。
[JGiven](https://github.com/TNG/JGiven) 是可讀、分階段 Java 測試的重要
先行專案。ToppleCat 聚焦在另一個邊界：以 hidden retests、mutation
evidence 與安全 feedback，獨立檢查委派工作的 done claim。它不使用
Cucumber 或 Gherkin，也不在可執行 Java 合約之外增加第二套 authoring
language。

## ToppleCat 會抓到什麼

| 綠燈仍可能代表…… | ToppleCat 如何檢查 |
| --- | --- |
| 實作只針對看得見的範例調整。 | Reviewer 控制的 **hidden retests**。 |
| 測試有執行，卻無法察覺壞掉的行為。 | PIT 驅動的 **mutation gate**。 |
| expected 已讀取，卻沒有和實際結果比較。 | 強制執行的 **expected consumption**。 |
| 審閱後公開合約或驗證強度被改變。 | 強制、由 reviewer 封存的 **contract-integrity gate**。 |
| 舊的或不完整的輸出被誤認為本次證明。 | Run-scoped gates、digests 與明確的 **evidence verdict**。 |

Hidden retest 與 mutation 回答的是不同問題。Hidden retest 檢查實作是否能超越
看得見的範例而泛化；預設 PIT producer 衡量的是**公開可執行合約的 mutation
strength**，只使用 `sourceSets.test`、public test classes 與 public case rows。
Reviewer rows 與 reviewer-only JUnit tests 永遠不會協助這個 producer 殺死 mutant。
若某個 boundary 必須殺死 mutant，它就應屬於 public contract；ToppleCat 不會增加
per-case mutation score，也不會推斷 custom producer 的範圍。

## 看 ToppleCat 推倒一次假完成

JUnit sample 一開始刻意保留一個能通過公開 case 的缺陷。可重複執行的
demo 會用 hidden boundary 拒絕這項宣稱、套用真正的修正、再次驗證，
最後還原 checked-in 原始碼。

```bash
git clone https://github.com/samzhu/topplecat.git
cd topplecat
bash samples/junit-cart-orders/demo.sh
```

最後輸出會指出 `evidence.json`、安全的 agent feedback 與 HTML 報表。
完整 FAIL → PASS 故事請閱讀
[JUnit 操作教學](samples/junit-cart-orders/TUTORIAL.md)。

## 對應的開發流程

```text
Java 驗收合約 + 具型別 public/reviewer cases
          |
          v
toppleCatCheck -> toppleCatReview -> toppleCatHide
          |
          v
AI agent 只看公開工作樹並使用 ./gradlew test
          |
          v
Reviewer 或 CI 執行 toppleCatVerify
          |
          v
PASS / FAIL / INCOMPLETE 證據與人類報表
```

1. **撰寫可執行合約。** 公開測試與 case rows 放在 `src/test`；獨立設計
   的 reviewer retests 放在 `src/hiddenTest`。
2. **檢查並審閱。** `toppleCatCheck` 驗證合約；`toppleCatReview`
   產生包含完整 reviewer 資料的審閱頁。
3. **移交 reviewer custody。** `toppleCatHide` 把 `src/hiddenTest`
   移入本機明文 custody storage；這不是保密邊界。若要演進既有的 reviewer
   suite，授權 reviewer 必須使用明確的還原、審閱與更新流程，而非一般 hide。
4. **正常實作。** 只把 public-only environment 交給 implementation
   agent，平常使用 `./gradlew test`。
5. **驗證完成宣稱。** `toppleCatVerify` 先確認封存的公開合約與驗證 policy
   仍完全相符；只有相符時才暫時還原 reviewer source 並執行已啟用 gate。無論
   結果都會寫出證據並重新隱藏來源。

## 安裝 0.0.3

ToppleCat `0.0.3` 是本文件所說明的版本。Consumer 專案需要 Java 25 與支援它的
Gradle 版本。正式發佈後，Plugin 與 library resolution 都加入 Maven Central
即可；使用正式版的 consumer 不需要 `mavenLocal()`。

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
    id("io.github.samzhu.topplecat") version "0.0.3"
}

dependencies {
    testImplementation(
        "io.github.samzhu.topplecat:topplecat-junit:0.0.3"
    )
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

空白 consumer 專案可用 `./gradlew toppleCatInit` 建立不覆寫現有檔案的
起始合約。它是選用 bootstrap，不是一般工作流程。Repository 內的 demo
刻意使用 `publishToMavenLocal`，以便測試目前 checkout 的原始碼而不是
release artifact；那是 contributor／demo workflow，不是上面的安裝方式。

## 撰寫可執行合約

Canonical `@ToppleTest` 是由 `ToppleStage` 方法組成的簡短商業流程。
編譯器會限制 scenario method，只允許依序呼叫 Stage；setup、service call、
assertion、helper 與 control flow 都放在 Stage 內。

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

每個 Stage step 先呼叫 `recorded(...)`，執行工作，最後回傳 `self()`。
ToppleCat 會把這些呼叫編譯成穩定、可讀的情境句子，同時仍由 JUnit
執行真正的 Java method。

Case rows 可以保留巢狀 DTO、list、map 與 API result：

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

每個 row 恰好包含 `caseId`、`acId`、`inputs`、`expected`。Jackson 會
把資料反序列化成指定的 Java type。每個頂層 `expected` key 都是一項
assertion obligation：`c.verify("receipt", actual)` 會深度比較並完成它；
只讀取 expected 不算驗證。

ToppleCat 支援 JSON 與 YAML，不支援 CSV，也不引入自然語言 runtime。

## 執行驗證閘門

以下指令從 consumer 專案執行：

```bash
./gradlew toppleCatCheck
./gradlew toppleCatReview
./gradlew toppleCatHide
./gradlew test
./gradlew toppleCatVerify
```

`toppleCatRestore` 是 reviewer 檢查或修改 hidden source 時使用的還原指令，
不屬於 implementation loop。授權 reviewer 還原並修改既有 suite 後，必須依下列
custody 更新流程進行：

```text
toppleCatRestore
    -> edit src/hiddenTest
    -> toppleCatCheck
    -> toppleCatReview
    -> reviewer accepts the review
    -> toppleCatUpdateEscrow
```

`toppleCatUpdateEscrow` 會先驗證並暫存完整的新版 reviewer source，再進行
啟用。Filesystem 支援時會要求 atomic move；不支援時仍保留相同的驗證與
復原路徑。一般 `toppleCatHide` 仍會拒絕已修改的 restored suite。公開
implementation export 不含 reviewer source 或本機 escrow，因此這個僅供
reviewer 使用的 task 會安全地失敗。

## 閱讀結果

| Artifact | 受眾 | 作用 |
| --- | --- | --- |
| `build/topplecat/reports/review/index.html` | 僅 reviewer | 交付前的 Spec 脈絡、public/hidden cases、Stage 句子與 canonical source。 |
| `build/topplecat/reports/spec/index.html` | 公開 | 驗證後產生、供人類閱讀的公開合約。 |
| `build/topplecat/reports/verification/index.html` | 僅 reviewer | Public/hidden case 結果、steps、failures、gates 與 attachments。 |
| `build/topplecat/evidence.json` | Reviewer / CI | 機器 verdict 與 evidence digests。 |
| `build/topplecat/agent-feedback.json` | Implementation agent | 已移除 reviewer 細節的安全 gate-level feedback。 |

Verification report 是可離線開啟的自足 bundle。公開產物不包含 reviewer
values、case IDs、source names/paths、attachments 或 raw private
failures。

第一個必要 gate 是 `CONTRACT_INTEGRITY`：它會把目前的公開 test source、case
data、專案內 Gradle build logic、語意 definition 與有效 verification policy，
和 Hide 或 UpdateEscrow 時由 reviewer 封存的核准內容比較。最終 verdict 是
`PASS`、`FAIL` 或 `INCOMPLETE`。Hidden retest、mutation 與
expected-consumption safeguards 預設啟用；若 reviewer 明確停用其中一項，evidence
會記錄 `DISABLED`，不會假裝已通過。Contract integrity 本身無法停用。

若 contract integrity 不是 `PASS`，ToppleCat 會把其餘四個 gate 記錄為
`INCOMPLETE`、重新隱藏 reviewer source，並移除任何過期的公開 Spec bundle。
授權 reviewer 必須依 Restore → Check → Review → UpdateEscrow 的流程，才能
核准刻意變更的公開合約或 policy。

Aggregate verdict 為 `FAIL` 或 `INCOMPLETE` 時，`toppleCatVerify` 與
`toppleCatReport` 會在 evidence、reports、安全 feedback 與 run archive
完整產生後讓 Gradle build 失敗。因此最終綠燈代表 aggregate `PASS`；
無論成功或失敗，都可從 `evidence.json` 讀取 gate-level 細節。

## 保護 Reviewer 資料

本機 `.topplecat/escrow/` 是機械式明文儲存，不是加密。`./gradlew clean`
會移除生成的 `build/`，但不會移除 escrow。從 working tree 移除
`src/hiddenTest` 也不會把它從 Git history 擦除；若 history 曾含 reviewer
source，由它建立的 worktree 不是保密邊界。絕對不要把 reviewer material
commit 到 implementation agent 可讀的 history。交付時應使用不含 `.git`、
`.topplecat/`、`build/` 的 public export；history 從未含 reviewer material
的隔離環境；或 public repository 搭配獨立 private reviewer repository／CI。

## 選擇 Sample

| Sample | 適合從這裡開始的情境 |
| --- | --- |
| [JUnit cart orders](samples/junit-cart-orders) | 使用一般 JUnit 與 domain/service DTO。 |
| [Spring Boot cart orders](samples/spring-boot-cart-orders) | 想在 Spring Boot test project 中使用 ToppleCat。 |

其他差異與完整 demo 指令請參考 [samples 導覽](samples/README.md)。

## 文件

- [開始使用](docs/guide/getting-started.md)
- [0.0.3 發佈說明](docs/releases/0.0.3.zh-TW.md)
- [撰寫合約](docs/guide/authoring.md)
- [驗證與證據](docs/guide/verification-and-evidence.md)
- [疑難排解](docs/guide/troubleshooting.md)
- [架構](docs/architecture.md)
- [貢獻指南](CONTRIBUTING.md)
- [安全政策](SECURITY.md)

Repository 也附帶
[`topplecat-verification`](.agents/skills/topplecat-verification/SKILL.md)
agent skill，協助撰寫合約、維持 reviewer custody 並驗證 done claim。

## 專案狀態

ToppleCat 目前是 pre-1.0，API 仍可能調整。開發環境需要 Java 25，並使用
repository 內的 Gradle 9.1.0 wrapper。
