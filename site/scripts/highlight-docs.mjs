import { readdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { codeToHtml } from "shiki";

const siteRoot = fileURLToPath(new URL("..", import.meta.url));
const docsRoot = join(siteRoot, "dist", "docs");
const codeBlock = /<pre><code class="language-([^"]+)">([\s\S]*?)<\/code><\/pre>/g;

const toppleCatTheme = {
  name: "topplecat-dark",
  type: "dark",
  colors: {
    "editor.background": "#062e2b",
    "editor.foreground": "#f8f0e4",
  },
  tokenColors: [
    {
      scope: ["comment", "punctuation.definition.comment"],
      settings: { foreground: "#a9bdb1", fontStyle: "italic" },
    },
    {
      scope: ["string", "string.quoted"],
      settings: { foreground: "#f3bd69" },
    },
    {
      scope: ["constant.numeric", "constant.language", "constant.character"],
      settings: { foreground: "#f3cf96" },
    },
    {
      scope: ["keyword", "storage.type", "storage.modifier"],
      settings: { foreground: "#f08a49" },
    },
    {
      scope: ["entity.name.function", "support.function"],
      settings: { foreground: "#83d6c7" },
    },
    {
      scope: ["entity.name.type", "entity.name.class", "support.type"],
      settings: { foreground: "#b8dac9" },
    },
    {
      scope: ["variable", "variable.other"],
      settings: { foreground: "#f8f0e4" },
    },
    {
      scope: ["punctuation", "meta.brace"],
      settings: { foreground: "#c9d8cf" },
    },
  ],
};

function decodeHtml(value) {
  return value
    .replace(/&#x([0-9a-f]+);/gi, (_match, code) => String.fromCodePoint(Number.parseInt(code, 16)))
    .replace(/&#([0-9]+);/g, (_match, code) => String.fromCodePoint(Number.parseInt(code, 10)))
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&amp;/g, "&");
}

async function htmlFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) files.push(...await htmlFiles(path));
    else if (entry.isFile() && entry.name.endsWith(".html")) files.push(path);
  }
  return files;
}

async function highlightFile(path) {
  const source = await readFile(path, "utf8");
  const matches = [...source.matchAll(codeBlock)];
  if (!matches.length) return 0;

  let output = "";
  let offset = 0;
  for (const match of matches) {
    output += source.slice(offset, match.index);
    const language = match[1];
    const highlighted = await codeToHtml(decodeHtml(match[2]), {
      lang: language,
      theme: toppleCatTheme,
    });
    output += highlighted
      .replace('<pre class="shiki ', '<pre data-topplecat-highlight="shiki" class="shiki ')
      .replace("<code>", `<code class="language-${language}">`);
    offset = match.index + match[0].length;
  }
  output += source.slice(offset);
  await writeFile(path, output, "utf8");
  return matches.length;
}

let highlightedBlocks = 0;
for (const path of await htmlFiles(docsRoot)) highlightedBlocks += await highlightFile(path);
if (!highlightedBlocks) throw new Error("No documentation code blocks were highlighted");
console.log(`Highlighted ${highlightedBlocks} documentation code blocks with Shiki`);
