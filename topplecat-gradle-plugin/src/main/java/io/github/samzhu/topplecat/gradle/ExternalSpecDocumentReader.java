package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.ToppleCatException;
import io.github.samzhu.topplecat.report.ReviewAcLocation;
import io.github.samzhu.topplecat.report.ReviewDocument;
import io.github.samzhu.topplecat.report.ReviewDocumentAsset;
import io.github.samzhu.topplecat.report.SpecMarkdownBlock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Reads and validates complete selected Markdown documents into one checked projection. */
final class ExternalSpecDocumentReader {
  static final String ACCEPTANCE_MARKER = "<!-- topplecat:acceptance -->";
  private static final Pattern AC_ID =
      Pattern.compile("(?<![A-Za-z0-9_-])(AC-[A-Za-z0-9][A-Za-z0-9-]*)(?![A-Za-z0-9_-])");
  private static final Pattern AC_DECLARATION =
      Pattern.compile("^(AC-[A-Za-z0-9][A-Za-z0-9-]*)[：:][ \\t]+(\\S(?:.*\\S)?)$");

  private ExternalSpecDocumentReader() {}

  static ParsedSpecs read(Path projectRoot, Collection<Path> configuredEntries) {
    if (configuredEntries.isEmpty()) {
      return ParsedSpecs.empty();
    }
    Path root = projectRoot.toAbsolutePath().normalize();
    List<ReviewDocument> documents = new ArrayList<>();
    Map<String, ReviewAcLocation> locations = new LinkedHashMap<>();
    Map<String, String> digests = new LinkedHashMap<>();
    Map<String, List<Declaration>> declarations = new LinkedHashMap<>();
    List<Reference> references = new ArrayList<>();
    List<SelectedSpecDiagnostic> diagnostics = new ArrayList<>();
    for (Path document : markdownDocuments(configuredEntries)) {
      rejectSymbolicPathComponent(root, document, displayPath(root, document));
      ParsedDocument parsed = parseDocument(root, document);
      documents.add(parsed.document());
      digests.put(parsed.document().path(), parsed.document().sha256());
      diagnostics.addAll(parsed.diagnostics());
      references.addAll(parsed.references());
      parsed
          .declarations()
          .forEach(
              declaration ->
                  declarations
                      .computeIfAbsent(declaration.acId(), ignored -> new ArrayList<>())
                      .add(declaration));
      parsed.locations().forEach(locations::putIfAbsent);
    }
    declarations.forEach(
        (acId, entries) -> {
          if (entries.size() > 1) {
            String allLocations =
                entries.stream()
                    .map(Declaration::location)
                    .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            for (Declaration entry : entries) {
              diagnostics.add(
                  diagnostic(
                      "TC-SPEC-AC-DUPLICATE",
                      entry.path(),
                      entry.line(),
                      entry.column(),
                      acId,
                      "The AC is declared more than once at " + allLocations + ".",
                      "Keep exactly one canonical heading and acceptance marker for this AC.",
                      "Remove the duplicate declaration from the owning canonical Spec."));
            }
            locations.remove(acId);
          }
        });
    Map<String, List<Declaration>> validDeclarations = declarations;
    references.forEach(
        reference -> {
          List<Declaration> entries = validDeclarations.getOrDefault(reference.acId(), List.of());
          if (entries.isEmpty()) {
            diagnostics.add(
                diagnostic(
                    "TC-SPEC-AC-DECLARATION-MISSING",
                    reference.path(),
                    reference.line(),
                    reference.column(),
                    reference.acId(),
                    "The selected Spec contains an ordinary reference to "
                        + reference.acId()
                        + " but no valid declaration selects it across the selected documents.",
                    "Declare the AC in one visible heading and place one exact standalone"
                        + " acceptance marker after its rules.",
                    "Repair the canonical Spec rather than relying on a bare AC reference."));
          }
        });
    return new ParsedSpecs(documents, locations, digests, diagnostics, true);
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
      if (Files.isSymbolicLink(entry)) {
        throw new ToppleCatException(
            "ToppleCat selected Spec must not be a symbolic link: " + entry);
      }
      if (Files.isDirectory(entry)) {
        try (Stream<Path> paths = Files.walk(entry)) {
          paths
              .filter(Files::isRegularFile)
              .filter(path -> !Files.isSymbolicLink(path))
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
    byte[] bytes;
    try {
      bytes = Files.readAllBytes(document);
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot read ToppleCat Spec document " + document + ": " + exception.getMessage(),
          exception);
    }
    String source = new String(bytes, StandardCharsets.UTF_8);
    String displayPath = displayPath(root, document);
    AssetCollector assets = new AssetCollector(root, document);
    CanonicalMarkdownStructure.Parsed markdown =
        CanonicalMarkdownStructure.parse(
            source,
            destination -> {
              AssetResolution resolution = assets.resolve(destination);
              return new CanonicalMarkdownStructure.ResolvedImage(
                  resolution.destination(), resolution.message());
            });
    Structure structure = structure(displayPath, lines(source), markdown.events());
    List<SpecMarkdownBlock> rawBlocks = markdown.blocks();
    List<SpecMarkdownBlock> projectedBlocks = new ArrayList<>(rawBlocks);
    List<Integer> markerBlocks = new ArrayList<>();
    for (int index = 0; index < rawBlocks.size(); index++) {
      if (rawBlocks.get(index).kind() == SpecMarkdownBlock.Kind.ACCEPTANCE_MARKER) {
        markerBlocks.add(index);
      }
    }
    for (int index = 0;
        index < structure.pairedIds().size() && index < markerBlocks.size();
        index++) {
      int blockIndex = markerBlocks.get(index);
      SpecMarkdownBlock marker = projectedBlocks.get(blockIndex);
      projectedBlocks.set(
          blockIndex,
          new SpecMarkdownBlock(
              marker.kind(),
              marker.headingLevel(),
              marker.text(),
              marker.items(),
              marker.language(),
              marker.destination(),
              marker.title(),
              marker.tableHeaders(),
              marker.tableRows(),
              structure.pairedIds().get(index),
              marker.children()));
    }
    Map<String, ReviewAcLocation> locations = new LinkedHashMap<>();
    for (int index = 0;
        index < structure.pairedIds().size() && index < markerBlocks.size();
        index++) {
      String acId = structure.pairedIds().get(index);
      int blockIndex = markerBlocks.get(index);
      locations.putIfAbsent(
          acId,
          new ReviewAcLocation(
              displayPath, blockIndex + 1, displayPath + "#acceptance-" + blockIndex));
    }
    return new ParsedDocument(
        new ReviewDocument(displayPath, Hashing.sha256(bytes), projectedBlocks, assets.assets()),
        locations,
        structure.declarations(),
        structure.references(),
        structure.diagnostics());
  }

  private static void rejectSymbolicPathComponent(Path root, Path candidate, String suppliedPath) {
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path absoluteCandidate = candidate.toAbsolutePath().normalize();
    if (!absoluteCandidate.startsWith(normalizedRoot)) {
      throw new ToppleCatException(
          "ToppleCat selected Spec must stay inside the repository root: " + suppliedPath);
    }
    Path current = normalizedRoot;
    Path relative = normalizedRoot.relativize(absoluteCandidate);
    for (Path component : relative) {
      current = current.resolve(component);
      if (Files.isSymbolicLink(current)) {
        throw new ToppleCatException(
            "ToppleCat selected Spec must not follow a symbolic link component ("
                + current
                + "): "
                + suppliedPath);
      }
    }
  }

  private static List<String> lines(String source) {
    String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
    String[] split = normalized.split("\\n", -1);
    if (split.length > 1 && split[split.length - 1].isEmpty()) {
      return List.of(split).subList(0, split.length - 1);
    }
    return List.of(split);
  }

  private static Structure structure(
      String path, List<String> lines, List<CanonicalMarkdownStructure.Event> events) {
    List<SelectedSpecDiagnostic> diagnostics = new ArrayList<>();
    List<Declaration> declarations = new ArrayList<>();
    List<String> pairedIds = new ArrayList<>();
    Declaration open = null;
    Map<String, MarkerLocation> completed = new LinkedHashMap<>();
    for (CanonicalMarkdownStructure.Event event : events) {
      if (event.kind() == CanonicalMarkdownStructure.EventKind.HEADING) {
        List<String> ids = ids(event.text());
        if (ids.isEmpty()) {
          continue;
        }
        Matcher declaration = AC_DECLARATION.matcher(plainMarkdown(event.text()));
        if (!declaration.matches() || ids.size() != 1) {
          if (open != null) {
            String boundaryId = ids.isEmpty() ? "" : ids.getFirst();
            diagnostics.add(
                diagnostic(
                    "TC-SPEC-AC-DECLARATION-OVERLAP",
                    path,
                    event.line(),
                    event.column(),
                    boundaryId,
                    "This AC-bearing heading starts before "
                        + open.acId()
                        + " receives its marker.",
                    "Close the earlier AC with its exact standalone acceptance marker before this"
                        + " heading.",
                    "Repair this heading and place the marker owned by "
                        + open.acId()
                        + " before it."));
            diagnostics.add(
                diagnostic(
                    "TC-SPEC-AC-MARKER-MISSING",
                    path,
                    open.line(),
                    open.column(),
                    open.acId(),
                    "The declaration reaches the next AC-bearing heading without its acceptance"
                        + " marker.",
                    "Place " + ACCEPTANCE_MARKER + " after this AC's authored rules and examples.",
                    "Add the exact standalone marker before the next AC-bearing heading."));
            open = null;
          }
          for (String acId : ids) {
            diagnostics.add(
                diagnostic(
                    "TC-SPEC-AC-HEADING-INVALID",
                    path,
                    event.line(),
                    event.column(),
                    acId,
                    "The heading mentions " + acId + " but does not declare a business title.",
                    "Use a visible Markdown heading whose plain text is "
                        + acId
                        + ": business title (a full-width colon is also valid).",
                    "Add the title and keep the exact acceptance marker after the authored"
                        + " rules."));
          }
          continue;
        }
        String acId = declaration.group(1);
        if (open != null) {
          diagnostics.add(
              diagnostic(
                  "TC-SPEC-AC-DECLARATION-OVERLAP",
                  path,
                  event.line(),
                  event.column(),
                  acId,
                  "This AC declaration starts before " + open.acId() + " receives its marker.",
                  "Close the earlier AC with its exact standalone acceptance marker before"
                      + " declaring another AC.",
                  "Move or add the marker owned by " + open.acId() + " before this heading."));
          diagnostics.add(
              diagnostic(
                  "TC-SPEC-AC-MARKER-MISSING",
                  path,
                  open.line(),
                  open.column(),
                  open.acId(),
                  "The declaration reaches another AC heading without its acceptance marker.",
                  "Place " + ACCEPTANCE_MARKER + " after this AC's authored rules and examples.",
                  "Add the exact standalone marker before the next AC heading."));
        }
        open = new Declaration(acId, path, event.line(), event.column(), 0, 0);
      } else if (event.kind() == CanonicalMarkdownStructure.EventKind.MARKER) {
        if (open == null) {
          String code =
              completed.isEmpty() ? "TC-SPEC-AC-MARKER-ORPHAN" : "TC-SPEC-AC-MARKER-DUPLICATE";
          diagnostics.add(
              diagnostic(
                  code,
                  path,
                  event.line(),
                  event.column(),
                  "",
                  code.endsWith("DUPLICATE")
                      ? "The acceptance marker is extra; the first marker at "
                          + completed.values().stream()
                              .reduce((first, last) -> last)
                              .map(MarkerLocation::location)
                              .orElse("the completed AC")
                          + " and this extra marker at "
                          + path
                          + ":"
                          + event.line()
                          + ":"
                          + event.column()
                          + " are both reported."
                      : "The acceptance marker does not follow a valid AC declaration.",
                  "Place one exact standalone marker after the authored rules of its declared AC.",
                  "Remove the marker or add the matching visible AC heading before it."));
        } else {
          Declaration paired =
              new Declaration(
                  open.acId(), path, open.line(), open.column(), event.line(), event.column());
          declarations.add(paired);
          pairedIds.add(open.acId());
          completed.put(open.acId(), new MarkerLocation(path, event.line(), event.column()));
          open = null;
        }
      }
    }
    if (open != null) {
      diagnostics.add(
          diagnostic(
              "TC-SPEC-AC-MARKER-MISSING",
              path,
              open.line(),
              open.column(),
              open.acId(),
              "The AC declaration reaches the end of the document without its acceptance marker.",
              "Place " + ACCEPTANCE_MARKER + " after this AC's authored rules and examples.",
              "Add the exact standalone marker before the document ends."));
    }
    return new Structure(declarations, pairedIds, references(path, lines), diagnostics);
  }

  private static List<Reference> references(String path, List<String> lines) {
    List<Reference> references = new ArrayList<>();
    Fence fence = null;
    for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
      String line = lines.get(lineNumber);
      if (fence != null) {
        if (isClosingFence(line, fence)) {
          fence = null;
        }
        continue;
      }
      Fence opened = openingFence(line);
      if (opened != null) {
        fence = opened;
        continue;
      }
      Matcher matcher = AC_ID.matcher(line);
      while (matcher.find()) {
        references.add(new Reference(matcher.group(1), path, lineNumber + 1, matcher.start() + 1));
      }
    }
    return references;
  }

