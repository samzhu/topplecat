# Review and report design

Generated pages are projections of the executable contract. For fields allowed
for their audience, they preserve AC identities, Step sentences, cases,
expected values, Property declarations, and execution results without adding
interpretation.

## Know the audiences

| Artifact | Audience | Content |
| --- | --- | --- |
| Contract Review | Reviewer | Selected Spec context, public and reviewer rows, Step sentences, Properties, acceptance source, and non-blocking quality advisories. |
| Public Spec | Public | Safe projection of the public executable contract. |
| Verification Evidence | Reviewer | Current execution, private failures, Property classifications, and counterexamples. |
| Current-run Evidence | Reviewer / CI | Machine aggregate verdict and independent gate results. |
| Safe agent feedback | Implementation agent | Gate-level remediation without private answers. |

Confine reviewer case IDs, values, source paths, raw failures, Property trial
values, counterexamples, and replay material to reviewer-only artifacts. Public
Spec contains only the safe public contract projection; safe agent feedback
contains gate-level remediation only.

Build the implementation handoff from an export containing public source,
public rows, Properties, and selected Spec context only. Keep reviewer source,
build output, local custody state, and Git history containing reviewer material
inside the reviewer boundary.

Quality advisories are reviewer prompts, not inferred requirements. They show
only their rule code, AC, expected path, and public/reviewer counts. Keep them
out of the executable contract, Seal, Verification Evidence, Public Spec, and
safe feedback.

External workflow automation or humans create and inspect these artifacts. The
acceptance-authoring skill prepares readable source material; it does not treat
a generated page as another authoring surface or accept a completion claim.
