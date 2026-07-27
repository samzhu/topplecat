# 常見問題

[English](faq.md)

## 為什麼 ToppleCat 不用 Cucumber 或 `.feature`？

因為 ToppleCat 希望人看到的規格，和 JUnit 真正執行的程式放在一起。

Cucumber 採用不同做法。情境寫在 Gherkin `.feature` 檔案裡，再由 step
definition 把每一行文字接到程式。當不熟悉程式語言的領域專家需要親自撰寫
情境時，這層分工很有用；代價是團隊也要維護文字與程式之間的對應。

ToppleCat 選擇直接用 Java 寫主要的 `@ToppleTest`：

```java
@ToppleTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c) {
    given.a_cart(c.input("cart", Cart.class));
    when.creates_an_order();
    then.receipt_matches(c);
}
```

這個方法會先由 ToppleCat 編譯器檢查，再交給 JUnit 執行。裡面的 Stage
呼叫也會變成規格與驗證報告中的句子。畫面上看到的情境和實際跑過的測試，
中間沒有另一份可單獨修改的 `.feature`，也不需要文字到 step definition
的對應。

這對代理開發很重要。如果 AI 程式代理可以同時修改文字情境、step
definition 和正式程式，它可能把合約一起改弱，再交出一個看似正常的綠燈。
ToppleCat 不再增加一份可以單獨修改的文字合約，並在交付實作前封存核准過
的 Java 合約、案例資料、建置邏輯與驗證政策。

## 這裡說的 single source of truth（單一事實來源）是什麼？

不是把整份合約硬塞進同一個檔案。

ToppleCat 的權威合約由一般 Java 驗收測試，以及有型別的 JSON 或 YAML
案例資料組成。Java 定義行為和斷言，案例資料提供輸入與預期結果。產生的
JSON 與 HTML 只是閱讀方式，不是另一份可以修改的規格。

重點是報告不能寫一套，JUnit 背後卻執行另一套解讀。

## Fowler 網站上的 DSL 文章和 ToppleCat 有什麼關係？

