#!/usr/bin/env python3
"""Verify the final combined Pages artifact at its public URL seam."""

from __future__ import annotations

import argparse
import http.server
import re
import shutil
import socketserver
import tempfile
import threading
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser
from pathlib import Path


ORIGIN = "https://topplecat.samzhu.dev"
PAGE_IDS = (
    "home",
    "getting-started",
    "authoring-contracts",
    "verification-and-evidence",
    "troubleshooting",
    "product-definition",
    "architecture",
    "glossary",
    "release-notes",
)
REQUIRED_ANCHORS = {
    "home": {
        "documentation-home",
        "problem",
        "checks",
        "start-here",
        "reports",
        "audience",
        "terms",
        "choose-your-task",
        "ai-help",
    },
    "getting-started": {"contract-example", "sample-workflow", "formal-verify", "human-decision"},
    "authoring-contracts": {"contract-example", "acceptance-method", "typed-case-rows", "human-completeness"},
    "verification-and-evidence": {"delivery-example", "three-evidence-layers", "gates-and-verdicts", "reviewer-boundary"},
    "troubleshooting": {"symptom-map", "public-acceptance", "incomplete-evidence", "safe-next-action"},
    "product-definition": {"use-moment", "responsibility-boundary", "what-topplecat-does-not-own"},
    "architecture": {"four-modules", "execution-flow", "information-boundary", "contract-authority"},
    "glossary": {"executable-contract", "acceptance-condition", "independent-safeguard", "mechanical-seal"},
    "release-notes": {"current-release", "documentation-surface", "upgrade-notes"},
}
HOME_SECTION_ORDER = (
    "problem",
    "checks",
    "start-here",
    "reports",
    "audience",
    "terms",
    "choose-your-task",
    "ai-help",
)
DENIED_PATHS = ("docs/design", "docs/research", "docs/validation", ".scratch", "src/hiddenTest", ".topplecat", "integration-tests")
DENIED_MARKERS = ("coupon-hidden-800", "ReviewerBoundary", "reviewer-only-value", "raw failure from an actual delivery")


