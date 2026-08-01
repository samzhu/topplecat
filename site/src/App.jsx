import { useEffect, useRef, useState } from "react";
import { flushSync } from "react-dom";
import catSpriteAvif from "./assets/characters/cat-action-sprite.avif";
import catSpriteWebp from "./assets/characters/cat-action-sprite.webp";
import coasterLayer from "./assets/props/coaster.svg";
import tippedCup320Avif from "./assets/props/cup-tipped-320.avif";
import tippedCup640Avif from "./assets/props/cup-tipped-640.avif";
import tippedCup960Avif from "./assets/props/cup-tipped-960.avif";
import tippedCup320Webp from "./assets/props/cup-tipped-320.webp";
import tippedCup640Webp from "./assets/props/cup-tipped-640.webp";
import tippedCup960Webp from "./assets/props/cup-tipped-960.webp";
import uprightCup320Avif from "./assets/props/cup-upright-320.avif";
import uprightCup640Avif from "./assets/props/cup-upright-640.avif";
import uprightCup960Avif from "./assets/props/cup-upright-960.avif";
import uprightCup320Webp from "./assets/props/cup-upright-320.webp";
import uprightCup640Webp from "./assets/props/cup-upright-640.webp";
import uprightCup960Webp from "./assets/props/cup-upright-960.webp";
import stageFloorLayer from "./assets/scene/tabletop.svg";

const repositoryUrl = "https://github.com/samzhu/topplecat";
const verificationGuideUrl =
  `${repositoryUrl}/blob/main/docs/guide/verification-and-evidence.md`;
const pluginLine = 'id("io.github.samzhu.topplecat") version "0.0.11"';

const scenarioCode = `@ToppleAcceptanceTest("AC-CART-COUPON")
void appliesCoupon(
    ToppleCase c,
    ToppleScenario scenario,
    CouponStage coupon
) {
    scenario.given(coupon).a_cart(c.input("cart", Cart.class));
    scenario.when(coupon).creates_an_order();
    scenario.then(coupon).receipt_matches(c);
}`;

