package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.CaseDefinition;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ContractQualityAdvisor;
import io.github.samzhu.topplecat.core.SelectedSpecScope;
import io.github.samzhu.topplecat.core.SelectedSpecScopeJson;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.report.DeliveryScope;
import io.github.samzhu.topplecat.report.HtmlBundleWriter;
import io.github.samzhu.topplecat.report.ReportJson;
import io.github.samzhu.topplecat.report.ReportLanguage;
import io.github.samzhu.topplecat.report.ReportViews;
import io.github.samzhu.topplecat.report.ReviewMethod;
import io.github.samzhu.topplecat.report.ReviewProperty;
import io.github.samzhu.topplecat.report.ReviewView;
import io.github.samzhu.topplecat.report.SelectedSpecProjection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

/** Writes the reviewer-only, no-verdict Spec Review from the checked ContractDefinition. */
public abstract class ToppleCatReviewTask extends ToppleCatReviewerPresentationTask {
  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getProjectRoot();

  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getPublicTestSourceRoot();

  @org.gradle.api.tasks.Internal
  public abstract DirectoryProperty getReviewRoot();

  @InputFile
  public abstract RegularFileProperty getDefinitionFile();

  @InputFile
  public abstract RegularFileProperty getSelectedSpecScopeFile();

  @InputFile
  public abstract RegularFileProperty getSelectedSpecProjectionFile();

  /** Presentation-only task input; excluded from the checked contract and Mechanical Seal. */
  @Input
  public abstract org.gradle.api.provider.Property<String> getReportLanguage();

  @TaskAction
  public void review() {
    Path root = getProjectRoot().get().getAsFile().toPath();
    ContractDefinition definition = readDefinition();
    if (!getSpecOptionProvided().getOrElse(false)
        || getSelectedSpecPaths().getOrElse(List.of()).isEmpty()) {
      throw new GradleException(
          "[TC-SPEC-SELECTION-REQUIRED] toppleCatReview requires at least one --spec canonical"
              + " repository-relative Markdown path.");
    }
    SelectedSpecScope selectedScope = readSelectedScope();
    SelectedSpecProjection projection = readSelectedProjection();
    if (!selectedScope.selected() || projection.selectedSpecDocuments().isEmpty()) {
      throw new GradleException(
          "[TC-SPEC-SELECTION-REQUIRED] toppleCatReview requires a checked --spec projection.");
    }
    if (!selectedScope.specDocuments().stream()
        .map(document -> document.path() + "=" + document.sha256())
        .toList()
        .equals(
            projection.selectedSpecDocuments().stream()
                .map(document -> document.path() + "=" + document.sha256())
                .toList())) {
      throw new GradleException(
          "Checked Selected Spec scope and projection differ; rerun toppleCatCheck before Review.");
    }
    if (selectedScope.selected()) {
      definition =
          ContractDefinition.withComputedDigest(
              definition.acceptanceConditions().stream()
                  .filter(
                      contract -> selectedScope.acceptanceConditionIds().contains(contract.acId()))
                  .toList());
    }
    Map<String, String> titles = new LinkedHashMap<>();
    Map<String, ReviewMethod> methods = new LinkedHashMap<>();
    Map<String, List<ReviewProperty>> properties = new LinkedHashMap<>();
    Map<String, List<io.github.samzhu.topplecat.core.StepTemplate>> templates =
        new LinkedHashMap<>();
    for (AcceptanceContract contract : definition.acceptanceConditions()) {
      titles.put(contract.acId(), contract.title());
      templates.put(contract.acId(), contract.scenario().steps());
      methods.put(
          contract.acId(),
          new ReviewMethod(
              ScenarioText.render(contract.scenario().steps()),
              projection.acceptanceMethodSources().getOrDefault(contract.acId(), ""),
              contract.scenario().acceptanceTestMethodIdentity(),
              contract.scenario().sourceRef().file(),
              contract.scenario().sourceRef().line()));
      properties.put(
          contract.acId(),
          contract.properties().stream()
              .map(
                  property ->
                      new ReviewProperty(
                          property.title(),
                          property.methodIdentity(),
                          property.sourceRef().file(),
                          property.sourceRef().line(),
                          property.tries(),
                          property.maxDiscards(),
                          property.maxShrinks(),
                          projection.propertySources().getOrDefault(property.methodIdentity(), "")))
              .toList());
    }
    List<ToppleCaseData> cases =
        definition.acceptanceConditions().stream()
            .flatMap(contract -> contract.cases().stream())
            .map(ToppleCatReviewTask::caseData)
            .toList();
    DeliveryScope deliveryScope =
        DeliveryScope.from(
            selectedScope,
            selectedScope.selected() ? "SELECTED_SPECS" : "ALL",
            "ALL_PUBLIC_ACCEPTANCE_CONTRACTS",
            selectedScope.selected() ? "SELECTED_SPECS" : "FULL_CONTRACT",
            0,
            0);
    ReviewView view =
        ReportViews.withReviewProperties(
            ReportViews.review(
                titles,
                cases,
                projection.selectedSpecDocuments(),
                projection.acceptanceLocations(),
                methods,
                templates,
                Instant.now(),
                deliveryScope),
            properties);
    view =
        ReportViews.withContractQualityAdvisories(
            view,
            ContractQualityAdvisor.analyze(
                definition.acceptanceConditions().stream()
                    .flatMap(contract -> contract.cases().stream())
                    .toList()));
    Path review = getReviewRoot().get().getAsFile().toPath();
    HtmlBundleWriter.review(review, view, root, ReportLanguage.fromTag(getReportLanguage().get()));
    getLogger().lifecycle("ToppleCat reviewer review written: {}", review.resolve("index.html"));
  }

  private SelectedSpecScope readSelectedScope() {
    try {
      return SelectedSpecScopeJson.read(
          Files.readString(getSelectedSpecScopeFile().get().getAsFile().toPath()));
    } catch (IOException | RuntimeException exception) {
      throw new GradleException("Cannot read checked Selected Spec scope.", exception);
    }
  }

  private SelectedSpecProjection readSelectedProjection() {
    try {
      return ReportJson.readSelectedSpecProjection(
          Files.readString(getSelectedSpecProjectionFile().get().getAsFile().toPath()));
    } catch (IOException | RuntimeException exception) {
      throw new GradleException("Cannot read checked Selected Spec projection.", exception);
    }
  }

  private ContractDefinition readDefinition() {
    Path definition = getDefinitionFile().get().getAsFile().toPath();
    try {
      return ContractDefinitionJson.read(Files.readString(definition));
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot read ToppleCat contract definition " + definition + ": " + exception.getMessage(),
          exception);
    }
  }

  private static ToppleCaseData caseData(CaseDefinition testCase) {
    return new ToppleCaseData(
        testCase.caseId(),
        testCase.acId(),
        testCase.visibility(),
        testCase.inputs(),
        testCase.expected(),
        Path.of("contract-definition.json"));
  }
}