  private static List<String> ids(String value) {
    List<String> ids = new ArrayList<>();
    Matcher matcher = AC_ID.matcher(value == null ? "" : value);
    while (matcher.find()) {
      ids.add(matcher.group(1));
    }
    return ids;
  }

  private static String plainMarkdown(String value) {
    String plain = value == null ? "" : value;
    plain = plain.replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1");
    plain = plain.replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1");
    plain = plain.replaceAll("`([^`]*)`", "$1");
    plain = plain.replaceAll("<[^>]*>", "");
    plain = plain.replaceAll("[*_~]", "");
    plain = plain.replace("\\\\", "");
    return plain.replaceAll("\\s+", " ").trim();
  }

  private static SelectedSpecDiagnostic diagnostic(
      String code,
      String path,
      int line,
      int column,
      String acId,
      String problem,
      String required,
      String fix) {
    return new SelectedSpecDiagnostic(code, path, line, column, acId, problem, required, fix);
  }

  private static Fence openingFence(String line) {
    String stripped = line.stripLeading();
    if (line.length() - stripped.length() > 3 || stripped.length() < 3) {
      return null;
    }
    char delimiter = stripped.charAt(0);
    if (delimiter != '`' && delimiter != '~') {
      return null;
    }
    int count = 0;
    while (count < stripped.length() && stripped.charAt(count) == delimiter) {
      count++;
    }
    if (count < 3) {
      return null;
    }
    String info = stripped.substring(count).trim();
    if (delimiter == '`' && info.contains("`")) {
      return null;
    }
    return new Fence(delimiter, count, info);
  }