const copyByLocale = {
  en: {
    htmlLang: "en",
    documentTitle: "ToppleCat: Make every agent’s “done” prove itself",
    metaDescription:
      "ToppleCat verifies AI-delivered Java with executable acceptance contracts, hidden tests, mutation testing, property-based testing, and current-run evidence.",
    nav: {
      label: "Primary navigation",
      home: "ToppleCat home",
      gates: "Safeguards",
      contract: "The contract",
      workflow: "Workflow",
      install: "Get started",
      switchLanguage: "Current language: English. Switch to Traditional Chinese.",
    },
    hero: {
      titleBefore: "Make every agent’s",
      titleAfter: "“done” prove itself.",
      summary:
        "Executable Java acceptance contracts catch shortcuts, hard-coded answers, changed rules, and incomplete work.",
      explore: "See how it works",
      repository: "View on GitHub",
      scene:
        "A ToppleCat watches a PASS label on a coffee mug, reaches for it, then tips the mug so the label becomes FAKE.",
    },
    marqueeLabel: "ToppleCat capabilities",
    marqueeItems: [
      "Hidden tests",
      "Mutation testing",
      "Property-Based Testing",
      "Expected values asserted",
      "Contract integrity",
      "Run-scoped evidence",
    ],
    manifesto: [
      "A green public test is useful feedback,",
      "not proof of completion.",
      "ToppleCat runs the accepted contract again,",
      "challenges the implementation",
      "from different angles,",
      "and records",
      "one fresh verdict",
      "for this delivery.",
    ],
    gates: {
      kicker: "Different checks catch different mistakes",
      heading: "Your public contract runs first. Then ToppleCat pushes harder.",
      summary:
        "Hidden cases, changed code, and generated inputs test different failure modes. Contract integrity and expected-value checks keep the evidence honest.",
      cards: [
        {
          label: "Hidden Tests",
          detailLabel: "Reviewer-owned cases",
          title: "Try business cases the agent never saw.",
          body: "Reviewer-owned rows retest the selected ACs with different examples inside the reviewer boundary.",
          className: "hidden-retest",
        },
        {
          label: "Mutation Testing",
          detailLabel: "Changed code",
          title: "Break the implementation on purpose.",
          body: "See whether each public Acceptance Method ran against the changed code and whether that same method detected it.",
          className: "mutation",
        },
        {
          label: "Property-Based Testing",
          detailLabel: "Generated inputs",
          title: "Challenge one rule with many inputs.",
          body: "Bounded, reproducible trials look for a counterexample without borrowing hidden cases.",
          className: "property",
        },
        {
          label: "Expected values",
          detailLabel: "Assertion obligation",
          title: "Every declared result must be checked.",
          body: "Reading an expected value is not enough. The test must compare it with the result.",
          className: "expected",
        },
        {
          label: "Contract integrity",
          detailLabel: "Sealed before handoff",
          title: "The agent cannot move the goalposts.",
          body: "The public contract and verification policy must still match the reviewer’s seal before any other result can count.",
          className: "integrity",
        },
      ],
    },
    scenario: {
      heading: "The review reads like the test runs.",
      body:
        "Each selected Spec AC maps to one Java acceptance method. The compiler fixes its Given, When, Then, and And steps before handoff, so reports cannot invent a second version of the story.",
      chainLabel: "Executable contract mapping",
      chain: ["Spec AC", "Java method", "Typed rows", "Review and evidence"],
      codeLabel: "Executable Java",
      codeAriaLabel: "ToppleScenario acceptance example",
    },
    views: {
      heading: "One executable contract. Two human reports.",
      summary:
        "The reviewer reads the complete selected Spec before handoff, then gets one failure-first report from formal verification. Neither becomes another source of truth.",
      items: [
        {
          title: "Spec Review",
          detail:
            "Before handoff, the reviewer reads the complete selected Markdown, diagrams, executable Scenario, public and reviewer rows, Properties, and Acceptance Method source.",
          className: "contract",
        },
        {
          title: "Verification Report",
          detail:
            "After Verify, the reviewer sees the delivery conclusion first, then separate public, hidden, Property, mutation, and integrity findings. Safe agent feedback reveals the failing gate, never the hidden answer.",
          className: "evidence",
        },
      ],
    },
    proof: {
      kicker: "A separate formal workflow",
      heading: "Development stays fast. Verification stays strict.",
      body:
        "People choose the Spec and complete its rules. ToppleCat binds those ACs to executable Java, keeps the accepted contract unchanged, and tests the agent’s completion claim.",
      guide: "Read the verification guide",
      steps: [
        {
          command: "toppleCatCheck --spec specs/023-checkout/spec.md",
          label: "Check",
          title: "Make the contract internally consistent",
          body: "Validate the selected ACs, typed rows, Java bindings, and compiler-described Scenario steps.",
        },
        {
          command: "toppleCatReview --spec specs/023-checkout/spec.md",
          label: "Review",
          title: "Read exactly what will run",
          body: "Render the complete selected Spec and its bound executable material before any implementation handoff.",
        },
        {
          command: "toppleCatSeal --spec specs/023-checkout/spec.md",
          label: "Seal",
          title: "Keep the approved question fixed",
          body: "Move reviewer source into local custody and seal the public contract plus verification policy.",
        },
        {
          command: "test",
          label: "Develop",
          title: "Let the agent work against public information",
          body: "The implementation agent uses ordinary project tests for fast feedback. This green check is not the final verdict.",
        },
        {
          command: "toppleCatVerify --spec specs/023-checkout/spec.md",
          label: "Verify",
          title: "Test the done claim with fresh evidence",
          body: "Run the selected public contract and enabled safeguards, write current evidence, then re-hide reviewer source.",
        },
      ],
      verdictLabel: "Aggregate verdict",
      verdictBody:
        "Only every required gate passing in the current run lets the claim stand.",
    },
    install: {
      heading: "Try the sample before changing your project.",
      summary:
        "Watch a deliberately wrong implementation fail, then see the corrected version produce current evidence.",
      note: "Requires Java 25 and a Gradle version that supports it.",
      copy: "Copy plugin line",
      copied: "Copied",
      sample: "Run the sample",
      install: "Install ToppleCat",
      skill: "Using an SDD agent? Add the project-local acceptance skill",
    },
    footer: {
      tagline: "Executable acceptance for agent-written Java.",
      readme: "README",
      readmePath: "README.md",
    },
  },
  "zh-TW": {
    htmlLang: "zh-Hant-TW",
    documentTitle: "ToppleCat：讓每一個 agent 的「完成」都能自證",
    metaDescription:
      "ToppleCat 以可執行的 Java 驗收契約、隱藏測試、變異測試、性質導向測試與本次執行證據，驗證 AI 交付的程式碼。",
    nav: {
      label: "主要導覽",
      home: "ToppleCat 首頁",
      gates: "防護檢查",
      contract: "可執行契約",
      workflow: "工作流程",
      install: "開始使用",
      switchLanguage: "目前語言：繁體中文。切換為英文。",
    },
    hero: {
      titleBefore: "讓每一個 agent 的",
      titleAfter: "「完成」都能自證。",
      summary:
        "可執行的 Java 驗收契約，找出取巧、硬編碼答案、規則遭更動與未完成的工作。",
      explore: "看看如何運作",
      repository: "在 GitHub 查看",
      scene:
        "ToppleCat 看著咖啡杯上方的 PASS，伸手撥倒杯子，標示隨之變成 FAKE。",
    },
    marqueeLabel: "ToppleCat 功能",
    marqueeItems: [
      "隱藏測試",
      "變異測試",
      "性質導向測試",
      "驗證預期值",
      "契約完整性",
      "本次執行證據",
    ],
    manifesto: [
      "綠燈的公開測試，",
      "只是有用的回饋，",
      "不是完成的證明。",
      "ToppleCat 會重新執行",
      "已接受的契約，",
      "從不同角度挑戰實作，",
      "並為這次交付",
      "留下全新的判定。",
    ],
    gates: {
      kicker: "不同檢查捕捉不同錯誤",
      heading: "先跑公開契約。再讓 ToppleCat 加強驗證。",
      summary:
        "隱藏案例、變更後的程式碼與產生的輸入，會測試不同的失敗模式。契約完整性與預期值檢查，確保證據可信。",
      cards: [
        {
          label: "隱藏測試",
          detailLabel: "審查者持有案例",
          title: "嘗試 agent 從未看過的業務案例。",
          body: "審查者持有的案例列在審查邊界中，以不同範例重新測試所選 AC。",
          className: "hidden-retest",
        },
        {
          label: "變異測試",
          detailLabel: "變更後的程式碼",
          title: "刻意破壞實作。",
          body: "確認每個公開 Acceptance Method 是否執行到被改壞的程式，以及同一方法是否真的偵測到改變。",
          className: "mutation",
        },
        {
          label: "性質導向測試",
          detailLabel: "產生的輸入",
          title: "用大量輸入挑戰同一條規則。",
          body: "可重現且有界限的試驗尋找反例，但不借用隱藏案例。",
          className: "property",
        },
        {
          label: "預期值",
          detailLabel: "斷言義務",
          title: "每個宣告的結果都必須檢查。",
          body: "讀取預期值還不夠；測試必須將它與結果比較。",
          className: "expected",
        },
        {
          label: "契約完整性",
          detailLabel: "交付前封存",
          title: "agent 無法搬動終點線。",
          body: "公開契約與驗證政策，必須仍與審查者的 seal 一致，其他結果才有資格計入。",
          className: "integrity",
        },
      ],
    },
    scenario: {
      heading: "審查讀到的內容，就是測試實際執行的內容。",
      body:
        "每個選取的 Spec AC 都對應一個 Java acceptance method。編譯器會在交付前固定 Given、When、Then 與 And 步驟，讓報告無法編造第二個故事版本。",
      chainLabel: "可執行契約對應關係",
      chain: ["Spec AC", "Java acceptance method", "型別化案例列", "審查與證據"],
      codeLabel: "可執行 Java",
      codeAriaLabel: "ToppleScenario 驗收範例",
    },
    views: {
      heading: "一份可執行契約，兩份人類報告。",
      summary:
        "交付前，審閱者閱讀完整的已選 Spec；正式驗證後，再從一份問題優先的報告查看結果。兩者都不會成為第二個事實來源。",
      items: [
        {
          title: "Spec Review",
          detail:
            "交付前，審閱者會看到完整的已選 Markdown、圖表、可執行 Scenario、公開與隱藏案例、Properties，以及 Acceptance Method 原始碼。",
          className: "contract",
        },
        {
          title: "Verification Report",
          detail:
            "Verify 後先顯示交付結論，再分開呈現公開驗收、隱藏測試、Property、Mutation 與 Integrity 問題；安全的 agent 回饋只顯示失敗 gate，絕不揭露隱藏答案。",
          className: "evidence",
        },
      ],
    },
    proof: {
      kicker: "獨立且正式的流程",
      heading: "開發維持快速，驗證維持嚴格。",
      body:
        "人選擇 Spec 並完成規則。ToppleCat 把這些 AC 連結到可執行 Java，維持已接受的契約不變，並檢驗 agent 的完成宣稱。",
      guide: "閱讀驗證指南（英文）",
      steps: [
        {
          command: "toppleCatCheck --spec specs/023-checkout/spec.md",
          label: "Check",
          title: "讓契約保持內部一致",
          body: "驗證選取的 AC、型別化案例列、Java 連結，以及由編譯器描述的 Scenario 步驟。",
        },
        {
          command: "toppleCatReview --spec specs/023-checkout/spec.md",
          label: "Review",
          title: "閱讀即將執行的完整內容",
          body: "在任何實作交付前，呈現完整的已選 Spec 與其綁定的可執行材料。",
        },
        {
          command: "toppleCatSeal --spec specs/023-checkout/spec.md",
          label: "Seal",
          title: "固定已接受的題目",
          body: "將審查者原始碼移入本機保管，並封存公開契約與驗證政策。",
        },
        {
          command: "test",
          label: "Develop",
          title: "讓 agent 依公開資訊開發",
          body: "實作 agent 使用一般專案測試取得快速回饋；這個綠燈不是最終判定。",
        },
        {
          command: "toppleCatVerify --spec specs/023-checkout/spec.md",
          label: "Verify",
          title: "用全新證據檢驗完成宣稱",
          body: "執行選取的公開契約與啟用的 safeguards、寫入本次證據，然後重新隱藏審查者原始碼。",
        },
      ],
      verdictLabel: "彙總判定",
      verdictBody: "只有本次執行中每個必要 gate 都通過，完成宣稱才能成立。",
    },
    install: {
      heading: "先執行範例，再改動自己的專案。",
      summary: "先看刻意錯誤的實作失敗，再看修正後的版本產生本次證據。",
      note: "需要 Java 25 與支援它的 Gradle 版本。",
      copy: "複製 plugin 設定",
      copied: "已複製",
      sample: "執行範例",
      install: "安裝 ToppleCat",
      skill: "正在使用 SDD agent？加入專案內的 acceptance skill",
    },
    footer: {
      tagline: "為 agent 撰寫的 Java 提供可執行驗收。",
      readme: "繁中 README",
      readmePath: "README.zh-TW.md",
    },
  },
};

