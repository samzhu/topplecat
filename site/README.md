# ToppleCat project page

This Vite/React project builds the public project page for GitHub Pages.

```bash
cd site
npm install
npm run dev
npm run build
```

The deployment workflow uploads `site/dist` as a GitHub Pages artifact. It does
not create or maintain a `gh-pages` branch.

## Get started content

The Get started section introduces ToppleCat as a verification step inside an
existing agent workflow, not a plugin that decides what to test. It may link to
external Spec or skill projects as optional references, but must state that the
external workflow owns the Spec, plan, and task state. The project-local
`topplecat-acceptance` skill binds human-selected ACs to Java/JUnit acceptance
work; ToppleCat's Reviewer sequence happens after that contract is prepared.

## Public safeguard demonstrations

The page includes five clearly labelled, synthetic checkout demonstrations for
human visitors. Each card uses a responsive image excerpt from its matching
synthetic Verification Report and opens the same evidence in a native dialog
with bounded explanation layers. A fully synthetic story may show the complete
report details needed to demonstrate how ToppleCat found the problem, including
synthetic reviewer cases, values, counterexamples, replay material, paths, and
producer diagnostics. Never use material from an actual delivery. Keep every
story separate from Reviewer HTML, Current-run Evidence, safe agent feedback,
private diagnostics, and organizational approval. Verify the rendered
production page after content, interaction, responsive, accessibility, or
language changes; the demonstrations are a curated explanation surface, not a
second Verification Report or an Implementation Agent handoff.

## Hero animation

Read [`ANIMATION.md`](ANIMATION.md) before changing the hero artwork, sprite,
layer positions, or GSAP timeline. It records the three-state contract and the
visual checks required to keep the cat, cup, coaster, and verdict synchronized.

## Assets

Production artwork is grouped by role under `src/assets`. See
[`src/assets/README.md`](src/assets/README.md) for the inventory, naming rules,
and sprite alignment contract. Generated drafts and discarded iterations do not
belong in the production asset tree.

## Social sharing

The initial HTML head provides Open Graph and X Card metadata for crawlers that
do not execute React. Its card image lives at
`public/social/topplecat-social-card-v1.jpg`, where Vite preserves the stable
public URL. Publish a new versioned filename for a card update so Meta and
LinkedIn do not retain a cached previous image.

## Search discovery

The canonical English homepage is the only URL in `public/sitemap.xml`; the
language query changes the client-side interface and is not an independently
indexed page. `robots.txt` advertises that sitemap. The static HTML head holds
the canonical URL, social metadata, and JSON-LD that describes the visible
ToppleCat website and Java source project. Keep that metadata aligned with the
visible English homepage; do not add AI-only files, hidden text, or FAQ markup
for content that is not displayed.
