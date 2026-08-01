package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.SelectedSpecDocument;
import io.github.samzhu.topplecat.core.SelectedSpecScope;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Resolves a command-selected delivery scope without creating project-local current-Spec state. */
final class SpecScopeResolver {
  private SpecScopeResolver() {}

  static ResolvedSpecScope resolve(
      Path projectRoot, List<String> commandLineSpecPaths, boolean commandLineSpecProvided) {
    Path root = projectRoot.toAbsolutePath().normalize();
    List<Path> documents =
        commandLineSpecProvided ? selectedMarkdownDocuments(root, commandLineSpecPaths) : List.of();
    ExternalSpecDocumentReader.ParsedSpecs parsed =
        documents.isEmpty()
            ? ExternalSpecDocumentReader.ParsedSpecs.empty()
            : ExternalSpecDocumentReader.read(root, documents);
    if (commandLineSpecProvided && parsed.acceptanceConditionIds().isEmpty()) {
      throw new ToppleCatException(
          "Selected ToppleCat Spec documents contain no AC-... identifiers. Select a Markdown Spec"
              + " that anchors at least one executable acceptance condition.");
    }
    List<SelectedSpecDocument> sealedDocuments = new ArrayList<>();
    for (Path document : documents) {
      try {
        sealedDocuments.add(
            new SelectedSpecDocument(
                relative(root, document), Hashing.sha256(Files.readAllBytes(document))));
      } catch (IOException exception) {
        throw new ToppleCatException(
            "Cannot hash selected ToppleCat Spec " + document + ": " + exception.getMessage(),
            exception);
      }
    }
    SelectedSpecScope scope =
        SelectedSpecScope.create(
            sealedDocuments, commandLineSpecProvided ? parsed.acceptanceConditionIds() : List.of());
    return new ResolvedSpecScope(scope, parsed, documents, commandLineSpecProvided);
  }

  private static List<Path> selectedMarkdownDocuments(Path root, List<String> rawPaths) {
    if (rawPaths == null || rawPaths.isEmpty()) {
      throw new ToppleCatException(
          "ToppleCat --spec requires at least one repository-relative Markdown file.");
    }
    LinkedHashSet<Path> documents = new LinkedHashSet<>();
    for (String rawPath : rawPaths) {
      if (rawPath == null || rawPath.isBlank()) {
        throw new ToppleCatException(
            "ToppleCat --spec requires a repository-relative Markdown file.");
      }
      Path candidate = Path.of(rawPath);
      if (candidate.isAbsolute()) {
        throw new ToppleCatException(
            "ToppleCat --spec path must be repository-relative: " + rawPath);
      }
      Path document = root.resolve(candidate).normalize();
      if (!document.startsWith(root)) {
        throw new ToppleCatException(
            "ToppleCat --spec path must stay inside the repository root: " + rawPath);
      }
      if (!Files.isRegularFile(document)) {
        throw new ToppleCatException(
            "ToppleCat --spec must name an existing Markdown file: " + rawPath);
      }
      if (!document.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md")) {
        throw new ToppleCatException("ToppleCat --spec must name a Markdown file: " + rawPath);
      }
      documents.add(document);
    }
    return documents.stream().sorted().toList();
  }

  private static String relative(Path root, Path document) {
    Path normalized = document.toAbsolutePath().normalize();
    if (!normalized.startsWith(root)) {
      throw new ToppleCatException("Selected ToppleCat Spec must stay inside the repository root.");
    }
    return root.relativize(normalized).toString().replace('\\', '/');
  }

  record ResolvedSpecScope(
      SelectedSpecScope scope,
      ExternalSpecDocumentReader.ParsedSpecs parsedSpecs,
      List<Path> documents,
      boolean commandLineSelected) {
    ResolvedSpecScope {
      documents = List.copyOf(documents);
    }
  }
}
