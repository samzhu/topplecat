# Hero animation maintenance

The hero is a deliberately simple three-frame, stop-motion scene. Treat the
cat pose, cup state, verdict label, and timing as one animation contract. Do not
adjust one of them without checking the complete scene.

The visual reference is
[`../docs/images/topplecat-readme-hero.png`](../docs/images/topplecat-readme-hero.png).

## State contract

Only these three visible states are allowed:

| State | Cat sprite | Upright cup | Tipped cup | PASS | FAKE | Meaning |
| --- | --- | --- | --- | --- | --- | --- |
| Rest | `0% 0%` | visible | hidden | visible | hidden | The claim appears to pass. |
| Contact | `50% 0%` | visible | hidden | visible | hidden | The paw reaches the rim; nothing has moved yet. |
| Verdict | `100% 0%` | hidden | visible | hidden | visible | The cup is side-tipped and the claim is exposed as fake. |

Every change belonging to a state must happen at the same GSAP label with
`.set()`. Do not fade or tween the cat, cups, or labels between states. In
particular, do not introduce a fourth state containing a fully extended paw and
an upright cup; that pause makes the nudge feel late and disconnected.

The current timing is:

| Segment | Duration |
| --- | --- |
| Initial delay | `0.95s` |
| Rest hold | `1.2s` |
| Contact hold | `0.32s` |
| Verdict hold | `1.65s` |
| Repeat delay | `0.75s` |

The contact frame should be brief but readable. It is the anticipation frame,
not a second resting pose.

## Asset contract

Production assets and their roles are documented in
[`src/assets/README.md`](src/assets/README.md). The animation additionally
depends on these exact layout properties:

- `original/characters/cat-action-sprite.org.png` is `2661 × 887`: three equal
  `887 × 887` frames on one horizontal strip. Its production derivatives are
  `characters/cat-action-sprite.avif` and `.webp`; neither format may crop,
  resize, or alter the sprite grid.
- CSS uses `background-size: 300% 100%` and the positions `0%`, `50%`, and
  `100%`. Do not substitute approximate percentages.
- The grounded paw occupies the same pixel coordinate in all three cat frames.
  Preserve the transparent padding. Never auto-trim the frames independently.
- `original/props/cup-upright.org.png` and
  `original/props/cup-tipped.org.png` are both `1254 × 1254`. Their responsive
  production AVIF/WebP derivatives must retain transparent edges and visual
  scale so the cup does not shrink when it tips.
- The coaster, tabletop, cup states, labels, cat, and background remain
  independent layers. Do not bake them back into one composite image.
- `original/backgrounds/argyle-tile.org.png` is a seamless `976 × 872` tile.
  Its production AVIF/WebP derivatives must match exactly on the left/right and
  top/bottom edges.
- Export transparent artwork with a real alpha channel. Check it on both light
  and dark backgrounds before using it.

When replacing the cat sprite, compose all three poses on the same frame grid,
align them to the grounded paw, and export the complete strip once. This avoids
per-image decoding flashes and small coordinate differences.

## Edge and proportion contract

Matching canvas dimensions are necessary but not sufficient. Transparent
padding can hide a change in the visible object's size, so compare the opaque
silhouette rather than relying only on the PNG width and height.

Keep these proportions stable:

- The cat's head, torso, grounded legs, and outline thickness remain the same
  apparent size in all three frames. A pose may extend farther, but the cat must
  not grow, shrink, or become wider as a whole.
- After mentally undoing the rotation, the tipped cup's rim, body, and handle
  match the upright cup's perceived scale.
- The coaster remains slightly wider than the cup base. In Rest and Contact,
  the cup is visually supported by it rather than embedded in or floating above
  it.
- The reaching paw meets the outside edge of the rim. It must not stop short,
  pass through the cup, or cover so much of the rim that the layer order looks
  wrong.
- Resize related states together. Never fix only the upright cup, tipped cup,
  or one cat frame at a breakpoint.
- Scale the complete `.hero-art` for responsive layouts. Do not independently
  scale scene objects on mobile unless the entire composition is recalibrated.

Every visible object edge must also remain intact:

- Do not crop ears, whiskers, toes, paw outlines, cup handles, cup rims, spill
  droplets, coaster edges, or the tabletop boundary.
- Reject white or dark alpha halos, rectangular matte edges, stray pixels,
  duplicated outlines, and abrupt cuts in antialiased curves.
- Do not use `clip-path`, masking, or another layer to conceal damaged source
  artwork. Repair or replace the asset instead.
