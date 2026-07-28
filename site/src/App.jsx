import { useEffect, useRef, useState } from "react";
import { flushSync } from "react-dom";
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

const accordionItems = [
  {
    title: "Public contract",
    detail:
      "Ordinary Java acceptance tests and typed JSON or YAML rows state the behaviour in executable form.",
    className: "contract",
  },
  {
    title: "Reviewer boundary",
    detail:
      "Independent cases stay with the reviewer, then return only for the final verification run.",
    className: "boundary",
  },
  {
    title: "Current evidence",
    detail:
      "A claim passes only when the current run records a complete aggregate verdict, never a stale green check.",
    className: "evidence",
  },
];

const verificationSteps = [
  {
    command: "toppleCatCheck",
    title: "Inspect the contract before it leaves review",
    body: "Validates case data, Java bindings, and the canonical Stage DSL without running tests or generating a new source of truth.",
  },
  {
    command: "toppleCatHide",
    title: "Hand implementation a public-only tree",
    body: "Moves reviewer source into local custody and seals the reviewed public contract plus verification policy.",
  },
  {
    command: "toppleCatVerify",
    title: "Make the done claim earn its footing",
    body: "Restores reviewer checks only in the reviewer boundary, runs enabled gates, writes evidence, then re-hides the source.",
  },
];

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
  const motion = useRef({ Flip: null });
  const [activeAccordion, setActiveAccordion] = useState(0);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let context;
    const startMotion = async () => {
      const [{ gsap }, { Flip }, { ScrollTrigger }] = await Promise.all([
        import("gsap"),
        import("gsap/Flip"),
        import("gsap/ScrollTrigger"),
      ]);
      if (cancelled) return;

      gsap.registerPlugin(Flip, ScrollTrigger);
      motion.current = { Flip };
      context = gsap.context(() => {
      const media = gsap.matchMedia();
      const q = gsap.utils.selector(scope);

      media.add("(prefers-reduced-motion: no-preference)", () => {
        // Keep the cat, cup, and verdict as three atomic states.
        // See site/ANIMATION.md before changing this timeline.
        const catTimeline = gsap.timeline({
          delay: 0.95,
          repeat: -1,
          repeatDelay: 0.75,
        });

        catTimeline
          .set(q(".motion-cat-sprite"), {
            autoAlpha: 1,
            backgroundPosition: "0% 0%",
          })
          .set(q(".motion-rest-cup"), { autoAlpha: 1 })
          .set(q(".motion-fake-cup"), { autoAlpha: 0 })
          .set(q(".sprite-label--pass"), { autoAlpha: 1 })
          .set(q(".sprite-label--fake"), { autoAlpha: 0 })
          .to({}, { duration: 1.2 }, 0)
          .addLabel("frameContact", 1.2)
          .set(q(".motion-cat-sprite"), {
            backgroundPosition: "50% 0%",
          }, "frameContact")
          .to({}, { duration: 0.32 }, 1.2)
          .addLabel("frameFake", 1.52)
          .set(q(".motion-cat-sprite"), {
            backgroundPosition: "100% 0%",
          }, "frameFake")
          .set(q(".motion-rest-cup"), { autoAlpha: 0 }, "frameFake")
          .set(q(".motion-fake-cup"), { autoAlpha: 1 }, "frameFake")
          .set(q(".sprite-label--pass"), { autoAlpha: 0 }, "frameFake")
          .set(q(".sprite-label--fake"), { autoAlpha: 1 }, "frameFake")
          .to({}, { duration: 1.65 }, 1.52);

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

        let heroVisible = true;
        let documentVisible = document.visibilityState === "visible";
        const syncPlayback = () => {
          if (heroVisible && documentVisible) {
            catTimeline.resume();
          } else {
            catTimeline.pause();
          }
        };
        const observer = new IntersectionObserver(
          ([entry]) => {
            heroVisible = entry.isIntersecting;
            syncPlayback();
          },
          { threshold: 0.15 },
        );
        const onVisibilityChange = () => {
          documentVisible = document.visibilityState === "visible";
          syncPlayback();
        };
        const hero = q(".hero-cinematic")[0];
        if (hero) observer.observe(hero);
        document.addEventListener("visibilitychange", onVisibilityChange);

        return () => {
          observer.disconnect();
          document.removeEventListener("visibilitychange", onVisibilityChange);
          catTimeline.kill();
        };
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
    };

    const timer = window.setTimeout(() => {
      void startMotion();
    }, 0);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      context?.revert();
    };
  }, []);

  const changeAccordion = (index) => {
    if (index === activeAccordion) return;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) {
      setActiveAccordion(index);
      return;
    }

    const { Flip } = motion.current;
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
      await navigator.clipboard.writeText('id("io.github.samzhu.topplecat") version "0.0.5"');
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
    }
  };

  return (
    <main className="page-shell" ref={scope}>
      <div className="page-background-layer" aria-hidden="true" />
      <div className="ambient-orb orb-one" />
      <div className="ambient-orb orb-two" />

      <nav className="site-nav" aria-label="Primary navigation">
        <a className="wordmark" href="#top" aria-label="ToppleCat home">
          <span className="wordmark-mark" aria-hidden="true">T</span>
          <span>ToppleCat</span>
        </a>
        <div className="nav-links">
          <a href="#gates">The gates</a>
          <a href="#proof">The proof</a>
          <a href="#install">Get started</a>
        </div>
        <a className="nav-repository" href={repositoryUrl} target="_blank" rel="noreferrer">
          GitHub <Arrow />
        </a>
      </nav>

      <section className="hero hero-cinematic" id="top">
        <h1 className="visually-hidden">ToppleCat turns a shaky done claim into evidence.</h1>
        <div className="hero-art">
          <div className="hero-scene hero-stage" role="img" aria-label="A ToppleCat watches a PASS label on a coffee mug, reaches for it, then tips the mug so the label becomes FAKE.">
            <img className="stage-floor" src={stageFloorLayer} alt="" aria-hidden="true" />
            <img className="motion-coaster" src={coasterLayer} alt="" aria-hidden="true" />
            <div className="motion-frame motion-cat motion-cat-sprite" aria-hidden="true" />
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

      <div className="marquee" aria-label="ToppleCat capabilities">
        <div className="marquee-track">
          <span>Hidden retests</span><i />
          <span>Mutation gates</span><i />
          <span>Expected values asserted</span><i />
          <span>Contract integrity</span><i />
          <span>Run-scoped evidence</span><i />
          <span>Hidden retests</span><i />
          <span>Mutation gates</span><i />
          <span>Expected values asserted</span><i />
          <span>Contract integrity</span><i />
          <span>Run-scoped evidence</span><i />
        </div>
      </div>

      <section className="manifesto content-width">
        <p className="manifesto-copy">
          {"A final green check is a report, not a verdict. ToppleCat makes an agent’s completion claim pass through the same constraints that define the work. If the implementation is thin, hard-coded, or only tuned to the visible example, the cat gives it a nudge.".split(" ").map((word, index) => (
            <span className="reveal-word" key={`${word}-${index}`}>{word} </span>
          ))}
        </p>
      </section>

      <section className="gates content-width" id="gates">
        <div className="section-heading">
          <p className="kicker">Five checks. One current verdict.</p>
          <h2>Proof has more than one way to catch a shortcut.</h2>
        </div>

        <div className="gates-grid">
          <article className="gate-card hidden-retest">
            <div className="card-topline"><span>01</span><span>Reviewer-controlled</span></div>
            <div>
              <h3>Hidden retests expose code written for the example, not the rule.</h3>
              <p>Independent business cases return only inside the reviewer verification boundary.</p>
            </div>
            <div className="card-line-art" aria-hidden="true"><span /><span /><span /></div>
          </article>

          <article className="gate-card mutation">
            <div className="card-topline"><span>02</span><span>PIT-backed</span></div>
            <h3>Mutate the behaviour.</h3>
            <p>Public acceptance tests must notice what changed.</p>
            <div className="mutation-grid" aria-hidden="true"><b /><b /><b /><b /><b /><b /><b /><b /><b /></div>
          </article>

          <article className="gate-card expected">
            <div className="card-topline"><span>03</span><span>Non-negotiable</span></div>
            <h3>Expected means asserted.</h3>
            <p>Reading a value does not count. Every declared result must be compared with reality.</p>
            <div className="expected-underline" aria-hidden="true" />
          </article>

          <article className="gate-card integrity">
            <div className="integrity-copy">
              <div className="card-topline"><span>04</span><span>Sealed before handoff</span></div>
              <h3>Contract integrity keeps the rules from moving after review.</h3>
            </div>
            <p>Tests, case data, build logic, semantic definition, and policy must still match reviewer approval before the other gates may run.</p>
            <div className="integrity-seal" aria-hidden="true"><span>PASS</span></div>
          </article>
        </div>
      </section>

      <section className="accordion-section content-width">
        <div className="section-heading compact-heading">
          <p className="kicker">Keep the truth in the right hands</p>
          <h2>Java stays authoritative. Evidence stays honest.</h2>
        </div>
        <div className="horizontal-accordion">
          {accordionItems.map((item, index) => (
            <button
              className={`accordion-panel ${item.className} ${activeAccordion === index ? "is-active" : ""}`}
              key={item.title}
              onClick={() => changeAccordion(index)}
              aria-pressed={activeAccordion === index}
            >
              <span className="accordion-index">0{index + 1}</span>
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
            <p className="kicker">The verification flow</p>
            <h2>Trust the run that can show its work.</h2>
            <p>ToppleCat is deliberately narrow: it verifies Java and JUnit delegation work. It does not replace code review, QA, CI isolation, or a sandbox.</p>
            <a className="text-link" href={`${repositoryUrl}#how-it-fits-the-development-flow`} target="_blank" rel="noreferrer">Read the complete flow <Arrow /></a>
          </div>
          <div className="proof-steps">
            {verificationSteps.map((step, index) => (
              <article className="proof-step" key={step.command}>
                <span className="step-number">0{index + 1}</span>
                <p className="command">./gradlew {step.command}</p>
                <h3>{step.title}</h3>
                <p>{step.body}</p>
              </article>
            ))}
            <div className="verdict-card">
              <span>Aggregate verdict</span>
              <strong>PASS</strong>
              <p>Only every required gate passing in the current run lets the claim stand.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="install content-width" id="install">
        <div className="install-headline">
          <p className="kicker">Built for Java and JUnit</p>
          <h2>Give every confident “done” a place to land.</h2>
        </div>
        <div className="install-panel">
          <div>
            <p className="install-note">Requires Java 25 and a Gradle version that supports it.</p>
            <code>id("io.github.samzhu.topplecat") version "0.0.5"</code>
          </div>
          <button className="copy-button" onClick={copyInstall}>
            {copied ? "Copied" : "Copy plugin line"} <Arrow />
          </button>
        </div>
        <div className="install-actions">
          <a className="button button-amber" href={`${repositoryUrl}#install-005`} target="_blank" rel="noreferrer">Read installation guide <Arrow /></a>
          <a className="button button-dark" href={`${repositoryUrl}/tree/main/samples/junit-cart-orders`} target="_blank" rel="noreferrer">Run the JUnit sample <Arrow /></a>
        </div>
      </section>

      <footer className="site-footer">
        <a className="wordmark footer-wordmark" href="#top"><span className="wordmark-mark" aria-hidden="true">T</span><span>ToppleCat</span></a>
        <p>Evidence for the “done” claim.</p>
        <div className="footer-links">
          <a href={`${repositoryUrl}/blob/main/README.zh-TW.md`} target="_blank" rel="noreferrer">繁體中文</a>
          <a href={`${repositoryUrl}/blob/main/LICENSE`} target="_blank" rel="noreferrer">Apache-2.0</a>
          <a href={repositoryUrl} target="_blank" rel="noreferrer">GitHub <Arrow /></a>
        </div>
      </footer>
    </main>
  );
}

export default App;
