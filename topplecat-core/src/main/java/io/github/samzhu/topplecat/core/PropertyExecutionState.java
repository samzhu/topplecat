package io.github.samzhu.topplecat.core;

/** Lifecycle state emitted for one declared property in one current run. */
public enum PropertyExecutionState {
  STARTED,
  COMPLETED_PASS,
  COMPLETED_COUNTEREXAMPLE,
  COMPLETED_INCOMPLETE;

  public boolean terminal() {
    return this != STARTED;
  }
}
