package io.github.samzhu.topplecat.core;

import java.util.Objects;

/** One javac-emitted descriptor for a supplementary {@code @ToppleProperty} method. */
public record CompilerPropertyDescriptor(
    String schemaVersion,
    String acId,
    String title,
    String declaringBinaryName,
    String methodName,
    String methodDescriptor,
    SourceRef sourceRef,
    int tries,
    int maxDiscards,
    int maxShrinks,
    String sourceDigest) {
  public static final String SCHEMA_VERSION = "topplecat.compiler-property.v1";

  public CompilerPropertyDescriptor {
    if (!SCHEMA_VERSION.equals(schemaVersion)
        || blank(acId)
        || blank(title)
        || blank(declaringBinaryName)
        || blank(methodName)
        || blank(methodDescriptor)
        || tries < 1
        || tries > 100_000
        || maxDiscards < 0
        || maxShrinks < 0
        || sourceDigest == null
        || !sourceDigest.matches("[0-9a-f]{64}")) {
      throw new ToppleCatException("Compiler property descriptor is invalid.");
    }
    sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
  }

  public String methodIdentity() {
    return declaringBinaryName + "#" + methodName + methodDescriptor;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
