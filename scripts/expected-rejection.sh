#!/usr/bin/env bash
# Shared release/demo helper for a red-team check that must fail verification.

expect_topplecat_rejection() {
  local label="$1"
  local evidence="$2"
  local feedback="$3"
  local rejected_gate="$4"
  shift 4

  local output_file
  output_file="$(mktemp)"
  local command_exit
  set +e
  "$@" >"$output_file" 2>&1
  command_exit=$?
  set -e

  local evidence_exit=0
  if [[ "$command_exit" -eq 0 ]]; then
    evidence_exit=1
  else
    set +e
    python3 - "$evidence" "$feedback" "$rejected_gate" <<'PY'
import json
import pathlib
import sys

evidence_path = pathlib.Path(sys.argv[1])
feedback_path = pathlib.Path(sys.argv[2])
rejected_gate = sys.argv[3]

def read(path, label):
    if not path.is_file():
        raise SystemExit(f"{label} was not written: {path}")
    return json.loads(path.read_text())

def gates(data, label):
    values = {gate.get("name"): gate.get("verdict") for gate in data.get("gates", [])}
    for required in ("CONTRACT_INTEGRITY", rejected_gate):
        if required not in values:
            raise SystemExit(f"{label} is missing the {required} gate")
    return values

evidence = read(evidence_path, "evidence.json")
feedback = read(feedback_path, "agent-feedback.json")
evidence_gates = gates(evidence, "evidence.json")
feedback_gates = gates(feedback, "agent-feedback.json")

if evidence.get("verdict") != "FAIL":
    raise SystemExit(f"evidence.json verdict is {evidence.get('verdict')!r}, not 'FAIL'")
if evidence_gates["CONTRACT_INTEGRITY"] != "PASS":
    raise SystemExit("the approved contract did not pass integrity before the attack ran")
if evidence_gates[rejected_gate] != "FAIL":
    raise SystemExit(f"evidence.json gate {rejected_gate} is not 'FAIL'")
if feedback.get("verdict") != "FAIL" or feedback_gates[rejected_gate] != "FAIL":
    raise SystemExit("agent-feedback.json does not safely report the rejected gate")
PY
    evidence_exit=$?
    set -e
  fi

  if [[ "$evidence_exit" -ne 0 ]]; then
    echo "Release verification failed: expected rejection did not produce the required evidence: $label." >&2
    if [[ "$command_exit" -eq 0 ]]; then
      echo "The verification command passed when this red-team attack should have failed." >&2
    fi
    echo "Full Gradle output follows so the unexpected failure can be diagnosed:" >&2
    cat "$output_file" >&2
    rm -f "$output_file"
    return 1
  fi

  rm -f "$output_file"
  printf 'EXPECTED FAILURE: %s was rejected.\n' "$label"
  printf 'Confirmed current-run evidence and safe agent feedback: %s=FAIL.\n' "$rejected_gate"
}
