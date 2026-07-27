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

## Assets

Production artwork is grouped by role under `src/assets`. See
[`src/assets/README.md`](src/assets/README.md) for the inventory, naming rules,
and sprite alignment contract. Generated drafts and discarded iterations do not
belong in the production asset tree.
