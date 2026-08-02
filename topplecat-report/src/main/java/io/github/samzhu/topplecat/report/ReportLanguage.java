package io.github.samzhu.topplecat.report;

import java.util.Arrays;

/** Invocation-scoped presentation language for Reviewer-only HTML reports. */
public enum ReportLanguage {
  EN("en"),
  ZH_TW("zh-TW");

  private final String tag;

  ReportLanguage(String tag) {
    this.tag = tag;
  }

  public String tag() {
    return tag;
  }

  /** Parses the two supported explicit command values without consulting ambient locale state. */
  public static ReportLanguage fromTag(String value) {
    return Arrays.stream(values())
        .filter(language -> language.tag.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unsupported ToppleCat report language "
                        + quoted(value)
                        + ". Supported values: en, zh-TW."));
  }

  private static String quoted(String value) {
    return value == null || value.isBlank() ? "(blank)" : "'" + value + "'";
  }
}
