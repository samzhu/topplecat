#!/usr/bin/env bash
# Regression test for verify-release.sh temporary state cleanup on success and failure.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tmp_parent="$(mktemp -d)"
success_log="$(mktemp)"

cleanup() {
  local status=$?
  trap - EXIT
  rm -f "$success_log"
  rm -rf "$tmp_parent"
  exit "$status"
}
trap cleanup EXIT

assert_empty() {
  if find "$tmp_parent" -mindepth 1 -maxdepth 1 -print -quit | grep -q .; then
    echo "verify-release cleanup regression: temporary state leaked under $tmp_parent" >&2
    find "$tmp_parent" -mindepth 1 -maxdepth 1 -print >&2
    exit 1
  fi
}

failing_gradle="$tmp_parent/failing-gradle"
printf '%s\n' '#!/usr/bin/env bash' 'exit 97' > "$failing_gradle"
chmod +x "$failing_gradle"

if TMPDIR="$tmp_parent" GRADLE_CMD="$failing_gradle" "$root/scripts/verify-release.sh" >/dev/null 2>&1; then
  echo "verify-release cleanup regression: failure path unexpectedly passed" >&2
  exit 1
fi
rm -f "$failing_gradle"
assert_empty

TMPDIR="$tmp_parent" GRADLE_CMD="$root/gradlew" "$root/scripts/verify-release.sh" >"$success_log" 2>&1
assert_empty

for expected in \
  "Confirmed: REVIEWER_JUNIT=FAIL in synthetic current-run evidence." \
  "EXPECTED FAILURE: Spring hidden-test attack was rejected." \
  "EXPECTED FAILURE: Mutation-gate attack was rejected." \
  "Confirmed current-run evidence and safe agent feedback: REVIEWER_JUNIT=FAIL." \
  "Confirmed current-run evidence and safe agent feedback: MUTATION=FAIL." \
  "verify-release PASS"; do
  if ! grep -Fq "$expected" "$success_log"; then
    echo "verify-release output regression: missing expected status: $expected" >&2
    cat "$success_log" >&2
    exit 1
  fi
done

if grep -Fq "BUILD FAILED" "$success_log"; then
  echo "verify-release output regression: successful release output exposed a raw BUILD FAILED" >&2
  cat "$success_log" >&2
  exit 1
fi

if grep -Fq "warning: no " "$success_log"; then
  echo "verify-release output regression: Javadoc missing-comment warnings were not suppressed" >&2
  cat "$success_log" >&2
  exit 1
fi

echo "verify-release cleanup PASS: success and failure paths remove temporary state"
