#!/usr/bin/env bash
# Runs the complete release gate, including samples, mutation verification, skill, and docs checks.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gradle="${GRADLE_CMD:-$root/gradlew}"
junit_sample="$root/samples/junit-cart-orders"
spring_sample="$root/samples/spring-boot-cart-orders"
release_state_root="${TOPPLECAT_RELEASE_STATE_ROOT:-$(mktemp -d)}"
release_state_root_owned=false
if [[ -z "${TOPPLECAT_RELEASE_STATE_ROOT:-}" ]]; then
  release_state_root_owned=true
else
  mkdir -p "$release_state_root"
fi

junit_state_root="$release_state_root/junit-cart-orders"
spring_state_root="$release_state_root/spring-boot-cart-orders"
junit_service="$junit_sample/src/main/java/sample/cartorders/OrderService.java"
junit_service_backup="$(mktemp)"
cp "$junit_service" "$junit_service_backup"

restore_sample() {
  local sample="$1"
  local state_root="$2"
  "$gradle" --no-watch-fs -p "$sample" -Ptopplecat.useMavenLocal=true -q \
    -Dtopplecat.stateRoot="$state_root" toppleCatRestore
}

run_sample() {
  local sample="$1"
  local state_root="$2"
  shift 2
  "$gradle" -p "$sample" -Ptopplecat.useMavenLocal=true -Dtopplecat.stateRoot="$state_root" "$@"
}

cleanup() {
  local status=$?
  trap - EXIT
  set +e
  restore_sample "$junit_sample" "$junit_state_root" >/dev/null 2>&1 || true
  restore_sample "$spring_sample" "$spring_state_root" >/dev/null 2>&1 || true
  if [[ -f "$junit_service_backup" ]]; then
    cp "$junit_service_backup" "$junit_service"
    rm -f "$junit_service_backup"
  fi
  if [[ "$release_state_root_owned" == true && -d "$release_state_root" ]]; then
    rm -rf "$release_state_root"
  fi
  exit "$status"
}
trap cleanup EXIT
mkdir -p "$junit_state_root" "$spring_state_root"

cd "$root"
if ! command -v python3 >/dev/null 2>&1; then
  echo "Release gate failed: python3 is required for scripts/verify-docs.py. Install Python 3 and rerun." >&2
  exit 1
fi
"$gradle" clean check
"$gradle" publishToMavenLocal
bash "$root/scripts/verify-artifacts.sh"
published_junit_jar="$root/topplecat-junit/build/libs/topplecat-junit-0.2.2.jar"
if [[ ! -f "$published_junit_jar" ]]; then
  echo "Release gate failed: expected JUnit artifact was not built: $published_junit_jar" >&2
  exit 1
fi
for removed_type in \
  ToppleStageField \
  ProvidedState \
  ExpectedState \
  ToppleStageSentence \
  ToppleTest \
  ToppleAc \
  ToppleAcBinding \
  ToppleAcExtension \
  ToppleScenarioProcessor; do
  if jar tf "$published_junit_jar" | grep -Fq "io/github/samzhu/topplecat/junit/${removed_type}.class"; then
    echo "Release gate failed: removed public type remains in the JUnit artifact: $removed_type" >&2
    exit 1
  fi
done
bash "$root/scripts/verify-scenario-authoring.sh"
# Verify the documented tasks from each sample project, not from this repository root.
run_sample "$junit_sample" "$junit_state_root" help --task toppleCatReview
run_sample "$spring_sample" "$spring_state_root" help --task toppleCatReview
run_sample "$junit_sample" "$junit_state_root" help --task toppleCatInit

# Keep sample runs against candidate artifacts.
assert_artifact_version() {
  local file="$1"
  local expected="$2"
  if ! grep -Eq "\"${expected}\"|${expected}" "$file"; then
    echo "Release gate failed: ${file} does not resolve expected dependency ${expected}" >&2
    exit 1
  fi
}

run_sample "$junit_sample" "$junit_state_root" help --task toppleCatSeal
run_sample "$spring_sample" "$spring_state_root" help --task toppleCatSeal
echo "Release verification: running red-team attacks. Each attack must be rejected; expected rejections are labelled below."
TOPPLECAT_USE_MAVEN_LOCAL=true bash "$root/samples/junit-cart-orders/demo.sh" all
TOPPLECAT_STATE_ROOT="$spring_state_root" bash "$root/samples/spring-boot-cart-orders/demo.sh"
TOPPLECAT_STATE_ROOT="$release_state_root/mutation-gate" bash "$root/integration-tests/mutation-gate/verify.sh"

# The checked-in learning service intentionally has a synthetic shortcut. Make
# a temporary correct copy in place only for this release-gate baseline, then
# restore it through the EXIT trap after the custody checks below.
python3 - "$junit_service" <<'PY'
import pathlib, sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()
wrong = 'int discount = "SAVE100".equals(cart.coupon()) ? cart.subtotal() / 5 : 0;'
correct = 'int discount = "SAVE100".equals(cart.coupon()) ? 100 : 0;'
if wrong not in text:
    raise SystemExit(f"Release gate cannot find the synthetic shortcut in {path}")
path.write_text(text.replace(wrong, correct, 1))
PY
run_sample "$junit_sample" "$junit_state_root" toppleCatSeal toppleCatVerify

assert_artifact_version "$junit_sample/build.gradle.kts" "0.2.2"
assert_artifact_version "$spring_sample/build.gradle.kts" "0.2.2"
assert_artifact_version "$root/integration-tests/mutation-gate/build.gradle.kts" "0.2.2"

JUNIT_SAMPLE="$junit_sample" SPRING_SAMPLE="$spring_sample" python3 - <<'PY'
import json
import os
import pathlib

