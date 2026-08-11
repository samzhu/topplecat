#!/usr/bin/env bash
# Runs the published-consumer demonstrations for the two supported Stage DSL samples.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "$root/samples/junit-cart-orders/demo.sh" all
for lesson in public-acceptance hidden-tests property-based-testing mutation-testing contract-integrity; do
  report="$root/samples/junit-cart-orders/build/topplecat/demo-reports/$lesson/index.html"
  if [[ ! -s "$report" ]] || ! grep -Fq '<html' "$report"; then
    echo "Missing synthetic Verification Report: $report" >&2
    exit 1
  fi
done
bash "$root/samples/spring-boot-cart-orders/demo.sh"

echo "verify-samples PASS"
