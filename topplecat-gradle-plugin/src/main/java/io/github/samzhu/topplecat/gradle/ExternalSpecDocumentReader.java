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
import java.util.stream.Stream;

/** Reads and validates complete selected Markdown documents into one checked projection. */
final class ExternalSpecDocumentReader {
  static final String ACCEPTANCE_MARKER = "<!-- topplecat:acceptance:AC-ID -->";

  private ExternalSpecDocumentReader() {}

  static ParsedSpecs read(Path projectRoot, Collection<Path> configuredEntries) {
    if (configuredEntries.isEmpty()) {
      return ParsedSpecs.empty();
    }
    Path root = projectRoot.toAbsolutePath().normalize();
    List<ReviewDocument> documents = new ArrayList<>();
    Map<String, ReviewAcLocation> locations = new LinkedHashMap<>();
    Map<String, String> digests = new LinkedHashMap<>();
    Map<String, List<Marker>> markersById = new LinkedHashMap<>();
    List<SelectedSpecDiagnostic> diagnostics = new ArrayList<>();
    for (Path document : markdownDocuments(configuredEntries)) {
      rejectSymbolicPathComponent(root, document, displayPath(root, document));
      ParsedDocument parsed = parseDocument(root, document);
      documents.add(parsed.document());
      digests.put(parsed.document().path(), parsed.document().sha256());
      diagnostics.addAll(parsed.diagnostics());
      parsed
          .markers()
          .forEach(
              marker ->
                  markersById
                      .computeIfAbsent(marker.acId(), ignored -> new ArrayList<>())
                      .add(marker));
      parsed.locations().forEach(locations::putIfAbsent);
    }
    markersById.forEach(
        (acId, entries) -> {
          if (entries.size() > 1) {
            String allLocations =
                entries.stream()
                    .map(Marker::location)
                    .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            for (Marker entry : entries) {
              diagnostics.add(
                  diagnostic(
                      "TC-SPEC-AC-MARKER-DUPLICATE",
                      entry.path(),
                      entry.line(),
                      entry.column(),
                      acId,
                      "The ID-bearing acceptance marker for "
                          + acId
                          + " appears more than once at "
                          + allLocations
                          + ".",
                      "Keep exactly one exact standalone marker for this AC across the selected"
                          + " documents.",
                      "Remove every duplicate marker except the one that should load the AC"
                          + " projection."));
            }
            locations.remove(acId);
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
    List<SpecMarkdownBlock> rawBlocks = markdown.blocks();
    List<CanonicalMarkdownStructure.Event> markers =
        markdown.events().stream()
            .filter(event -> event.kind() == CanonicalMarkdownStructure.EventKind.MARKER)
            .toList();
    List<Integer> markerBlocks = new ArrayList<>();
    for (int index = 0; index < rawBlocks.size(); index++) {
      if (rawBlocks.get(index).kind() == SpecMarkdownBlock.Kind.ACCEPTANCE_MARKER) {
        markerBlocks.add(index);
      }
    }
    Map<String, ReviewAcLocation> locations = new LinkedHashMap<>();
    List<Marker> parsedMarkers = new ArrayList<>();
    for (int index = 0; index < markers.size() && index < markerBlocks.size(); index++) {
      CanonicalMarkdownStructure.Event marker = markers.get(index);
      String acId = marker.acId();
      int blockIndex = markerBlocks.get(index);
      parsedMarkers.add(new Marker(acId, displayPath, marker.line(), marker.column()));
      locations.putIfAbsent(
          acId, new ReviewAcLocation(displayPath, blockIndex + 1, displayPath + "#review-" + acId));
    }
    List<SelectedSpecDiagnostic> diagnostics = new ArrayList<>();
    for (CanonicalMarkdownStructure.Event event : markdown.events()) {
      if (event.kind() != CanonicalMarkdownStructure.EventKind.INVALID_MARKER) {
        continue;
      }
      boolean legacy = event.text().equals("<!-- topplecat:acceptance -->");
      diagnostics.add(
          diagnostic(
              legacy ? "TC-SPEC-AC-MARKER-LEGACY" : "TC-SPEC-AC-MARKER-MALFORMED",
              displayPath,
              event.line(),
              event.column(),
              event.acId(),
              legacy
                  ? "The released generic acceptance marker has no AC identity and cannot select"
                      + " executable material."
                  : "This acceptance directive is not the exact standalone ID-bearing marker.",
              "Use exactly <!-- topplecat:acceptance:AC-ID --> with one canonical AC ID.",
              "Replace or remove the directive; headings and ordinary AC references do not declare"
                  + " scope."));
    }
    return new ParsedDocument(
        new ReviewDocument(displayPath, Hashing.sha256(bytes), rawBlocks, assets.assets()),
        locations,
        parsedMarkers,
        diagnostics);
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
      return List.copyOf(locations.keySet());
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
      List<Marker> markers,
      List<SelectedSpecDiagnostic> diagnostics) {}

  private record Marker(String acId, String path, int line, int column) {
    String location() {
      return path + ":" + line + ":" + column;
    }
  }

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
