package io.github.samzhu.topplecat.core;

import java.util.Comparator;
import java.util.List;

/** Run-scoped generated-trial evidence; it never substitutes for {@link VerificationRun}. */
public record PropertyResults(String schemaVersion, String runId, List<PropertyResult> results) {
  public static final String SCHEMA_VERSION = "topplecat.property-results.v2";

  public PropertyResults {
    if (!SCHEMA_VERSION.equals(schemaVersion) || runId == null || runId.isBlank()) {
      throw new ToppleCatException("Property results are invalid.");
    }
    results =
        results == null
            ? List.of()
            : results.stream()
                .sorted(Comparator.comparing(PropertyResult::methodIdentity))
                .toList();
    if (results.stream().map(PropertyResult::methodIdentity).distinct().count() != results.size()) {
      throw new ToppleCatException("Property results contain duplicate method identities.");
    }
  }
}
