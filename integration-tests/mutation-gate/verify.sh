#!/usr/bin/env bash
# Proves that a surviving PIT mutant fails ToppleCat verification without leaking private details.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
project="$root/integration-tests/mutation-gate"
gradle="$root/gradlew"

"$gradle" publishToMavenLocal
set +e
"$gradle" -p "$project" toppleCatVerify
status=$?
set -e

if [[ $status -eq 0 ]]; then
  echo "Expected a surviving mutant to fail verification." >&2
  exit 1
fi

mutation="$project/build/topplecat/mutation-results.json"
evidence="$project/build/topplecat/evidence.json"
feedback="$project/build/topplecat/agent-feedback.json"
for artifact in "$mutation" "$evidence" "$feedback"; do
  if [[ ! -f "$artifact" ]]; then
    echo "Mutation-gate integration test failed: expected artifact is missing: $artifact. Run the verification task and inspect its Gradle output." >&2
    exit 1
  fi
done
grep -Fq '"MUTATION"' "$evidence"
grep -Fq '"FAIL"' "$mutation"
if grep -Fq 'AC-MUTATION-GATE' "$feedback"; then
  echo "Agent feedback leaked an acceptance-condition identifier." >&2
  exit 1
fi

echo "mutation-gate integration test PASS: expected verification failure with safe feedback"
