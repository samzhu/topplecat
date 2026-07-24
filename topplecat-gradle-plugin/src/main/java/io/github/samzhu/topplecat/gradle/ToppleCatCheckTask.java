package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCaseReader;
import io.github.samzhu.topplecat.core.ToppleCaseSource;
import io.github.samzhu.topplecat.core.ToppleCatException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Builds the sole ContractDefinition from javac descriptors and typed public/reviewer case data. */
public abstract class ToppleCatCheckTask extends DefaultTask {
    @org.gradle.api.tasks.Internal
    public abstract DirectoryProperty getProjectRoot();

    @org.gradle.api.tasks.Internal
    public abstract DirectoryProperty getPublicCaseRoot();

    @org.gradle.api.tasks.Internal
    public abstract DirectoryProperty getHiddenSourceRoot();

    /** Existing case roots are inputs without requiring an optional directory to exist. */
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getCaseSources();

    /** Class output roots that contain the processor-owned descriptor index. */
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getDescriptorClassDirectories();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecDocs();

    @org.gradle.api.tasks.Internal
    public abstract DirectoryProperty getReviewRoot();

    @OutputFile
    public abstract RegularFileProperty getDefinitionFile();

    @TaskAction
    public void check() {
        deleteReview(getReviewRoot().get().getAsFile().toPath());
        deleteReview(getReviewRoot().get().getAsFile().toPath().getParent().getParent().resolve("review"));
        Path publicCases = getPublicCaseRoot().get().getAsFile().toPath();
        Path hiddenSource = getHiddenSourceRoot().get().getAsFile().toPath();
        validateCaseFileExtensions(publicCases);
        List<ToppleCaseSource> sources = new ArrayList<>(List.of(new ToppleCaseSource(publicCases, CaseVisibility.PUBLIC)));
        Path hiddenCases = hiddenSource.resolve("resources/topplecat/cases");
        if (Files.exists(hiddenCases)) {
            validateCaseFileExtensions(hiddenCases);
            sources.add(new ToppleCaseSource(hiddenCases, CaseVisibility.HIDDEN));
        }
        List<ToppleCaseData> cases;
        try {
            cases = ToppleCaseReader.readAll(sources);
        } catch (ToppleCatException exception) {
            throw new ToppleCatException("ToppleCat check could not read case data: " + exception.getMessage()
                    + " Fix the named JSON/YAML file and row, then run toppleCatCheck again.", exception);
        }
        if (cases.stream().noneMatch(testCase -> testCase.visibility() == CaseVisibility.PUBLIC)) {
            throw new ToppleCatException("No public ToppleCat JSON/YAML cases found under " + publicCases);
        }
        List<CompilerScenarioDescriptor> descriptors = CompilerDescriptorReader.read(getDescriptorClassDirectories().getFiles()
                .stream().map(file -> file.toPath()).toList());
        ContractDefinition definition = ContractDefinitionBuilder.build(descriptors, cases);
        ExternalSpecDocumentReader.ParsedSpecs specDocs = ExternalSpecDocumentReader.read(
                getProjectRoot().get().getAsFile().toPath(), getSpecDocs().getFiles().stream().map(file -> file.toPath()).toList());
        warnForSpecAlignment(specDocs, descriptors);
        writeDefinition(definition);
        getLogger().lifecycle("ToppleCat check passed: {} ACs, {} case rows, definition {}.",
                definition.acceptanceConditions().size(), cases.size(), definition.digest());
    }

    private void writeDefinition(ContractDefinition definition) {
        Path output = getDefinitionFile().get().getAsFile().toPath();
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, ContractDefinitionJson.write(definition), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new GradleException("Cannot write ToppleCat contract definition " + output + ": "
                    + exception.getMessage(), exception);
        }
    }

    private void warnForSpecAlignment(ExternalSpecDocumentReader.ParsedSpecs specDocs,
                                      List<CompilerScenarioDescriptor> descriptors) {
        if (!specDocs.configured()) {
            return;
        }
        Set<String> acIds = descriptors.stream().map(CompilerScenarioDescriptor::acId).collect(Collectors.toSet());
        specDocs.narratives().keySet().stream().sorted().filter(acId -> !acIds.contains(acId)).forEach(acId ->
                getLogger().warn("ToppleCat check warning: external spec {} mentions {}, but no Java binding exists. "
                                + "Add @ToppleTest(\"{}\") or remove the stale AC id.",
                        String.join(", ", specDocs.sources().getOrDefault(acId, List.of("unknown spec document"))), acId, acId));
        descriptors.stream().filter(descriptor -> !specDocs.narratives().containsKey(descriptor.acId())).forEach(descriptor ->
                getLogger().warn("ToppleCat check warning: Java binding {} at {} has no AC anchor in configured specDocs. "
                                + "Add {} to a Markdown heading or paragraph, or remove the stale binding.",
                        descriptor.acId(), descriptor.sourceRef().file(), descriptor.acId()));
    }

    private static void deleteReview(Path reviewRoot) {
        if (!Files.exists(reviewRoot)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(reviewRoot)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            throw new GradleException("Cannot clear stale ToppleCat reviewer review " + reviewRoot + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static void validateCaseFileExtensions(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            Path unsupported = paths.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().matches(".*\\.(json|ya?ml)"))
                    .findFirst().orElse(null);
            if (unsupported != null) {
                throw new ToppleCatException("Topple case source must be JSON or YAML: " + unsupported);
            }
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot inspect Topple case root " + root + ": " + exception.getMessage(), exception);
        }
    }
}