Unmesh Joshi 在
[DSLs Enable Reliable Use of LLMs](https://martinfowler.com/articles/llm-and-dsls.html)
提到，小而明確的 DSL 可以縮小大型語言模型自由發揮的空間，再交給解析器、
型別檢查器或編譯器判斷產生的內容是否有效。代理做錯時，也能根據明確的錯誤
重新修正。

ToppleCat 採用的是小型 Java 內部 DSL。`@ToppleTest`、Stage 方法與有型別
的案例資料，提供一組有限的詞彙來描述驗收合約。ToppleCat 編譯器會檢查寫法，
JUnit 會執行它，同一組 Stage 呼叫也會轉成讓人閱讀的規格。真正長期保留的是
可執行合約，而不是當初交給代理的提示詞。

不過，DSL 寫得正確，不代表功能一定正確。合法的 Java 程式仍可能漏做規則、
把公開答案寫死，或根本沒有拿預期結果來比對。因此 ToppleCat 還會搭配審閱者
專用案例重測、預期結果使用檢查、突變測試與合約完整性檢查。

## ToppleCat 的突變測試真的有用 PIT 嗎？

有。[PIT](https://pitest.org/) 就是 ToppleCat 預設使用的突變測試引擎。
使用者不一定會在自己的 `build.gradle` 看到手動套用 PIT，因為使用預設的
`pitest` 工作時，ToppleCat 會自動套用並設定 PIT 的 Gradle 外掛。

兩邊負責的事情不一樣：

- PIT 會改動正式程式的位元碼、執行指定的公開測試，最後產生突變測試報告。
- ToppleCat 會指定主要的公開 `@ToppleTest`、讀取 PIT 報告、把突變結果
  對應回主要 AC、檢查門檻，並記錄這次驗證的判定。

ToppleCat 0.0.5 使用 PIT Gradle 外掛 1.19.0、PIT 1.25.5，以及 PIT 的
JUnit 5 外掛 1.2.3。這些只是目前內部使用的版本，不是新的 ToppleCat
規格寫法。專案也可以改用自訂的突變測試產生器；改用後，就由該產生器負責
執行突變測試，再把報告交給 ToppleCat 判定。

## 為什麼 ToppleCat 的模組裡看不到 `org.pitest` 相依套件？

因為 PIT 是執行驗證時使用的工具，不是 ToppleCat 的 Java API。
`topplecat-gradle-plugin` 直接依賴的是 `info.solidsoft.pitest` Gradle
外掛。消費者使用預設突變測試時，這個外掛才會替 `pitest` 工作下載
`org.pitest` 的引擎、命令列執行器與 JUnit 5 外掛。

ToppleCat 不會在公開模組中匯入 PIT 類別，而是用自己的解析器讀取 PIT
產生的 XML 報告。因此，PIT 不會出現在應用程式的正式或測試 classpath。
突變測試跑過後，可以在 Gradle 的相依套件快取找到下載的 `org.pitest`
檔案，不會在 ToppleCat 原始碼目錄裡看到它們。

## PIT 的授權允許 ToppleCat 這樣使用嗎？

可以。PIT 本體、JUnit 5 外掛與 Gradle PIT 外掛都採用 Apache License
2.0。這份授權允許一般與商業使用，也允許在遵守授權條件的前提下修改或
重新散布。

ToppleCat 目前只是把官方發布、未修改的套件當成建置工具。ToppleCat
自己的外掛 JAR 沒有包進 PIT 或 Gradle PIT 外掛的類別；發布的相依資訊
會讓 Gradle 另外下載它們。ToppleCat 本身也是 Apache 2.0 授權，沒有使用
另外收費的 ArcMutate 產品。

如果未來把 PIT 程式碼直接包進 ToppleCat，或修改後再散布，就要依 Apache
2.0 保留適用的授權、著作權、修改說明與 NOTICE 資訊。

## Dan North 與 Dave Farley 的 BDD 討論談了什麼？

Dan North 是行為驅動開發（BDD）的提出者，也是 JBehave 的作者。他在教導
測試驅動開發（TDD）時，發現團隊經常卡在「該從哪裡開始、該測什麼、失敗時
該怎麼說明」。因此，他改用業務行為來描述軟體，並把驗收條件寫成可以執行的
範例。

這集
[The Origins of Behaviour Driven Development](https://open.spotify.com/episode/5sWDCL6J21dFbjDyyeVT9B)
花了不少時間討論 Cucumber、內部 DSL 與可執行範例，相關內容大約從
16:13 談到 38:00。

這段討論帶給 ToppleCat 的影響很直接：

- BDD 的重點是建立共同理解，不是產生純文字檔案。
- 情境可以直接用專案的程式語言撰寫，而且仍然能讀得像業務範例。
- 從 Cucumber 開始，情境與正式程式之間可能多出 Gherkin 解析、步驟比對、
  step definition 和系統驅動層。
- 純文字 feature 有一個很實際的用途：領域專家需要逐行共同撰寫情境，
  而且這份共享結果能改善業務與工程團隊的溝通。

節目也提到一套規模很大的 SpecFlow 情境，完整執行要花很多小時。團隊後來
把它整理成少量、直接的測試，內部 DSL 也自然形成。這段經驗不是在說
Cucumber 一定不好，而是 `.feature` 應該用來解決真實的協作需求，不必把它
當成 BDD 的預設入口。

## JGiven 影響了哪些部分？

[JGiven](https://github.com/TNG/JGiven) 的定位是用一般 Java 寫 BDD。
情境使用流暢、帶有領域語意的 Java API，交給 JUnit 或 TestNG 執行，
最後產生領域專家也能閱讀的報告。

ToppleCat 採用了相近的基本形式：

- 情境直接寫成 Java；
- Stage 方法提供領域用語；
- 測試框架執行真正的情境；
- 報告從執行結果產生。

ToppleCat 不是 JGiven 的分支，也不是要取代它。ToppleCat 多處理一個問題：
用審閱者專用案例、預期結果使用檢查、突變測試、合約完整性與本次執行證據，
檢查 AI 程式代理的完成宣稱。

ToppleCat 沒有把 JGiven 加入相依套件，也沒有包入或匯入
`com.tngtech.jgiven` 類別。JGiven 是設計參考，不是 ToppleCat 執行時會用到
的元件。文件附上連結，是為了說明靈感來源，也方便讀者比較兩者的做法。

JGiven 本身採用 Apache License 2.0，和 ToppleCat 的 Apache 2.0 授權相容。
不過，ToppleCat 現在沒有散布 JGiven 的程式碼或二進位檔案，所以沒有因為
使用其套件而產生的散布要求。

## 產品負責人還看得懂規格嗎？

可以。審閱者可以在 HTML 報告裡查看業務標題、Stage 句子、公開案例與
預期結果，不需要閱讀 Stage 實作或正式程式。

但 Java 不會自己變得好讀。作者仍要使用真正的領域用語，例如
`a_cart_with_two_eligible_items()`，而不是把底層技術操作包裝成一長串方法名。

## 哪些情況比較適合 Cucumber？

當 `.feature` 本身就是團隊共同工作的文件時，Cucumber 可能更合適：

- 領域專家真的會親自撰寫或修改情境步驟；
- 他們熟悉情境中的逐行細節；
- 團隊願意把 step definition 當成另一個需要維護的介面；
- 整個組織需要查看 feature 的執行狀態。

這是合理的選擇，只是不是 ToppleCat 要解決的問題。

## 同一個專案可以同時使用 Cucumber 和 ToppleCat 嗎？

可以，但 Cucumber 情境仍是另一套測試。ToppleCat 不會讀取 `.feature`，
也不會把它當成 AC 的權威對應。ToppleCat 的 AC 仍需要 Java annotation；
資料驅動的行為也需要主要 `@ToppleTest` 與有型別的案例資料。

## 參考資料

- [Unmesh Joshi：DSLs Enable Reliable Use of LLMs](https://martinfowler.com/articles/llm-and-dsls.html)
- [PIT Mutation Testing](https://pitest.org/)
- [PIT 授權](https://github.com/hcoles/pitest/blob/master/LICENSE.txt)
- [Gradle PIT 外掛授權](https://github.com/szpak/gradle-pitest-plugin/blob/master/LICENSE-2.0.txt)
- [Dan North：Introducing BDD](https://dannorth.net/blog/introducing-bdd/)
- [Dan North 與 Dave Farley：The Origins of Behaviour Driven Development](https://open.spotify.com/episode/5sWDCL6J21dFbjDyyeVT9B)
- [JGiven：Behavior-Driven Development in plain Java](https://github.com/TNG/JGiven)
- [JGiven 授權](https://github.com/TNG/JGiven/blob/master/LICENSE)
- [Cucumber 入門](https://cucumber.io/docs/)
- [Cucumber step definitions](https://cucumber.io/docs/cucumber/step-definitions/)
