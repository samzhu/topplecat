package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.ToppleCatException;
import io.github.samzhu.topplecat.report.SpecMarkdownBlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Reads the intentionally small, safe Markdown subset used as public spec context. */
final class ExternalSpecDocumentReader {
    private static final Pattern AC_ID = Pattern.compile("(?<![A-Za-z0-9_-])(AC-[A-Za-z0-9][A-Za-z0-9-]*)(?![A-Za-z0-9_-])");
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*(?:[-*+]\\s+|\\d+[.)]\\s+)(.+?)\\s*$");

    private ExternalSpecDocumentReader() {
    }

    static ParsedSpecs read(Path projectRoot, Collection<Path> configuredEntries) {
        if (configuredEntries.isEmpty()) {
            return ParsedSpecs.empty();
        }
        List<Path> documents = markdownDocuments(configuredEntries);
        Map<String, List<SpecMarkdownBlock>> narratives = new LinkedHashMap<>();
        Map<String, List<String>> sources = new LinkedHashMap<>();
        for (Path document : documents) {
            DocumentAnchors anchors = parseDocument(document);
            for (Map.Entry<String, List<SpecMarkdownBlock>> entry : anchors.narratives().entrySet()) {
                narratives.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).addAll(entry.getValue());
                sources.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(displayPath(projectRoot, document));
            }
        }
        return new ParsedSpecs(narratives, sources, true);
    }

    static List<Path> markdownDocuments(Collection<Path> configuredEntries) {
        Set<Path> documents = new LinkedHashSet<>();
        for (Path entry : configuredEntries) {
            if (!Files.exists(entry)) {
                throw new ToppleCatException("Configured ToppleCat specDocs entry does not exist: " + entry
                        + ". Create the file or directory, or remove it from toppleCat.specDocs.");
            }
            if (Files.isDirectory(entry)) {
                try (Stream<Path> paths = Files.walk(entry)) {
                    paths.filter(Files::isRegularFile).filter(ExternalSpecDocumentReader::isMarkdown)
                            .sorted().forEach(documents::add);
                } catch (IOException exception) {
                    throw new ToppleCatException("Cannot read ToppleCat specDocs directory " + entry + ": "
                            + exception.getMessage(), exception);
                }
            } else if (isMarkdown(entry)) {
                documents.add(entry);
            } else {
                throw new ToppleCatException("ToppleCat specDocs entry " + entry
                        + " is not a Markdown file. Use a .md file or a directory containing .md files.");
            }
        }
        return documents.stream().sorted().toList();
    }

    private static boolean isMarkdown(Path path) {
        return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md");
    }

    private static DocumentAnchors parseDocument(Path document) {
        List<String> lines;
        try {
            lines = Files.readAllLines(document);
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot read ToppleCat spec document " + document + ": "
                    + exception.getMessage(), exception);
        }
        List<Anchor> anchors = new ArrayList<>();
        Heading currentHeading = null;
        boolean fencedCode = false;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (line.stripLeading().startsWith("```")) {
                fencedCode = !fencedCode;
                continue;
            }
            if (fencedCode) {
                continue;
            }
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                currentHeading = new Heading(lineIndex, heading.group(1).length());
                addAnchors(anchors, heading.group(2), currentHeading.line(), currentHeading.level());
                continue;
            }
            if (!line.isBlank() && !LIST_ITEM.matcher(line).matches()) {
                int start = currentHeading == null ? lineIndex : currentHeading.line();
                int level = currentHeading == null ? 1 : currentHeading.level();
                addAnchors(anchors, line, start, level);
            }
        }
        Map<String, List<SpecMarkdownBlock>> narratives = new LinkedHashMap<>();
        for (Anchor anchor : anchors) {
            int end = sectionEnd(lines, anchor.start(), anchor.level());
            narratives.computeIfAbsent(anchor.acId(), ignored -> new ArrayList<>())
                    .addAll(blocks(lines.subList(anchor.start(), end)));
        }
        return new DocumentAnchors(narratives);
    }

    private static void addAnchors(List<Anchor> anchors, String text, int start, int level) {
        Matcher matcher = AC_ID.matcher(text);
        while (matcher.find()) {
            anchors.add(new Anchor(matcher.group(1), start, level));
        }
    }

    private static int sectionEnd(List<String> lines, int start, int level) {
        boolean fencedCode = false;
        for (int index = start + 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.stripLeading().startsWith("```")) {
                fencedCode = !fencedCode;
                continue;
            }
            if (!fencedCode) {
                Matcher heading = HEADING.matcher(line);
                if (heading.matches() && heading.group(1).length() <= level) {
                    return index;
                }
            }
        }
        return lines.size();
    }

    private static List<SpecMarkdownBlock> blocks(List<String> lines) {
        List<SpecMarkdownBlock> blocks = new ArrayList<>();
        List<String> paragraph = new ArrayList<>();
        List<String> list = new ArrayList<>();
        boolean fencedCode = false;
        for (String line : lines) {
            if (line.stripLeading().startsWith("```")) {
                flush(blocks, paragraph, list);
                fencedCode = !fencedCode;
                continue;
            }
            if (fencedCode) {
                continue;
            }
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                flush(blocks, paragraph, list);
                blocks.add(new SpecMarkdownBlock(SpecMarkdownBlock.Kind.HEADING, heading.group(1).length(),
                        heading.group(2), List.of()));
                continue;
            }
            Matcher item = LIST_ITEM.matcher(line);
            if (item.matches()) {
                flushParagraph(blocks, paragraph);
                list.add(item.group(1));
                continue;
            }
            if (line.isBlank()) {
                flush(blocks, paragraph, list);
                continue;
            }
            flushList(blocks, list);
            paragraph.add(line.trim());
        }
        flush(blocks, paragraph, list);
        return List.copyOf(blocks);
    }

    private static void flush(List<SpecMarkdownBlock> blocks, List<String> paragraph, List<String> list) {
        flushParagraph(blocks, paragraph);
        flushList(blocks, list);
    }

    private static void flushParagraph(List<SpecMarkdownBlock> blocks, List<String> paragraph) {
        if (!paragraph.isEmpty()) {
            blocks.add(new SpecMarkdownBlock(SpecMarkdownBlock.Kind.PARAGRAPH, 0, String.join(" ", paragraph), List.of()));
            paragraph.clear();
        }
    }

    private static void flushList(List<SpecMarkdownBlock> blocks, List<String> list) {
        if (!list.isEmpty()) {
            blocks.add(new SpecMarkdownBlock(SpecMarkdownBlock.Kind.LIST, 0, "", List.copyOf(list)));
            list.clear();
        }
    }

    private static String displayPath(Path projectRoot, Path document) {
        Path root = projectRoot.toAbsolutePath().normalize();
        Path source = document.toAbsolutePath().normalize();
        return source.startsWith(root) ? root.relativize(source).toString() : source.toString();
    }

    record ParsedSpecs(Map<String, List<SpecMarkdownBlock>> narratives, Map<String, List<String>> sources,
                       boolean configured) {
        ParsedSpecs {
            narratives = immutableLists(narratives);
            sources = immutableLists(sources);
        }

        static ParsedSpecs empty() {
            return new ParsedSpecs(Map.of(), Map.of(), false);
        }

        private static <T> Map<String, List<T>> immutableLists(Map<String, List<T>> source) {
            Map<String, List<T>> copy = new LinkedHashMap<>();
            source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
            return Map.copyOf(copy);
        }
    }

    private record Heading(int line, int level) {
    }

    private record Anchor(String acId, int start, int level) {
    }

    private record DocumentAnchors(Map<String, List<SpecMarkdownBlock>> narratives) {
    }
}
