package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.CompilerScenarioDescriptorJson;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ScenarioTemplate;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/** Reads the current classpath's javac descriptors for JUnit-side new Scenario resolution. */
final class ToppleScenarioDescriptors {
  private static final String DIRECTORY = "META-INF/topplecat/contracts/";
  private static final String INDEX = DIRECTORY + "index";

  private ToppleScenarioDescriptors() {}

  static ScenarioTemplate find(Method method, String acId) {
    String methodIdentity =
        method.getDeclaringClass().getName()
            + "#"
            + method.getName()
            + MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                .descriptorString();
    ScenarioTemplate formal = fromCurrentContractDefinition(methodIdentity, acId);
    if (formal != null) {
      return formal;
    }
    String scenarioId = acId + "|" + methodIdentity;
    List<ScenarioTemplate> matches = new ArrayList<>();
    ClassLoader loader = method.getDeclaringClass().getClassLoader();
    try {
      Enumeration<URL> indexes = loader.getResources(INDEX);
      while (indexes.hasMoreElements()) {
        URL index = indexes.nextElement();
        for (String name : read(index).lines().toList()) {
          if (name.isBlank()) {
            continue;
          }
          if (!name.matches("[0-9a-f]{64}\\.json")) {
            throw new ToppleCatException(
                "ToppleCat compiler descriptor index contains an invalid entry: " + name);
          }
          ScenarioTemplate descriptor =
              CompilerScenarioDescriptorJson.read(
                      read(URI.create(index.toExternalForm()).resolve(name).toURL()))
                  .scenario();
          if (descriptor.scenarioId().equals(scenarioId)) {
            matches.add(descriptor);
          }
        }
      }
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot read the compiler descriptor for " + scenarioId + ": " + exception.getMessage(),
          exception);
    }
    if (matches.isEmpty()) {
      throw new ToppleCatException(
          "ToppleCat cannot find a compiler descriptor for new Scenario acceptance method "
              + scenarioId
              + ". Recompile the acceptance test with ToppleAcceptanceProcessor.");
    }
    if (matches.size() != 1) {
      throw new ToppleCatException(
          "ToppleCat found duplicate compiler descriptors for Scenario " + scenarioId + ".");
    }
    return matches.getFirst();
  }

  private static ScenarioTemplate fromCurrentContractDefinition(
      String methodIdentity, String acId) {
    String configured = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
    if (configured == null || configured.isBlank()) {
      return null;
    }
    try {
      ContractDefinition definition =
          ContractDefinitionJson.read(Files.readString(Path.of(configured)));
      return definition.acceptanceConditions().stream()
          .filter(contract -> contract.acId().equals(acId))
          .map(contract -> contract.scenario())
          .filter(scenario -> scenario.acceptanceTestMethodIdentity().equals(methodIdentity))
          .findFirst()
          .orElseThrow(
              () ->
                  new ToppleCatException(
                      "ToppleCat contract definition has no new Scenario descriptor for "
                          + acId
                          + "."));
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot read ToppleCat contract definition " + configured + ": " + exception.getMessage(),
          exception);
    }
  }

  private static String read(URL url) throws IOException {
    try (InputStream input = url.openStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
