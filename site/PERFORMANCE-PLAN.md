# ToppleCat 專案頁 PageSpeed 改善工作計劃

狀態：實作與本機 production 驗收完成；正式 HTTPS 的部署後量測待發布授權
日期：2026-07-28
範圍：`site/` 的 Vite／React 專案頁；不變更 ToppleCat 的 Java 模組、GitHub Pages 網域或部署方式。

## 目標與目前基線

以正式網址 `https://topplecat.samzhu.dev/` 作為唯一測量目標。先前提供的
PageSpeed 分享連結指向 `http://`，無法作為可重現的測量基線。

2026-07-28 以 Lighthouse 13 對正式 HTTPS URL 執行三次冷快取測量的中位數如下。這是
可重現的發布前基線；Google PageSpeed Insights 的正式複測仍保留到發布後執行。

| 指標 | Mobile | Desktop |
| --- | ---: | ---: |
| Performance（三次） | 63（63／63／63） | 70（70／81／65） |
| First Contentful Paint | 2.40 秒 | 1.20 秒 |
| Largest Contentful Paint | 8.12 秒 | 3.21 秒 |
| Total Blocking Time | 0 ms | 0 ms |
| Cumulative Layout Shift | 0.000038 | 0.000059 |
| 初始網路傳輸量 | 3.14 MB | 3.14 MB |

初步診斷把最大效益放在首屏圖像：四張 PNG 約為 2.86 MB；PageSpeed 估計
「改善圖片傳送」可節省約 1.7–2.3 MB。外部 Satoshi 字型目前以 CSS `@import`
載入，亦可能屬於 render-blocking 要求的一部分。JavaScript 的未使用碼約
60 KB，但 TBT 為 0，因此不是第一優先。

本計劃的成果目標是：在不改變視覺設計、三格貓咪動畫、可存取性偏好或 GitHub
Pages 部署模式的前提下，將首訪傳輸量降至 1.25 MB 以下，並以三次冷快取
Mobile 測量的中位數驗證 LCP 朝 2.5 秒目標改善。若網路條件或外部服務使目標
未達成，必須記錄實際數字與剩餘瓶頸，不以單次高分取代驗收。

## 2026-07-28 實作紀錄與本機驗收

已完成的項目：

- 原始 PNG 已原封不動地移至 `src/assets/original/` 並改名為 `.org.png`；其
  SHA-256、尺寸和 alpha 通道均由 `npm run verify:assets` 驗證。production 只
  引用 AVIF/WebP 衍生檔，`dist/` 不含 `.org.png`。
- 加入鎖定版本的 `sharp` 與 `npm run optimize:assets`，產生背景、三格 cat
  sprite，以及杯子的 320／640／960px AVIF 與 WebP 尺寸組。這讓低 DPI 小螢幕
  可以選較小圖片，而高 DPI 螢幕仍可選 640 或 960px。
- CSS 使用 `image-set()` 優先選 AVIF、WebP 作 fallback；杯子以 `<picture>`、
  `srcset` 與 `sizes` 選擇合適尺寸。已確認的 LCP cat sprite 以 AVIF preload 和
  `fetchpriority="high"` 優先發現。
- Satoshi 改為 `preconnect` 加上非阻塞 stylesheet 與 `display=swap`，不再出現在
  Lighthouse 的 render-blocking 項目。
- GSAP、Flip、ScrollTrigger 改成首屏初始繪製後才動態載入；首個 JavaScript
  chunk 由約 121 KB gzip 降為約 28 KB gzip。CSS 保留三個原子動畫狀態和
  reduced-motion 的靜態 FAKE 終態。
- Lighthouse 具體定位到原正式站的 Accessibility 96：桌面導覽列與 evidence
  accordion 的索引文字對比不足。兩者已改為不透明的既有深綠／淺色 token；本機
  production 的 Mobile 和 Desktop Accessibility 均為 100，`color-contrast` 為
  通過。
- 加入最小的 `public/robots.txt` 與無外部相依的 `favicon.svg`。原正式站缺少
  `robots.txt`，而本機預覽會請求不存在的預設 favicon；最新 production 測量的
  Mobile 和 Desktop Best Practices、SEO 均為 100，且沒有 console error。

本機以 `vite build` 後的 `vite preview`、Lighthouse 13 冷快取測量結果：

| 指標 | Mobile 本機 production | Desktop 本機 production |
| --- | ---: | ---: |
| Performance | 94 | 99 |
| Accessibility / Best Practices / SEO | 100 / 100 / 100 | 100 / 100 / 100 |
| FCP | 1.43 秒 | 0.38 秒 |
| LCP | 2.95 秒 | 0.99 秒 |
| TBT | 0 ms | 0 ms |
| CLS | 0 | 0 |
| 首次傳輸 | 約 276 KB | 約 255 KB |

