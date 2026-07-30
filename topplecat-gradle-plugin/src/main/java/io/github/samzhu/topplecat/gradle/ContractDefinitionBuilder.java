package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.CaseDefinition;
import io.github.samzhu.topplecat.core.CompilerPropertyDescriptor;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.PropertyDefinition;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Joins compiler-owned scenario shape with typed case data into the sole contract model. */
final class ContractDefinitionBuilder {
  private ContractDefinitionBuilder() {}

  static ContractDefinition build(
      List<CompilerScenarioDescriptor> descriptors, List<ToppleCaseData> cases) {
    return build(descriptors, List.of(), cases);
  }

  static ContractDefinition build(
      List<CompilerScenarioDescriptor> descriptors,
      List<CompilerPropertyDescriptor> propertyDescriptors,
      List<ToppleCaseData> cases) {
    Map<String, CompilerScenarioDescriptor> scenarios =
        descriptors.stream()
            .collect(
                Collectors.toMap(
                    CompilerScenarioDescriptor::acId,
                    descriptor -> descriptor,
                    (left, right) -> {
                      throw new ToppleCatException(
                          "Compiler descriptors contain duplicate AC "
                              + left.acId()
                              + ". Keep one @ToppleAcceptanceTest method per AC.");
                    }));
    for (ToppleCaseData testCase : cases) {
      if (!scenarios.containsKey(testCase.acId())) {
        throw new ToppleCatException(
            "Case "
                + testCase.caseId()
                + " in "
                + testCase.source()
                + " references AC "
                + testCase.acId()
                + ", but javac emitted no @ToppleAcceptanceTest descriptor. "
                + "Add a compilable @ToppleAcceptanceTest(\""
                + testCase.acId()
                + "\") method or correct the case acId.");
      }
    }
    for (CompilerPropertyDescriptor property : propertyDescriptors) {
      if (!scenarios.containsKey(property.acId())) {
        throw new ToppleCatException(
            "Property "
                + property.methodIdentity()
                + " references AC "
                + property.acId()
                + ", but javac emitted no @ToppleAcceptanceTest descriptor. Add the"
                + " acceptance method before review.");
      }
    }
    List<AcceptanceContract> contracts =
        descriptors.stream()
            .map(
                descriptor -> {
                  List<ToppleCaseData> boundCases =
                      cases.stream()
                          .filter(testCase -> descriptor.acId().equals(testCase.acId()))
                          .toList();
                  boundCases.forEach(
                      testCase ->
                          descriptor
                              .steps()
                              .forEach(
                                  step ->
                                      step.argumentBindings().stream()
                                          .filter(binding -> !binding.jsonPointer().isBlank())
                                          .filter(
                                              binding ->
                                                  testCase
                                                          .inputs()
                                                          .at(
                                                              binding
                                                                  .jsonPointer()
                                                                  .replaceFirst("^/inputs", ""))
                                                          .isMissingNode()
                                                      && testCase
                                                          .expected()
                                                          .at(
                                                              binding
                                                                  .jsonPointer()
                                                                  .replaceFirst("^/expected", ""))
                                                          .isMissingNode())
                                          .findFirst()
                                          .ifPresent(
                                              binding -> {
                                                throw new ToppleCatException(
                                                    "Case "
                                                        + testCase.caseId()
                                                        + " in "
                                                        + testCase.source()
                                                        + " has no value at compiler binding "
                                                        + binding.jsonPointer()
                                                        + " for AC "
                                                        + descriptor.acId()
                                                        + ". Add that input/expected path or"
                                                        + " correct the Stage argument.");
                                              })));
                  List<PropertyDefinition> properties =
                      propertyDefinitions(descriptor, propertyDescriptors);
                  return new AcceptanceContract(
                      descriptor.acId(),
                      descriptor.title(),
                      descriptor.scenario(),
                      boundCases.stream()
                          .map(
                              testCase ->
                                  new CaseDefinition(
                                      testCase.caseId(),
                                      testCase.acId(),
                                      testCase.visibility(),
                                      testCase.inputs(),
                                      testCase.expected()))
                          .toList(),
                      properties);
                })
            .toList();
    return ContractDefinition.withComputedDigest(contracts);
  }

  private static List<PropertyDefinition> propertyDefinitions(
      CompilerScenarioDescriptor descriptor, List<CompilerPropertyDescriptor> properties) {
    return properties.stream()
        .filter(property -> descriptor.acId().equals(property.acId()))
        .map(
            property ->
                new PropertyDefinition(
                    property.acId(),
                    property.methodIdentity(),
                    property.title(),
                    property.tries(),
                    property.maxDiscards(),
                    property.maxShrinks(),
                    property.sourceRef(),
                    property.sourceDigest()))
        .toList();
  }
}