function localeFromUrl() {
  return new URLSearchParams(window.location.search).get("lang") === "zh-TW"
    ? "zh-TW"
    : "en";
}

function replaceLocaleInUrl(locale) {
  const url = new URL(window.location.href);
  if (locale === "zh-TW") {
    url.searchParams.set("lang", "zh-TW");
  } else {
    url.searchParams.delete("lang");
  }
  window.history.replaceState(window.history.state, "", `${url.pathname}${url.search}${url.hash}`);
}

function Arrow() {
  return <span aria-hidden="true" className="arrow">↗</span>;
}

function Cup({ className, avifSources, webpSources, fallback }) {
  return (
    <picture className={className}>
      <source type="image/avif" srcSet={avifSources} sizes="(max-width: 700px) 52vw, 23vw" />
      <source type="image/webp" srcSet={webpSources} sizes="(max-width: 700px) 52vw, 23vw" />
      <img src={fallback} width="960" height="960" alt="" />
    </picture>
  );
}

const cupSources = {
  upright: {
    avif: `${uprightCup320Avif} 320w, ${uprightCup640Avif} 640w, ${uprightCup960Avif} 960w`,
    webp: `${uprightCup320Webp} 320w, ${uprightCup640Webp} 640w, ${uprightCup960Webp} 960w`,
    fallback: uprightCup960Webp,
  },
  tipped: {
    avif: `${tippedCup320Avif} 320w, ${tippedCup640Avif} 640w, ${tippedCup960Avif} 960w`,
    webp: `${tippedCup320Webp} 320w, ${tippedCup640Webp} 640w, ${tippedCup960Webp} 960w`,
    fallback: tippedCup960Webp,
  },
};

