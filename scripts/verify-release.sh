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

restore_sample() {
  local sample="$1"
  local state_root="$2"
  if [[ ! -d "$sample/src/hiddenTest" ]]; then
    "$gradle" -p "$sample" -q -Dtopplecat.stateRoot="$state_root" toppleCatRestore >/dev/null 2>&1 || true
  fi
}

run_sample() {
  local sample="$1"
  local state_root="$2"
  shift 2
  "$gradle" -p "$sample" -Dtopplecat.stateRoot="$state_root" "$@"
}

cleanup() {
  local status=$?
  trap - EXIT
  set +e
  restore_sample "$junit_sample" "$junit_state_root"
  restore_sample "$spring_sample" "$spring_state_root"
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
bash "$root/scripts/verify-toppletest-stage-dsl.sh"
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

run_sample "$junit_sample" "$junit_state_root" help --task toppleCatHide
run_sample "$spring_sample" "$spring_state_root" help --task toppleCatHide
echo "Release verification: running red-team attacks. Each attack must be rejected; expected rejections are labelled below."
TOPPLECAT_STATE_ROOT="$junit_state_root" bash "$root/samples/junit-cart-orders/demo.sh"
TOPPLECAT_STATE_ROOT="$spring_state_root" bash "$root/samples/spring-boot-cart-orders/demo.sh"
TOPPLECAT_STATE_ROOT="$release_state_root/mutation-gate" bash "$root/integration-tests/mutation-gate/verify.sh"

assert_artifact_version "$junit_sample/build.gradle.kts" "0.0.4"
assert_artifact_version "$spring_sample/build.gradle.kts" "0.0.4"
assert_artifact_version "$root/integration-tests/mutation-gate/build.gradle.kts" "0.0.4"

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
    for required in ("JUNIT", "REVIEWER_JUNIT", "EXPECTED_CONSUMPTION"):
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

for spec in \
  "$junit_sample/build/topplecat/reports/spec/index.html" \
  "$spring_sample/build/topplecat/reports/spec/index.html"; do
  if grep -Eiq 'coupon-hidden-800|\b800\b|customer-2|ReviewerBoundary|hiddenTest' "$spec"; then
    echo "Private detail leaked into $spec" >&2
    exit 1
  fi
done

restore_sample "$junit_sample" "$junit_state_root"
run_sample "$junit_sample" "$junit_state_root" toppleCatCheck

if [[ -e "$junit_sample/build/topplecat/reports/review/index.html" ]]; then
  echo "toppleCatCheck must not leave a reviewer HTML artifact" >&2
  exit 1
fi

run_sample "$junit_sample" "$junit_state_root" toppleCatReview

restore_sample "$junit_sample" "$junit_state_root"
junit_review="$junit_sample/build/topplecat/reports/review/data.json"
if [[ ! -f "$junit_review" ]]; then
  echo "Reviewer review was not generated by toppleCatReview" >&2
  exit 1
fi
if ! grep -Fq 'coupon-hidden-800' "$junit_review"; then
  echo "Reviewer review did not include the reviewer case" >&2
  exit 1
fi
if ! grep -Fq '套用優惠券並建立訂單' "$junit_review" || ! grep -Fq 'sourceCode' "$junit_review"; then
  echo "Reviewer review is missing static stage context or source fallback" >&2
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