本機還驗證了桌面 REST／CONTACT／VERDICT 的 sprite 與杯子狀態、390px 寬手機的
無橫向溢位、無 browser console warning/error，以及 `prefers-reduced-motion`
的靜態 FAKE 畫面。這些數字是本機品質閘門，不取代正式站結果；發布後仍須依
Phase 4 在 `https://topplecat.samzhu.dev/` 完成六次 PageSpeed 冷快取測量並記錄
中位數。

另以 `PAGES_BASE_PATH=/topplecat/` 建置檢查過 Vite 的 HTML 輸出：favicon、LCP
preload、CSS 與 JavaScript 均正確改寫為該 base path 下的 fingerprinted URL，故不會
破壞現有 GitHub Pages workflow 的 custom-domain 或 project-path 部署模式。

正式站目前由 GitHub Pages 提供，HTML 和已 fingerprint 的 JS 都回傳
`cache-control: max-age=600`。由於現有 Pages 工作流沒有安全、可驗證的自訂 header
表面，本輪不加入 service worker、redirect、CDN 或 DNS 變更；長期 immutable cache
需作為獨立的 hosting 決策。

## 不可違反的素材保留規則

原始美術檔是可回復的來源，不是 production 資產。實作時必須：

1. 將現有 PNG 原檔移至
   `site/src/assets/original/{backgrounds,characters,props}/`，名稱改為
   `*.org.png`。
2. 衍生的 `.avif` 與 `.webp` 仍放在既有用途目錄，並且只有它們能被
   `App.jsx`、`styles.css` 或 `index.html` 引用。
3. 原圖不得放進 `site/public/`，不得由任何程式 import，也不得出現在
   `site/dist/`。Vite 只會打包被引用的 `src` 資產，故可追蹤保存原圖而不增加
   網頁傳輸量。
4. 更新 `site/src/assets/README.md`，明確記錄原圖位置、衍生檔的命名規則、
   sprite 的三等分尺寸及共同 paw anchor 不得改變。
5. 轉檔前後保留原始尺寸、SHA-256 與目視比對紀錄；壓縮失真、透明通道遺失或
   sprite 裁切錯位時，回到原圖重做，不覆寫原圖。

## 執行階段

### 0. 建立可比較的基線

- 以 HTTPS 正式站，在 PageSpeed Insights 分別執行 Mobile、Desktop 各三次冷快取
  測量，記錄中位數與每次的 LCP 元素、request waterfall、圖片節省量及
  render-blocking 資源。
- 在本機執行 `npm ci && npm run build`，記錄 `site/dist/assets` 的檔名、gzip
  後 JavaScript/CSS 大小與每張圖片大小。
- 擷取 1440px 桌面、390px 手機的首屏，以及動畫的 PASS、FAKE 兩個畫面；另擷取
  `prefers-reduced-motion: reduce` 的靜態 FAKE 畫面。這些是視覺回歸的比對基準。
- 確認這一輪真正的 LCP 候選元素後才決定 preload 對象；不得憑猜測預載所有 hero
  素材。

### 1. 建立可重現的圖片衍生流程

- 新增以 Node 執行的轉檔腳本（預計為
  `site/scripts/optimize-assets.mjs`），採用可鎖定版本的 encoder，例如 `sharp`
  作為開發期依賴；腳本須能在 macOS 與 GitHub Actions 的 Linux 環境重跑。
- 第一輪僅做無裁切的格式轉換與有品質紀錄的壓縮：

  | 原圖 | 要產生的網頁資產 | 特別限制 |
  | --- | --- | --- |
  | `argyle-tile.org.png` | `argyle-tile.avif`、`argyle-tile.webp` | 保持 976 × 872 與 seamless tile 邊緣。 |
  | `cat-action-sprite.org.png` | `cat-action-sprite.avif`、`cat-action-sprite.webp` | 保持 2661 × 887、alpha 與三個相等 frame。 |
  | `cup-upright.org.png` | AVIF/WebP 響應式尺寸組 | 保持透明邊界及杯子基準點。 |
  | `cup-tipped.org.png` | AVIF/WebP 響應式尺寸組 | 保持 spill、透明邊界及動畫位置。 |

- 杯子依實際 CSS 顯示尺寸輸出合適的 `srcset` 尺寸組；背景與 sprite 先維持原始
  像素尺寸，避免圖案比例、Retina 清晰度或 frame 對齊改變。只有量測證實仍可縮小
  時，才新增下一輪尺寸縮減。
- 腳本的輸出必須是 deterministic；再次執行不應產生未追蹤的臨時檔或覆寫 `.org.png`。

### 2. 讓瀏覽器優先發現正確的首屏資產

- 將 CSS 背景與 sprite 從 JavaScript inline style 改為在 `styles.css` 宣告的
  `image-set()`，提供 AVIF 優先、WebP fallback。這能讓 CSS 解析時發現資源，
  也避免等待 React bundle 執行才取得 URL。
