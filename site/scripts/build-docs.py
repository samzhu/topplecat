#!/usr/bin/env python3
"""Build both human-authored MkDocs sites and the shared Pages discovery files."""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path


SITE = Path(__file__).resolve().parents[1]
DIST = SITE / "dist"
ORIGIN = os.environ.get("PUBLIC_ORIGIN", "https://topplecat.samzhu.dev").rstrip("/")
BASE = os.environ.get("PAGES_BASE_PATH", "/").strip()
if not BASE or BASE == "/":
    BASE = ""
else:
    BASE = "/" + BASE.strip("/")

PAGES = (
    "index.html",
    "getting-started/index.html",
    "authoring-contracts/index.html",
    "verification-and-evidence/index.html",
    "troubleshooting/index.html",
    "product-definition/index.html",
    "architecture/index.html",
    "glossary/index.html",
    "release-notes/index.html",
)


def public_url(path: str) -> str:
    path = path.strip("/")
    return f"{ORIGIN}{BASE}/{path}" if path else f"{ORIGIN}{BASE}/"


def run_mkdocs(config: str, docs_url: str) -> None:
    env = os.environ.copy()
    env["DOCS_SITE_URL"] = docs_url
    env["PUBLIC_ORIGIN"] = ORIGIN
    env["PAGES_BASE_PATH"] = BASE
    subprocess.run(
        [sys.executable, "-m", "mkdocs", "build", "--config-file", config, "--clean"],
        cwd=SITE,
        env=env,
        check=True,
    )


def copy_markdown(source_root: Path, output_root: Path) -> None:
    for source in source_root.glob("*.md"):
        shutil.copy2(source, output_root / source.name)


def write_discovery() -> None:
    docs_pages = ["docs", *[f"docs/{path.removesuffix('/index.html')}" for path in PAGES[1:]]]
    zh_pages = ["docs/zh-TW", *[f"docs/zh-TW/{path.removesuffix('/index.html')}" for path in PAGES[1:]]]
    all_pages = ["", *docs_pages, *zh_pages]

    sitemap = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ]
    sitemap.extend(f"  <url><loc>{public_url(path) if not path else public_url(path) + '/'}</loc></url>" for path in all_pages)
    sitemap.append("</urlset>")
    (DIST / "sitemap.xml").write_text("\n".join(sitemap) + "\n", encoding="utf-8")
    (DIST / "robots.txt").write_text(
        f"User-agent: *\nAllow: /\nSitemap: {public_url('sitemap.xml')}\n", encoding="utf-8"
    )

    (DIST / "llms.txt").write_text(
        "# ToppleCat current documentation\n\n"
        "This is an experimental index of the current, human-authored page-level Markdown manifests. "
        "Ordinary HTML links and the sitemap are the primary discovery mechanisms; this file is not a Web standard.\n\n"
        f"- [English Markdown manifest]({public_url('docs/llms.txt')})\n"
        f"- [繁體中文 Markdown manifest]({public_url('docs/zh-TW/llms.txt')})\n",
        encoding="utf-8",
    )

    def manifest(prefix: str, label: str) -> str:
        lines = [f"# {label} current page Markdown", "", "Experimental convenience only; this is not a crawler guarantee.", ""]
        for path in PAGES:
            slug = "" if path == "index.html" else path.removesuffix("/index.html")
            target = f"{prefix}{slug}.md" if slug else f"{prefix}index.md"
            lines.append(f"- [{slug or 'Documentation home'}]({public_url(target)})")
        return "\n".join(lines) + "\n"

    (DIST / "docs/llms.txt").write_text(manifest("docs/", "English"), encoding="utf-8")
    (DIST / "docs/zh-TW/llms.txt").write_text(
        manifest("docs/zh-TW/", "繁體中文"), encoding="utf-8"
    )


def update_root_index() -> None:
    index = DIST / "index.html"
    text = index.read_text(encoding="utf-8")
    root_url = public_url("")
    text = text.replace("https://topplecat.samzhu.dev/", root_url)
    index.write_text(text, encoding="utf-8")


def main() -> None:
    (DIST / "docs").mkdir(parents=True, exist_ok=True)
    run_mkdocs("mkdocs-en.yml", public_url("docs"))
    run_mkdocs("mkdocs-zh-TW.yml", public_url("docs/zh-TW"))
    copy_markdown(SITE / "docs/en", DIST / "docs")
    copy_markdown(SITE / "docs/zh-TW", DIST / "docs/zh-TW")
    write_discovery()
    update_root_index()
    print(f"Built bilingual documentation into {DIST}")


if __name__ == "__main__":
    main()
