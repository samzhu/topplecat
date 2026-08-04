# Website assets

Production assets imported by the project page belong in this directory. Vite
fingerprints imported assets during the build, so filenames describe purpose
rather than carrying manual version suffixes.

## Structure

| Directory | Contents |
| --- | --- |
| `backgrounds/` | Seamless, repeatable scene backgrounds |
| `characters/` | Character artwork and animation sprite sheets |
| `props/` | Independently positioned cups, coaster, and other objects |
| `scene/` | Structural scene layers such as the tabletop |
| `original/` | Preserved source PNGs; never import these or place them in `public/`. |

## Current assets

| Asset | Purpose |
| --- | --- |
| `backgrounds/argyle-tile.avif` / `.webp` | Seamless green argyle hero background |
| `characters/cat-action-sprite.avif` / `.webp` | Three equally sized cat animation frames |
| `props/coaster.svg` | Stationary coaster layer |
| `props/cup-upright-{320,640,960}.avif` / `.webp` | Upright cup used by the PASS frames |
| `props/cup-tipped-{320,640,960}.avif` / `.webp` | Side-tipped cup and spill used by the FAKE frame |
| `scene/tabletop.svg` | Full-width wooden tabletop layer |
| `demonstrations/*-{640,1280}.jpg` | Responsive excerpts from the six clearly labelled, synthetic Verification Report demonstrations for human visitors |

The preserved sources are intentionally versioned below `original/`:

| Source | SHA-256 | Dimensions |
| --- | --- | --- |
| `original/backgrounds/argyle-tile.org.png` | `65f75f05744b20bc67e280ed975b40579664eacd12b9ac2295d81624e7b70c7b` | 976 × 872 |
| `original/characters/cat-action-sprite.org.png` | `20f36ddb63c00d8fb43aefdb9322f65fc4646f803a56483e3d93c2edb5a7f1b5` | 2661 × 887 |
| `original/props/cup-upright.org.png` | `1d207b60a9d5ec74707118d2913ff24daa1a280ba497720d4245aa1f1c8b531b` | 1254 × 1254 |
| `original/props/cup-tipped.org.png` | `d41d4323156fb9ca0c31d9cd5e8032a1afc7dd4c8a2f0710dcf802e88c6d79ce` | 1254 × 1254 |

Run `npm run optimize:assets` after an approved source-art change. The command
regenerates AVIF and WebP derivatives without modifying the `.org.png` files;
`npm run verify:assets` validates their dimensions and alpha-channel presence.

The three character frames must remain aligned to the same grounded paw anchor.
Keep transparent padding intact when replacing a character or prop, because the
hero composition relies on those shared coordinates.

Equal canvas dimensions do not guarantee equal visual proportions. Compare the
visible alpha bounds and keep silhouette scale, outline thickness, and
antialiased edges consistent between states. Reject cropped details, matte
halos, stray pixels, and assets whose apparent size changes when swapped. See
[`../../ANIMATION.md`](../../ANIMATION.md) for the complete edge and proportion
contract.

Discarded iterations and editor metadata should not be added below
`src/assets`. The original visual reference remains
`docs/images/topplecat-readme-hero.png`.

The demonstration excerpts are curated synthetic assets, not exports from an
actual delivery. When replacing one, use a story whose synthetic report details
help a human understand the safeguard, produce both 640px and 1280px variants,
and keep the `Reproducible demonstration` label and bounded explanation. Do
not add actual Reviewer HTML, Current-run Evidence, or private diagnostics from
an actual delivery.
