---
title: 開始使用
description: 從 Maven Central 把 ToppleCat 加進 Java/JUnit 專案，再準備可供 AI 實作與驗證的驗收內容。
page_id: getting-started
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../getting-started/
alternate_language: en
alternate_label: English
alternate_en: ../getting-started/
alternate_zh_tw: ./getting-started/
markdown_url: getting-started.md
copy_label: Copy Markdown
copied_label: Copied
---

# 把 ToppleCat 加進 Java 專案

ToppleCat 0.1.0 已發布到 Maven Central。直接把 Gradle plugin 和 JUnit library 加進
現有專案即可，不必下載 ToppleCat 原始碼，也不用自己 build。

使用 ToppleCat 需要 JDK 25 與相容的 Gradle 版本。

## 它在檢查什麼 {#contract-example}

結帳規則是：使用 `SAVE100` 優惠券時，訂單小計折 100 元。開發者先把規則寫成一般的
Java/JUnit 驗收方法：

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("使用 SAVE100 折抵訂單小計")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
}
```

JSON 或 YAML 案例列提供一台具體購物車，以及預期收到的收據。這個方法和案例列合在
一起，就是公開的可執行契約。實作 AI 可以讀它，也可以在開發時執行
`./gradlew test`。

正式驗證時，ToppleCat 會用審閱者另外選出的案例重跑同一個公開方法。AI 不需要先
知道那些案例，仍然可以照公開規則完成實作。

## 把 ToppleCat 和撰寫契約的 skill 加到專案 {#ai-assisted-authoring}

如果要讓實作 AI 和 ToppleCat 一起工作，要準備兩樣東西：

- Gradle plugin 在 Java/JUnit 專案裡執行 ToppleCat。
- `topplecat-acceptance` skill 告訴 AI，怎麼把你選定的驗收條件寫成
  Java/JUnit 能執行的驗收方法與案例。

[ToppleCat 的正式套件](https://central.sonatype.com/namespace/io.github.samzhu.topplecat)
已發布到 Maven Central。先在 `settings.gradle.kts` 設定 plugin marker 與 library 的來源：

```kotlin
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

再到 `build.gradle.kts` 加入 plugin 與 JUnit dependencies：

```kotlin
plugins {
    java
    id("io.github.samzhu.topplecat") version "0.1.0"
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test { useJUnitPlatform() }
```

接著，把這個 skill 安裝到 AI 要工作的專案：

```text
npx skills@latest add samzhu/topplecat --skill topplecat-acceptance
```

