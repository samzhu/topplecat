---
title: 把規則寫成可執行檢查
description: 先寫清楚 Java 交付必須做到什麼，讓實作 AI 與 ToppleCat 面對同一份公開契約。
page_id: authoring-contracts
language_code: zh-TW
language_name: 繁體中文
language_label: 目前語言
alternate_url: ../authoring-contracts/
alternate_language: en
alternate_label: English
alternate_en: ../authoring-contracts/
alternate_zh_tw: ./authoring-contracts/
markdown_url: authoring-contracts.md
copy_label: Copy Markdown
copied_label: Copied
---

# 把業務規則寫成可執行檢查

在請 AI 實作功能前，先用看得見的結果回答一個問題：發生什麼情況時，你會相信這條
規則真的有作用？

## 先寫規則，再談程式標記 {#contract-example}

假設規則是：「訂單成立後，系統要回傳含有正確總額的收據。」接著寫一筆具體例子，
讓開發者、產品負責人和 AI 都能讀懂：這台購物車送進去，應該拿到這張收據。

ToppleCat 不會替你定義什麼叫訂單成立。它保存人選定的規則和例子，在實作完成後檢查
雙方原本同意的內容。

在 ToppleCat 裡，描述流程的 Java 方法叫做 **Acceptance Method（驗收方法）**，JSON
或 YAML 例子叫做 **Typed Case Row（型別案例資料列）**。兩者合在一起，就是公開的
**Executable Contract（可執行契約）**。

## 用 Java 描述行為 {#acceptance-method}

每條選定規則，也就是 Acceptance Condition，都有一個公開的
`@ToppleAcceptanceTest("AC-...")` 方法。方法名稱要說清楚業務結果：

```java
@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("SAVE100 reduces the order subtotal")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
}
```

這個方法應該短到可以當成一段故事閱讀。`ToppleCase` 提供當次例子，
`ToppleScenario` 記錄 Given、When、Then 的順序，`CouponStage` 裡的方法負責真正的
準備工作、服務呼叫與斷言。

想看能實際執行的完整寫法，可以選擇閱讀
[JUnit cart-orders 學習專案](https://github.com/samzhu/topplecat/tree/main/samples/junit-cart-orders)。
它使用已發布的 0.1.0，並提供五項完全合成的保障課程；不需要先執行範例才能照著本頁撰寫。

方法格式有明確限制：`ToppleCase` 必須放第一個，後面是一個 `ToppleScenario`，再
接一個或多個不同的具體 Stage。Stage 不能是 final，並且要有可存取的無參數
constructor。每一行直接呼叫 `scenario.given|when|then|and(stage).step(...)`；
條件判斷、helper 與 assertions 放在 Stage 方法裡。

`@DisplayName` 與 `@As` 應使用審閱者看得懂的業務文字。這些人寫的句子會原樣
保留在契約與報告中。

## 加入輸入與預期結果 {#typed-case-rows}

公開案例列放在 `src/test/resources/topplecat/cases/`：

```yaml
- caseId: order-public-example
  acId: AC-ORDER-CREATE
  inputs:
    request: {items: [{sku: example-sku, quantity: 1}]}
  expected:
    response: {accepted: true}
```

每一列有四個部分：案例自己的 ID、它所屬的規則、輸入，以及預期結果。公開案例讓
實作 AI 知道規則長什麼樣子。審閱者控制的案例重用同一條規則與同一個方法，但會
選擇不同的邊界。它們不是祕密的新需求。

AI 收到的是公開契約。正式驗證之後執行的也是同一份公開內容，ToppleCat 不會在
交付後偷換另一套公開規格。

## 確認預期結果真的有比較

讀取預期值不等於驗證它。使用 `c.verify("receipt", actual)`，把實際收據
和人寫下的完整預期收據比較。ToppleCat 會記錄每個最上層預期值是否真的被斷言、只是
被讀取，或根本沒有執行到。

如果一條規則應該對很多輸入都成立，可以再寫公開的 `@ToppleProperty`。例如：商品
順序改變不應影響訂單總額。性質檢查使用有界的產生輸入，透過自己的獨立檢查回報；
它不會取代具體案例。

## 決定要交給 AI 什麼 {#human-completeness}

AI 可以依照人已確認的規則，完成 Java 接線和案例檔案。把本頁、選定的業務規則與公開
例子交給它，要求每條規則只用一個 Acceptance Method，並比較完整、可觀察的結果。

規則與例子是否完整，仍由人決定。契約裡沒寫退款例外、VIP 折扣或法規要求，
ToppleCat 不會自行推論，也不會替組織批准交付。

接著閱讀[ToppleCat 如何檢查交付](verification-and-evidence.md#delivery-example)，了解同一份
公開契約如何變成這次執行的證據。精確的參數與
輸入產生器規則保留在 repository 的
[authoring guide](https://github.com/samzhu/topplecat/blob/main/docs/guide/authoring.md)。
