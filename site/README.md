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

## Hero animation

Read [`ANIMATION.md`](ANIMATION.md) before changing the hero artwork, sprite,
layer positions, or GSAP timeline. It records the three-state contract and the
visual checks required to keep the cat, cup, coaster, and verdict synchronized.

## Assets

Production artwork is grouped by role under `src/assets`. See
[`src/assets/README.md`](src/assets/README.md) for the inventory, naming rules,
and sprite alignment contract. Generated drafts and discarded iterations do not
belong in the production asset tree.
