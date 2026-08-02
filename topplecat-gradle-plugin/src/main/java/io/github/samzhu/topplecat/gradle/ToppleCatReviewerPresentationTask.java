package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.report.ReportLanguage;
import org.gradle.api.tasks.options.Option;

/** Adds the invocation-only Reviewer HTML presentation option to public Reviewer workflow tasks. */
public abstract class ToppleCatReviewerPresentationTask extends ToppleCatScopedTask {
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
}
