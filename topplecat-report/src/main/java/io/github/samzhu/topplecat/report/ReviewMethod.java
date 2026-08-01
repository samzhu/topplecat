package io.github.samzhu.topplecat.report;

import java.util.List;

/** Static source context for the one public Acceptance Method bound to an AC. */
public record ReviewMethod(
    List<String> staticSentences,
    String sourceCode,
    String methodIdentity,
    String sourceFile,
    long sourceLine) {
  public ReviewMethod {
    staticSentences = List.copyOf(staticSentences == null ? List.of() : staticSentences);
    sourceCode = sourceCode == null ? "" : sourceCode;
    methodIdentity = methodIdentity == null ? "" : methodIdentity;
    sourceFile = sourceFile == null ? "" : sourceFile;
    sourceLine = Math.max(0, sourceLine);
  }

  public ReviewMethod(List<String> staticSentences, String sourceCode) {
    this(staticSentences, sourceCode, "", "", 0);
  }
}
