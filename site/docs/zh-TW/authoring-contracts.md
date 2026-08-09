---
title: Authoring contracts
description: 把人選的 Acceptance Condition 綁到一般 Java/JUnit method 與型別化案例列。
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

# Authoring contracts

## 先寫一條具體規則 {#contract-example}

以 checkout 規則為例：subtotal 1,000 元以上，receipt total 應該是 900。人決定這條
規則，再選一筆公開案例，例如 `subtotal: 1000`、`total: 900`。ToppleCat 不會發明
下限規則、不會替人選案例，也不會判斷還需要幾筆案例。

公開 Java method 與型別化案例列就是 Executable Contract。產生的 JSON 與 HTML 是這份
契約的 projection，不是第二種 authoring language。

## Acceptance Method 形狀 {#acceptance-method}

每個 Acceptance Condition 綁定一個 literal 公開
`@ToppleAcceptanceTest("AC-...")` method。給它一個讓 Reviewer 看得懂的 JUnit
`@DisplayName`，並把 method 保持為小型 Scenario orchestration：

```java
@ToppleAcceptanceTest("AC-ORDER-CREATE")
@DisplayName("Create an accepted order")
void createsOrder(ToppleCase c, ToppleScenario scenario, OrderStage order) {
    scenario.given(order).an_order_request(c.input("request", OrderRequest.class));
    scenario.when(order).submits_it();
    scenario.then(order).confirms_accepted_order(c);
}
```

參數依序是 `ToppleCase`、一個非 generic 的 `ToppleScenario`，以及一個或多個不同的
具體 `ToppleStage` 型別。Stage 不能是 final，必須可以 proxy，而且要有可存取的無參數
constructor。setup、service call、分支與 assertions 都放進一般 Stage method。

每次直接呼叫都必須是 `scenario.given|when|then|and(stage).step(...)`。compiler 負責
phase 順序、Stage 選擇、overload identity 與呈現的 Step。`@As` 提供人看的業務文字，
但不能讓 runtime code 改寫 compiler 描述的 Step。

## 型別化案例列 {#typed-case-rows}

公開案例列放在 `src/test/resources/topplecat/cases/`：

```yaml
- caseId: order-public-example
  acId: AC-ORDER-CREATE
  inputs:
    request: {items: [{sku: example-sku, quantity: 1}]}
  expected:
    response: {accepted: true}
```

一列恰好有 `caseId`、`acId`、`inputs` 與 `expected`。reviewer-owned 案例在 reviewer
custody 中使用同一 schema，並指向已存在的 public AC；它是獨立選出的例子，不是新規則。
交給 Implementation Agent 的公開契約，和 formal Verify 實際執行的公開契約是同一份 bytes。

## Expected values 與 Properties

每個 top-level expected value 一開始都是 `UNTOUCHED`。`c.verify("receipt", actual)`
會比較並標成 `ASSERTED`；`c.expected("receipt", Type.class)` 只讀取並標成 `READ`；
沒有存取就維持 untouched。只有 `ASSERTED` 能滿足 expected-consumption enforcement。

當案例本身不夠涵蓋一條人核准的 invariant 時，使用 `@ToppleProperty`。它有獨立的
`PROPERTY` Gate，使用有界 generator，不能產生案例列，也不能改善 Mutation Testing。
產生的輸入是本次執行證據，不是 Typed Case Row。

## 人負責契約完整性 {#human-completeness}

人或 External Workflow 選定目前 Spec，並且負責讓規則與案例完整。ToppleCat 把選定的
AC 綁到一般 Java/JUnit 工作，檢查 compiler-defined Scenario，再執行 sealed contract。
它不判斷漏了哪些 requirement、不替組織 sign-off，也不是 task manager。

完整的 sample 路徑請看 [Getting started](getting-started.md#sample-workflow)。相同公開契約
如何進入正式證據，請看 [Architecture](architecture.md#contract-authority) 與
[Verification and evidence](verification-and-evidence.md#three-evidence-layers)。
