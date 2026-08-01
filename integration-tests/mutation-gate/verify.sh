#!/usr/bin/env bash
# Proves that a surviving PIT mutant fails ToppleCat verification without leaking private details.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
project="$root/integration-tests/mutation-gate"
gradle="$root/gradlew"
source "$root/scripts/expected-rejection.sh"
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

if [[ -e "$fixture_root" ]]; then
  echo "Mutation-gate integration test cannot safely create its temporary reviewer fixture: $fixture_root already exists." >&2
  exit 1
fi
if [[ -d "$project/.topplecat" ]]; then
  rm -rf "$project/.topplecat"
fi
mkdir -p "$fixture_root/java/integration/mutation" "$fixture_root/resources/topplecat/cases"
fixture_created=true
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
rm -rf "$project/build"

"$gradle" publishToMavenLocal

# Verify deliberately reuses an existing Mechanical Seal. Create that seal in the same temporary
# reviewer state as the red-team attack; otherwise Integrity correctly blocks the mutation gate.
"$gradle" -p "$project" -Dtopplecat.stateRoot="$state_root" toppleCatSeal

mutation="$project/build/topplecat/mutation-results.json"
evidence="$project/build/topplecat/evidence.json"
feedback="$project/build/topplecat/agent-feedback.json"
if ! expect_topplecat_rejection \
  "Mutation-gate attack" \
  "$evidence" \
  "$feedback" \
  "MUTATION" \
  "$gradle" -p "$project" -Dtopplecat.stateRoot="$state_root" toppleCatVerify; then
  exit 1
fi

for artifact in "$mutation" "$evidence" "$feedback"; do
  if [[ ! -f "$artifact" ]]; then
    echo "Mutation-gate integration test failed: expected artifact is missing: $artifact. Run the verification task and inspect its Gradle output." >&2
    exit 1
  fi
done
MUTATION="$mutation" EVIDENCE="$evidence" FEEDBACK="$feedback" python3 - <<'PY'
import json
import os

mutation = json.loads(open(os.environ["MUTATION"]).read())
evidence = json.loads(open(os.environ["EVIDENCE"]).read())
feedback = open(os.environ["FEEDBACK"]).read()

if mutation.get("schemaVersion") != "topplecat.mutation-results.v1":
    raise SystemExit("Mutation results must use the sole v1 schema")
if mutation.get("pitVersion") != "1.25.5":
    raise SystemExit("Mutation results must identify PIT 1.25.5")
if mutation.get("managedProfileId") != "topplecat-managed-v1":
    raise SystemExit("Mutation results must identify the managed mutation profile")
expected_operators = [
    "TRUE_RETURNS",
    "FALSE_RETURNS",
    "PRIMITIVE_RETURNS",
    "EMPTY_RETURNS",
    "NULL_RETURNS",
    "REMOVE_CONDITIONALS_EQUAL_IF",
    "REMOVE_CONDITIONALS_EQUAL_ELSE",
    "REMOVE_CONDITIONALS_ORDER_IF",
    "REMOVE_CONDITIONALS_ORDER_ELSE",
    "CONDITIONALS_BOUNDARY",
    "VOID_METHOD_CALLS",
    "MATH",
]
if mutation.get("managedOperatorIds") != expected_operators:
    raise SystemExit("Mutation results must retain the exact managed operator profile")
assessment = next(
    (item for item in mutation.get("assessments", []) if item.get("acId") == "AC-MUTATION-GATE"),
    None,
)
if assessment is None:
    raise SystemExit("Mutation results must contain the acceptance-method assessment")
if assessment.get("coveredMutantCount", 0) < 1:
    raise SystemExit("The acceptance method did not cover the surviving mutant")
if assessment.get("killedByAcceptanceMethodMutantCount", 0) >= assessment.get("coveredMutantCount", 0):
    raise SystemExit("The managed survivor did not reduce the acceptance-method detection rate")
if assessment.get("detectionRate", 100) >= assessment.get("sealedThreshold", 0):
    raise SystemExit("The acceptance-method detection rate did not fall below its threshold")
if assessment.get("attributionGap"):
    raise SystemExit("The managed survivor must be attributed to the public acceptance method")
mutations = mutation.get("mutations", [])
raw_mutators = {item.get("mutator", "") for item in mutations}
signal_families = {
    "return replacement": any(".mutators.returns." in raw for raw in raw_mutators),
    "forced conditional": any(".RemoveConditionalMutator_" in raw for raw in raw_mutators),
    "conditional boundary": (
        "org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator"
        in raw_mutators
    ),
    "void method-call removal": (
        "org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator"
        in raw_mutators
    ),
    "arithmetic replacement": (
        "org.pitest.mutationtest.engine.gregor.mutators.MathMutator" in raw_mutators
    ),
}
missing_signals = [name for name, present in signal_families.items() if not present]
if missing_signals:
    raise SystemExit(
        "Managed PIT did not produce all five required real mutation signal families: "
        + ", ".join(missing_signals)
    )
if not any(
    item.get("status") == "SURVIVED"
    and item.get("mutator")
        == "org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator"
    for item in mutations
):
    raise SystemExit("The managed PIT evidence must retain a raw SURVIVED void-call mutant")

gates = {item.get("name"): item.get("verdict") for item in evidence.get("gates", [])}
if gates.get("CONTRACT_INTEGRITY") != "PASS":
    raise SystemExit("Mutation-gate evidence must contain CONTRACT_INTEGRITY: PASS")
if gates.get("MUTATION") != "FAIL":
    raise SystemExit("Mutation-gate evidence must record MUTATION: FAIL, not INCOMPLETE")

for reviewer_only in (
    "AC-MUTATION-GATE",
    "CouponAcceptanceTest",
    "CouponService",
    "acceptsThePublicCase",
    "coveringTests",
    "killingTests",
    "succeedingTests",
    "producerMutationCount",
    "coveredMutantCount",
    "killedByAcceptanceMethodMutantCount",
    "detectionRate",
    "topplecat-managed-v1",
    "1.25.5",
    "VoidMethodCallMutator",
    "PrimitiveReturnsMutator",
    "RemoveConditionalMutator",
    "ConditionalsBoundaryMutator",
    "MathMutator",
    "SURVIVED",
    "KILLED",
):
    if reviewer_only in feedback:
        raise SystemExit(f"Agent feedback leaked reviewer-only mutation data: {reviewer_only}")
PY
grep -Fq '"MUTATION"' "$evidence"

echo "mutation-gate integration test PASS: expected verification failure with safe feedback"
