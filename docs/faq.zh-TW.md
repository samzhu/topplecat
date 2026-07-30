# 常見問題

## JGiven 是 ToppleCat 的相依套件嗎？

不是。JGiven 只是 Scenario、Stage 與 Step 可讀性的高層概念參考。ToppleCat
不依賴它的 runtime 或報告系統，也不把它當成設計權威；當代理驗證需要不同
邊界時，ToppleCat 會採用自己的設計。

## ToppleCat 預設使用哪個突變測試 producer？

預設 producer 是 PIT。它會改變正式程式行為，並回報公開驗收工作是否偵測到每個改變。

## ToppleCat 會使用 Cucumber、`.feature` 或 JGiven runtime/report 嗎？

不會。Java/JUnit 驗收方法與型別化 JSON 或 YAML 資料列是唯一的可執行事實來源；
ToppleCat 不新增這些撰寫或 runtime 介面。
