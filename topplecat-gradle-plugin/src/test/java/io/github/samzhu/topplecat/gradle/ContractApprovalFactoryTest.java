package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.MutationProducerKind;
import io.github.samzhu.topplecat.core.VerificationPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractApprovalFactoryTest {
    @TempDir
    Path project;

    @Test
    void sealsPublicSourcesCasesAndLocalBuildLogicButNotProductionOrBuildOutput() throws Exception {
        Path publicSources = project.resolve("src/test/java/example");
        Path cases = project.resolve("src/test/resources/topplecat/cases");
        Files.createDirectories(publicSources);
        Files.createDirectories(cases);
        Files.writeString(project.resolve("build.gradle"), "plugins { id 'java' }\n");
        Files.writeString(publicSources.resolve("ContractTest.java"), "class ContractTest {}\n");
        Files.writeString(cases.resolve("cases.json"), "[]\n");
        Files.createDirectories(project.resolve("src/main/java/example"));
        Files.writeString(project.resolve("src/main/java/example/Production.java"), "class Production {}\n");
        Files.createDirectories(project.resolve("build/generated"));
        Files.writeString(project.resolve("build/generated/ignored.txt"), "ignored\n");

        ContractDefinition definition = ContractDefinition.withComputedDigest(List.of());
        VerificationPolicy policy = new VerificationPolicy("0.0.4", true, true, true, 100,
                MutationProducerKind.DEFAULT, null);
        var approved = ContractApprovalFactory.create(project, List.of(publicSources), cases, definition, policy);

        assertEquals(List.of("build.gradle", "src/test/java/example/ContractTest.java",
                "src/test/resources/topplecat/cases/cases.json"),
                approved.publicFiles().stream().map(entry -> entry.path()).toList());

        Files.writeString(project.resolve("src/main/java/example/Production.java"), "class Production { int value; }\n");
        var productionOnly = ContractApprovalFactory.create(project, List.of(publicSources), cases, definition, policy);
        assertEquals(EvidenceVerdict.PASS, ContractApprovalFactory.compare(approved, productionOnly).verdict());

        Files.writeString(cases.resolve("cases.json"), "[ { } ]\n");
        var changed = ContractApprovalFactory.create(project, List.of(publicSources), cases, definition, policy);
        var result = ContractApprovalFactory.compare(approved, changed);
        assertEquals(EvidenceVerdict.FAIL, result.verdict());
        assertTrue(result.changedPaths().contains("src/test/resources/topplecat/cases/cases.json"));
        assertTrue(result.publicDefinitionMatches());
    }
}
