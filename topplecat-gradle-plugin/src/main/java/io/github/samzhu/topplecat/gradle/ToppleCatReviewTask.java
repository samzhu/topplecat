package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.CaseDefinition;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.report.ReportViews;
import io.github.samzhu.topplecat.report.ReviewMethod;
import io.github.samzhu.topplecat.report.ReviewView;
import io.github.samzhu.topplecat.report.HtmlBundleWriter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes a reviewer-only, no-verdict contract review from the checked ContractDefinition. */
public abstract class ToppleCatReviewTask extends DefaultTask {
    @org.gradle.api.tasks.Internal
    public abstract DirectoryProperty getProjectRoot();

    @org.gradle.api.tasks.Internal
    public abstract DirectoryProperty getPublicTestSourceRoot();

    @org.gradle.api.tasks.Internal
    public abstract DirectoryProperty getReviewRoot();

    @InputFile
    public abstract RegularFileProperty getDefinitionFile();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecDocs();

    @TaskAction
    public void review() {
        Path root = getProjectRoot().get().getAsFile().toPath();
        ContractDefinition definition = readDefinition();
        ExternalSpecDocumentReader.ParsedSpecs specs = ExternalSpecDocumentReader.read(root,
                getSpecDocs().getFiles().stream().map(file -> file.toPath()).toList());
        Map<String, String> titles = new LinkedHashMap<>();
        Map<String, ReviewMethod> methods = new LinkedHashMap<>();
        for (AcceptanceContract contract : definition.acceptanceConditions()) {
            titles.put(contract.acId(), contract.title());
            methods.put(contract.acId(), new ReviewMethod(ScenarioText.render(contract.scenario().steps()),
                    sourceCode(getPublicTestSourceRoot().get().getAsFile().toPath(), contract.scenario().sourceRef().file())));
        }
        List<ToppleCaseData> cases = definition.acceptanceConditions().stream().flatMap(contract -> contract.cases().stream())
                .map(ToppleCatReviewTask::caseData).toList();
        ReviewView view = ReportViews.review(titles, cases, specs.narratives(), methods, Instant.now());
        Path review = getReviewRoot().get().getAsFile().toPath();
        HtmlBundleWriter.review(review, view);
        getLogger().lifecycle("ToppleCat reviewer review written: {}", review.resolve("index.html"));
    }

    private ContractDefinition readDefinition() {
        Path definition = getDefinitionFile().get().getAsFile().toPath();
        try {
            return ContractDefinitionJson.read(Files.readString(definition));
        } catch (IOException exception) {
            throw new GradleException("Cannot read ToppleCat contract definition " + definition + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static ToppleCaseData caseData(CaseDefinition testCase) {
        return new ToppleCaseData(testCase.caseId(), testCase.acId(), testCase.visibility(), testCase.inputs(),
                testCase.expected(), Path.of("contract-definition.json"));
    }

    private static String sourceCode(Path root, String fileName) {
        try (var sources = Files.walk(root)) {
            Path source = sources.filter(path -> path.getFileName().toString().equals(fileName)).findFirst().orElse(null);
            return source == null ? "" : Files.readString(source);
        } catch (IOException exception) {
            throw new GradleException("Cannot read canonical source for reviewer review: " + exception.getMessage(), exception);
        }
    }
}