function App() {
  const scope = useRef(null);
  const motion = useRef({ gsap: null, Flip: null, gsapPromise: null });
  const [locale, setLocale] = useState(localeFromUrl);
  const [activeAccordion, setActiveAccordion] = useState(0);
  const [copied, setCopied] = useState(false);
  const copy = copyByLocale[locale];

  const loadGsap = async () => {
    if (motion.current.gsap) return motion.current;
    if (!motion.current.gsapPromise) {
      motion.current.gsapPromise = Promise.all([
        import("gsap"),
        import("gsap/ScrollTrigger"),
      ]).then(([{ gsap }, { ScrollTrigger }]) => {
        gsap.registerPlugin(ScrollTrigger);
        motion.current = { ...motion.current, gsap, ScrollTrigger };
        return motion.current;
      });
    }
    return motion.current.gsapPromise;
  };

  useEffect(() => {
    const handlePopState = () => setLocale(localeFromUrl());
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  useEffect(() => {
    document.documentElement.lang = copy.htmlLang;
    document.title = copy.documentTitle;
    document.querySelector('meta[name="description"]')?.setAttribute("content", copy.metaDescription);
    setCopied(false);

    const frame = window.requestAnimationFrame(() => {
      motion.current.ScrollTrigger?.refresh();
    });
    return () => window.cancelAnimationFrame(frame);
  }, [copy]);

  useEffect(() => {
    let cancelled = false;
    let context;
    const section = scope.current?.querySelector(".manifesto");
    if (!section || window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      return undefined;
    }

    const observer = new IntersectionObserver(([entry]) => {
      if (!entry.isIntersecting) return;
      observer.disconnect();
      void loadGsap().then(({ gsap, ScrollTrigger }) => {
        if (cancelled) return;
        context = gsap.context(() => {
          const media = gsap.matchMedia();
          const q = gsap.utils.selector(scope);

          media.add("(prefers-reduced-motion: no-preference)", () => {
            gsap.utils.toArray(q(".reveal-word")).forEach((word) => {
              gsap.to(word, {
                opacity: 1,
                scrollTrigger: {
                  trigger: q(".manifesto")[0],
                  start: "top 72%",
                  end: "bottom 48%",
                  scrub: 0.55,
                },
              });
            });

            gsap.utils.toArray(q(".gate-card")).forEach((card) => {
              gsap.from(card, {
                y: 44,
                opacity: 0,
                scrollTrigger: {
                  trigger: card,
                  start: "top 88%",
                  once: true,
                },
              });
            });

            return undefined;
          });

          media.add("(min-width: 960px) and (prefers-reduced-motion: no-preference)", () => {
            const proofLayout = q(".proof-layout")[0];
            const proofIntro = q(".proof-intro")[0];
            if (!proofLayout || !proofIntro) return undefined;

            return ScrollTrigger.create({
              trigger: proofLayout,
              start: "top top+=110",
              end: "bottom bottom-=120",
              pin: proofIntro,
              pinSpacing: false,
            });
          });

          media.add("(prefers-reduced-motion: reduce)", () => {
            gsap.set(q(".reveal-word"), { opacity: 1 });
            gsap.set(q(".gate-card"), { opacity: 1, y: 0 });
          });

          return () => media.revert();
        }, scope);
      });
    }, { rootMargin: "0px" });
    observer.observe(section);

    return () => {
      cancelled = true;
      observer.disconnect();
      context?.revert();
    };
  }, []);

  const changeLanguage = () => {
    const nextLocale = locale === "en" ? "zh-TW" : "en";
    replaceLocaleInUrl(nextLocale);
    setLocale(nextLocale);
  };

  const changeAccordion = async (index) => {
    if (index === activeAccordion) return;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) {
      setActiveAccordion(index);
      return;
    }

    let { Flip } = motion.current;
    if (!Flip) {
      const { gsap } = await loadGsap();
      ({ Flip } = await import("gsap/Flip"));
      gsap.registerPlugin(Flip);
      motion.current = { ...motion.current, Flip };
    }
    if (!Flip) {
      setActiveAccordion(index);
      return;
    }
    const panels = scope.current?.querySelectorAll(".accordion-panel");
    const state = panels ? Flip.getState(panels) : null;
    flushSync(() => setActiveAccordion(index));
    if (!state) return;
    Flip.from(state, {
      duration: 0.28,
      ease: "power3.out",
      absolute: false,
      nested: true,
      scale: true,
    });
  };

  const copyInstall = async () => {
    try {
      await navigator.clipboard.writeText(pluginLine);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
    }
  };

  return (
    <main className="page-shell" data-locale={locale} ref={scope}>
      <div className="page-background-layer" aria-hidden="true" />
      <div className="ambient-orb orb-one" />
      <div className="ambient-orb orb-two" />

      <nav className="site-nav" aria-label={copy.nav.label}>
        <a className="wordmark" href="#top" aria-label={copy.nav.home}>
          <span className="wordmark-mark" aria-hidden="true">T</span>
          <span>ToppleCat</span>
        </a>
        <div className="nav-links">
          <a href="#gates">{copy.nav.gates}</a>
          <a href="#contract">{copy.nav.contract}</a>
          <a href="#proof">{copy.nav.workflow}</a>
          <a href="#install">{copy.nav.install}</a>
        </div>
        <button
          className="language-toggle"
          type="button"
          onClick={changeLanguage}
          aria-label={copy.nav.switchLanguage}
          aria-pressed={locale === "zh-TW"}
        >
          <span className={locale === "en" ? "is-active" : ""} aria-hidden="true">EN</span>
          <span className={locale === "zh-TW" ? "is-active" : ""} aria-hidden="true">繁中</span>
        </button>
        <a className="nav-repository" href={repositoryUrl} target="_blank" rel="noreferrer">
          GitHub <Arrow />
        </a>
      </nav>

      <section className="hero hero-cinematic" id="top">
        <div className="hero-message">
          <h1>
            {copy.hero.titleBefore}{locale === "en" ? " " : ""}{copy.hero.titleAfter}
          </h1>
          <p className="hero-summary">{copy.hero.summary}</p>
          <div className="hero-actions">
            <a className="button button-amber" href="#gates">
              {copy.hero.explore} <Arrow />
            </a>
            <a
              className="button button-ghost"
              href={repositoryUrl}
              target="_blank"
              rel="noreferrer"
            >
              {copy.hero.repository} <Arrow />
            </a>
          </div>
        </div>
        <div className="hero-art">
          <div className="hero-scene hero-stage" role="img" aria-label={copy.hero.scene}>
            <img className="stage-floor" src={stageFloorLayer} alt="" aria-hidden="true" />
            <img className="motion-coaster" src={coasterLayer} alt="" aria-hidden="true" />
            <div className="motion-frame motion-cat motion-cat-sprite" aria-hidden="true">
              <picture className="motion-cat-strip">
                <source type="image/avif" srcSet={catSpriteAvif} />
                <source type="image/webp" srcSet={catSpriteWebp} />
                <img src={catSpriteWebp} width="2661" height="887" alt="" />
              </picture>
            </div>
            <Cup
              className="motion-frame motion-cup motion-upright-cup motion-rest-cup"
              avifSources={cupSources.upright.avif}
              webpSources={cupSources.upright.webp}
              fallback={cupSources.upright.fallback}
            />
            <Cup
              className="motion-frame motion-cup motion-fake-cup"
              avifSources={cupSources.tipped.avif}
              webpSources={cupSources.tipped.webp}
              fallback={cupSources.tipped.fallback}
            />
            <span className="sprite-label sprite-label--pass" aria-hidden="true">PASS</span>
            <span className="sprite-label sprite-label--fake" aria-hidden="true">FAKE</span>
          </div>
        </div>
      </section>

      <div className="marquee" aria-label={copy.marqueeLabel}>
        <div className="marquee-track">
          {[...copy.marqueeItems, ...copy.marqueeItems].map((item, index) => (
            <span className="marquee-item" key={`${item}-${index}`}>{item}<i aria-hidden="true" /></span>
          ))}
        </div>
      </div>

      <section className="manifesto content-width">
        <p className="manifesto-copy">
          {copy.manifesto.map((phrase, index) => (
            <span className="reveal-word" key={index}>
              {phrase}{locale === "en" ? " " : ""}
            </span>
          ))}
        </p>
      </section>

      <section className="gates content-width" id="gates">
        <div className="section-heading">
          <p className="kicker">{copy.gates.kicker}</p>
          <h2>{copy.gates.heading}</h2>
          <p className="section-summary">{copy.gates.summary}</p>
        </div>

        <div className="gates-grid">
          {copy.gates.cards.map((card) => (
            card.className === "integrity" ? (
              <article className="gate-card integrity" key={card.className}>
                <div className="integrity-copy">
                  <div className="card-topline"><span>{card.label}</span><span>{card.detailLabel}</span></div>
                  <h3>{card.title}</h3>
                </div>
                <p>{card.body}</p>
                <div className="integrity-seal" aria-hidden="true"><span>PASS</span></div>
              </article>
            ) : (
              <article className={`gate-card ${card.className}`} key={card.className}>
                <div className="card-topline"><span>{card.label}</span><span>{card.detailLabel}</span></div>
                <div>
                  <h3>{card.title}</h3>
                  <p>{card.body}</p>
                </div>
                {card.className === "hidden-retest" && (
                  <div className="card-line-art" aria-hidden="true"><span /><span /><span /></div>
                )}
                {card.className === "mutation" && (
                  <div className="mutation-grid" aria-hidden="true"><b /><b /><b /><b /><b /><b /><b /><b /><b /></div>
                )}
                {card.className === "expected" && <div className="expected-underline" aria-hidden="true" />}
              </article>
            )
          ))}
        </div>
      </section>

      <section className="scenario-section content-width" id="contract">
        <div className="scenario-copy">
          <h2>{copy.scenario.heading}</h2>
          <p>{copy.scenario.body}</p>
          <div className="contract-chain" aria-label={copy.scenario.chainLabel}>
            {copy.scenario.chain.map((item, index) => (
              <span className="contract-chain-item" key={item}>
                <span>{item}</span>
                {index < copy.scenario.chain.length - 1 && <span aria-hidden="true">→</span>}
              </span>
            ))}
          </div>
        </div>
        <pre className="scenario-code" data-code-label={copy.scenario.codeLabel} aria-label={copy.scenario.codeAriaLabel}><code>{scenarioCode}</code></pre>
      </section>

      <section className="accordion-section content-width" id="reports">
        <div className="section-heading compact-heading">
          <h2>{copy.views.heading}</h2>
          <p className="section-summary">{copy.views.summary}</p>
        </div>
        <div className="horizontal-accordion">
          {copy.views.items.map((item, index) => (
            <button
              className={`accordion-panel ${item.className} ${activeAccordion === index ? "is-active" : ""}`}
              key={item.className}
              onClick={() => { void changeAccordion(index); }}
              aria-pressed={activeAccordion === index}
            >
              <span className="accordion-content">
                <span className="accordion-title">{item.title}</span>
                <span className="accordion-detail" aria-hidden={activeAccordion !== index}>{item.detail}</span>
              </span>
              <Arrow />
            </button>
          ))}
        </div>
      </section>

      <section className="proof" id="proof">
        <div className="proof-layout content-width">
          <div className="proof-intro">
            <p className="kicker">{copy.proof.kicker}</p>
            <h2>{copy.proof.heading}</h2>
            <p>{copy.proof.body}</p>
            <a className="text-link" href={verificationGuideUrl} target="_blank" rel="noreferrer">{copy.proof.guide} <Arrow /></a>
          </div>
          <div className="proof-steps">
            {copy.proof.steps.map((step) => (
              <article className="proof-step" key={step.label}>
                <span className="step-label">{step.label}</span>
                <p className="command">./gradlew {step.command}</p>
                <h3>{step.title}</h3>
                <p>{step.body}</p>
              </article>
            ))}
            <div className="verdict-card">
              <span>{copy.proof.verdictLabel}</span>
              <strong>PASS</strong>
              <p>{copy.proof.verdictBody}</p>
            </div>
          </div>
        </div>
      </section>

      <section className="install content-width" id="install">
        <div className="install-headline">
          <h2>{copy.install.heading}</h2>
          <p className="section-summary">{copy.install.summary}</p>
        </div>
        <div className="install-panel">
          <div>
            <p className="install-note">{copy.install.note}</p>
            <code>{pluginLine}</code>
          </div>
          <button className="copy-button" onClick={copyInstall}>
            {copied ? copy.install.copied : copy.install.copy} <Arrow />
          </button>
        </div>
        <div className="install-actions">
          <a className="button button-amber" href={`${repositoryUrl}/tree/main/samples/junit-cart-orders`} target="_blank" rel="noreferrer">{copy.install.sample} <Arrow /></a>
          <a className="button button-dark" href={`${repositoryUrl}#install-0011`} target="_blank" rel="noreferrer">{copy.install.install} <Arrow /></a>
          <a className="acceptance-skill-link" href={`${repositoryUrl}/tree/main/.agents/skills/topplecat-acceptance`} target="_blank" rel="noreferrer">
            {copy.install.skill} <Arrow />
          </a>
        </div>
      </section>

      <footer className="site-footer">
        <a className="wordmark footer-wordmark" href="#top" aria-label={copy.nav.home}><span className="wordmark-mark" aria-hidden="true">T</span><span>ToppleCat</span></a>
        <p>{copy.footer.tagline}</p>
        <div className="footer-links">
          <a href={`${repositoryUrl}/blob/main/${copy.footer.readmePath}`} target="_blank" rel="noreferrer">{copy.footer.readme}</a>
          <a href={`${repositoryUrl}/blob/main/LICENSE`} target="_blank" rel="noreferrer">Apache-2.0</a>
          <a href={repositoryUrl} target="_blank" rel="noreferrer">GitHub <Arrow /></a>
        </div>
      </footer>
    </main>
  );
}

export default App;
