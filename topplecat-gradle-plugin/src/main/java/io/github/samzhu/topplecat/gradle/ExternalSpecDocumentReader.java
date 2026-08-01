package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.ToppleCatException;
import io.github.samzhu.topplecat.report.ReviewAcLocation;
import io.github.samzhu.topplecat.report.ReviewDocument;
import io.github.samzhu.topplecat.report.ReviewDocumentAsset;
import io.github.samzhu.topplecat.report.SpecMarkdownBlock;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Reads complete selected Markdown documents into a safe, reviewer-only document projection. */
final class ExternalSpecDocumentReader {
  private static final Pattern AC_ID =
      Pattern.compile("(?<![A-Za-z0-9_-])(AC-[A-Za-z0-9][A-Za-z0-9-]*)(?![A-Za-z0-9_-])");
  private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*\\s*$");
  private static final Pattern UNORDERED_ITEM = Pattern.compile("^\\s*[-*+]\\s+(.+?)\\s*$");
  private static final Pattern ORDERED_ITEM = Pattern.compile("^\\s*\\d+[.)]\\s+(.+?)\\s*$");
  private static final Pattern TASK_ITEM =
      Pattern.compile("^\\s*[-*+]\\s+\\[([ xX])]\\s+(.+?)\\s*$");
  private static final Pattern QUOTE = Pattern.compile("^\\s*>\\s?(.*)$");
  private static final Pattern RULE = Pattern.compile("^\\s{0,3}([-*_])(?:\\s*\\1){2,}\\s*$");
  private static final Pattern FENCE = Pattern.compile("^\\s*```\\s*([^\\s`]*)\\s*$");
  private static final Pattern IMAGE =
      Pattern.compile("^\\s*!\\[([^]]*)]\\(([^\\s)]+)(?:\\s+\\\"([^\\\"]*)\\\")?\\)\\s*$");

  private ExternalSpecDocumentReader() {}

  static ParsedSpecs read(Path projectRoot, Collection<Path> configuredEntries) {
    if (configuredEntries.isEmpty()) {
      return ParsedSpecs.empty();
    }
    Path root = projectRoot.toAbsolutePath().normalize();
    List<ReviewDocument> documents = new ArrayList<>();
    Map<String, ReviewAcLocation> locations = new LinkedHashMap<>();
    for (Path document : markdownDocuments(configuredEntries)) {
      ParsedDocument parsed = parseDocument(root, document);
      documents.add(parsed.document());
      parsed.locations().forEach(locations::putIfAbsent);
    }
    return new ParsedSpecs(documents, locations, true);
  }

  static List<Path> markdownDocuments(Collection<Path> configuredEntries) {
    Set<Path> documents = new LinkedHashSet<>();
    for (Path entry : configuredEntries) {
      if (!Files.exists(entry)) {
        throw new ToppleCatException(
            "Configured ToppleCat specDocs entry does not exist: "
                + entry
                + ". Create the file or directory, or remove it from toppleCat.specDocs.");
      }
      if (Files.isDirectory(entry)) {
        try (Stream<Path> paths = Files.walk(entry)) {
          paths
              .filter(Files::isRegularFile)
              .filter(ExternalSpecDocumentReader::isMarkdown)
              .sorted()
              .forEach(documents::add);
        } catch (IOException exception) {
          throw new ToppleCatException(
              "Cannot read ToppleCat specDocs directory " + entry + ": " + exception.getMessage(),
              exception);
        }
      } else if (isMarkdown(entry)) {
        documents.add(entry);
      } else {
        throw new ToppleCatException(
            "ToppleCat specDocs entry "
                + entry
                + " is not a Markdown file. Use a .md file or a directory containing .md files.");
      }
    }
    return documents.stream().sorted().toList();
  }

