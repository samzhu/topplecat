package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.CompilerPropertyDescriptor;
import io.github.samzhu.topplecat.core.CompilerPropertyDescriptorJson;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptorJson;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reads only the current javac descriptor index, deliberately ignoring stale loose files. */
final class CompilerDescriptorReader {
  private static final Path DIRECTORY = Path.of("META-INF", "topplecat", "contracts");
  private static final Path PROPERTY_DIRECTORY = Path.of("META-INF", "topplecat", "properties");

  private CompilerDescriptorReader() {}

  static List<CompilerScenarioDescriptor> read(Collection<Path> classDirectories) {
    List<CompilerScenarioDescriptor> descriptors = new ArrayList<>();
    for (Path classes :
        classDirectories.stream().map(Path::toAbsolutePath).distinct().sorted().toList()) {
      Path directory = classes.resolve(DIRECTORY);
      Path index = directory.resolve("index");
      if (!Files.isRegularFile(index)) {
        continue;
      }
      try {
        for (String name : Files.readAllLines(index)) {
          if (name.isBlank()) {
            continue;
          }
          if (!name.matches("[0-9a-f]{64}\\.json")) {
            throw new ToppleCatException(
                "ToppleCat compiler descriptor index contains an invalid entry: " + name);
          }
          Path descriptor = directory.resolve(name).normalize();
          if (!descriptor.getParent().equals(directory) || !Files.isRegularFile(descriptor)) {
            throw new ToppleCatException(
                "ToppleCat compiler descriptor index references a missing descriptor: " + name);
          }
          descriptors.add(CompilerScenarioDescriptorJson.read(Files.readString(descriptor)));
        }
      } catch (IOException exception) {
        throw new ToppleCatException(
            "Cannot read ToppleCat compiler descriptors under "
                + classes
                + ": "
                + exception.getMessage(),
            exception);
      }
    }
    List<CompilerScenarioDescriptor> result =
        descriptors.stream()
            .sorted(
                Comparator.comparing(CompilerScenarioDescriptor::acId)
                    .thenComparing(CompilerScenarioDescriptor::scenarioId))
            .toList();
    Set<String> acIds = new HashSet<>();
    for (CompilerScenarioDescriptor descriptor : result) {
      if (!acIds.add(descriptor.acId())) {
        throw new ToppleCatException(
            "AC "
                + descriptor.acId()
                + " has duplicate @ToppleAcceptanceTest descriptors. "
                + "Keep one acceptance method per AC.");
      }
    }
    return List.copyOf(result);
  }

  static List<CompilerPropertyDescriptor> readProperties(Collection<Path> classDirectories) {
    List<CompilerPropertyDescriptor> descriptors = new ArrayList<>();
    for (Path classes :
        classDirectories.stream().map(Path::toAbsolutePath).distinct().sorted().toList()) {
      Path directory = classes.resolve(PROPERTY_DIRECTORY);
      Path index = directory.resolve("index");
      if (!Files.isRegularFile(index)) {
        continue;
      }
      try {
        for (String name : Files.readAllLines(index)) {
          if (name.isBlank()) {
            continue;
          }
          if (!name.matches("[0-9a-f]{64}\\.json")) {
            throw new ToppleCatException(
                "ToppleCat Property descriptor index contains an invalid entry: " + name);
          }
          Path descriptor = directory.resolve(name).normalize();
          if (!descriptor.getParent().equals(directory) || !Files.isRegularFile(descriptor)) {
            throw new ToppleCatException(
                "ToppleCat Property descriptor index references a missing descriptor: " + name);
          }
          descriptors.add(CompilerPropertyDescriptorJson.read(Files.readString(descriptor)));
        }
      } catch (IOException exception) {
        throw new ToppleCatException(
            "Cannot read ToppleCat Property compiler descriptors under "
                + classes
                + ": "
                + exception.getMessage(),
            exception);
      }
    }
    List<CompilerPropertyDescriptor> result =
        descriptors.stream()
            .sorted(Comparator.comparing(CompilerPropertyDescriptor::methodIdentity))
            .toList();
    Set<String> identities = new HashSet<>();
    for (CompilerPropertyDescriptor descriptor : result) {
      if (!identities.add(descriptor.methodIdentity())) {
        throw new ToppleCatException(
            "ToppleCat compiler descriptors contain duplicate Property method identity "
                + descriptor.methodIdentity()
                + ".");
      }
    }
    return List.copyOf(result);
  }
}
