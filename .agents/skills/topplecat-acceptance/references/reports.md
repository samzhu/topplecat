# Review and report design

## Machine handoff contract

The `commands` field is a user-facing execution handoff, never a log of files
read, shell inspection, or repository discovery. A selected delivery carries
the same explicit Spec paths through Check, Review, and scoped Verify; a
whole-contract handoff carries Check, Seal, and Verify without selection and
does not claim Review readiness. A failed selected handoff has no commands,
scope, narratives, or public/reviewer material. Preserve authored narrative
step text exactly; do not paraphrase it. Successful selected and whole-contract
handoffs leave `failureRouting.owner` and `failureRouting.action` empty; only
failed selected routing uses those fields.

Selected command shape:

```text
./gradlew toppleCatCheck --spec <canonical-path> [--spec <canonical-path>...]
./gradlew toppleCatReview --spec <canonical-path> [--spec <canonical-path>...]
./gradlew toppleCatVerify --spec <canonical-path> [--spec <canonical-path>...]
```

Whole-contract command shape:

```text
./gradlew toppleCatCheck
./gradlew toppleCatSeal
./gradlew toppleCatVerify
```

Generated pages are projections of the executable contract. For fields allowed
for their audience, they preserve AC identities, Step sentences, cases,
expected values, Property declarations, and execution results without adding
interpretation.

## Know the audiences

| Artifact | Audience | Content |
| --- | --- | --- |
| Spec Review | Reviewer | Complete selected Spec documents, public and reviewer rows, Step sentences, Properties, acceptance source, and non-blocking quality advisories. |
| Verification Report | Reviewer | Current execution, private failures, Property classifications, counterexamples, and mutation diagnostics. |
| Current-run Evidence | Reviewer / External Workflow | Machine aggregate verdict and independent Gate results. |
| Safe agent feedback | Implementation Agent | Gate-level remediation without private answers. |

Confine reviewer case IDs, values, source paths, raw failures, Property trial
values, counterexamples, and replay material to reviewer-only artifacts. Safe
agent feedback contains gate-level remediation only.

Build the implementation handoff from an export containing public source,
public rows, Properties, and selected Spec context only. Keep reviewer source,
build output, local custody state, and Git history containing reviewer material
inside the reviewer boundary.

Quality advisories are reviewer prompts, not inferred requirements. They show
only their rule code, AC, expected path, and public/reviewer counts. Keep them
out of the executable contract, Seal, Verification Report, and safe feedback.

External Workflow may execute tasks and consume Current-run Evidence. The
Reviewer reads both HTML reports and decides whether to accept the delivery;
the Implementation Agent receives only public material and safe agent feedback.
The acceptance-authoring skill prepares readable source material and never
treats a generated page as another authoring surface.

If a human selected a Spec but selection or canonical-content validation fails
(including a supplied relative path whose canonical `.md` file is missing),
the handoff stops before command preparation. It must not be rerouted as
whole-contract maintenance and must not put diagnostics in either public or
reviewer custody handoff. Report only the failure owner and smallest repair
action in the routing result; leave selected scope, narratives, commands, and
both handoffs empty.

The same empty-field rule applies to every failed selected handoff: do not
retain the attempted path or AC IDs in a scope field, and do not include a
local filesystem path or wrapper-link destination in the routing message.
Absolute paths are machine-specific input and must be rejected rather than
normalized. Authored Gherkin-style Given/When/Then/And/But narratives are preserved verbatim, including
their step keywords and group order, whenever a successful handoff reports
them.

Reviewer HTML uses English by default and supports invocation-only
`--language en` or `--language zh-TW` on Review, Seal, Reseal, and Verify.
This localizes ToppleCat-owned report presentation and HTML accessibility
metadata only. It does not translate authored contract prose, rename external
producer outcomes, enter Current-run Evidence, or widen the Implementation
Agent information boundary.
