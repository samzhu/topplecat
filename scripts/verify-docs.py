#!/usr/bin/env python3
"""Checks tracked public Markdown links and release-facing documentation boundaries."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote


ROOT = Path(__file__).resolve().parent.parent
PRIVATE_SECTIONS = {"grimo", "history", "decisions", "deepwiki", "maintainers"}
SECRET_TOKENS = ("coupon-hidden-800", "customer-2", "ReviewerBoundary")
LEGACY_REPORT_NAMES = ("spec.html", "review.html")
ACTIVE_TERMINOLOGY_PATHS = (
    "README.md",
    "README.zh-TW.md",
    "docs/README.md",
    "docs/architecture.md",
    "docs/guide",
    "docs/faq.md",
    "docs/faq.zh-TW.md",
    "CONTEXT.md",
    "docs/design/README.md",
    "docs/design/executable-acceptance-boundary.md",
    "docs/design/property-based-testing.md",
    "docs/design/property-completion-evidence-fidelity.md",
    "docs/design/topple-scenario-authoring.md",
    "docs/design/independent-safeguard-results.md",
    "docs/design/mutation-attribution.md",
    "docs/design/managed-mutation-profile.md",
    "docs/design/contract-quality-advisory.md",
    "docs/design/human-readable-reports.md",
    "docs/releases/0.0.13.md",
    "docs/releases/0.0.13.zh-TW.md",
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
    "docs/architecture.md",
    "docs/guide/getting-started.md",
    "docs/guide/authoring.md",
    "docs/guide/verification-and-evidence.md",
    "docs/guide/troubleshooting.md",
)
EXPECTED_DESIGN_FILES = {
    "README.md",
    "executable-acceptance-boundary.md",
    "property-based-testing.md",
    "property-completion-evidence-fidelity.md",
    "topple-scenario-authoring.md",
    "independent-safeguard-results.md",
    "mutation-attribution.md",
    "managed-mutation-profile.md",
    "contract-quality-advisory.md",
    "human-readable-reports.md",
}
CURRENT_RELEASE_FILES = {"0.0.13.md", "0.0.13.zh-TW.md"}
RELEASE_NOTE = re.compile(r"^(\d+\.\d+\.\d+)(\.zh-TW)?\.md$")
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


def main() -> int:
    failures: list[str] = []
    public_documents = [path for path in source_markdown() if is_public_document(path)]
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
    design_dir = ROOT / "docs/design"
    if design_dir.is_dir():
        design_files = {path.name for path in design_dir.iterdir() if path.is_file()}
        if design_files != EXPECTED_DESIGN_FILES:
            failures.append(
                "docs/design: expected only formal records "
                + ", ".join(sorted(EXPECTED_DESIGN_FILES))
            )
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
        if release_files != CURRENT_RELEASE_FILES:
            failures.append(
                "docs/releases: expected only the 0.0.13 English and Traditional-Chinese notes"
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

    english_release = ROOT / "docs/releases/0.0.13.md"
    chinese_release = ROOT / "docs/releases/0.0.13.zh-TW.md"
    if english_release.is_file() and chinese_release.is_file():
        release_markers = (
            (
                english_release,
                (
                    "Verification Report",
                    "Current-run Evidence",
                    "terminal event",
                    "PROPERTY",
                ),
            ),
            (
                chinese_release,
                (
                    "Verification Report",
                    "Current-run Evidence",
                    "terminal event",
                    "PROPERTY",
                ),
            ),
        )
        for release, markers in release_markers:
            text = release.read_text(encoding="utf-8")
            for marker in markers:
                if marker not in text:
                    failures.append(
                        f"{release.relative_to(ROOT)}: missing synchronized 0.0.13 change {marker}"
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
