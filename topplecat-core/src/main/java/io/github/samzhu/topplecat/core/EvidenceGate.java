package io.github.samzhu.topplecat.core;

/** Safe gate summary included in machine evidence and agent feedback. */
public record EvidenceGate(String name, EvidenceVerdict verdict, String reason) {
  public EvidenceGate(String name, EvidenceVerdict verdict) {
    this(name, verdict, null);
  }
}
