#!/usr/bin/env bash
# Release gate: single Scenario authoring contract.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gradle="${GRADLE_CMD:-$root/gradlew}"

cd "$root"
"$gradle" :topplecat-junit:test \
  --tests io.github.samzhu.topplecat.junit.ToppleAcceptanceProcessorTest
"$gradle" :topplecat-gradle-plugin:test \
  --tests io.github.samzhu.topplecat.gradle.ToppleCatPluginFunctionalTest

echo "Scenario authoring contract PASS"
