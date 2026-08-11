#!/usr/bin/env bash
# A synthetic, repeatable ToppleCat learning project. It never changes checked-in source.
set -euo pipefail

sample="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
scenario="${1:---help}"

usage() {
  cat <<'EOF'
ToppleCat cart-orders learning project (all data is synthetic teaching material)

Choose what you want to understand:
  public-acceptance      Public typed examples reject a wrong result.
  hidden-tests           Independent examples reject a shortcut that public rows miss.
  property-based-testing A bounded invariant rejects a gap between example rows.
  mutation-testing       Managed PIT finds a production change the acceptance method misses.
  contract-integrity     A post-Seal contract change is not trusted.
  all                    Run all five lessons.

Run: ./demo.sh <lesson>
EOF
}

case "$scenario" in
  --help|-h|help) usage; exit 0 ;;
  public-acceptance|hidden-tests|property-based-testing|mutation-testing|contract-integrity|all) ;;
  *) echo "Unknown lesson: $scenario" >&2; usage >&2; exit 2 ;;
esac

run_lesson() {
  local lesson="$1"
  local work state service public_cases acceptance build_file report_dir
  work="$(mktemp -d)"
  state="$(mktemp -d)"
  trap 'rm -rf "$work" "$state"' RETURN
  cp -R "$sample/." "$work"
  rm -rf "$work/build" "$work/.gradle"
  service="$work/src/main/java/sample/cartorders/OrderService.java"
  public_cases="$work/src/test/resources/topplecat/cases/coupon-public.json"
  acceptance="$work/src/test/java/sample/cartorders/CouponAcceptanceTest.java"
  build_file="$work/build.gradle.kts"
  report_dir="$sample/build/topplecat/demo-reports/$lesson"

  printf '\n== %s: synthetic teaching material ==\n' "$lesson"
  case "$lesson" in
    public-acceptance)
      echo "Synthetic delivery: SAVE100 now returns no discount; public examples observe it."
      set_fixed_discount "$service"
      (cd "$work" && ./gradlew -q -Dtopplecat.stateRoot="$state" toppleCatSeal)
      replace_text "$service" 'int discount = "SAVE100".equals(cart.coupon()) ? 100 : 0;' 'int discount = 0;'
      expect_gate "$work" "$state" JUNIT
      echo "Supports: public examples caught this stated result. Cannot prove omitted rules do not exist."
      ;;
    hidden-tests)
      echo "Synthetic delivery: the checked-in 20% shortcut still passes the visible 500 example."
      (cd "$work" && ./gradlew -q -Dtopplecat.stateRoot="$state" toppleCatSeal)
      expect_gate "$work" "$state" REVIEWER_JUNIT
      echo "Supports: independent examples challenged the same public rule. It adds no private rule."
      ;;
    property-based-testing)
      echo "Synthetic delivery: a shortcut handles only the known example subtotals."
      echo "It passes Typed Case Rows but violates the fixed-discount invariant for generated carts."
      set_fixed_discount "$service"
      (cd "$work" && ./gradlew -q -Dtopplecat.stateRoot="$state" toppleCatSeal)
      replace_text "$service" 'int discount = "SAVE100".equals(cart.coupon()) ? 100 : 0;' 'int discount = "SAVE100".equals(cart.coupon()) && (cart.subtotal() == 500 || cart.subtotal() == 800 || cart.subtotal() == 1200) ? 100 : 0;'
      expect_gate "$work" "$state" PROPERTY
      echo "Supports: generated trials found an invariant violation. It is not proof of every input."
      ;;
    mutation-testing)
      echo "Synthetic delivery: the public acceptance method observes only that a receipt exists."
      echo "Managed PIT now changes production behavior and finds an attributed survivor."
      set_fixed_discount "$service"
      (cd "$work" && ./gradlew -q -Dtopplecat.stateRoot="$state" toppleCatSeal toppleCatVerify)
      python3 - "$build_file" "$acceptance" <<'PY'
import pathlib, sys
build = pathlib.Path(sys.argv[1])
build.write_text(build.read_text().replace('enabled.set(false)', 'enabled.set(true)'))
path = pathlib.Path(sys.argv[2])
path.write_text(path.read_text().replace(
    'c.verify("receipt", receipt);',
    'c.verify("receipt", new OrderReceipt("SAVE100".equals(cart.coupon()) ? 100 : 0, "SAVE100".equals(cart.coupon()) ? cart.subtotal() - 100 : cart.subtotal()));'))
