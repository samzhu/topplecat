# Website assets

Only production assets imported by the project page belong in this directory.
Vite fingerprints them during the build, so filenames describe purpose rather
than carrying manual version suffixes.

## Structure

| Directory | Contents |
| --- | --- |
| `backgrounds/` | Seamless, repeatable scene backgrounds |
| `characters/` | Character artwork and animation sprite sheets |
| `props/` | Independently positioned cups, coaster, and other objects |
| `scene/` | Structural scene layers such as the tabletop |

## Current assets

| Asset | Purpose |
| --- | --- |
| `backgrounds/argyle-tile.png` | Seamless green argyle hero background |
| `characters/cat-action-sprite.png` | Three equally sized cat animation frames |
| `props/coaster.svg` | Stationary coaster layer |
| `props/cup-upright.png` | Upright cup used by the PASS frames |
| `props/cup-tipped.png` | Side-tipped cup and spill used by the FAKE frame |
| `scene/tabletop.svg` | Full-width wooden tabletop layer |

The three character frames must remain aligned to the same grounded paw anchor.
Keep transparent padding intact when replacing a character or prop, because the
hero composition relies on those shared coordinates.

Generated source images, discarded iterations, and editor metadata should not
be added below `src/assets`. The original visual reference remains
`docs/images/topplecat-readme-hero.png`.