def gate(data, name):
    for item in data.get("gates", []):
        if item.get("name") == name:
            return item.get("verdict"), item.get("reason", "")
    raise SystemExit(f"Missing required gate {name} in evidence")

def verify_artifact(path):
    data = json.loads(pathlib.Path(path).read_text())
    ci_verdict, _ = gate(data, "CONTRACT_INTEGRITY")
    if ci_verdict != "PASS":
        raise SystemExit(f"Evidence {path} did not pass CONTRACT_INTEGRITY: {ci_verdict}")
    for required in ("JUNIT", "REVIEWER_JUNIT", "EXPECTED_CONSUMPTION", "PROPERTY"):
        gv = gate(data, required)
        if gv[0] != "PASS":
            raise SystemExit(f"Evidence {path} gate {required} is {gv[0]} with reason {gv[1]!r}")
    mutation = gate(data, "MUTATION")
    if mutation[0] not in ("PASS", "DISABLED"):
        raise SystemExit(f"Evidence {path} mutation gate is {mutation[0]} with reason {mutation[1]!r}")
    if data.get("verdict") != "PASS":
        raise SystemExit(f"Evidence {path} aggregate verdict is {data.get('verdict')} and is not PASS")

verify_artifact(pathlib.Path(os.environ["JUNIT_SAMPLE"]) / "build/topplecat/evidence.json")
verify_artifact(pathlib.Path(os.environ["SPRING_SAMPLE"]) / "build/topplecat/evidence.json")
PY

bash "$root/scripts/validate-skill.sh"
python3 "$root/scripts/verify-docs.py"

for artifact in \
  "$junit_sample/build/topplecat/evidence.json" \
  "$spring_sample/build/topplecat/evidence.json"; do
  if grep -Fq 'CONTRACT_INTEGRITY' "$artifact"; then
    :
  else
    echo "Evidence is missing CONTRACT_INTEGRITY in $artifact" >&2
    exit 1
  fi
done

for feedback in \
  "$junit_sample/build/topplecat/agent-feedback.json" \
  "$spring_sample/build/topplecat/agent-feedback.json"; do
  if grep -Eiq 'coupon-hidden-800|\b800\b|customer-2|ReviewerBoundary|hiddenTest' "$feedback"; then
    echo "Private detail leaked into $feedback" >&2
    exit 1
  fi
done

for retired_public_report in \
  "$junit_sample/build/topplecat/reports/public" \
  "$spring_sample/build/topplecat/reports/public"; do
  if [[ -e "$retired_public_report" ]]; then
    echo "Retired public report path was published: $retired_public_report" >&2
    exit 1
  fi
done

for verification_report in \
  "$junit_sample/build/topplecat/reports/verification/index.html" \
  "$spring_sample/build/topplecat/reports/verification/index.html"; do
  verification_data="${verification_report%/index.html}/data.json"
  if [[ ! -f "$verification_report" ]] \
    || [[ ! -f "$verification_data" ]] \
    || ! grep -Fq 'topplecat.verification-view.v10' "$verification_data"; then
    echo "Reviewer Verification Report was not generated: $verification_report" >&2
    exit 1
  fi
done

restore_sample "$junit_sample" "$junit_state_root"
run_sample "$junit_sample" "$junit_state_root" --no-watch-fs --rerun-tasks toppleCatCheck

if [[ -e "$junit_sample/build/topplecat/reports/review/index.html" ]]; then
  echo "toppleCatCheck must not leave a Spec Review artifact" >&2
  exit 1
fi

review_failure_output="$(mktemp)"
if run_sample "$junit_sample" "$junit_state_root" --no-watch-fs --rerun-tasks toppleCatReview \
  >"$review_failure_output" 2>&1; then
  rm -f "$review_failure_output"
  echo "toppleCatReview without --spec must be rejected" >&2
  exit 1
fi
if ! grep -Fq "TC-SPEC-SELECTION-REQUIRED" "$review_failure_output"; then
  cat "$review_failure_output" >&2
  rm -f "$review_failure_output"
  echo "toppleCatReview without --spec failed without the required selection diagnostic" >&2
  exit 1
fi
rm -f "$review_failure_output"
echo "Expected failure: toppleCatReview without --spec was rejected before dependent Check."

run_sample "$junit_sample" "$junit_state_root" --no-watch-fs --rerun-tasks toppleCatReview \
  --spec specs/cart-orders.md

restore_sample "$junit_sample" "$junit_state_root"
junit_review="$junit_sample/build/topplecat/reports/review/data.json"
if [[ ! -f "$junit_review" ]]; then
  echo "Spec Review was not generated by toppleCatReview" >&2
  exit 1
fi
if ! grep -Fq 'coupon-hidden-800' "$junit_review"; then
  echo "Spec Review did not include the reviewer case" >&2
  exit 1
fi
if ! grep -Fq 'SAVE100 reduces the order subtotal' "$junit_review" || ! grep -Fq 'sourceCode' "$junit_review"; then
  echo "Spec Review is missing static Scenario context or Acceptance Method source" >&2
  exit 1
fi

legacy_runtime="cu""cumber|gher""kin|topplecat-""cli|topplecat-""testkit"
residual="$(grep -R -nEI \
  --exclude-dir=build \
  --exclude-dir=.gradle \
  --exclude-dir=.topplecat \
  "$legacy_runtime" \
  topplecat-core \
  topplecat-junit \
  topplecat-report \
  topplecat-gradle-plugin \
  samples \
  integration-tests \
  scripts/verify-release.sh || true)"
if [[ -n "$residual" ]]; then
  echo "Legacy runtime or contract surface remains:" >&2
  printf '%s\n' "$residual" >&2
  exit 1
fi

echo "verify-release PASS"
