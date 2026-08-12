#!/usr/bin/env bash
# Publishes the X.Y.Z-tagged release to the Central Portal as a user-managed deployment.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gradle="${GRADLE_CMD:-$root/gradlew}"
namespace="${CENTRAL_NAMESPACE:-io.github.samzhu}"
dry_run=false

usage() {
  cat <<'EOF'
Usage: scripts/publish-central.sh [--dry-run]

Publishes the current X.Y.Z-tagged release through the Central Portal staging API.
The script runs the complete release gate, signs artifacts with GPG, uploads
them, and creates a USER_MANAGED Portal deployment. It never publishes the
deployment to Maven Central automatically. If untracked files are present, the
script asks whether to continue; staged or unstaged changes still stop it.

Environment variables:
  CENTRAL_NAMESPACE  Central Portal namespace (default: io.github.samzhu)
  GRADLE_CMD         Gradle command to use (default: ./gradlew)
  GRADLE_USER_HOME   Gradle user home containing gradle.properties
EOF
}

fail() {
  printf 'Release failed: %s\n' "$*" >&2
  exit 1
}

cancel() {
  printf 'Release cancelled: %s\n' "$*" >&2
  exit 1
}

read_gradle_property() {
  local property="$1"
  awk -F= -v property="$property" '
    $1 == property {
      print substr($0, length(property) + 2)
      exit
    }
  ' "$credentials_file"
}

cleanup_credentials() {
  unset token_username token_password bearer_token
}
trap cleanup_credentials EXIT

confirm_untracked_files() {
  local untracked_files answer

  untracked_files="$(git -C "$root" ls-files --others --exclude-standard)"
  [[ -z "$untracked_files" ]] && return 0

  printf 'Working tree has untracked files:\n%s\n' "$untracked_files" >&2
  [[ -t 0 ]] || fail "cannot ask whether to continue with untracked files without an interactive terminal"

  while true; do
    if ! read -r -p 'Continue publishing with these untracked files? [y/n] ' answer; then
      cancel "no decision was provided"
    fi

    case "$answer" in
      y|Y) return 0 ;;
      n|N) cancel "untracked files remain" ;;
      *) printf 'Please answer y or n.\n' >&2 ;;
    esac
  done
}

case "${1:-}" in
  "") ;;
  --dry-run) dry_run=true ;;
  --help|-h)
    usage
    exit 0
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac

[[ "$namespace" =~ ^[[:alnum:]][[:alnum:]._-]*$ ]] || fail "invalid Central namespace: $namespace"
[[ -x "$gradle" ]] || fail "Gradle wrapper is not executable: $gradle"

# The release artifact is compiled with --release 21, but Central publication is
# intentionally restricted to the JDK 25 environment that owns this release
# line. Inspect the JVMs reported by this exact Gradle command before reading
# credentials or making any Central, signing, or upload call.
gradle_runtime="$("$gradle" --version 2>&1)" || \
  fail "could not inspect the Gradle runtime with $gradle"
launcher_jvm="$(printf '%s\n' "$gradle_runtime" | sed -n 's/^Launcher JVM: //p' | head -n 1)"
daemon_jvm="$(printf '%s\n' "$gradle_runtime" | sed -n 's/^Daemon JVM: //p' | head -n 1)"
[[ -n "$launcher_jvm" && -n "$daemon_jvm" ]] || \
  fail "Gradle runtime inspection did not report both Launcher JVM and Daemon JVM"
if [[ ! "$launcher_jvm" =~ (^|[^0-9])25([.][0-9]+|[^0-9]|$) ]] \
  || [[ ! "$daemon_jvm" =~ (^|[^0-9])25([.][0-9]+|[^0-9]|$) ]]; then
  fail "Central publication requires a JDK 25 Gradle runtime; detected Launcher JVM '$launcher_jvm' and Daemon JVM '$daemon_jvm'"
fi
printf 'Gradle runtime preflight PASS: Launcher JVM %s; Daemon JVM %s\n' "$launcher_jvm" "$daemon_jvm"

credentials_file="${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties"

if [[ "$dry_run" == true ]]; then
  printf 'Dry run: would run the release gate, sign with GPG, upload to %s, and create a USER_MANAGED Portal deployment.\n' \
    "$namespace"
  exit 0
fi

git -C "$root" diff --quiet || fail "working tree has unstaged changes"
git -C "$root" diff --cached --quiet || fail "working tree has staged changes"
confirm_untracked_files

version="$("$gradle" -q properties | awk -F': ' '$1 == "version" { print $2; exit }')"
[[ -n "$version" ]] || fail "could not determine the Gradle project version"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || \
  fail "Central releases must use an X.Y.Z version (got: $version)"
[[ "$version" != *SNAPSHOT* ]] || fail "Central releases must not use a SNAPSHOT version: $version"

tag="$version"
git -C "$root" tag --points-at HEAD | grep -Fxq "$tag" || \
  fail "current HEAD must be tagged $tag (without a v prefix) before publishing"

central_path="${namespace//.//}/topplecat/topplecat-core/$version/topplecat-core-$version.pom"
central_status="$(curl --silent --show-error --location --output /dev/null --write-out '%{http_code}' --head \
  "https://repo.maven.apache.org/maven2/$central_path")"
case "$central_status" in
  404) ;;
  200) fail "io.github.samzhu.topplecat:topplecat-core:$version already exists on Maven Central" ;;
  *) fail "could not verify Maven Central version availability (HTTP $central_status)" ;;
esac

[[ -f "$credentials_file" ]] || fail "missing Central credentials file: $credentials_file"
token_username="$(read_gradle_property centralPortalUsername)"
token_password="$(read_gradle_property centralPortalPassword)"
[[ -n "$token_username" && -n "$token_password" ]] || \
  fail "centralPortalUsername and centralPortalPassword must be set in $credentials_file"

bearer_token="$(printf '%s:%s' "$token_username" "$token_password" | base64 | tr -d '\n')"
auth_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
  --header "Authorization: Bearer $bearer_token" \
  "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?ip=any&profile_id=$namespace")"
[[ "$auth_status" == 200 ]] || fail "Central Portal token was rejected (HTTP $auth_status)"

printf 'Running the complete release gate for %s...\n' "$version"
GRADLE_CMD="$gradle" "$root/scripts/verify-release.sh"

[[ -t 0 ]] || fail "run this script from an interactive terminal so GPG can request its passphrase"
export GPG_TTY="$(tty)"
gpg-connect-agent updatestartuptty /bye >/dev/null
printf 'ToppleCat %s release signing preflight\n' "$version" | gpg --clearsign >/dev/null

printf 'Uploading signed artifacts for %s...\n' "$version"
cd "$root"
"$gradle" --no-daemon publishAllPublicationsToCentralStagingRepository \
  -PcentralRelease=true \
  --console=plain

printf 'Creating the Central Portal deployment...\n'
curl --fail-with-body --silent --show-error --request POST \
  --header "Authorization: Bearer $bearer_token" \
  "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$namespace?publishing_type=user_managed"
printf '\nRelease %s is awaiting validation in https://central.sonatype.com/publishing/deployments\n' "$version"
printf 'When its state is VALIDATED, review it and click Publish in the Portal.\n'
