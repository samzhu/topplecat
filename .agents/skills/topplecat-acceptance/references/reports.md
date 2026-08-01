# Review and report design

Generated pages are projections of the executable contract. For fields allowed
for their audience, they preserve AC identities, Step sentences, cases,
expected values, Property declarations, and execution results without adding
interpretation.

## Know the audiences

| Artifact | Audience | Content |
| --- | --- | --- |
| Spec Review | Reviewer | Complete selected Spec documents, public and reviewer rows, Step sentences, Properties, acceptance source, and non-blocking quality advisories. |
| Verification Report | Reviewer | Current execution, private failures, Property classifications, counterexamples, and mutation diagnostics. |
| Current-run Evidence | Reviewer / CI | Machine aggregate verdict and independent gate results. |
| Safe agent feedback | Implementation agent | Gate-level remediation without private answers. |

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

External workflow automation or humans create and inspect these artifacts. The
acceptance-authoring skill prepares readable source material; it does not treat
a generated page as another authoring surface or accept a completion claim.
