package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.CaseDefinition;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCatException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Joins compiler-owned scenario shape with typed case data into the sole contract model. */
final class ContractDefinitionBuilder {
    private ContractDefinitionBuilder() {
    }

    static ContractDefinition build(List<CompilerScenarioDescriptor> descriptors, List<ToppleCaseData> cases) {
        Map<String, CompilerScenarioDescriptor> scenarios = descriptors.stream().collect(Collectors.toMap(
                CompilerScenarioDescriptor::acId, descriptor -> descriptor, (left, right) -> {
                    throw new ToppleCatException("Compiler descriptors contain duplicate canonical AC " + left.acId()
                            + ". Keep one @ToppleTest method per AC.");
                }));
        for (ToppleCaseData testCase : cases) {
            if (!scenarios.containsKey(testCase.acId())) {
                throw new ToppleCatException("Case " + testCase.caseId() + " in " + testCase.source()
                        + " references AC " + testCase.acId() + ", but javac emitted no canonical @ToppleTest descriptor. "
                        + "Add a compilable @ToppleTest(\"" + testCase.acId() + "\") method or correct the case acId.");
            }
        }
        List<AcceptanceContract> contracts = descriptors.stream().map(descriptor -> {
            List<ToppleCaseData> boundCases = cases.stream().filter(testCase -> descriptor.acId().equals(testCase.acId())).toList();
            boundCases.forEach(testCase -> descriptor.steps().forEach(step -> step.argumentBindings().stream()
                    .filter(binding -> !binding.jsonPointer().isBlank())
                    .filter(binding -> testCase.inputs().at(binding.jsonPointer().replaceFirst("^/inputs", "")).isMissingNode()
                            && testCase.expected().at(binding.jsonPointer().replaceFirst("^/expected", "")).isMissingNode())
                    .findFirst().ifPresent(binding -> {
                        throw new ToppleCatException("Case " + testCase.caseId() + " in " + testCase.source()
                                + " has no value at compiler binding " + binding.jsonPointer() + " for AC "
                                + descriptor.acId() + ". Add that input/expected path or correct the Stage argument.");
                    })));
            return new AcceptanceContract(descriptor.acId(), descriptor.title(), descriptor.scenario(), boundCases.stream()
                    .map(testCase -> new CaseDefinition(testCase.caseId(), testCase.acId(), testCase.visibility(),
                            testCase.inputs(), testCase.expected()))
                    .toList());
        }).toList();
        return ContractDefinition.withComputedDigest(contracts);
    }
}
