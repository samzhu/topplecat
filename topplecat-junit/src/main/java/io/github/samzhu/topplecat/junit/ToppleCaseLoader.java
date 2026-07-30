package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.ToppleCaseReader;
import io.github.samzhu.topplecat.core.ToppleCaseSource;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

final class ToppleCaseLoader {
  private ToppleCaseLoader() {}

  static List<ToppleCase> load(String acId) {
    CaseVisibility visibility = visibility();
    List<ToppleCaseSource> sources = new ArrayList<>();
    String configuredSources =
        visibility == CaseVisibility.HIDDEN
            ? System.getProperty(ToppleJunit.HIDDEN_CASE_SOURCES_PROPERTY, "")
            : System.getProperty(
                ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, ToppleJunit.DEFAULT_PUBLIC_CASE_ROOT);
    paths(configuredSources).forEach(path -> sources.add(new ToppleCaseSource(path, visibility)));
    List<ToppleCase> result =
        ToppleCaseReader.readAll(sources).stream()
            .filter(testCase -> acId.equals(testCase.acId()))
            .filter(testCase -> ToppleJunit.acceptanceConditionSelected(testCase.acId()))
            .map(ToppleCase::new)
            .toList();
    if (result.isEmpty()) {
      throw new ToppleCatException(
          "@ToppleAcceptanceTest("
              + acId
              + ") found no "
              + visibility.name().toLowerCase()
              + " JSON/YAML cases.");
    }
    return result;
  }

  private static CaseVisibility visibility() {
    String configured =
        System.getProperty(ToppleJunit.CASE_EXECUTION_SCOPE_PROPERTY, "PUBLIC_ONLY");
    return switch (configured) {
      case "PUBLIC_ONLY" -> CaseVisibility.PUBLIC;
      case "HIDDEN_ONLY" -> CaseVisibility.HIDDEN;
      default ->
          throw new ToppleCatException(
              "ToppleCat case execution scope must be PUBLIC_ONLY or HIDDEN_ONLY.");
    };
  }

  private static List<Path> paths(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return List.of();
    }
    return Arrays.stream(encoded.split(Pattern.quote(File.pathSeparator)))
        .filter(value -> !value.isBlank())
        .map(Path::of)
        .toList();
  }
}