  private static ParsedDocument parseDocument(Path root, Path document) {
    List<String> lines;
    byte[] bytes;
    try {
      lines = Files.readAllLines(document);
      bytes = Files.readAllBytes(document);
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot read ToppleCat Spec document " + document + ": " + exception.getMessage(),
          exception);
    }
    String displayPath = displayPath(root, document);
    AssetCollector assets = new AssetCollector(root, document);
    List<SpecMarkdownBlock> blocks = blocks(lines, assets);
    Map<String, ReviewAcLocation> locations = new LinkedHashMap<>();
    for (int index = 0; index < blocks.size(); index++) {
      if (blocks.get(index).kind() == SpecMarkdownBlock.Kind.CODE_FENCE
          || blocks.get(index).kind() == SpecMarkdownBlock.Kind.MERMAID
          || blocks.get(index).kind() == SpecMarkdownBlock.Kind.FALLBACK) {
        continue;
      }
      String text = blockText(blocks.get(index));
      Matcher matcher = AC_ID.matcher(text);
      while (matcher.find()) {
        locations.putIfAbsent(matcher.group(1), new ReviewAcLocation(displayPath, index + 1));
      }
    }
    return new ParsedDocument(
        new ReviewDocument(displayPath, Hashing.sha256(bytes), blocks, assets.assets()), locations);
  }

  private static List<SpecMarkdownBlock> blocks(List<String> lines, AssetCollector assets) {
    List<SpecMarkdownBlock> result = new ArrayList<>();
    List<String> paragraph = new ArrayList<>();
    List<String> list = new ArrayList<>();
    SpecMarkdownBlock.Kind listKind = null;
    for (int index = 0; index < lines.size(); ) {
      String line = lines.get(index);
      Matcher fence = FENCE.matcher(line);
      if (fence.matches()) {
        flush(result, paragraph, list, listKind);
        list.clear();
        listKind = null;
        String language = fence.group(1).toLowerCase(Locale.ROOT);
        StringBuilder source = new StringBuilder();
        index++;
        while (index < lines.size() && !FENCE.matcher(lines.get(index)).matches()) {
          if (!source.isEmpty()) {
            source.append('\n');
          }
          source.append(lines.get(index++));
        }
        if (index < lines.size()) {
          index++;
          result.add(
              block(
                  language.equals("mermaid")
                      ? SpecMarkdownBlock.Kind.MERMAID
                      : SpecMarkdownBlock.Kind.CODE_FENCE,
                  0,
                  source.toString(),
                  List.of(),
                  language,
                  "",
                  "",
                  List.of(),
                  List.of()));
        } else {
          result.add(
              block(
                  SpecMarkdownBlock.Kind.FALLBACK,
                  0,
                  "Unclosed fenced code block:\n" + source,
                  List.of(),
                  language,
                  "",
                  "",
                  List.of(),
                  List.of()));
        }
        continue;
      }
      Matcher heading = HEADING.matcher(line);
      if (heading.matches()) {
        flush(result, paragraph, list, listKind);
        list.clear();
        listKind = null;
        result.add(
            block(
                SpecMarkdownBlock.Kind.HEADING,
                heading.group(1).length(),
                heading.group(2),
                List.of(),
                "",
                "",
                "",
                List.of(),
                List.of()));
        index++;
        continue;
      }
      if (RULE.matcher(line).matches()) {
        flush(result, paragraph, list, listKind);
        list.clear();
        listKind = null;
        result.add(
            block(
                SpecMarkdownBlock.Kind.HORIZONTAL_RULE,
                0,
                "",
                List.of(),
                "",
                "",
                "",
                List.of(),
                List.of()));
        index++;
        continue;
      }
      Matcher image = IMAGE.matcher(line);
      if (image.matches()) {
        flush(result, paragraph, list, listKind);
        list.clear();
        listKind = null;
        AssetResolution resolution = assets.resolve(image.group(2));
        result.add(
            block(
                SpecMarkdownBlock.Kind.IMAGE,
                0,
                image.group(1),
                List.of(),
                "",
                resolution.destination(),
                image.group(3) == null ? resolution.message() : image.group(3),
                List.of(),
                List.of()));
        index++;
        continue;
      }
      if (isTableHeader(lines, index)) {
        flush(result, paragraph, list, listKind);
        list.clear();
        listKind = null;
        List<String> headers = splitTable(line);
        List<List<String>> rows = new ArrayList<>();
        index += 2;
        while (index < lines.size()
            && lines.get(index).contains("|")
            && !lines.get(index).isBlank()) {
          rows.add(splitTable(lines.get(index++)));
        }
        result.add(
            block(SpecMarkdownBlock.Kind.TABLE, 0, "", List.of(), "", "", "", headers, rows));
        continue;
      }
      Matcher quote = QUOTE.matcher(line);
      if (quote.matches()) {
        flush(result, paragraph, list, listKind);
        list.clear();
        listKind = null;
        List<String> quoteLines = new ArrayList<>();
        while (index < lines.size() && (quote = QUOTE.matcher(lines.get(index))).matches()) {
          quoteLines.add(quote.group(1));
          index++;
        }
        result.add(
            block(
                SpecMarkdownBlock.Kind.BLOCK_QUOTE,
                0,
                String.join("\n", quoteLines),
                List.of(),
                "",
                "",
                "",
                List.of(),
                List.of()));
        continue;
      }
      Matcher task = TASK_ITEM.matcher(line);
      Matcher unordered = UNORDERED_ITEM.matcher(line);
      Matcher ordered = ORDERED_ITEM.matcher(line);
      if (task.matches() || unordered.matches() || ordered.matches()) {
        SpecMarkdownBlock.Kind nextKind =
            task.matches()
                ? SpecMarkdownBlock.Kind.TASK_LIST
                : unordered.matches()
                    ? SpecMarkdownBlock.Kind.LIST
                    : SpecMarkdownBlock.Kind.ORDERED_LIST;
        if (listKind != null && listKind != nextKind) {
          flush(result, paragraph, list, listKind);
          list.clear();
        }
        flushParagraph(result, paragraph);
        listKind = nextKind;
        list.add(
            task.matches()
                ? "[" + task.group(1).toLowerCase(Locale.ROOT) + "] " + task.group(2)
                : unordered.matches() ? unordered.group(1) : ordered.group(1));
        index++;
        continue;
      }
      if (line.isBlank()) {
        flush(result, paragraph, list, listKind);
        list.clear();
        listKind = null;
        index++;
        continue;
      }
      if (line.stripLeading().startsWith("<")) {
        flush(result, paragraph, list, listKind);
        list.clear();
        listKind = null;
        result.add(
            block(
                SpecMarkdownBlock.Kind.FALLBACK,
                0,
                line,
                List.of(),
                "",
                "",
                "",
                List.of(),
                List.of()));
      } else {
        flushList(result, list, listKind);
        list.clear();
        listKind = null;
        paragraph.add(line.trim());
      }
      index++;
    }
    flush(result, paragraph, list, listKind);
    return List.copyOf(result);
  }

  private static SpecMarkdownBlock block(
      SpecMarkdownBlock.Kind kind,
      int headingLevel,
      String text,
      List<String> items,
      String language,
      String destination,
      String title,
      List<String> tableHeaders,
      List<List<String>> tableRows) {
    String anchor = firstAc(text);
    if (anchor.isBlank()) {
      for (String item : items) {
        anchor = firstAc(item);
        if (!anchor.isBlank()) {
          break;
        }
      }
    }
    return new SpecMarkdownBlock(
        kind,
        headingLevel,
        text,
        items,
        language,
        destination,
        title,
        tableHeaders,
        tableRows,
        anchor);
  }

  private static void flush(
      List<SpecMarkdownBlock> result,
      List<String> paragraph,
      List<String> list,
      SpecMarkdownBlock.Kind listKind) {
    flushParagraph(result, paragraph);
    flushList(result, list, listKind);
  }

  private static void flushParagraph(List<SpecMarkdownBlock> result, List<String> paragraph) {
    if (!paragraph.isEmpty()) {
      result.add(
          block(
              SpecMarkdownBlock.Kind.PARAGRAPH,
              0,
              String.join(" ", paragraph),
              List.of(),
              "",
              "",
              "",
              List.of(),
              List.of()));
      paragraph.clear();
    }
  }

  private static void flushList(
      List<SpecMarkdownBlock> result, List<String> list, SpecMarkdownBlock.Kind listKind) {
    if (!list.isEmpty()) {
      result.add(
          block(
              listKind == null ? SpecMarkdownBlock.Kind.LIST : listKind,
              0,
              "",
              List.copyOf(list),
              "",
              "",
              "",
              List.of(),
              List.of()));
    }
  }

  private static boolean isTableHeader(List<String> lines, int index) {
    return index + 1 < lines.size()
        && lines.get(index).contains("|")
        && lines
            .get(index + 1)
            .matches("^\\s*\\|?\\s*:?-{3,}:?\\s*(?:\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$");
  }

  private static List<String> splitTable(String line) {
    String trimmed = line.trim();
    if (trimmed.startsWith("|")) {
      trimmed = trimmed.substring(1);
    }
    if (trimmed.endsWith("|")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return java.util.Arrays.stream(trimmed.split("\\|", -1)).map(String::trim).toList();
  }

  private static String blockText(SpecMarkdownBlock block) {
    if (!block.text().isBlank()) {
      return block.text();
    }
    return String.join(" ", block.items());
  }

  private static String firstAc(String value) {
    Matcher matcher = AC_ID.matcher(value == null ? "" : value);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static boolean isMarkdown(Path path) {
    return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md");
  }

  private static String displayPath(Path projectRoot, Path document) {
    Path root = projectRoot.toAbsolutePath().normalize();
    Path source = document.toAbsolutePath().normalize();
    return root.relativize(source).toString().replace('\\', '/');
  }

  record ParsedSpecs(
      List<ReviewDocument> documents, Map<String, ReviewAcLocation> locations, boolean configured) {
    ParsedSpecs {
      documents = List.copyOf(documents == null ? List.of() : documents);
      locations = Map.copyOf(new LinkedHashMap<>(locations == null ? Map.of() : locations));
    }

    static ParsedSpecs empty() {
      return new ParsedSpecs(List.of(), Map.of(), false);
    }

    List<String> acceptanceConditionIds() {
      return locations.keySet().stream().sorted().toList();
    }
  }

  private record ParsedDocument(ReviewDocument document, Map<String, ReviewAcLocation> locations) {}

  private record AssetResolution(String destination, String message) {}

  private static final class AssetCollector {
    private final Path root;
    private final Path realRoot;
    private final Path document;
    private final Map<String, ReviewDocumentAsset> assets = new LinkedHashMap<>();

    private AssetCollector(Path root, Path document) {
      this.root = root;
      this.document = document;
      try {
        this.realRoot = root.toRealPath();
      } catch (IOException exception) {
        throw new ToppleCatException(
            "Cannot resolve ToppleCat repository root for Spec assets.", exception);
      }
    }

    private AssetResolution resolve(String rawDestination) {
      if (rawDestination == null || rawDestination.isBlank()) {
        return new AssetResolution("", "Image reference is missing.");
      }
      String destination = rawDestination.replace('\\', '/');
      if (destination.matches("(?i)https?://.+")) {
        return new AssetResolution(
            destination, "Remote image is not downloaded into this offline report.");
      }
      if (destination.matches("(?i)[a-z][a-z0-9+.-]*:.*")
          || destination.startsWith("/")
          || destination.contains("..")) {
        return new AssetResolution(
            "", "Image reference is outside the selected repository boundary.");
      }
      Path candidate = document.getParent().resolve(destination).normalize();
      if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
        return new AssetResolution("", "Image could not be read from the selected repository.");
      }
      try {
        Path real = candidate.toRealPath();
        if (!real.startsWith(realRoot)) {
          return new AssetResolution(
              "", "Image reference escaped the selected repository through a symlink.");
        }
        String extension = extension(candidate);
        String mediaType = mediaType(extension);
        if (mediaType.isBlank()) {
          return new AssetResolution(
              "", "Image format is not supported in the safe offline report.");
        }
        String digest = Hashing.sha256(Files.readAllBytes(real));
        String bundlePath = "assets/spec/" + digest + "." + extension;
        String sourcePath = root.relativize(candidate).toString().replace('\\', '/');
        assets.putIfAbsent(sourcePath, new ReviewDocumentAsset(sourcePath, bundlePath, mediaType));
        return new AssetResolution(bundlePath, "");
      } catch (IOException exception) {
        return new AssetResolution("", "Image could not be read from the selected repository.");
      }
    }

    private List<ReviewDocumentAsset> assets() {
      return assets.values().stream()
          .sorted(java.util.Comparator.comparing(ReviewDocumentAsset::bundlePath))
          .toList();
    }

    private static String extension(Path path) {
      String name = path.getFileName().toString();
      int dot = name.lastIndexOf('.');
      return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String mediaType(String extension) {
      return switch (extension) {
        case "png" -> "image/png";
        case "jpg", "jpeg" -> "image/jpeg";
        case "gif" -> "image/gif";
        case "webp" -> "image/webp";
        default -> "";
      };
    }
  }
}