- 將兩張杯子改為 `<picture>`／`<source srcSet>`，提供 AVIF、WebP 與正確的
  `sizes`。保留現有的絕對定位、`alt=""`、可見性與 GSAP class 名稱。
- 只對 Phase 0 識別出的 LCP 素材加入 `rel="preload" as="image"` 與
  `fetchpriority="high"`。輸出後必須檢查 Vite 是否將 `index.html` 的資產 URL
  正確 fingerprint；若沒有，改用可由 Vite 安全轉換的寫法。
- LCP 圖片不可標示 `loading="lazy"`。FAKE 杯子若要降優先，須在慢速網路下確認
  1.52 秒切換時不會顯示空白或延遲。

### 3. 移除剩餘關鍵路徑阻塞

- 把 `styles.css` 的外部 Satoshi `@import` 改為 `index.html` 中明確的
  `preconnect` 與 stylesheet 請求，保留 `display=swap` 及系統字型 fallback。
  實作前後比較 waterfall、FCP、LCP、FOIT/FOUT 與 CLS；若無淨收益則保留現況並
  在結果中說明。
- 圖片改善後再測量主 bundle。只有證據顯示其仍拖慢 LCP 或互動時，才將
  GSAP 的 `ScrollTrigger`／`Flip` 等非首屏必要功能拆成延後載入的 chunk。不得
  破壞 reduced-motion、accordion click 動畫、頁面捲動 pin 或首次 hero 畫面。
- PageSpeed 的快取生命週期建議最後處理。先確認 GitHub Pages 對 fingerprinted
  assets 的實際 header；若無法在目前 Pages 流程安全設定長期 immutable cache，
  將它列為未來 CDN／hosting 決策，不在本次改動 DNS 或部署平台。

### 4. 視覺、功能與效能驗收

- 以 production build 在本機 preview 檢查桌面、手機、觸控、鍵盤、深連結、複製
  安裝指令、accordion、動畫循環、背景平鋪與 reduced-motion。
- 檢查 production bundle：`.org.png` 不得出現在 `site/dist/`；僅有必要的
  AVIF/WebP 和已 fingerprint 的 SVG/JS/CSS 檔案。
- 執行：

  ```bash
  cd site
  npm ci
  npm run build
  git diff --check
  ```

- 部署後在正式 HTTPS URL 重跑 Phase 0 的六次 PageSpeed 測量。驗收時記錄原始與
  新的中位數，而非只引用最佳一次；Mobile LCP、總傳輸量與 render-blocking 時間
  必須改善，Desktop 2.5 秒 LCP、CLS 0、Best Practices 100 與 SEO 100 不得退步。
- 修正 PageSpeed Accessibility 96 所指的對比問題，或將其另列為有定位到 selector
  的後續工作；不得在效能改動中悄悄降低可讀性。

## 預計變更檔案

- `site/PERFORMANCE-PLAN.md`（本計劃）
- `site/src/assets/README.md`
- `site/src/assets/original/**`（追蹤保留的 `.org.png`）
- `site/src/assets/backgrounds/**`、`characters/**`、`props/**`（僅衍生 AVIF/WebP）
- `site/scripts/optimize-assets.mjs`、`site/package.json`、`site/package-lock.json`
  （可重現的轉檔命令）
- `site/src/App.jsx`、`site/src/styles.css`、`site/index.html`
- `site/public/robots.txt`、`site/public/favicon.svg`

不會改動 Java 模組、產生 `site/dist/`、提交原圖到 `public/`、變更 DNS，或在沒有
另行授權下 commit、push、發布。

## 風險與停止條件

- 若 AVIF/WebP 造成透明邊緣、色彩漸層或 sprite frame 對齊肉眼可見地退化，該素材
  不採用該品質設定，保留原圖並以較高品質重新產生。
- 若 preload 的不是實際 LCP 元素，它會和真正的首屏資源競爭；移除 preload 並以
  實測 waterfall 重新判定。
- 若字型載入調整引入 FOUT、CLS 或可讀性倒退，回復字型策略，讓圖片改善獨立落地。
- 若 CDN cache header 無法在既有 GitHub Pages 控制，記錄為架構選項，不能以未驗證的
  redirect、service worker 或 DNS 修改當作本次的快速修復。

## 研究依據

- [Optimize LCP](https://web.dev/articles/optimize-lcp?hl=en)：LCP 資源必須及早發現；
  preload 與高優先權只能用在已確認的 LCP 資源。
- [Responsive images](https://web.dev/articles/responsive-images)：依顯示尺寸與格式提供
  圖片能降低不必要傳輸。
- [Image performance](https://web.dev/learn/performance/image-performance?hl=en)：使用適當
  的現代圖片格式與尺寸可改善載入時間與 LCP。
- [Understanding the critical path](https://web.dev/learn/performance/understanding-the-critical-path?hl=en)：
  render-blocking 要求需要以實際依賴鏈量測，而非盲目非同步化。
