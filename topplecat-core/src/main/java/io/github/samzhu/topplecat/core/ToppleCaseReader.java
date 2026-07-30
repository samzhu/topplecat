package io.github.samzhu.topplecat.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Reads ToppleCat JSON or YAML case rows without converting their typed payloads to strings. */
public final class ToppleCaseReader {
  private static final Set<String> CASE_FIELDS = Set.of("caseId", "acId", "inputs", "expected");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final YAMLMapper YAML = YAMLMapper.builder().build();

  private ToppleCaseReader() {}

  public static List<ToppleCaseData> readAll(List<ToppleCaseSource> sources) {
    List<ToppleCaseData> cases = new ArrayList<>();
    for (ToppleCaseSource source : sources) {
      cases.addAll(read(source));
    }
    cases.sort(Comparator.comparing(ToppleCaseData::acId).thenComparing(ToppleCaseData::caseId));
    Set<String> ids = new HashSet<>();
    for (ToppleCaseData testCase : cases) {
      if (!ids.add(testCase.caseId())) {
        throw new ToppleCatException("Duplicate Topple caseId: " + testCase.caseId());
      }
    }
    return List.copyOf(cases);
  }

  public static List<ToppleCaseData> read(ToppleCaseSource source) {
    List<Path> files = caseFiles(source.path());
    List<ToppleCaseData> result = new ArrayList<>();
    for (Path file : files) {
      result.addAll(readFile(file, source.visibility()));
    }
    return List.copyOf(result);
  }

  private static List<Path> caseFiles(Path root) {
    if (!Files.exists(root)) {
      return List.of();
    }
    if (Files.isRegularFile(root)) {
      requireSupported(root);
      return List.of(root);
    }
    try (Stream<Path> paths = Files.walk(root)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(ToppleCaseReader::supported)
          .sorted()
          .toList();
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot read Topple case root " + root + ": " + exception.getMessage(), exception);
    }
  }

  private static List<ToppleCaseData> readFile(Path file, CaseVisibility visibility) {
    try {
      JsonNode root = mapper(file).readTree(Files.readString(file));
      List<JsonNode> rows = rows(root, file);
      List<ToppleCaseData> result = new ArrayList<>();
      for (int index = 0; index < rows.size(); index++) {
        result.add(toCase(rows.get(index), visibility, file, index));
      }
      return List.copyOf(result);
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot parse Topple case file " + file + ": " + exception.getMessage(), exception);
    }
  }

  private static List<JsonNode> rows(JsonNode root, Path file) {
    if (root != null && root.isArray()) {
      return root.valueStream().toList();
    }
    if (root != null
        && root.isObject()
        && root.propertyNames().size() == 1
        && root.get("cases") != null
        && root.get("cases").isArray()) {
      return root.get("cases").valueStream().toList();
    }
    throw new ToppleCatException(
        "Topple case file " + file + " must be an array or an object with only a cases array.");
  }

  private static ToppleCaseData toCase(
      JsonNode row, CaseVisibility visibility, Path file, int index) {
    String location = file + " row " + (index + 1);
    if (row == null || !row.isObject()) {
      throw new ToppleCatException("Topple case " + location + " must be an object.");
    }
    Set<String> fields = new HashSet<>();
    fields.addAll(row.propertyNames());
    if (!CASE_FIELDS.containsAll(fields) || !fields.containsAll(CASE_FIELDS)) {
      throw new ToppleCatException(
          "Topple case "
              + location
              + " must contain exactly "
              + CASE_FIELDS
              + ", but found "
              + fields
              + ".");
    }
    return new ToppleCaseData(
        text(row, "caseId", location),
        text(row, "acId", location),
        visibility,
        row.get("inputs"),
        row.get("expected"),
        file);
  }

  private static String text(JsonNode row, String field, String location) {
    JsonNode value = row.get(field);
    if (value == null || !value.isString() || value.asString().isBlank()) {
      throw new ToppleCatException(
          "Topple case " + location + " requires a non-blank string " + field + ".");
    }
    return value.asString();
  }

  private static ObjectMapper mapper(Path file) {
    String extension = extension(file);
    return extension.equals("yaml") || extension.equals("yml") ? YAML : JSON;
  }

  private static boolean supported(Path path) {
    String extension = extension(path);
    return extension.equals("json") || extension.equals("yaml") || extension.equals("yml");
  }

  private static void requireSupported(Path path) {
    if (!supported(path)) {
      throw new ToppleCatException("Topple case source must be JSON or YAML: " + path);
    }
  }

  private static String extension(Path path) {
    String name = path.getFileName().toString();
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
