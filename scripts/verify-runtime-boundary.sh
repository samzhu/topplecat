#!/usr/bin/env bash
# Checks the supported runtime floor before a consumer invokes the plugin.
set -euo pipefail

java_major="$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+)(\..*)?".*/\1/p' | head -n 1)"
if [[ ! "$java_major" =~ ^[0-9]+$ ]]; then
  echo "ToppleCat unsupported runtime: could not determine the active Java version." >&2
  exit 1
fi
if (( java_major < 21 )); then
  echo "ToppleCat unsupported runtime: JDK 21 or newer is required; detected JDK $java_major." >&2
  echo "A Java 17 consumer source target is supported only when the Gradle/plugin execution environment runs on JDK 21 or 25." >&2
  exit 1
fi
echo "ToppleCat runtime PASS: JDK $java_major is supported."