class Page(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.attrs: dict[str, list[dict[str, str]]] = {}
        self.links: list[dict[str, str]] = []
        self.headings: set[str] = set()
        self.text: list[str] = []
        self._heading = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = {key: value or "" for key, value in attrs}
        self.attrs.setdefault(tag, []).append(values)
        if tag == "a" and values.get("href"):
            self.links.append(values)
        if tag in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            self._heading = True

    def handle_endtag(self, tag: str) -> None:
        if tag in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            self._heading = False

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.handle_starttag(tag, attrs)

    def handle_data(self, data: str) -> None:
        self.text.append(data)
        if self._heading:
            self.headings.add(data.strip())


class MarkdownHandler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {**http.server.SimpleHTTPRequestHandler.extensions_map, ".md": "text/markdown; charset=utf-8"}

    def log_message(self, *_args: object) -> None:
        return


def parse(path: Path) -> Page:
    parser = Page()
    parser.feed(path.read_text(encoding="utf-8"))
    return parser


def expected_urls(base: str, page_id: str, language: str) -> tuple[str, str, str]:
    prefix = f"{base.rstrip('/')}/docs" if language == "en" else f"{base.rstrip('/')}/docs/zh-TW"
    slug = "" if page_id == "home" else f"/{page_id}"
    html = f"{prefix}{slug}/"
    md = f"{prefix}/{page_id if page_id != 'home' else 'index'}.md"
    other_prefix = f"{base.rstrip('/')}/docs/zh-TW" if language == "en" else f"{base.rstrip('/')}/docs"
    other_html = f"{other_prefix}{slug}/"
    return html, md, other_html


def fetch(base_url: str, path: str) -> tuple[int, str, str]:
    url = urllib.parse.urljoin(base_url, path.lstrip("/"))
    try:
        with urllib.request.urlopen(url) as response:
            return response.status, response.headers.get("content-type", ""), response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        return error.code, error.headers.get("content-type", ""), error.read().decode("utf-8", "replace")


def check(root: Path, base: str, base_url: str | None = None) -> list[str]:
    failures: list[str] = []
    artifact_files = [path for path in root.rglob("*") if path.is_file()]
    highlighted_blocks = 0
    for path in artifact_files:
        relative = path.relative_to(root).as_posix()
        if any(denied in relative for denied in DENIED_PATHS):
            failures.append(f"denied artifact path: {relative}")
        content = path.read_text(encoding="utf-8", errors="ignore")
        if path.suffix == ".html":
            highlighted_blocks += content.count('data-topplecat-highlight="shiki"')
            if re.search(r'<pre><code class="language-[^"]+">', content):
                failures.append(f"unhighlighted fenced code block: {relative}")
        for marker in DENIED_MARKERS:
            if marker in content:
                failures.append(f"denied artifact marker {marker!r}: {relative}")
    if highlighted_blocks < 1:
        failures.append("artifact has no build-time Shiki syntax highlighting")

    if not base_url:
        base_url = ""

    def get(path: str) -> tuple[int, str, str]:
        if base_url:
            return fetch(base_url, path)
        local = root / path.lstrip("/")
        if local.is_dir():
            local = local / "index.html"
        if not local.is_file():
            return 404, "", ""
        content_type = "text/markdown; charset=utf-8" if local.suffix == ".md" else "text/html; charset=utf-8"
        return 200, content_type, local.read_text(encoding="utf-8", errors="replace")

    root_status, _, root_html = get(f"{base}/" if base else "/")
    if root_status != 200 or "See how it works" not in root_html or "Read the docs" not in root_html or "View on GitHub" not in root_html:
        failures.append("project page is missing one of the three required calls to action")

    for language, html_lang in (("en", "en"), ("zh-TW", "zh-Hant")):
        for page_id in PAGE_IDS:
            html_path, md_path, alternate = expected_urls(base, page_id, language)
            status, content_type, html = get(html_path)
            if status != 200:
                failures.append(f"missing HTML page: {html_path}")
                continue
            page = parse(root / html_path.lstrip("/") / "index.html") if not base_url else Page()
            if base_url:
                page.feed(html)
            language_code = "en" if language == "en" else "zh-TW"
            html_attrs = page.attrs.get("html", [{}])[0]
            if html_attrs.get("lang") != html_lang:
                failures.append(f"{html_path}: wrong html lang")
            if f'data-doc-page-id="{page_id}"' not in html or f'data-doc-language="{language_code}"' not in html:
                failures.append(f"{html_path}: missing page identity metadata")
            expected_canonical = f"{ORIGIN}{html_path}"
            if f'rel="canonical" href="{expected_canonical}"' not in html:
                failures.append(f"{html_path}: wrong self canonical")
            expected_alt = f"{ORIGIN}{alternate}"
            if f'hreflang="{("zh-TW" if language == "en" else "en")}" href="{expected_alt}"' not in html:
                failures.append(f"{html_path}: wrong paired hreflang")
            if not page.attrs.get("h1"):
                failures.append(f"{html_path}: missing visible heading")
            expected_homepage = f"{base}/" if base else "/"
            for logo_class in ("md-header__button", "md-nav__button"):
                logo_links = [
                    link
                    for link in page.links
                    if logo_class in link.get("class", "").split()
                    and "md-logo" in link.get("class", "").split()
                ]
                if len(logo_links) != 1 or logo_links[0].get("href") != expected_homepage:
                    failures.append(
                        f"{html_path}: {logo_class} brand logo does not link to product homepage"
                    )
            copy_href = f"data-copy-markdown=\"{('index.md' if page_id == 'home' else '../' + page_id + '.md')}\""
            if "topplecat-copy-button" not in html or copy_href not in html:
                failures.append(f"{html_path}: missing page-level Copy Markdown control")
            if any(term in html for term in ("Edit this page", "View on GitHub", "Search")):
                failures.append(f"{html_path}: forbidden article action or search UI present")
            anchors = {attrs.get("id") for attrs in page.attrs.get("h2", []) + page.attrs.get("h3", []) if attrs.get("id")}
            anchors.update(attrs.get("id") for tag in page.attrs.values() for attrs in tag if attrs.get("id"))
            for anchor in REQUIRED_ANCHORS[page_id]:
                if anchor not in anchors and f'id="{anchor}"' not in html:
                    failures.append(f"{html_path}: missing stable anchor #{anchor}")
            if page_id == "home":
                positions = [html.find(f'id="{anchor}"') for anchor in HOME_SECTION_ORDER]
                if any(position < 0 for position in positions) or positions != sorted(positions):
                    failures.append(f"{html_path}: first-visit sections are out of order")
            md_status, md_type, md = get(md_path)
            if md_status != 200 or "text/markdown" not in md_type or not md.startswith("---"):
                failures.append(f"{md_path}: missing clean Markdown sibling or content type")
            if md and ("docs/design" in md or "src/hiddenTest" in md):
                failures.append(f"{md_path}: Markdown sibling crosses public boundary")
            visible_text = "".join(page.text)
            if len(visible_text.strip()) < 220:
                failures.append(f"{html_path}: article is not readable without JavaScript")
            for link in page.links:
                href = link.get("href", "")
                if not href or href.startswith(("#", "mailto:", "tel:", "http:", "https:", "javascript:")):
                    continue
                link_path = urllib.parse.urljoin(html_path, href)
                if urllib.parse.urlsplit(link_path).path.endswith(".md"):
                    target_status, _, _ = get(link_path)
                else:
                    target_status, _, _ = get(link_path if link_path.endswith("/") else link_path + "/")
                if target_status != 200:
                    failures.append(f"{html_path}: dead ordinary link {href}")

    sitemap_status, _, sitemap = get(f"{base}/sitemap.xml" if base else "/sitemap.xml")
    sitemap_urls = re.findall(r"<loc>([^<]+)</loc>", sitemap)
    expected_sitemap = [f"{ORIGIN}{base}/"] + [f"{ORIGIN}{expected_urls(base, page, 'en')[0]}" for page in PAGE_IDS] + [f"{ORIGIN}{expected_urls(base, page, 'zh-TW')[0]}" for page in PAGE_IDS]
    if sitemap_status != 200 or sitemap_urls != expected_sitemap or any(url.endswith(".md") for url in sitemap_urls):
        failures.append("sitemap does not contain exactly the canonical HTML pages")
    robots_status, _, robots = get(f"{base}/robots.txt" if base else "/robots.txt")
    if robots_status != 200 or f"Sitemap: {ORIGIN}{base}/sitemap.xml" not in robots or "Allow: /" not in robots:
        failures.append("robots.txt is missing the ordinary allow and sitemap policy")
    root_llms_status, _, root_llms = get(f"{base}/llms.txt" if base else "/llms.txt")
    if root_llms_status != 200 or "docs/llms.txt" not in root_llms or "docs/zh-TW/llms.txt" not in root_llms or "llms-full" in root_llms:
        failures.append("root llms.txt is not the two-manifest index")
    for language, prefix in (("en", "docs"), ("zh-TW", "docs/zh-TW")):
        status, _, manifest = get(f"{base}/{prefix}/llms.txt" if base else f"/{prefix}/llms.txt")
        if status != 200 or "Experimental convenience" not in manifest or "llms-full" in manifest:
            failures.append(f"{prefix}/llms.txt is missing its experimental declaration")
        for page_id in PAGE_IDS:
            _, md_path, _ = expected_urls(base, page_id, language)
            if f"{ORIGIN}{md_path}" not in manifest:
                failures.append(f"{prefix}/llms.txt is missing {md_path}")

    css_files = list((root / "docs").rglob("*.css")) if (root / "docs").exists() else []
    js_files = list((root / "docs").rglob("*.js")) if (root / "docs").exists() else []
    css = "\n".join(path.read_text(encoding="utf-8", errors="ignore") for path in css_files)
    js = "\n".join(path.read_text(encoding="utf-8", errors="ignore") for path in js_files)
    if "prefers-reduced-motion" not in css or "navigator.clipboard" not in js or "fetch(" not in js:
        failures.append("artifact is missing reduced-motion or exact Markdown-copy behavior")
    return failures


def served_check(root: Path, base: str) -> list[str]:
    class BasePathHandler(MarkdownHandler):
        def translate_path(self, path: str) -> str:
            if base and (path == base or path.startswith(base + "/")):
                path = path[len(base):] or "/"
            return super().translate_path(path)

    handler = lambda *args, **kwargs: BasePathHandler(*args, directory=str(root), **kwargs)
    server = socketserver.ThreadingTCPServer(("127.0.0.1", 0), handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        return check(root, base, f"http://127.0.0.1:{server.server_address[1]}/")
    finally:
        server.shutdown()
        server.server_close()


def negative_tests(root: Path, base: str) -> list[str]:
    failures: list[str] = []
    scenarios = (
        ("missing translation", "docs/zh-TW/getting-started/index.html", lambda p: p.unlink()),
        ("removed anchor", "docs/architecture/index.html", lambda p: p.write_text(p.read_text(encoding="utf-8").replace('id="four-modules"', 'id="removed"'), encoding="utf-8")),
        (
            "wrong brand homepage",
            "docs/index.html",
            lambda p: p.write_text(
                p.read_text(encoding="utf-8").replace(
                    'href="/" title="ToppleCat documentation" class="md-header__button md-logo"',
                    'href="./" title="ToppleCat documentation" class="md-header__button md-logo"',
                    1,
                ),
                encoding="utf-8",
            ),
        ),
        ("broken manifest target", "docs/llms.txt", lambda p: p.write_text(p.read_text(encoding="utf-8") + "\n- [broken](https://topplecat.samzhu.dev/docs/missing.md)\n", encoding="utf-8")),
        ("denied marker", "docs/index.md", lambda p: p.write_text(p.read_text(encoding="utf-8") + "\nReviewerBoundary\n", encoding="utf-8")),
    )
    for name, relative, mutate in scenarios:
        with tempfile.TemporaryDirectory(prefix="topplecat-artifact-test-") as directory:
            candidate = Path(directory) / "artifact"
            shutil.copytree(root, candidate)
            target = candidate / relative
            if target.exists():
                mutate(target)
            if not check(candidate, base):
                failures.append(f"negative test did not reject {name}")
    return failures


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", default=None)
    parser.add_argument("--base-path", default="")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    root = Path(args.artifact) if args.artifact else Path(__file__).resolve().parents[1] / "site/dist"
    if not root.is_dir():
        print(f"artifact does not exist: {root}")
        return 1
    base = args.base_path.strip()
    if base and not base.startswith("/"):
        base = "/" + base
    failures = served_check(root, base)
    if args.self_test:
        failures.extend(negative_tests(root, base))
    if failures:
        print("Site artifact validation failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print(f"Site artifact validation PASS: {len(PAGE_IDS) * 2} bilingual pages, served from {root}")
    if args.self_test:
        print("Site artifact negative tests PASS: missing pair, anchor, brand homepage, manifest target, and denied marker rejected")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
