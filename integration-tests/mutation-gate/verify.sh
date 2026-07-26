#!/usr/bin/env bash
# Proves that a surviving PIT mutant fails ToppleCat verification without leaking private details.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
project="$root/integration-tests/mutation-gate"
gradle="$root/gradlew"
owns_state_root=false
fixture_created=false
fixture_root="$project/src/hiddenTest"
if [[ -n "${TOPPLECAT_STATE_ROOT:-}" ]]; then
  state_root="$TOPPLECAT_STATE_ROOT"
else
  state_root="$(mktemp -d)"
  owns_state_root=true
fi
mkdir -p "$state_root"

cleanup() {
  local status=$?
  trap - EXIT
  set +e
  if [[ ! -d "$project/src/hiddenTest" ]]; then
    "$gradle" -p "$project" -q -Dtopplecat.stateRoot="$state_root" toppleCatRestore >/dev/null 2>&1
  fi
  if [[ "$fixture_created" == true ]]; then
    rm -rf "$fixture_root"
  fi
  if [[ "$owns_state_root" == true ]]; then
    rm -rf "$state_root"
  fi
  exit "$status"
}
trap cleanup EXIT

if [[ -d "$project/.topplecat" ]]; then
  rm -rf "$project/.topplecat"
fi
if [[ ! -f "$fixture_root/java/integration/mutation/ReviewerMutationBoundaryTest.java" \
      || ! -f "$fixture_root/resources/topplecat/cases/coupon-reviewer.json" ]]; then
  mkdir -p "$fixture_root/java/integration/mutation" "$fixture_root/resources/topplecat/cases"
  cat > "$fixture_root/java/integration/mutation/ReviewerMutationBoundaryTest.java" <<'EOF'
package integration.mutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewerMutationBoundaryTest {
    @Test
    void reviewerBoundaryRemainsAvailableDuringVerification() {
        assertTrue(true);
    }
}
EOF
  cat > "$fixture_root/resources/topplecat/cases/coupon-reviewer.json" <<'EOF'
[
  {
    "caseId": "coupon-reviewer",
    "acId": "AC-MUTATION-GATE",
    "inputs": {"discount": 100},
    "expected": {"discount": 100}
  }
]
EOF
  fixture_created=true
fi
rm -rf "$project/build"

"$gradle" publishToMavenLocal
set +e
"$gradle" -p "$project" -Dtopplecat.stateRoot="$state_root" toppleCatVerify
status=$?
set -e

if [[ $status -eq 0 ]]; then
  echo "Expected a surviving mutant to fail verification." >&2
  exit 1
fi

mutation="$project/build/topplecat/mutation-results.json"
evidence="$project/build/topplecat/evidence.json"
feedback="$project/build/topplecat/agent-feedback.json"
for artifact in "$mutation" "$evidence" "$feedback"; do
  if [[ ! -f "$artifact" ]]; then
    echo "Mutation-gate integration test failed: expected artifact is missing: $artifact. Run the verification task and inspect its Gradle output." >&2
    exit 1
  fi
done
EVIDENCE="$evidence" python3 - <<'PY'
import json
import os

data = json.loads(open(os.environ["EVIDENCE"]).read())
gate = next((item for item in data.get("gates", []) if item.get("name") == "CONTRACT_INTEGRITY"), None)
if gate is None or gate.get("verdict") != "PASS":
    raise SystemExit("Mutation-gate evidence must contain CONTRACT_INTEGRITY: PASS")
PY
grep -Fq '"MUTATION"' "$evidence"
grep -Fq '"FAIL"' "$mutation"
if grep -Fq 'AC-MUTATION-GATE' "$feedback"; then
  echo "Agent feedback leaked an acceptance-condition identifier." >&2
  exit 1
fi

echo "mutation-gate integration test PASS: expected verification failure with safe feedback"
