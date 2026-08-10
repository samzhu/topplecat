---
title: 開始使用
description: 執行一個真的會攔下錯誤 AI 交付的範例，再把 ToppleCat 加進 Java/JUnit 專案。
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

# 看 ToppleCat 攔下一個錯誤交付

如果你還不確定 ToppleCat 值不值得用，先跑範例，不必先讀完設定手冊。專案原始碼
內有一個故意寫得太狹隘的結帳服務。它能通過公開測試，卻會被審閱者另外準備的檢查
抓到。相同腳本接著換上修正版，再跑出通過結果。

執行範例需要 shell、Git 與 JDK 25。要把 ToppleCat 加進其他專案，還需要相容的
Gradle 版本。

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

## 執行可重現範例 {#sample-workflow}

clone 專案後執行：

```bash
bash samples/junit-cart-orders/demo.sh
```

腳本會走完三件事：

1. 把目前的 ToppleCat build 發布到本機 Maven cache。
2. 封存準備好的驗收契約，驗證故意寫錯的結帳服務。這次必須被拒絕。
3. 換上修正版再次驗證。這次必須取得 `PASS`。

任何一個結果不符預期，腳本都會失敗。程式、公開契約與清理流程都在
[JUnit cart-orders sample](https://github.com/samzhu/topplecat/tree/main/samples/junit-cart-orders)，
ToppleCat 自己的發布驗證也會執行這條路徑。

## 加到自己的專案

ToppleCat 0.1.0 需要 Java 25。在 Gradle 專案加入 plugin 與 JUnit dependencies：

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

公開驗收方法放在 `src/test/java`，公開案例列放在
`src/test/resources/topplecat/cases/`。把工作交給實作 AI 前，由負責的人確認選定
規格和準備好的契約，再將完整契約封存：

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
```

接下來，實作 AI 只需要公開專案，並用一般的 `./gradlew test` 取得開發回饋。
測試綠燈有幫助，但還不是正式的交付判定。

你可以把本頁 Markdown 交給 coding agent，請它安裝 plugin，並依照你已確認的規則
建立公開契約。不要叫它猜沒寫出的業務需求，也不要讓它接觸審閱者控制的資料。

## 驗證交付結果 {#formal-verify}

AI 說工作完成後，執行：

```bash
./gradlew test
./gradlew toppleCatVerify
```

`toppleCatVerify` 會重跑公開契約、執行每一道已啟用的獨立檢查，並產生只給審閱者
看的 Verification Report。機器可讀的結論在 `build/topplecat/evidence.json`。

審閱者若只想快速看這次交付，可以指定 Spec 或 AC ID；報告會清楚標示範圍。CI
應該使用不帶範圍的 `toppleCatVerify`，檢查完整契約。

## 根據證據做決定 {#human-decision}

`PASS` 表示封存政策要求的每一道檢查都在這次執行中通過。`FAIL` 表示某道完成的
檢查找到阻擋問題。`INCOMPLETE` 表示 ToppleCat 沒有取得足夠、可信的當次證據。

這些結果都不會替人判斷原始業務規則是否完整。人要讀清楚跑了什麼、發生什麼，再決定
是否接受交付。

準備導入專案時，接著讀[把規則寫成可執行檢查](authoring-contracts.md#contract-example)。
要解讀報告時，讀[ToppleCat 如何檢查交付](verification-and-evidence.md#delivery-example)。
