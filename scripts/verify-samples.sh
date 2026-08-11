#!/usr/bin/env bash
# Runs the published-consumer demonstrations for the two supported Stage DSL samples.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "$root/samples/junit-cart-orders/demo.sh" all
bash "$root/samples/spring-boot-cart-orders/demo.sh"

echo "verify-samples PASS"
