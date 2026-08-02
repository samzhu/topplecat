package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.CaseDefinition;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ContractQualityAdvisor;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.report.DeliveryScope;
import io.github.samzhu.topplecat.report.HtmlBundleWriter;
import io.github.samzhu.topplecat.report.ReportLanguage;
import io.github.samzhu.topplecat.report.ReportViews;
import io.github.samzhu.topplecat.report.ReviewMethod;
import io.github.samzhu.topplecat.report.ReviewProperty;
import io.github.samzhu.topplecat.report.ReviewView;
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

  /** Presentation-only task input; excluded from the checked contract and Mechanical Seal. */
  @Input
  public abstract org.gradle.api.provider.Property<String> getReportLanguage();

  @TaskAction
  public void review() {
    Path root = getProjectRoot().get().getAsFile().toPath();
    ContractDefinition definition = readDefinition();
    SpecScopeResolver.ResolvedSpecScope scope =
        SpecScopeResolver.resolve(
            root,
            getSelectedSpecPaths().getOrElse(List.of()),
            getSpecOptionProvided().getOrElse(false));
    if (scope.scope().selected()) {
      definition =
          ContractDefinition.withComputedDigest(
              definition.acceptanceConditions().stream()
                  .filter(
                      contract -> scope.scope().acceptanceConditionIds().contains(contract.acId()))
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
              sourceCode(
                  getPublicTestSourceRoot().get().getAsFile().toPath(),
                  contract.scenario().sourceRef().file(),
                  contract.scenario().sourceRef().line()),
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
                          sourceCode(
                              getPublicTestSourceRoot().get().getAsFile().toPath(),
                              property.sourceRef().file(),
                              property.sourceRef().line())))
              .toList());
    }
    List<ToppleCaseData> cases =
        definition.acceptanceConditions().stream()
            .flatMap(contract -> contract.cases().stream())
            .map(ToppleCatReviewTask::caseData)
            .toList();
    DeliveryScope deliveryScope =
        DeliveryScope.from(
            scope.scope(),
            scope.scope().selected() ? "SELECTED_SPECS" : "ALL",
            "ALL_PUBLIC_ACCEPTANCE_CONTRACTS",
            scope.scope().selected() ? "SELECTED_SPECS" : "FULL_CONTRACT",
            0,
            0);
    ReviewView view =
        ReportViews.withReviewProperties(
            ReportViews.review(
                titles,
                cases,
                scope.parsedSpecs().documents(),
                scope.parsedSpecs().locations(),
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

  private static String sourceCode(Path root, String fileName, long methodLine) {
    try (var sources = Files.walk(root)) {
      Path source =
          sources
              .filter(path -> path.getFileName().toString().equals(fileName))
              .findFirst()
              .orElse(null);
      if (source == null) {
        return "";
      }
      return canonicalMethod(Files.readAllLines(source), methodLine);
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot read acceptance source for reviewer review: " + exception.getMessage(),
          exception);
    }
  }

  private static String canonicalMethod(List<String> lines, long oneBasedMethodLine) {
    if (lines.isEmpty()) {
      return "";
    }
    int declaration = (int) Math.max(0L, Math.min(lines.size() - 1L, oneBasedMethodLine - 1L));
    int start = declaration;
    while (start > 0 && lines.get(start - 1).stripLeading().startsWith("@")) {
      start--;
    }

    int end = declaration;
    int braces = 0;
    boolean bodyStarted = false;
    JavaLexicalState state = new JavaLexicalState();
    for (int lineIndex = declaration; lineIndex < lines.size(); lineIndex++) {
      String line = lines.get(lineIndex);
      for (int index = 0; index < line.length(); index++) {
        char current = line.charAt(index);
        char next = index + 1 < line.length() ? line.charAt(index + 1) : '\0';
        if (state.consume(current, next)) {
          continue;
        }
        if (current == '{') {
          braces++;
          bodyStarted = true;
        } else if (current == '}') {
          braces--;
        }
      }
      state.endLine();
      end = lineIndex;
      if (bodyStarted && braces == 0) {
        break;
      }
    }

    List<String> snippet = lines.subList(start, end + 1);
    int indentation =
        snippet.stream()
            .filter(line -> !line.isBlank())
            .mapToInt(ToppleCatReviewTask::leadingWhitespace)
            .min()
            .orElse(0);
    return snippet.stream()
        .map(line -> line.length() >= indentation ? line.substring(indentation) : line)
        .reduce((left, right) -> left + System.lineSeparator() + right)
        .orElse("");
  }

  private static int leadingWhitespace(String line) {
    int index = 0;
    while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
      index++;
    }
    return index;
  }

  private static final class JavaLexicalState {
    private boolean lineComment;
    private boolean blockComment;
    private boolean string;
    private boolean character;
    private boolean escaped;

    private boolean consume(char current, char next) {
      if (lineComment) {
        return true;
      }
      if (blockComment) {
        if (current == '*' && next == '/') {
          blockComment = false;
        }
        return true;
      }
      if (string || character) {
        if (escaped) {
          escaped = false;
        } else if (current == '\\') {
          escaped = true;
        } else if ((string && current == '"') || (character && current == '\'')) {
          string = false;
          character = false;
        }
        return true;
      }
      if (current == '/' && next == '/') {
        lineComment = true;
        return true;
      }
      if (current == '/' && next == '*') {
        blockComment = true;
        return true;
      }
      if (current == '"') {
        string = true;
        return true;
      }
      if (current == '\'') {
        character = true;
        return true;
      }
      return false;
    }

    private void endLine() {
      lineComment = false;
    }
  }
}
