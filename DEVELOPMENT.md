# Developing ToppleCat

This is the shared development map for people and AI agents working inside the
repository. Start here to find the source of truth, the relevant design
documents, and the checks required for a change.

Read [`CONTEXT.md`](CONTEXT.md) after this map. It is the canonical glossary
for ToppleCat-specific product terms.

This document does not replace:

- [`README.md`](README.md), which explains the product to users;
- [`CONTRIBUTING.md`](CONTRIBUTING.md), which explains how outside contributors
  collaborate and open pull requests; or
- [`AGENTS.md`](AGENTS.md), which defines mandatory boundaries for AI agents.

## Find the right documents

Read only the rows relevant to the task, except where `AGENTS.md` requires
additional reading.

| Task | Read before editing | Main implementation area |
| --- | --- | --- |
| Understand the product or change supported behavior | [product-design skill](.agents/skills/topplecat-product-design/SKILL.md), [`README.md`](README.md), [`docs/product.md`](docs/product.md), [`docs/architecture.md`](docs/architecture.md), and the affected guide | All four published modules |
| Implement an accepted design | [`docs/design/README.md`](docs/design/README.md), the active design record, and every current document it will change; delete the record after synchronization | The components and tests named by that design |
| Record a cross-cutting product decision before delegation | [`docs/design/README.md`](docs/design/README.md) | `docs/design/` |
| Change case schemas, evidence models, custody metadata, or safe feedback | [`docs/architecture.md`](docs/architecture.md), [`docs/guide/verification-and-evidence.md`](docs/guide/verification-and-evidence.md) | `topplecat-core/` |
| Change annotations, case injection, Stage DSL, or expected consumption | [`docs/guide/authoring.md`](docs/guide/authoring.md), [`docs/architecture.md`](docs/architecture.md) | `topplecat-junit/` |
| Change HTML/JSON reports or audience boundaries | [`docs/architecture.md`](docs/architecture.md), [`docs/guide/verification-and-evidence.md`](docs/guide/verification-and-evidence.md) | `topplecat-report/` |
| Change Gradle tasks, source-set custody, verification runs, or PIT integration | [`docs/architecture.md`](docs/architecture.md), [`docs/guide/verification-and-evidence.md`](docs/guide/verification-and-evidence.md), [`docs/guide/troubleshooting.md`](docs/guide/troubleshooting.md) | `topplecat-gradle-plugin/` |
| Change a reproducible consumer example | [`samples/README.md`](samples/README.md) and that sample's `README.md` and `TUTORIAL.md` | `samples/` |
| Change release-gate infrastructure | [`CONTRIBUTING.md`](CONTRIBUTING.md), [`docs/validation/README.md`](docs/validation/README.md), [CI workflow](.github/workflows/ci.yml) | Maintainer test infrastructure and `scripts/` |
| Change the project website | [`site/README.md`](site/README.md) | `site/` |
| Change the hero animation, composition, or timing | [`site/ANIMATION.md`](site/ANIMATION.md), [`site/src/assets/README.md`](site/src/assets/README.md), [visual reference](docs/images/topplecat-readme-hero.png) | `site/src/App.jsx`, `site/src/styles.css`, `site/src/assets/` |
| Change website deployment | [`site/README.md`](site/README.md), [Pages workflow](.github/workflows/pages.yml), [`site/vite.config.js`](site/vite.config.js) | `.github/workflows/pages.yml`, `site/` |
| Change public documentation | The implementation document for the affected feature and [`README.md`](README.md) | `README.md`, `docs/` |
| Prepare a release or its maintainer publication handoff | [release skill](.agents/skills/topplecat-release/SKILL.md), [`CONTRIBUTING.md`](CONTRIBUTING.md), and [`docs/validation/README.md`](docs/validation/README.md) | Release-facing docs and site, release notes, local Git tag, and the Maven/GitHub handoff |
| Change a repository-owned ToppleCat agent skill | The affected skill and [`docs/validation/README.md`](docs/validation/README.md) | `.agents/skills/` |

## Repository map

| Path | Responsibility |
| --- | --- |
| `topplecat-core/` | Case schema, evidence, custody metadata, and safe feedback model |
| `topplecat-junit/` | JUnit annotations, Stage DSL, typed case injection, expected consumption, and runtime stage records |
| `topplecat-report/` | Reviewer-only Spec Review and Verification Report projections and static HTML rendering |
| `topplecat-gradle-plugin/` | Gradle lifecycle, verification runs, report publication, and mutation gate |
| `samples/` | Independent consumer projects and reproducible walkthroughs |
| `docs/` | Indexed Product definition, current guides, Architecture, release notes, and maintainer validation rules |
| `docs/design/` | Accepted product designs awaiting implementation; completed records are deleted after current docs are synchronized |
| `site/` | Vite/React source for the static GitHub Pages project site |
| `scripts/` | Release, documentation, sample, cleanup, and skill validation |
| `.github/workflows/` | CI and GitHub Pages automation |
| `.agents/skills/topplecat-product-design/` | Repository-owned product framing and design guidance before implementation |
| `.agents/skills/topplecat-acceptance/` | Repository-owned authoring guidance for executable ToppleCat acceptance contracts |
| `.agents/skills/topplecat-release/` | Repository-owned preparation and publication guidance for developer-facing ToppleCat releases |

