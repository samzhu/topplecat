#!/usr/bin/env bash
# Regression test for the publication JDK preflight and its no-side-effect ordering.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

fake="$tmp/fake-gradle"
cat >"$fake" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
case "${FAKE_GRADLE_JDK:?}" in
  21)
    cat <<'OUT'
------------------------------------------------------------
Gradle 9.1.0
------------------------------------------------------------
Launcher JVM: 21.0.9 (test)
Daemon JVM: /test/java/21.0.9
OUT
    ;;
  25)
    cat <<'OUT'
------------------------------------------------------------
Gradle 9.1.0
------------------------------------------------------------
Launcher JVM: 25.0.1 (test)
Daemon JVM: /test/java/25.0.1
OUT
    ;;
esac
EOF
chmod +x "$fake"

set +e
rejected="$(FAKE_GRADLE_JDK=21 GRADLE_CMD="$fake" GRADLE_USER_HOME="$tmp" \
  "$root/scripts/publish-central.sh" --dry-run 2>&1)"
rejected_status=$?
set -e
if (( rejected_status == 0 )) || [[ "$rejected" != *"requires a JDK 25 Gradle runtime"* ]]; then
  echo "Publication preflight regression: JDK 21 was not rejected before side effects." >&2
  printf '%s\n' "$rejected" >&2
  exit 1
fi

accepted="$(FAKE_GRADLE_JDK=25 GRADLE_CMD="$fake" GRADLE_USER_HOME="$tmp" \
  "$root/scripts/publish-central.sh" --dry-run 2>&1)"
if [[ "$accepted" != *"Gradle runtime preflight PASS"* ]] \
  || [[ "$accepted" != *"Dry run: would run the release gate"* ]]; then
  echo "Publication preflight regression: JDK 25 dry-run did not pass." >&2
  printf '%s\n' "$accepted" >&2
  exit 1
fi

echo "publish-central preflight PASS: JDK 21 refusal and JDK 25 dry-run are side-effect free."
