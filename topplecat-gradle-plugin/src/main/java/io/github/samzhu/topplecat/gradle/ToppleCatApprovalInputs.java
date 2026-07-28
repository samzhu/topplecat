package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.MutationProducerKind;
import io.github.samzhu.topplecat.core.ReviewerContractApproval;
import io.github.samzhu.topplecat.core.SelectedSpecScope;
import io.github.samzhu.topplecat.core.VerificationPolicy;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Internal;

import java.io.IOException;
import java.nio.file.Files;

/** Shared managed-property contract for tasks that seal or compare a reviewer approval. */
interface ToppleCatApprovalInputs {
    @Internal
    DirectoryProperty getApprovalBuildRoot();

    @Internal
    ConfigurableFileCollection getApprovalPublicSourceRoots();

    @Internal
    DirectoryProperty getApprovalPublicCaseRoot();

    @Internal
    RegularFileProperty getApprovalDefinitionFile();

    @Internal
    Property<Boolean> getApprovalHiddenRetestEnabled();

    @Internal
    Property<Boolean> getApprovalExpectedConsumptionEnabled();

    @Internal
    Property<Boolean> getApprovalMutationEnabled();

    @Internal
    Property<Integer> getApprovalMutationThreshold();

    @Internal
    Property<String> getApprovalMutationProducerKind();

    @Internal
    Property<String> getApprovalMutationProducerTaskPath();

    @Internal
    ListProperty<String> getApprovalSelectedSpecPaths();

    @Internal
    Property<Boolean> getApprovalSpecOptionProvided();

    @Internal
    ConfigurableFileCollection getApprovalFixedSpecDocs();

    default ReviewerContractApproval currentApproval() {
        ContractDefinition definition;
        try {
            definition = ContractDefinitionJson.read(Files.readString(getApprovalDefinitionFile().get().getAsFile().toPath()));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read checked ToppleCat contract definition: " + exception.getMessage(), exception);
        }
        String taskPath = getApprovalMutationProducerTaskPath().getOrElse("");
        VerificationPolicy policy = new VerificationPolicy(ToppleCatVersion.CURRENT,
                getApprovalHiddenRetestEnabled().get(), getApprovalExpectedConsumptionEnabled().get(),
                getApprovalMutationEnabled().get(), getApprovalMutationThreshold().get(),
                MutationProducerKind.valueOf(getApprovalMutationProducerKind().get()),
                taskPath.isBlank() ? null : taskPath);
        SelectedSpecScope scope = SpecScopeResolver.resolve(getApprovalBuildRoot().get().getAsFile().toPath(),
                getApprovalSelectedSpecPaths().getOrElse(java.util.List.of()), getApprovalSpecOptionProvided().getOrElse(false),
                getApprovalFixedSpecDocs().getFiles().stream().map(file -> file.toPath()).toList()).scope();
        return ContractApprovalFactory.create(getApprovalBuildRoot().get().getAsFile().toPath(),
                getApprovalPublicSourceRoots().getFiles().stream().map(file -> file.toPath()).toList(),
                getApprovalPublicCaseRoot().get().getAsFile().toPath(), definition, policy, scope);
    }
}
