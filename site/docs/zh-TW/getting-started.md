---
title: Getting started
description: 安裝 ToppleCat、撰寫一份可執行驗收契約，並以 sample 為基礎跑完驗證流程。
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

# Getting started

## 第一個交付案例 {#contract-example}

假設 checkout Spec 規定：訂單金額滿 1,000 元，就折 100 元。人先選定這條規則，
寫下一筆公開案例，再決定要啟用哪些額外 safeguard。Implementation Agent 只會
看到公開契約，不會看到 reviewer-owned 案例。agent 宣稱完成後，Reviewer 執行同一份
契約、閱讀證據，再決定怎麼處理這次交付。

[JUnit cart-orders sample](https://github.com/samzhu/topplecat/tree/main/samples/junit-cart-orders)
是這條路徑的 executable reference。想跑完整的 consumer setup，可以執行它的
`demo.sh`；教學所說的程式與案例列都在 repository 中，沒有另外承諾一套不存在的 API。

## 安裝 plugin

ToppleCat 0.1.0 需要 Java 25 與相容的 Gradle 版本。在 consumer project 加入
plugin 和 JUnit dependency：

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

## 撰寫公開契約

把公開 Acceptance Method 放在 `src/test/java`，把型別化 JSON/YAML 案例列放在
`src/test/resources/topplecat/cases/`。一個 literal AC ID 綁一個公開方法。方法
描述 Scenario；一般的 Stage method 負責業務呼叫與 assertions。

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_cart(c.input("cart", Cart.class));
    scenario.when(coupon).creates_an_order();
    scenario.then(coupon).receipt_matches(c);
}
```

規則與案例是否完整，要由人自己負責。ToppleCat 檢查選定的 Executable Contract，
不會猜測沒有寫出的業務需求。 [Authoring contracts](authoring-contracts.md#typed-case-rows)
會詳細說明 compiler-defined Scenario 與 expected-value 規則。

## 跟著 sample workflow 走 {#sample-workflow}

repository sample 執行支援的 Gradle workflow：

```bash
cd samples/junit-cart-orders
bash demo.sh
```

在 consumer project 中，人或 External Workflow 會先選定並審閱 Spec，再封存完整契約：

```bash
./gradlew toppleCatCheck --spec specs/checkout/spec.md
./gradlew toppleCatReview --spec specs/checkout/spec.md
./gradlew toppleCatSeal
```

Implementation Agent 使用一般的 `./gradlew test` 取得回饋。這個綠燈有助於開發，
但不是正式判定。

## 執行正式 Verify {#formal-verify}

agent 宣稱 checkout 完成後，執行完整契約：

```bash
./gradlew test
./gradlew toppleCatVerify
```

Verify 會產生新的 Current-run Evidence，分別評估啟用的 safeguards，寫出 reviewer-only
Verification Report，再重新隱藏 reviewer source 後回傳 aggregate result。Reviewer 若
想快速查看，可指定 Spec 或重複指定 AC ID，但不能混用；範圍 `PASS` 只涵蓋選定範圍。

機器判定在 `build/topplecat/evidence.json`。人閱讀 Verification Report，再決定是否
接受交付。

## 這個結果代表什麼 {#human-decision}

`PASS` 表示這次執行中，sealed policy 要求的每個 Gate 都通過。它不證明 checkout Spec
完整、不證明未列出的輸入也都正確，也不表示組織已經批准交付。請讀
[Verification and evidence](verification-and-evidence.md#delivery-example) 了解 observation、
attribution 與 Gate verdict 三個層次。
