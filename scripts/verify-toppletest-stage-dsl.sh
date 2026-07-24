#!/usr/bin/env bash
# Release gate: canonical @ToppleTest Stage DSL authoring contract.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gradle="${GRADLE_CMD:-$root/gradlew}"

cd "$root"
"$gradle" :topplecat-junit:test \
  --tests io.github.samzhu.topplecat.junit.ToppleScenarioProcessorTest
"$gradle" :topplecat-gradle-plugin:test \
  --tests io.github.samzhu.topplecat.gradle.ToppleCatPluginFunctionalTest.enforcesTheCanonicalStageDslThroughCheckAndKeepsToppleAcAsOrdinaryJUnitCoverage \
  --tests io.github.samzhu.topplecat.gradle.ToppleCatPluginFunctionalTest.verifiesHiddenCasesWritesSeparatedReportsAndRehidesReviewerSource

echo "ToppleTest Stage DSL authoring contract PASS"