The published layout has exactly four modules. Do not turn maintainer test
infrastructure, samples, or the website into additional product modules.

## Sources of truth

ToppleCat's executable contract is authored in ordinary Java/JUnit tests and
typed JSON or YAML case rows. Generated JSON, HTML, and evidence files are
projections of that checked contract, not additional authoring surfaces.

Keep these boundaries in mind:

- Public tests and case data belong under `src/test`.
- The complete reviewer-only source set belongs under `src/hiddenTest` while
  the reviewer is authoring it.
- Generated reports and evidence must not add, omit, or reinterpret approved
  rules, cases, expected values, or scenario steps.
- Public handoff material and `agent-feedback.json` must not reveal
  reviewer-only data, identifiers, source names, paths, or raw failures.
- Humans remain responsible for the completeness of the approved contract.

Read [`docs/product.md`](docs/product.md) for product ownership and
[`docs/architecture.md`](docs/architecture.md) for the implemented contract,
verification, information, and delivery boundaries.

Design records under [`docs/design/`](docs/design/) are temporary implementation
handoffs. Every retained record is `Accepted` and therefore not current product
behavior. When implementation finishes, merge its lasting content into Product,
Architecture, guides, glossary, skills, and release-facing docs as applicable,
then delete the record.

## Local prerequisites

Core development uses:

- JDK 25;
- the repository's Gradle 9.1.0 wrapper; and
- a Unix-like shell for the release gate.

Website development additionally uses Node.js 24 and npm. Install its locked
dependencies with:

```bash
cd site
npm ci
python3 -m pip install --requirement requirements.txt
```

## Development workflow

### Java and Gradle changes

Start with the narrowest relevant module or test. For example:

```bash
./gradlew :topplecat-core:test
./gradlew :topplecat-junit:test
./gradlew :topplecat-report:test
./gradlew :topplecat-gradle-plugin:test
```

Use `--tests` when one test class or method is sufficient during development.
Before considering a product change complete, run:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
```

A green `test` task is development feedback. The final delegation verdict comes
from `toppleCatVerify` and the current evidence, not from an isolated unit-test
run.

### Sample changes

Read [`samples/README.md`](samples/README.md) before editing a sample. Run the
affected demo first, then both sample workflows when the shared setup changes:

```bash
bash samples/junit-cart-orders/demo.sh
bash samples/spring-boot-cart-orders/demo.sh
bash scripts/verify-samples.sh
```

### Website changes

Use the Vite source under `site/`; do not edit or commit `site/dist/`.

```bash
cd site
npm run dev
npm run build
```

`npm run build` builds the Vite project page and both MkDocs/Material language
sites into one `site/dist/` artifact. Validate the served artifact with
`python3 scripts/verify-site-artifact.py --self-test`; it checks the final URL
seam, not the documentation engine internals. GitHub Actions builds and
deploys that artifact only after project checks, the release gate, the sample
workflow, public-document verification, and artifact verification pass. The
repository does not maintain a `gh-pages` branch or a supported manual
deployment procedure.

Any visual or interactive change requires checking the rendered page, not only
the build output. Hero animation and artwork changes must follow every check in
[`site/ANIMATION.md`](site/ANIMATION.md), including the three atomic states,
layer order, object edges, proportions, desktop/mobile rendering, reduced
motion, and browser console.

### Documentation changes

Run the documentation verifier and whitespace check:

```bash
python3 scripts/verify-docs.py
git diff --check
```

Update the public documentation when commands, coordinates, report behavior,
authoring behavior, or compatibility changes. Historical release notes describe
released versions and must not be silently rewritten.

### Release infrastructure

The public validation checklist is maintained in
[`docs/validation/README.md`](docs/validation/README.md). Depending on the
change, it includes:

```bash
./gradlew check
GRADLE_CMD=./gradlew scripts/verify-release.sh
bash scripts/verify-release-cleanup-test.sh
bash scripts/validate-skill.sh
python3 scripts/verify-docs.py
git diff --check
```

Publishing, tagging, pushing, and creating a release require explicit
authorization. Follow the versioning rules in
[`CONTRIBUTING.md`](CONTRIBUTING.md).

## Generated and local-only files

Do not commit:

- Gradle `build/` directories or `.gradle/`;
- `site/node_modules/` or `site/dist/`;
- local `.topplecat/` state;
- local artwork drafts under `site/.artwork-archive/`;
- credentials, environment files, logs, coverage, or temporary output.

Do not edit generated reports or evidence as if they were source code. Re-run
the authoritative task that creates them.

## Before declaring a change complete

1. Confirm the change stayed within the task and product boundaries.
2. Re-read the task row in the document map and check every referenced
   contract.
3. Run the narrowest relevant test while developing.
4. Run the required final checks for that change type.
5. Inspect generated or rendered output when presentation matters.
6. Check `git diff` and preserve unrelated worktree changes.
7. Confirm generated output, local custody state, credentials, and temporary
   files are not part of the change.
8. Report what was verified and anything intentionally not run.
