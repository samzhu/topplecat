#!/usr/bin/env bash
# Demonstrates the Spring hidden retest workflow, then restores the checked-in sample state.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
sample="$root/samples/spring-boot-cart-orders"
gradle="$root/gradlew"
service="$sample/src/main/java/sample/cartorders/OrderService.java"
service_backup="$(mktemp)"

cleanup() {
  local status=$?
  trap - EXIT INT TERM
  set +e
  if [[ -f "$service_backup" ]]; then
    cp "$service_backup" "$service"
    rm -f "$service_backup"
  fi
  if [[ -f "$sample/.topplecat/escrow/manifest.json" && ! -d "$sample/src/hiddenTest" ]]; then
    "$gradle" -p "$sample" -q toppleCatRestore >/dev/null 2>&1 \
      || echo "Demo cleanup warning: could not restore reviewer source for $sample. Run ./gradlew -p samples/spring-boot-cart-orders toppleCatRestore." >&2
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

if [[ -f "$sample/.topplecat/escrow/manifest.json" && ! -d "$sample/src/hiddenTest" ]]; then
  "$gradle" -p "$sample" toppleCatRestore
fi
rm -rf "$sample/.topplecat" "$sample/build"
cp "$sample/demo/OrderService.broken.java" "$service"

"$gradle" publishToMavenLocal
"$gradle" -p "$sample" toppleCatCheck toppleCatHide

set +e
"$gradle" -p "$sample" toppleCatVerify
first_exit=$?
set -e
if [[ $first_exit -eq 0 ]]; then
  echo "Expected hidden retest failure, but verification passed." >&2
  exit 1
fi

cp "$sample/demo/OrderService.fixed.java" "$service"
"$gradle" -p "$sample" toppleCatVerify

echo "evidence: $sample/build/topplecat/evidence.json"
echo "agent feedback: $sample/build/topplecat/agent-feedback.json"