  private static boolean isClosingFence(String line, Fence fence) {
    String stripped = line.stripLeading();
    if (line.length() - stripped.length() > 3 || stripped.length() < fence.length()) {
      return false;
    }
    int count = 0;
    while (count < stripped.length() && stripped.charAt(count) == fence.delimiter()) {
      count++;
    }
    return count >= fence.length() && stripped.substring(count).trim().isEmpty();
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
      List<ReviewDocument> documents,
      Map<String, ReviewAcLocation> locations,
      Map<String, String> documentDigests,
      List<SelectedSpecDiagnostic> diagnostics,
      boolean configured) {
    ParsedSpecs {
      documents = List.copyOf(documents == null ? List.of() : documents);
      locations =
          Collections.unmodifiableMap(
              new LinkedHashMap<>(locations == null ? Map.of() : locations));
      documentDigests =
          Collections.unmodifiableMap(
              new LinkedHashMap<>(documentDigests == null ? Map.of() : documentDigests));
      diagnostics =
          (diagnostics == null ? List.<SelectedSpecDiagnostic>of() : diagnostics)
              .stream().sorted(Comparator.comparing(SelectedSpecDiagnostic::sortKey)).toList();
    }

    static ParsedSpecs empty() {
      return new ParsedSpecs(List.of(), Map.of(), Map.of(), List.of(), false);
    }

    List<String> acceptanceConditionIds() {
      return locations.keySet().stream().sorted().toList();
    }

    String diagnosticMessage() {
      StringBuilder message = new StringBuilder("Selected Spec validation failed:");
      for (SelectedSpecDiagnostic diagnostic : diagnostics) {
        message
            .append("\n- [")
            .append(diagnostic.ruleCode())
            .append("] ")
            .append(diagnostic.path())
            .append(":")
            .append(diagnostic.line())
            .append(":")
            .append(diagnostic.column())
            .append(diagnostic.acId().isBlank() ? "" : " (" + diagnostic.acId() + ")")
            .append(" Problem: ")
            .append(diagnostic.problem())
            .append(" Required: ")
            .append(diagnostic.required())
            .append(" Fix: ")
            .append(diagnostic.fix());
      }
      return message.toString();
    }
  }

  record SelectedSpecDiagnostic(
      String ruleCode,
      String path,
      int line,
      int column,
      String acId,
      String problem,
      String required,
      String fix) {
    String sortKey() {
      return String.format("%s\u0000%09d\u0000%09d\u0000%s", path, line, column, ruleCode);
    }
  }

  private record ParsedDocument(
      ReviewDocument document,
      Map<String, ReviewAcLocation> locations,
      List<Declaration> declarations,
      List<Reference> references,
      List<SelectedSpecDiagnostic> diagnostics) {}

  private record Structure(
      List<Declaration> declarations,
      List<String> pairedIds,
      List<Reference> references,
      List<SelectedSpecDiagnostic> diagnostics) {}

  private record Declaration(
      String acId, String path, int line, int column, int markerLine, int markerColumn) {
    String location() {
      return path + ":" + line + ":" + column;
    }
  }

  private record MarkerLocation(String path, int line, int column) {
    String location() {
      return path + ":" + line + ":" + column;
    }
  }

  private record Reference(String acId, String path, int line, int column) {}

  private record Fence(char delimiter, int length, String info) {}

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
          .sorted(Comparator.comparing(ReviewDocumentAsset::bundlePath))
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
