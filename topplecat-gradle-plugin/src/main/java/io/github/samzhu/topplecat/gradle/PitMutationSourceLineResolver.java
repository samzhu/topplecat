package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.pitest.PitMutation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/** Resolves only an unambiguous original source line for reviewer-only PIT diagnostics. */
final class PitMutationSourceLineResolver {
  private PitMutationSourceLineResolver() {}

  static Function<PitMutation, String> forDirectories(Collection<Path> sourceDirectories) {
    List<Path> roots =
        (sourceDirectories == null ? List.<Path>of() : sourceDirectories)
            .stream()
                .filter(path -> path != null && Files.isDirectory(path))
                .map(path -> path.toAbsolutePath().normalize())
                .distinct()
                .sorted()
                .toList();
    Map<String, List<Path>> byFileName = indexSourceFiles(roots);
    Map<Path, List<String>> linesByPath = new HashMap<>();
    return mutation -> {
      try {
        if (mutation == null || mutation.lineNumber() == null) {
          return null;
        }
        Path source = sourceFile(mutation, roots, byFileName);
        if (source == null) {
          return null;
        }
        List<String> lines =
            linesByPath.computeIfAbsent(
                source,
                path -> {
                  try {
                    return Files.readAllLines(path);
                  } catch (IOException | RuntimeException exception) {
                    return List.of();
                  }
                });
        int lineIndex = mutation.lineNumber() - 1;
        return lineIndex >= 0 && lineIndex < lines.size() ? lines.get(lineIndex) : null;
      } catch (RuntimeException exception) {
        // Source context is diagnostic-only and must never make Mutation Gate evidence unusable.
        return null;
      }
    };
  }

  private static Map<String, List<Path>> indexSourceFiles(List<Path> roots) {
    Map<String, List<Path>> result = new HashMap<>();
    for (Path root : roots) {
      try (Stream<Path> paths = Files.walk(root)) {
        paths
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".java"))
            .forEach(
                path ->
                    result
                        .computeIfAbsent(
                            path.getFileName().toString(), ignored -> new ArrayList<>())
                        .add(path.toAbsolutePath().normalize()));
      } catch (IOException ignored) {
        // Missing or unreadable source context is reported as unavailable, never guessed.
      }
    }
    result.replaceAll((ignored, paths) -> paths.stream().distinct().sorted().toList());
    return result;
  }

  private static Path sourceFile(
      PitMutation mutation, List<Path> roots, Map<String, List<Path>> byFileName) {
    Set<Path> candidates = new LinkedHashSet<>();
    String sourceFile = mutation.sourceFile();
    if (sourceFile != null && !sourceFile.isBlank()) {
      Path relative = Path.of(sourceFile.replace('\\', '/'));
      if (!relative.isAbsolute()) {
        for (Path root : roots) {
          Path candidate = root.resolve(relative).normalize();
          if (candidate.startsWith(root) && Files.isRegularFile(candidate)) {
            candidates.add(candidate);
          }
        }
      }
      candidates.addAll(byFileName.getOrDefault(relative.getFileName().toString(), List.of()));
    }
    String mutatedClass = mutation.mutatedClass();
    if (mutatedClass != null && !mutatedClass.isBlank()) {
      String topLevelClass = mutatedClass.split("\\$", 2)[0];
      String classRelative = topLevelClass.replace('.', '/') + ".java";
      for (Path root : roots) {
        Path candidate = root.resolve(classRelative).normalize();
        if (candidate.startsWith(root) && Files.isRegularFile(candidate)) {
          candidates.add(candidate);
        }
      }
    }
    return candidates.size() == 1 ? candidates.iterator().next() : null;
  }
}
