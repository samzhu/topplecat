#!/usr/bin/env bash
# Runs a self-contained consumer contract at an explicit source and execution JDK.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
runtime="${TOPPLECAT_CONSUMER_JDK:-$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+)(\..*)?".*/\1/p' | head -n 1)}"
source_release="${TOPPLECAT_CONSUMER_RELEASE:-17}"
[[ "$runtime" =~ ^(21|25)$ ]] || {
  echo "Consumer compatibility requires execution JDK 21 or 25; got $runtime." >&2
  exit 1
}
[[ "$source_release" =~ ^(17|21|25)$ ]] || {
  echo "Consumer compatibility supports source releases 17, 21, and 25; got $source_release." >&2
  exit 1
}

work="$(mktemp -d)"
state="$(mktemp -d)"
cleanup() {
  local status=$?
  rm -rf "$work" "$state"
  exit "$status"
}
trap cleanup EXIT

"$root/gradlew" -Ptopplecat.buildJdk="$runtime" publishToMavenLocal
cp -R "$root/samples/junit-cart-orders/." "$work/"
rm -rf "$work/build" "$work/.gradle"

# The checked-in learning sample is intentionally a synthetic shortcut. The
# compatibility fixture verifies the supported execution path with a correct
# implementation while retaining its public and reviewer-owned contract rows.
python3 - "$work/src/main/java/sample/cartorders/OrderService.java" <<'PY'
import pathlib, sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()
wrong = 'int discount = "SAVE100".equals(cart.coupon()) ? cart.subtotal() / 5 : 0;'
correct = 'int discount = "SAVE100".equals(cart.coupon()) ? 100 : 0;'
if wrong not in text:
    raise SystemExit(f"Consumer compatibility fixture cannot find synthetic shortcut in {path}")
path.write_text(text.replace(wrong, correct, 1))
PY

gradle=("$root/gradlew" "-p" "$work" "-Ptopplecat.useMavenLocal=true"
  "-Ptopplecat.consumerJdk=$runtime" "-Ptopplecat.consumerRelease=$source_release"
  "-Dtopplecat.stateRoot=$state")
"${gradle[@]}" test
"${gradle[@]}" toppleCatSeal toppleCatVerify

echo "Consumer compatibility PASS: source target $source_release, execution JDK $runtime, ordinary and formal verification completed."
