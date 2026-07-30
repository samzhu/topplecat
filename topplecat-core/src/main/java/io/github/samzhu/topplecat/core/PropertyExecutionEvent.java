package io.github.samzhu.topplecat.core;

/** Append-only Property lifecycle event cross-checked with the dedicated JUnit task XML. */
public record PropertyExecutionEvent(
    String schemaVersion,
    String runId,
    String acId,
    String methodIdentity,
    String sourceDigest,
    PropertyExecutionState state,
    PropertyResult result) {
  public static final String SCHEMA_VERSION = "topplecat.property-event.v2";

  public PropertyExecutionEvent {
    required(schemaVersion, "schemaVersion");
    required(runId, "runId");
    required(acId, "acId");
    required(methodIdentity, "methodIdentity");
    if (!SCHEMA_VERSION.equals(schemaVersion)
        || sourceDigest == null
        || !sourceDigest.matches("[0-9a-f]{64}")
        || state == null
        || state == PropertyExecutionState.STARTED && result != null
        || state.terminal()
            && (result == null
                || result.state() != state
                || !acId.equals(result.acId())
                || !methodIdentity.equals(result.methodIdentity()))) {
      throw new ToppleCatException("Property execution event is invalid.");
    }
  }

  private static void required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("Property execution event " + field + " is required.");
    }
  }
}