- Check object intersections at high zoom: paw over cup, cup over coaster, and
  cat over tabletop. Occlusion should follow the documented `z-index` order.
- Inspect transparent assets against the actual green background and a
  temporary contrasting background. A defect can disappear against only one
  colour.
- Check once at native/desktop scale, once around `200%` zoom, and once at the
  mobile rendered size. Thin halos and clipped pixels often appear only after
  resampling.

## Layer and positioning contract

The layer order is intentional:

| Layer | `z-index` | Rule |
| --- | ---: | --- |
| Tabletop | 1 | Structural floor; never follows the cup. |
| Coaster | 2 | Stationary through the entire loop. |
| Cup state | 4 | Upright and tipped images swap atomically. |
| Cat sprite | 5 | The paw stays in front of the cup. |
| PASS / FAKE | 6 | Separate HTML text above the artwork. |

All scene coordinates are relative to the same `16 / 9` `.hero-stage`. Use the
custom properties on `.hero-stage` for placement. Do not animate the parent
scene's `left`, `bottom`, `scale`, or `transform`; doing so makes the cat and
table appear to float.

These visual relationships must remain true:

- In Rest, the cup sits centered on the coaster.
- In Contact, the paw just touches the cup rim; the cup and coaster do not move.
- In Verdict, the cup moves slightly away from the cat and tips sideways. It
  does not flip toward the viewer.
- The tipped cup keeps the same perceived size as the upright cup.
- The coaster remains in its original position after the cup leaves it.
- The cat's grounded paw is the positional anchor across all three frames.

Text shadows must use `text-shadow`. A `box-shadow` on `.sprite-label` draws the
rectangular text box and creates a false line below PASS or FAKE.

## Responsive and accessibility rules

- Use percentage or container-relative sizing inside the hero. Avoid
  viewport-specific pixel offsets for individual animation layers.
- Check desktop and mobile after every asset or coordinate change. A desktop
  correction can easily separate the paw and cup on mobile.
- The page must not gain horizontal overflow at `390px` width.
- `prefers-reduced-motion: reduce` intentionally shows the final Verdict state.
  When a class name or layer changes, update the reduced-motion selectors too.
- The scene's accessible description must still describe PASS, the reach, and
  the FAKE verdict even though the decorative layers themselves are hidden from
  assistive technology.

## Common regressions

| Symptom | Likely cause |
| --- | --- |
| Cat drifts between frames | Frames were cropped separately or do not share the grounded-paw anchor. |
| Cat appears to have an extra paw | A second arm or cat layer was placed over a complete cat frame. |
| Cup changes size when tipped | Cup states use different canvas dimensions or unrelated CSS heights. |
| Cup changes size despite equal canvases | The visible alpha bounds differ even though the PNG dimensions match. |
| Cup looks embedded in the coaster | Incorrect layer order or mismatched vertical alignment. |
| Coaster moves with the cup | Coaster was grouped with or animated alongside the cup. |
| Paw cuts through or misses the rim | The visible silhouettes were aligned by canvas bounds instead of object edges. |
| Object has a white/dark halo | Alpha was exported against a matte colour or resampled poorly. |
| Whiskers, handle, or spill are cut off | Transparent padding was trimmed or the object was clipped. |
| A rectangular line appears under PASS / FAKE | `box-shadow` was used instead of `text-shadow`. |
| Image flashes between poses | Separate images are loading or fading instead of using the sprite strip. |
| The nudge feels late | An extra fully-reached/upright-cup state was introduced. |
| A black or torn rectangle appears | The source image has invalid transparency or was clipped. |
| Background seams are visible | The replacement tile does not wrap exactly at its edges. |

## Required verification

After any hero animation, asset, or layout change:

1. Run `cd site && npm run build`.
2. Watch at least two complete loops at a desktop viewport.
3. Capture or pause on all three states and compare the complete state table,
   not just the layer being edited.
4. Repeat at approximately `390 × 844`.
5. Confirm the paw touches the cup only in Contact.
6. Confirm there are exactly three state combinations and no partially mixed
   frame.
7. Confirm the coaster and tabletop never move.
8. Compare visible object proportions across frames; do not compare only the
   asset canvas dimensions.
9. Inspect silhouettes and intersections at normal size, around `200%` zoom,
   and at mobile size for halos, clipping, stray pixels, or broken occlusion.
10. Confirm PASS and FAKE have no rectangular underline or box.
11. Check for horizontal overflow and browser console warnings or errors.

Do not approve an animation change from a single still image. The transition
order and the full loop are part of the design.
