package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.report.ReportLanguage;
import java.util.List;
import org.gradle.api.tasks.options.Option;

/** Entry task for a full-contract or explicitly scoped verification run. */
public abstract class ToppleCatVerifyTask extends ToppleCatScopedTask {
  @Option(
      option = "ac",
      description = "Acceptance Condition to verify; may be supplied more than once. Cannot be combined with --spec.")
  public void setAcceptanceConditions(List<String> acIds) {
    ToppleCatExtension extension = getProject().getExtensions().getByType(ToppleCatExtension.class);
    extension
        .getCommandLineAcceptanceConditionIds()
        .set(acIds == null ? List.of() : List.copyOf(acIds));
    extension.getCommandLineAcceptanceConditionsProvided().set(true);
  }

  @Option(
      option = "language",
      description = "Reviewer HTML presentation language: en (default) or zh-TW.")
  public void setLanguage(String language) {
    ReportLanguage selected = ReportLanguage.fromTag(language);
    getProject()
        .getExtensions()
        .getByType(ToppleCatExtension.class)
        .getCommandLineReportLanguage()
        .set(selected.tag());
  }

  @Option(
      option = "all-hidden-tests",
      description = "Escalate this verification run to every hidden typed-row check.")
  public void setAllHiddenTests(boolean allHidden) {
    getProject()
        .getExtensions()
        .getByType(ToppleCatExtension.class)
        .getAllHiddenRequested()
        .set(allHidden);
  }
}