在讓 agent 讀寫專案前，先讀
[skill 原始碼](https://github.com/samzhu/topplecat/tree/main/.agents/skills/topplecat-acceptance)。
它不會替你補寫需求。它會協助 AI 問清楚規則，再把你選定的規則綁成 ToppleCat
可以執行的 Java/JUnit 驗收內容。

## 選用：執行 repository 範例 {#sample-workflow}

如果想親自看 ToppleCat 攔下錯誤實作，再換上修正版跑出 `PASS`，可以 clone repository
後執行：

```bash
bash samples/junit-cart-orders/demo.sh
```

這支腳本是 ToppleCat repository 的自我驗證。它會先把目前 checkout 的 ToppleCat build
發布到本機 Maven cache，確保測到的是這份原始碼；接著封存範例契約，確認故意寫錯的
結帳服務被拒絕，再換上修正版驗證一次。

一般使用者直接使用 Maven Central 的正式版本，不需要執行本機發布。範例程式與清理流程
放在 [JUnit cart-orders sample](https://github.com/samzhu/topplecat/tree/main/samples/junit-cart-orders)，
ToppleCat 的 release Gate 也會執行這條路徑。

## 和 AI 準備一次交付 {#prepare-with-an-ai}

結帳範例的規則很單純：可以結帳的購物車使用 `SAVE100` 時，折 100 元。公開案例會寫出
一台符合條件的購物車，以及應收到的收據。審閱者可以從同一條公開規則選另一台購物車，
確認程式不是只認得看得到的例子。這不是祕密需求，實作 AI 也不會看到那筆額外案例。

### 在 Codex 對話裡

在同一段對話中一起使用：

```text
$to-spec + $topplecat-acceptance
```

`$to-spec` 會把對話中已經談妥的規則整理成 Spec。`$topplecat-acceptance` 會把 Spec
裡每一條 Acceptance Condition（AC）轉成一個 Java/JUnit 驗收方法和對應案例。它會整理
一份實作 AI 可以看的公開資料，也會另外準備給審閱者的案例。

規則有兩種可能意思時，先回答問題，不要讓 AI 自己選。例如，優惠券是否能套用到含有
排除商品的購物車？這種決定要由人寫回規則，skill 不能代替你決定。

這時你已經有寫下來的規則、公開的 Java 驗收程式、公開案例，以及由同一條規則推導出的
審閱者案例。下一步是看 Java 程式究竟會檢查什麼。

## 和 Matt Pocock skills 一起使用 {#matt-pocock-workflow}

[Matt Pocock 的 skills](https://github.com/mattpocock/skills/tree/main/skills)
可以管理較大的工作流程：寫下 Spec、需要時拆成 tickets，再依照已確認的內容實作。
ToppleCat 在這條流程裡負責準備和保護驗收內容，不負責管理工作或替產品決定規則。

第一次在專案使用 Matt Pocock 的 engineering skills 時，先安裝那些 skills，並執行一次
`$setup-matt-pocock-skills`。它會記下這個專案的工作追蹤位置與領域文件。
設定完成後，針對一次交付依序做下面幾步。

### 在終端機裡：閱讀準備好的驗收內容

Spec 和 Java 驗收程式準備好後，執行：

```bash
./gradlew toppleCatReview --spec specs/checkout/spec.md
```

`toppleCatReview` 是 Gradle 指令，不是 agent skill。它會產生只給審閱者看的 Spec Review
頁面。你可以把選定的 Spec、Java 驗收方法編譯出的 Given/When/Then 呈現，以及案例放在
一起閱讀。這份頁面沒有測試結果；它要回答的是「Java 程式寫的驗收內容，是否就是原本的
規則？」

Review 會執行它需要的 Check，所以主流程不用再列 `toppleCatCheck`。如果只想快速檢查
驗收方法和案例有沒有綁好，可以另外執行
`./gradlew toppleCatCheck --spec specs/checkout/spec.md`。

### 在 agent 對話裡：需要時拆票

如果已確認的 Spec 有好幾塊可以分開完成的工作，使用 `$to-tickets`。它會產生可以各自
完成的工作票，並標示先後關係。小改動可以略過這一步，直接依 Spec 實作。

### 在終端機裡：實作前保護驗收內容

看完 Spec Review 後，執行：

```bash
./gradlew toppleCatSeal
```

Seal 會把只給審閱者看的原始碼移到本機保管區，並記錄當時完整的驗收內容與驗證設定。
接下來實作 AI 只會看到公開專案。之後正式驗證時，ToppleCat 可以檢查這些內容或政策是否
在 Seal 後被改動。

Seal 是內容完整性記錄，不是加密、作業系統隔離，也不是人已經接受最後交付。

### 在 agent 對話裡：實作已確認的工作

把 Spec 或 tickets 交給 `$implement`。實作 AI 只根據公開驗收程式和一般的
`./gradlew test` 回饋工作。不要把私人的 Spec Review、審閱者案例或審閱者原始碼交給它。

AI 說完成後，回到終端機進行正式驗證。

## 驗證交付結果 {#formal-verify}

實作 AI 說工作完成後，執行：

```bash
./gradlew toppleCatVerify
```

`toppleCatVerify` 會重跑公開契約、執行每一道已啟用的獨立檢查，並產生只給審閱者
看的 Verification Report。機器可讀的結論在 `build/topplecat/evidence.json`。這是 Gradle
指令，不是 agent skill。

審閱者若只想快速看這次交付，可以指定 Spec 或 AC ID；報告會清楚標示範圍。CI
應該使用不帶範圍的 `toppleCatVerify`，檢查完整契約。

## 根據證據做決定 {#human-decision}

`PASS` 表示封存政策要求的每一道檢查都在這次執行中通過。`FAIL` 表示某道完成的
檢查找到阻擋問題。`INCOMPLETE` 表示 ToppleCat 沒有取得足夠、可信的當次證據。

如果是指定 Spec 或 AC 的有限範圍驗證，`PASS` 只涵蓋那次指定的範圍。這些結果都不會替人
判斷原始業務規則是否完整。人要讀清楚跑了什麼、發生什麼，再決定是否接受交付。

準備導入專案時，接著讀[把規則寫成可執行檢查](authoring-contracts.md#contract-example)。
要解讀報告時，讀[ToppleCat 如何檢查交付](verification-and-evidence.md#delivery-example)。