PY
      (cd "$work" && ./gradlew -q -Dtopplecat.stateRoot="$state" toppleCatRestore toppleCatReseal)
      expect_gate "$work" "$state" MUTATION
      echo "Supports: this acceptance method missed an attributed production change. It does not score all quality."
      ;;
    contract-integrity)
      echo "Synthetic delivery: a public expected value changes after the Mechanical Seal."
      set_fixed_discount "$service"
      (cd "$work" && ./gradlew -q -Dtopplecat.stateRoot="$state" toppleCatSeal)
      python3 - "$public_cases" <<'PY'
import pathlib, sys
path = pathlib.Path(sys.argv[1])
path.write_text(path.read_text().replace('"discount": 100', '"discount": 99', 1))
PY
      expect_integrity_failure "$work" "$state"
      echo "Supports: Verify refused a changed contract. A Seal is not human approval."
      ;;
  esac

  retain_report "$work" "$report_dir"
  printf 'Read the synthetic Verification Report: %s\n' "$report_dir/index.html"
}

set_fixed_discount() {
  replace_text "$1" 'int discount = "SAVE100".equals(cart.coupon()) ? cart.subtotal() / 5 : 0;' 'int discount = "SAVE100".equals(cart.coupon()) ? 100 : 0;'
}

retain_report() {
  local work="$1" report_dir="$2" source="$work/build/topplecat/reports/verification"
  if [[ ! -f "$source/index.html" ]]; then
    echo "Lesson failed: Verify did not write a Verification Report." >&2
    return 1
  fi
  rm -rf "$report_dir"
  mkdir -p "$(dirname "$report_dir")"
  cp -R "$source" "$report_dir"
}

replace_text() {
  local file="$1" expected="$2" replacement="$3"
  python3 - "$file" "$expected" "$replacement" <<'PY'
import pathlib, sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()
if sys.argv[2] not in text:
    raise SystemExit(f"Synthetic lesson cannot find expected source in {path}")
path.write_text(text.replace(sys.argv[2], sys.argv[3], 1))
PY
}

expect_gate() {
  local work="$1" state="$2" gate="$3"
  if (cd "$work" && ./gradlew -q -Dtopplecat.stateRoot="$state" toppleCatVerify); then
    echo "Lesson failed: Verify unexpectedly passed." >&2; return 1
  fi
  python3 - "$work/build/topplecat/evidence.json" "$gate" <<'PY'
import json, pathlib, sys
evidence = json.loads(pathlib.Path(sys.argv[1]).read_text())
gates = {item['name']: item['verdict'] for item in evidence['gates']}
print(f"Gate results: {gates}")
if evidence['verdict'] != 'FAIL' or gates.get('CONTRACT_INTEGRITY') != 'PASS' or gates.get(sys.argv[2]) != 'FAIL':
    raise SystemExit(f"Unexpected lesson evidence: {gates}")
print(f"Confirmed: {sys.argv[2]}=FAIL in synthetic current-run evidence.")
PY
}

expect_integrity_failure() {
  local work="$1" state="$2"
  if (cd "$work" && ./gradlew -q -Dtopplecat.stateRoot="$state" toppleCatVerify); then
    echo "Lesson failed: Verify unexpectedly passed." >&2; return 1
  fi
  python3 - "$work/build/topplecat/evidence.json" <<'PY'
import json, pathlib, sys
gates = {item['name']: item['verdict'] for item in json.loads(pathlib.Path(sys.argv[1]).read_text())['gates']}
if gates.get('CONTRACT_INTEGRITY') != 'FAIL':
    raise SystemExit(f"Expected CONTRACT_INTEGRITY=FAIL, got {gates}")
print("Confirmed: CONTRACT_INTEGRITY=FAIL in synthetic current-run evidence.")
PY
}

if [[ "$scenario" == "all" ]]; then
  for lesson in public-acceptance hidden-tests property-based-testing mutation-testing contract-integrity; do run_lesson "$lesson"; done
else
  run_lesson "$scenario"
fi
