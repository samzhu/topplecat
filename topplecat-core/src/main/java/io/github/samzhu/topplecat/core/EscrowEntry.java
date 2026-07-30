package io.github.samzhu.topplecat.core;

/** One hash-addressed private source file. Paths are always project-relative. */
public record EscrowEntry(String path, String sha256, EscrowSourceKind sourceKind) {}
