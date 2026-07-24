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
REQUIRED_GUIDES = (
    "docs/architecture.md",
    "docs/guide/getting-started.md",
    "docs/guide/authoring.md",
    "docs/guide/verification-and-evidence.md",
    "docs/guide/troubleshooting.md",
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
            elif anchor and target.is_file() and target.suffix.lower() == ".md":
                if anchor not in markdown_anchors(target):
                    failures.append(f"{relative}: dead Markdown anchor {destination}")

    for document in public_documents:
        text = document.read_text(encoding="utf-8")
        relative = document.relative_to(ROOT)
        for legacy_name in LEGACY_REPORT_NAMES:
            if legacy_name in text:
                failures.append(f"{relative}: uses legacy report name {legacy_name}; use reports/.../index.html")

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
