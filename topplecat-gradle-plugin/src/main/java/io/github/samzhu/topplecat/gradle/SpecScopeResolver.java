package io.github.samzhu.topplecat.gradle;

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
    return resolve(projectRoot, commandLineSpecPaths, commandLineSpecProvided, List.of(), false);
  }

  static ResolvedSpecScope resolve(
      Path projectRoot,
      List<String> commandLineSpecPaths,
      boolean commandLineSpecProvided,
      List<String> commandLineAcceptanceConditionIds,
      boolean commandLineAcceptanceConditionsProvided) {
    Path root = projectRoot.toAbsolutePath().normalize();
    if (commandLineSpecProvided && commandLineAcceptanceConditionsProvided) {
      throw new ToppleCatException(
          "Use either ToppleCat --spec or --ac for Verify, not both in the same invocation.");
    }
    if (commandLineAcceptanceConditionsProvided) {
      return new ResolvedSpecScope(
          SelectedSpecScope.create(
              List.of(), selectedAcceptanceConditions(commandLineAcceptanceConditionIds)),
          ExternalSpecDocumentReader.ParsedSpecs.empty(),
          List.of(),
          true);
    }
    List<Path> documents =
        commandLineSpecProvided ? selectedMarkdownDocuments(root, commandLineSpecPaths) : List.of();
    ExternalSpecDocumentReader.ParsedSpecs parsed =
        documents.isEmpty()
            ? ExternalSpecDocumentReader.ParsedSpecs.empty()
            : ExternalSpecDocumentReader.read(root, documents);
    if (!parsed.diagnostics().isEmpty()) {
      throw new ToppleCatException(parsed.diagnosticMessage());
    }
    if (commandLineSpecProvided && parsed.acceptanceConditionIds().isEmpty()) {
      throw new ToppleCatException(
          "Selected ToppleCat Spec documents contain no AC-... identifiers. Select a Markdown Spec"
              + " that anchors at least one executable acceptance condition.");
    }
    List<SelectedSpecDocument> sealedDocuments = new ArrayList<>();
    for (Path document : documents) {
      String relativePath = relative(root, document);
      String digest = parsed.documentDigests().get(relativePath);
      if (digest == null) {
        throw new ToppleCatException(
            "Checked Selected Spec projection is missing " + relativePath + ".");
      }
      sealedDocuments.add(new SelectedSpecDocument(relativePath, digest));
    }
    SelectedSpecScope scope =
        SelectedSpecScope.create(
            sealedDocuments, commandLineSpecProvided ? parsed.acceptanceConditionIds() : List.of());
    return new ResolvedSpecScope(scope, parsed, documents, commandLineSpecProvided);
  }

  private static List<String> selectedAcceptanceConditions(List<String> rawAcIds) {
    if (rawAcIds == null || rawAcIds.isEmpty()) {
      throw new ToppleCatException("ToppleCat --ac requires at least one AC-... identifier.");
    }
    LinkedHashSet<String> acIds = new LinkedHashSet<>();
    for (String acId : rawAcIds) {
      if (acId == null || !acId.matches("AC-[A-Za-z0-9][A-Za-z0-9-]*")) {
        throw new ToppleCatException("ToppleCat --ac requires literal AC-... identifiers: " + acId);
      }
      acIds.add(acId);
    }
    return acIds.stream().sorted().toList();
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
      if (rawPath.replace('\\', '/').matches("(^|/)(\\.|\\.\\.)(/|$)")) {
        throw new ToppleCatException(
            "ToppleCat --spec path must not contain . or .. components: " + rawPath);
      }
      Path document = root.resolve(candidate);
      if (!document.startsWith(root)) {
        throw new ToppleCatException(
            "ToppleCat --spec path must stay inside the repository root: " + rawPath);
      }
      rejectSymbolicPathComponent(root, document, rawPath);
      document = document.normalize();
      if (!Files.isRegularFile(document)) {
        throw new ToppleCatException(
            "ToppleCat --spec must name an existing Markdown file: " + rawPath);
      }
      if (!document.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md")) {
        throw new ToppleCatException("ToppleCat --spec must name a Markdown file: " + rawPath);
      }
      try {
        Path realRoot = root.toRealPath();
        Path realDocument = document.toRealPath();
        if (!realDocument.startsWith(realRoot)) {
          throw new ToppleCatException(
              "ToppleCat --spec path must resolve inside the repository root: " + rawPath);
        }
      } catch (IOException exception) {
        throw new ToppleCatException(
            "ToppleCat --spec must resolve a regular Markdown file: " + rawPath, exception);
      }
      documents.add(document);
    }
    return documents.stream().sorted().toList();
  }

  private static void rejectSymbolicPathComponent(Path root, Path candidate, String suppliedPath) {
    Path current = root;
    Path relative = root.relativize(candidate);
    for (Path component : relative) {
      current = current.resolve(component);
      if (Files.isSymbolicLink(current)) {
        throw new ToppleCatException(
            "ToppleCat --spec path must not follow a symbolic link component ("
                + current
                + "): "
                + suppliedPath);
      }
    }
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
