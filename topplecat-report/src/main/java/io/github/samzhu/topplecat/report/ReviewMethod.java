package io.github.samzhu.topplecat.report;

import java.util.List;

/** Static source context for one acceptance test in a reviewer preview. */
public record ReviewMethod(List<String> staticSentences, String sourceCode) {
  public ReviewMethod {
    staticSentences = List.copyOf(staticSentences == null ? List.of() : staticSentences);
    sourceCode = sourceCode == null ? "" : sourceCode;
  }
}
