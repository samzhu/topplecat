#!/usr/bin/env bash
# Demonstrates the Spring Hidden Tests workflow, then restores the checked-in sample state.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
sample="$root/samples/spring-boot-cart-orders"
gradle="$root/gradlew"
source "$root/scripts/expected-rejection.sh"
service="$sample/src/main/java/sample/cartorders/OrderService.java"
service_backup="$(mktemp)"
owns_state_root=false
if [[ -n "${TOPPLECAT_STATE_ROOT:-}" ]]; then
  state_root="$TOPPLECAT_STATE_ROOT"
else
  state_root="$(mktemp -d)"
  owns_state_root=true
fi
mkdir -p "$state_root"

run_sample() {
  "$gradle" -p "$sample" -Ptopplecat.useMavenLocal=true -Dtopplecat.stateRoot="$state_root" "$@"
}

cleanup() {
  local status=$?
  trap - EXIT INT TERM
  set +e
  if [[ -f "$service_backup" ]]; then
    cp "$service_backup" "$service"
    rm -f "$service_backup"
  fi
  if [[ ! -d "$sample/src/hiddenTest" ]]; then
    run_sample -q toppleCatRestore >/dev/null 2>&1 \
      || echo "Demo cleanup warning: could not restore reviewer source for $sample." >&2
  fi
  if [[ "$owns_state_root" == true ]]; then
    rm -rf "$state_root"
  fi
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if [[ ! -f "$service" ]]; then
  echo "Spring cart-orders demo failed: missing $service. Restore the checked-in sample source before retrying." >&2
  exit 1
fi
cp "$service" "$service_backup"

if [[ ! -d "$sample/src/hiddenTest" ]]; then
  run_sample toppleCatRestore || true
fi
if [[ -d "$sample/.topplecat" ]]; then
  rm -rf "$sample/.topplecat"
fi
rm -rf "$sample/build"
cp "$sample/demo/OrderService.broken.java" "$service"

"$gradle" publishToMavenLocal
run_sample toppleCatCheck toppleCatSeal

if ! expect_topplecat_rejection \
  "Spring hidden-test attack" \
  "$sample/build/topplecat/evidence.json" \
  "$sample/build/topplecat/agent-feedback.json" \
  "REVIEWER_JUNIT" \
  run_sample toppleCatVerify; then
  exit 1
fi

cp "$sample/demo/OrderService.fixed.java" "$service"
run_sample toppleCatVerify

echo "evidence: $sample/build/topplecat/evidence.json"
echo "agent feedback: $sample/build/topplecat/agent-feedback.json"
