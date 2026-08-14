#!/usr/bin/env python3
"""Checks tracked public Markdown links and release-facing documentation boundaries."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote


ROOT = Path(__file__).resolve().parent.parent
PREVIOUS_RELEASE_VERSION = "0.2.0"
CURRENT_RELEASE_VERSION = "0.2.1"
MAVEN_CENTRAL_VERSION = "0.2.0"
PRIVATE_SECTIONS = {"grimo", "history", "decisions", "deepwiki", "maintainers"}
SECRET_TOKENS = ("coupon-hidden-800", "customer-2", "ReviewerBoundary")
LEGACY_REPORT_NAMES = ("spec.html", "review.html")
ACTIVE_TERMINOLOGY_PATHS = (
    "README.md",
    "README.zh-TW.md",
    "docs/README.md",
    "docs/product.md",
    "docs/architecture.md",
    "docs/guide",
    "CONTEXT.md",
    "docs/design",
    "docs/releases/0.2.0.md",
    "docs/releases/0.2.0.zh-TW.md",
    "samples",
    ".agents/skills",
    "site/src",
    "topplecat-report/src/main/resources",
)
LEGACY_TERMS = {
    r"@ToppleTest\b": "use @ToppleAcceptanceTest",
    r"@ToppleAc\b": "reviewer Java tests are not ToppleCat evidence",
    r"\bPropertyTrial\b": "use PropertyTrials",
    r"\btoppleCatHide\b": "use toppleCatSeal",
    r"\btoppleCatUpdateEscrow\b": "use toppleCatReseal",
    r"\bhiddenRetest\b": "use hiddenTests",
    r"toppleCat\.adversarial": "use individual safeguard DSL blocks",
    r"--all-hidden(?!-tests)\b": "use --all-hidden-tests",
    r"reports/spec": "use reports/review",
    r"\btoppleCatMigrateEscrow\b": "the current release does not migrate custody",
    r"@ToppleStageField\b": "use the single ToppleScenario API",
    r"@ProvidedState\b|@ExpectedState\b": "keep cross-Step state in a capability Stage",
    r"\brecorded\s*\(": "compiler-described Steps do not use runtime recording",
    r"\bself\s*\(": "Stage Steps are ordinary void methods",
    r"\bToppleStageSentence\b": "compiler descriptors render Step sentences",
    r"\bToppleStage\s*<": "ToppleStage is non-generic in the current release",
    r"\b[Hh]idden[ -][Pp]ropert(?:y|ies)\b": "Property-Based Testing has no hidden variant",
    r"\breviewer[- ]only propert(?:y|ies)\b": "Property-Based Testing has no reviewer-only variant",
    r"\bhiddenProperty(?:Test|Mode|ies)?\b": "Property-Based Testing has one independent execution path",
    r"隱藏性質|審閱者(?:專用|專屬)性質": "性質導向測試沒有隱藏或審閱者專用版本",
}
REQUIRED_GUIDES = (
    "docs/product.md",
    "docs/architecture.md",
    "docs/guide/getting-started.md",
    "docs/guide/authoring.md",
    "docs/guide/verification-and-evidence.md",
    "docs/guide/troubleshooting.md",
)
EXPECTED_ROOT_MARKDOWN_FILES = {
    "AGENTS.md",
    "CONTEXT.md",
    "CONTRIBUTING.md",
    "DEVELOPMENT.md",
    "README.md",
    "README.zh-TW.md",
    "SECURITY.md",
}
EXPECTED_DOC_ROOT_MARKDOWN_FILES = {"README.md", "product.md", "architecture.md"}
REQUIRED_DOC_INDEX_LINKS = (
    "../README.md",
    "product.md",
    "guide/getting-started.md",
    "guide/authoring.md",
    "guide/verification-and-evidence.md",
    "guide/troubleshooting.md",
    "architecture.md",
    "design/README.md",
    "../CONTEXT.md",
    "releases/0.2.0.md",
    "releases/0.2.0.zh-TW.md",
    "releases/0.2.1.md",
    "releases/0.2.1.zh-TW.md",
    "validation/README.md",
)
REQUIRED_DESIGN_SECTIONS = (
    "## User example",
    "## Problem",
    "## Decision and product boundaries",
    "## Visible interface and behavior",
    "## Failure and integrity rules",
    "## Acceptance evidence",
    "## Consequences and alternatives",
)
CURRENT_RELEASE_FILES = {"0.2.1.md", "0.2.1.zh-TW.md"}
EXPECTED_RELEASE_FILES = {
    "0.2.0.md",
    "0.2.0.zh-TW.md",
    "0.2.1.md",
    "0.2.1.zh-TW.md",
}
RELEASE_NOTE = re.compile(r"^(\d+\.\d+\.\d+)(\.zh-TW)?\.md$")
SEMVER_TAG = re.compile(r"^v?(\d+)\.(\d+)\.(\d+)$")
CONTEXT_TERMS = (
    "Executable Contract",
    "Acceptance Condition",
    "Acceptance Method",
    "Scenario",
    "Stage",
    "Step",
    "Typed Case Row",
    "Hidden Tests",
    "Mutation Testing",
    "Mutation Attribution",
    "ToppleCat Managed Mutation Profile",
    "Contract Quality Advisory",
    "Property-Based Testing",
    "Independent Safeguard",
    "Spec Review",
    "Selected Spec Document",
    "Verification Report",
    "Evidence Fidelity",
    "Delivery Scope",
    "Mechanical Seal",
    "Reviewer Custody",
    "Current-run Evidence",
    "Reviewer",
    "Implementation Agent",
    "External Workflow",
)
SAMPLE_ACCEPTANCE_METHOD = '''@ToppleAcceptanceTest("AC-CART-COUPON")
@DisplayName("SAVE100 reduces the order subtotal")
void appliesCoupon(ToppleCase c, ToppleScenario scenario, CouponStage coupon) {
    scenario.given(coupon).a_payable_cart(c.input("cart", Cart.class));
    scenario.when(coupon).checks_out();
    scenario.then(coupon).receipt_shows_discount_and_discounted_subtotal(c);
}'''
SDK_REFERENCE_DOCUMENTS = (
    "docs/guide/authoring.md",
    "site/docs/en/authoring-contracts.md",
    "site/docs/zh-TW/authoring-contracts.md",
)
MARKDOWN_LINK = re.compile(r"!?\[[^\]]*\]\(([^)\s]+)(?:\s+[^)]*)?\)")
HTML_LINK = re.compile(r"(?:href|src)=[\"']([^\"']+)[\"']", re.IGNORECASE)


def source_markdown() -> list[Path]:
    try:
        tracked = subprocess.check_output(
            ["git", "ls-files", "--", "*.md"], cwd=ROOT, text=True, stderr=subprocess.DEVNULL
        )
        untracked = subprocess.check_output(
            ["git", "ls-files", "--others", "--exclude-standard", "--", "*.md"],
            cwd=ROOT,
            text=True,
            stderr=subprocess.DEVNULL,
        )
        paths = {ROOT / line for line in (tracked + untracked).splitlines() if line}
    except (subprocess.CalledProcessError, FileNotFoundError):
        ignored = {".git", ".gradle", ".topplecat", "build"}
        paths = {
            path
            for path in ROOT.rglob("*.md")
            if not any(part in ignored for part in path.relative_to(ROOT).parts)
        }
    return sorted(path for path in paths if path.is_file())


def is_public_document(path: Path) -> bool:
    relative = path.relative_to(ROOT)
    return not (len(relative.parts) > 1 and relative.parts[0] == "docs" and relative.parts[1] in PRIVATE_SECTIONS)


def active_terminology_files() -> list[Path]:
    text_suffixes = {".md", ".java", ".kts", ".sh", ".py", ".js", ".jsx", ".ts", ".tsx"}
    paths: list[Path] = []
    for relative in ACTIVE_TERMINOLOGY_PATHS:
        candidate = ROOT / relative
        if candidate.is_file():
            paths.append(candidate)
        elif candidate.is_dir():
            paths.extend(
                path
                for path in candidate.rglob("*")
                if path.is_file()
                and path.suffix in text_suffixes
                and not any(part in {"build", ".gradle", ".git", ".topplecat"} for part in path.relative_to(ROOT).parts)
            )
    return sorted(paths)


def local_link_target(source: Path, destination: str) -> tuple[Path, str | None] | None:
    target = destination.strip("<>")
    if not target or target.startswith(("http://", "https://", "mailto:")):
        return None
    path_part, separator, anchor = target.partition("#")
    path_part = unquote(path_part.split("?", 1)[0])
    if not path_part:
        return source, unquote(anchor) if separator else None
    path = (ROOT / path_part.lstrip("/")) if path_part.startswith("/") else (source.parent / path_part)
    return path, unquote(anchor) if separator else None


def markdown_anchors(path: Path) -> set[str]:
    anchors: set[str] = set()
    occurrences: dict[str, int] = {}
    fence: str | None = None
    for line in path.read_text(encoding="utf-8").splitlines():
        fence_match = re.match(r"^\s*(`{3,}|~{3,})", line)
        if fence_match:
            marker = fence_match.group(1)[0]
            fence = None if fence == marker else marker
            continue
        if fence is not None:
            continue
        match = re.match(r"^#{1,6}\s+(.+?)\s*#*\s*$", line)
        if not match:
            continue
        heading = re.sub(r"<[^>]+>", "", match.group(1))
        explicit = re.search(r"\s*\{#([A-Za-z][A-Za-z0-9_\-:.]*)\}\s*$", heading)
        if explicit:
            anchors.add(explicit.group(1))
            heading = heading[: explicit.start()].rstrip()
        heading = re.sub(r"[^\w\- ]", "", heading.lower())
        base = re.sub(r"\s+", "-", heading.strip())
        count = occurrences.get(base, 0)
        occurrences[base] = count + 1
        anchors.add(base if count == 0 else f"{base}-{count}")
    return anchors


def has_balanced_fences(text: str) -> bool:
    fence: str | None = None
    for line in text.splitlines():
        match = re.match(r"^\s*(`{3,}|~{3,})", line)
        if not match:
            continue
        marker = match.group(1)[0]
        fence = None if fence == marker else marker
    return fence is None


def current_maven_central_claim_is_safe(text: str) -> bool:
    """Reject a sentence that presents 0.2.1 as a Maven Central artifact."""
    sentences = re.split(r"(?<=[.!?。！？])\s+", text)
    positive = re.compile(
        r"(?:available|published|released)\s+(?:from|on|to)\s+Maven\s+Central"
        r"|可從\s*Maven\s+Central\s*(?:取得|下載)"
        r"|(?:已發布|正式套件).*?Maven\s+Central",
        flags=re.IGNORECASE,
    )
    explicit_unpublished = re.compile(
        r"\b(?:not|has\s+not|is\s+not)\s+(?:currently\s+)?(?:been\s+)?(?:available|published|released)\b"
        r"|尚未\s*(?:提供|發布)|未\s*(?:發布|提供)",
        flags=re.IGNORECASE,
    )
    for sentence in sentences:
        if CURRENT_RELEASE_VERSION in sentence and positive.search(sentence) and not explicit_unpublished.search(sentence):
            return False
    return True


def main() -> int:
    failures: list[str] = []
    build_text = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
    version_text = (ROOT / "topplecat-gradle-plugin/src/main/java/io/github/samzhu/topplecat/gradle/ToppleCatVersion.java").read_text(encoding="utf-8")
    skill_text = (ROOT / ".agents/skills/topplecat-acceptance/SKILL.md").read_text(encoding="utf-8")
    if f'version = "{CURRENT_RELEASE_VERSION}"' not in build_text:
        failures.append(f"build.gradle.kts: current release must be {CURRENT_RELEASE_VERSION}")
    if f'CURRENT = "{CURRENT_RELEASE_VERSION}"' not in version_text:
        failures.append(f"ToppleCatVersion: current release must be {CURRENT_RELEASE_VERSION}")
    if f'topplecat-version: "{CURRENT_RELEASE_VERSION}"' not in skill_text:
        failures.append(f"acceptance skill: metadata must identify {CURRENT_RELEASE_VERSION}")
    tag_result = subprocess.run(
        ["git", "tag", "--list"], cwd=ROOT, text=True, capture_output=True, check=False
    )
    if tag_result.returncode == 0:
        versions = [
            tuple(int(part) for part in match.groups())
            for tag in tag_result.stdout.splitlines()
            if (match := SEMVER_TAG.fullmatch(tag.strip()))
        ]
        if versions:
            highest = max(versions)
            expected_candidate = (highest[0], highest[1], highest[2] + 1)
            actual_release = tuple(int(part) for part in CURRENT_RELEASE_VERSION.split("."))
            if highest != actual_release and actual_release != expected_candidate:
                failures.append(
                    "current release must either be the highest local tag or the next patch after "
                    f"the highest available release tag {highest[0]}.{highest[1]}.{highest[2]}, "
                    f"not {CURRENT_RELEASE_VERSION}"
                )
    historical = (ROOT / f"docs/releases/{PREVIOUS_RELEASE_VERSION}.md").read_text(encoding="utf-8")
    for feature_phrase in ("selected Spec Review", "topplecat:acceptance"):
        if feature_phrase in historical:
            failures.append(f"historical {PREVIOUS_RELEASE_VERSION} release note contains current feature phrase: {feature_phrase}")
    site_release = (ROOT / "site/docs/en/release-notes.md").read_text(encoding="utf-8")
    site_release_zh = (ROOT / "site/docs/zh-TW/release-notes.md").read_text(encoding="utf-8")
    if "Maven Central" not in site_release or "Maven Central" not in site_release_zh:
        failures.append("site current release notes must state the Maven Central publication boundary")
    for readme, required_unreleased in (
        (ROOT / "README.md", "not been published to Maven Central"),
        (ROOT / "README.zh-TW.md", "尚未發布到 Maven Central"),
    ):
        readme_text = readme.read_text(encoding="utf-8")
        normalized_readme = " ".join(readme_text.split())
        if CURRENT_RELEASE_VERSION not in readme_text:
            failures.append(f"{readme.name}: missing current release {CURRENT_RELEASE_VERSION}")
        if required_unreleased not in normalized_readme or not current_maven_central_claim_is_safe(readme_text):
            failures.append(f"{readme.name}: {CURRENT_RELEASE_VERSION} must not be described as published to Maven Central")
        if "publishToMavenLocal" not in readme_text or "mavenLocal()" not in readme_text:
            failures.append(f"{readme.name}: candidate setup must show publishToMavenLocal and mavenLocal()")
    false_claim = (ROOT / "README.md").read_text(encoding="utf-8").replace(
        "has not\nbeen published to Maven Central yet", "has been published to Maven Central"
    )
    if current_maven_central_claim_is_safe(false_claim):
        failures.append("README guard self-test did not reject a 0.2.1 Maven Central claim")
    public_documents = [path for path in source_markdown() if is_public_document(path)]
    root_markdown_files = {path.name for path in ROOT.glob("*.md")}
    if root_markdown_files != EXPECTED_ROOT_MARKDOWN_FILES:
        failures.append(
            "repository root: expected only indexed root Markdown files "
            + ", ".join(sorted(EXPECTED_ROOT_MARKDOWN_FILES))
        )
    docs_root = ROOT / "docs"
    docs_root_markdown_files = {path.name for path in docs_root.glob("*.md")}
    if docs_root_markdown_files != EXPECTED_DOC_ROOT_MARKDOWN_FILES:
        failures.append(
            "docs: expected only the index, Product definition, and Architecture at its root"
        )
    guide_dir = docs_root / "guide"
    for required in REQUIRED_GUIDES:
        if not (ROOT / required).is_file():
            failures.append(f"required public guide is missing: {required}")
    context = ROOT / "CONTEXT.md"
    if not context.is_file():
        failures.append("required root glossary is missing: CONTEXT.md")
    else:
        context_text = context.read_text(encoding="utf-8")
        for term in CONTEXT_TERMS:
            if term not in context_text:
                failures.append(f"CONTEXT.md: missing canonical term {term}")
        if "```" in context_text or "implementation plan" in context_text.lower():
            failures.append("CONTEXT.md: must remain a glossary without code or implementation plans")
    docs_index = ROOT / "docs/README.md"
    if not docs_index.is_file():
        failures.append("required documentation index is missing: docs/README.md")
    else:
        docs_index_text = docs_index.read_text(encoding="utf-8")
        for destination in REQUIRED_DOC_INDEX_LINKS:
            if f"]({destination})" not in docs_index_text:
                failures.append(f"docs/README.md: missing indexed document {destination}")
        for guide in sorted(guide_dir.glob("*.md")):
            destination = f"guide/{guide.name}"
            if f"]({destination})" not in docs_index_text:
                failures.append(f"docs/README.md: unindexed guide {destination}")

    design_dir = ROOT / "docs/design"
    if design_dir.is_dir():
        nested_design_dirs = [path.name for path in design_dir.iterdir() if path.is_dir()]
        if nested_design_dirs:
            failures.append(
                "docs/design: archive or nested directories are not allowed: "
                + ", ".join(sorted(nested_design_dirs))
            )
        design_files = {path.name for path in design_dir.glob("*.md")}
        design_index = design_dir / "README.md"
        design_index_text = (
            design_index.read_text(encoding="utf-8") if design_index.is_file() else ""
        )
        for name in sorted(design_files - {"README.md"}):
            if f"]({name})" not in design_index_text:
                failures.append(f"docs/design/README.md: missing indexed design record {name}")
            record = design_dir / name
            record_text = record.read_text(encoding="utf-8")
            status_match = re.search(r"^\*\*Status:\*\* ([A-Za-z]+)$", record_text, re.MULTILINE)
            if status_match is None or status_match.group(1) != "Accepted":
                failures.append(f"docs/design/{name}: retained design status must be Accepted")
            if not re.search(r"^\*\*Accepted date:\*\* \d{4}-\d{2}-\d{2}$", record_text, re.MULTILINE):
                failures.append(f"docs/design/{name}: missing Accepted date metadata")
            if "**Affected current documentation:**" not in record_text:
                failures.append(f"docs/design/{name}: missing affected current-document metadata")
            for heading in REQUIRED_DESIGN_SECTIONS:
                if heading not in record_text:
                    failures.append(f"docs/design/{name}: missing required section {heading}")
            if "## Implementation task plan" in record_text:
                failures.append(f"docs/design/{name}: retains a completed implementation task plan")
    release_dir = ROOT / "docs/releases"
    if release_dir.is_dir():
        release_files = {path.name for path in release_dir.iterdir() if path.is_file()}
        release_versions: dict[str, set[str]] = {}
        for name in release_files:
            match = RELEASE_NOTE.fullmatch(name)
            if match is None:
                failures.append(f"docs/releases: release note has an unsupported name: {name}")
                continue
            language = "zh-TW" if match.group(2) else "en"
            release_versions.setdefault(match.group(1), set()).add(language)
        if release_files != EXPECTED_RELEASE_FILES:
            failures.append(
                "docs/releases: expected the 0.2.0 history pair and current 0.2.1 English/Traditional-Chinese notes"
            )
        for version, languages in sorted(release_versions.items()):
            if languages != {"en", "zh-TW"}:
                failures.append(
                    f"docs/releases: {version} must have both English and Traditional-Chinese notes"
                )

    for document in public_documents:
        text = document.read_text(encoding="utf-8")
        relative = document.relative_to(ROOT)
        if not text.endswith("\n"):
            failures.append(f"{relative}: missing final newline")
        if re.search(r"[ \t]+$", text, re.MULTILINE):
            failures.append(f"{relative}: contains trailing whitespace")
        if not has_balanced_fences(text):
            failures.append(f"{relative}: contains an unclosed Markdown code fence")
        for section in PRIVATE_SECTIONS:
            forbidden = f"docs/{section}"
            if forbidden in text:
                failures.append(f"{relative}: references private documentation path {forbidden}")
        for token in SECRET_TOKENS:
            if token in text:
                failures.append(f"{relative}: exposes reviewer-only token {token}")
        if relative != Path("CONTRIBUTING.md") and "integration-tests/" in text:
            failures.append(f"{relative}: exposes maintainer-only integration test infrastructure")
        if re.search(r"\bToppleCat\b[^\n]{0,80}\bBDD (?:tool|framework)\b", text, re.IGNORECASE):
            failures.append(f"{relative}: describes ToppleCat as a BDD tool or framework")
        destinations = [match.group(1) for match in MARKDOWN_LINK.finditer(text)]
        destinations.extend(match.group(1) for match in HTML_LINK.finditer(text))
        for destination in destinations:
            resolved = local_link_target(document, destination)
            if resolved is None:
                continue
            target, anchor = resolved
            try:
                target.resolve().relative_to(ROOT.resolve())
            except ValueError:
                failures.append(f"{relative}: local link escapes the repository: {destination}")
                continue
            if not target.exists():
                failures.append(f"{relative}: dead relative link {destination}")
            elif (
                anchor
                and target.is_file()
                and target.suffix.lower() == ".md"
                and not (relative.parts[:2] == ("docs", "releases") and relative.name not in CURRENT_RELEASE_FILES)
            ):
                if anchor not in markdown_anchors(target):
                    failures.append(f"{relative}: dead Markdown anchor {destination}")

    for document in public_documents:
        text = document.read_text(encoding="utf-8")
        relative = document.relative_to(ROOT)
        for legacy_name in LEGACY_REPORT_NAMES:
            if legacy_name in text:
                failures.append(f"{relative}: uses legacy report name {legacy_name}; use reports/.../index.html")

    for document in active_terminology_files():
        text = document.read_text(encoding="utf-8")
        relative = document.relative_to(ROOT)
        for pattern, replacement in LEGACY_TERMS.items():
            if re.search(pattern, text):
                failures.append(f"{relative}: uses replaced terminology matching {pattern}; {replacement}")

    english_release = ROOT / f"docs/releases/{CURRENT_RELEASE_VERSION}.md"
    chinese_release = ROOT / f"docs/releases/{CURRENT_RELEASE_VERSION}.zh-TW.md"
    if english_release.is_file() and chinese_release.is_file():
        release_markers = (
            (
                english_release,
                (
                    "--spec",
                    "repository-relative",
                    "toppleCatSeal",
                    CURRENT_RELEASE_VERSION,
                ),
            ),
            (
                chinese_release,
                (
                    "--spec",
                    "repository-relative",
                    "toppleCatSeal",
                    CURRENT_RELEASE_VERSION,
                ),
            ),
        )
        for release, markers in release_markers:
            text = release.read_text(encoding="utf-8")
            for marker in markers:
                if marker not in text:
                    failures.append(
                        f"{release.relative_to(ROOT)}: missing synchronized {CURRENT_RELEASE_VERSION} change {marker}"
                    )

    for relative in SDK_REFERENCE_DOCUMENTS:
        document = ROOT / relative
        if not document.is_file() or SAMPLE_ACCEPTANCE_METHOD not in document.read_text(encoding="utf-8"):
            failures.append(
                f"{relative}: SDK Acceptance Method must match the cart-orders learning project"
            )

    if failures:
        print("Documentation validation failed:", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        print("Fix the public documentation or its relative links, then rerun scripts/verify-docs.py.", file=sys.stderr)
        return 1

    print(f"Documentation validation PASS: {len(public_documents)} public Markdown files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
