#!/usr/bin/env bash
# Validates the tracked ToppleCat repository skills.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
skill="topplecat-acceptance"
skill_root="$root/.agents/skills/$skill"
skill_path="$skill_root/SKILL.md"
interface_path="$skill_root/agents/openai.yaml"
old_skill_root="$root/.agents/skills/topplecat-verification"
references=(authoring.md safeguards.md reports.md)

fail() {
  echo "ToppleCat skill validation failed: $1" >&2
  exit 1
}

[[ -f "$skill_path" ]] || fail "$skill_path was not found."
[[ -f "$interface_path" ]] || fail "$interface_path was not found."
grep -Fq 'CONTEXT.md' "$skill_path" || fail "SKILL.md must require the root context glossary."
[[ ! -e "$old_skill_root" ]] || fail "legacy skill directory remains: $old_skill_root"
[[ "$(sed -n '1p' "$skill_path")" == '---' ]] || fail "missing YAML front matter."
[[ "$(sed -n '2p' "$skill_path")" == "name: $skill" ]] || fail "stale skill name."
[[ "$(sed -n '3p' "$skill_path")" == description:* ]] || fail "missing description."
[[ "$(sed -n '4p' "$skill_path")" == 'metadata:' ]] || fail "missing metadata."
[[ "$(sed -n '6p' "$skill_path")" == '---' ]] || fail "front matter must end on line 6."

description="$(sed -n '3s/^description: //p' "$skill_path")"
for trigger in \
  'ToppleCat' \
  'Java/JUnit' \
  'Acceptance Conditions' \
  'Typed Case Rows' \
  'Property-Based Testing' \
  'Use when'; do
  [[ "$description" == *"$trigger"* ]] \
    || fail "description is missing its $trigger trigger."
done

skill_version="$(sed -n 's/^  topplecat-version: "\([^"]*\)"$/\1/p' "$skill_path")"
project_version="$(sed -n 's/^[[:space:]]*version = "\([^"]*\)"$/\1/p' "$root/build.gradle.kts" | head -n 1)"
code_version="$(
  sed -n 's/.*CURRENT = "\([^"]*\)".*/\1/p' \
    "$root/topplecat-gradle-plugin/src/main/java/io/github/samzhu/topplecat/gradle/ToppleCatVersion.java"
)"
[[ -n "$skill_version" ]] || fail "metadata.topplecat-version is missing."
[[ "$skill_version" == "$project_version" ]] \
  || fail "skill version $skill_version differs from Gradle project version $project_version."
[[ "$skill_version" == "$code_version" ]] \
  || fail "skill version $skill_version differs from ToppleCatVersion.CURRENT $code_version."

for reference in "${references[@]}"; do
  path="$skill_root/references/$reference"
  [[ -s "$path" ]] || fail "$path was not found or is empty."
  grep -Fq "references/$reference" "$skill_path" \
    || fail "$skill_path does not link references/$reference."
done

for required in \
  'AC-...' \
  '@ToppleAcceptanceTest' \
  'reviewer rows' \
  '@ToppleProperty' \
  'unresolved gaps' \
  'External workflow automation and humans execute'; do
  grep -Fq -- "$required" "$skill_path" || fail "missing required behavior: $required"
done

for required in \
  'ToppleCase' \
  'ToppleScenario' \
  'ToppleStage' \
  'step().attach(...)' \
  'c.verify('; do
  grep -Fq -- "$required" "$skill_root/references/authoring.md" \
    || fail "authoring reference is missing current API: $required"
done

grep -Fq 'Step sentences' "$skill_root/references/reports.md" \
  || fail "reports reference must use the formal Step sentence term."
! grep -R -Fq 'Stage sentences' "$skill_root" \
  || fail "Stage sentences is stale terminology; use Step sentences."

for required in \
  'Hidden Tests' \
  'Mutation Testing' \
  'Property-Based Testing' \
  'REVIEWER_JUNIT=INCOMPLETE' \
  'REVIEWER_JUNIT=DISABLED'; do
  grep -Fq -- "$required" "$skill_root/references/safeguards.md" \
    || fail "safeguards reference is missing current behavior: $required"
done

for required in \
  'Spec Review' \
  'Verification Report' \
  'Current-run Evidence' \
  'Safe agent feedback'; do
  grep -Fq -- "$required" "$skill_root/references/reports.md" \
    || fail "reports reference is missing current artifact: $required"
done

grep -Fq 'display_name: "ToppleCat Acceptance"' "$interface_path" \
  || fail "agents/openai.yaml has a stale display name."
grep -Fq '$topplecat-acceptance' "$interface_path" \
  || fail "agents/openai.yaml has a stale default prompt."

if grep -Eq '\\./gradlew[[:space:]]+toppleCat' "$skill_path"; then
  fail "SKILL.md must leave ToppleCat task execution to people or external workflow automation."
fi

for forbidden in '@ToppleTest([^A-Za-z0-9_]|$)' '@ToppleAc([^A-Za-z0-9_]|$)' \
  'toppleCatHide([^A-Za-z0-9_]|$)' 'toppleCatUpdateEscrow([^A-Za-z0-9_]|$)' \
  'hiddenRetest([^A-Za-z0-9_]|$)'; do
  ! grep -R -Eq -- "$forbidden" "$skill_root" || fail "legacy term remains: $forbidden"
done

! grep -R -Fq 'topplecat-verification' "$skill_root" \
  || fail "legacy skill name remains inside the new skill."
! grep -R -Fq '[TODO' "$skill_root" || fail "skill template TODO remains."

line_count="$(wc -l < "$skill_path" | tr -d ' ')"
word_count="$(wc -w < "$skill_path" | tr -d ' ')"
[[ "$line_count" -le 120 && "$word_count" -le 800 ]] \
  || fail "skill exceeds progressive-disclosure limits."

echo "ToppleCat skill validation PASS: $skill $skill_version"

release_skill="topplecat-release"
release_root="$root/.agents/skills/$release_skill"
release_path="$release_root/SKILL.md"
release_interface="$release_root/agents/openai.yaml"
release_references=(release-notes.md maintainer-publishing.md)

[[ -f "$release_path" ]] || fail "$release_path was not found."
[[ -f "$release_interface" ]] || fail "$release_interface was not found."
[[ "$(sed -n '1p' "$release_path")" == '---' ]] || fail "$release_skill is missing YAML front matter."
[[ "$(sed -n '2p' "$release_path")" == "name: $release_skill" ]] || fail "$release_skill has a stale name."
[[ "$(sed -n '3p' "$release_path")" == description:* ]] || fail "$release_skill is missing a description."
[[ "$(sed -n '4p' "$release_path")" == '---' ]] || fail "$release_skill front matter must end on line 4."

release_description="$(sed -n '3s/^description: //p' "$release_path")"
[[ "$release_description" == "Prepare ToppleCat releases as an open-source Java framework."* ]] \
  || fail "$release_skill description lost its release trigger."
[[ "$release_description" == *"Use when"* ]] \
  || fail "$release_skill description must declare its branches."

for reference in "${release_references[@]}"; do
  path="$release_root/references/$reference"
  [[ -s "$path" ]] || fail "$path was not found or is empty."
  grep -Fq "references/$reference" "$release_path" \
    || fail "$release_path does not link references/$reference."
done

for required in \
  'CONTEXT.md' \
  'developer capability' \
  'site decision' \
  'generated notes' \
  'docs/validation/README.md' \
  'explicitly authorizes the release' \
  'annotated `X.Y.Z` tag' \
  'Do not require a' \
  'Push the verified tag to `origin`' \
  'Stop at the remote-tag boundary' \
  'Maven Central' \
  'GitHub Release'; do
  grep -Fq -- "$required" "$release_path" \
    || fail "$release_skill is missing required behavior: $required"
done

grep -Fq 'display_name: "ToppleCat Release"' "$release_interface" \
  || fail "$release_interface has a stale display name."
grep -Fq '$topplecat-release' "$release_interface" \
  || fail "$release_interface has a stale default prompt."
! grep -R -Fq '[TODO' "$release_root" || fail "$release_skill still contains a template TODO."

release_line_count="$(wc -l < "$release_path" | tr -d ' ')"
release_word_count="$(wc -w < "$release_path" | tr -d ' ')"
[[ "$release_line_count" -le 140 && "$release_word_count" -le 900 ]] \
  || fail "$release_skill exceeds progressive-disclosure limits."

echo "ToppleCat skill validation PASS: $release_skill"
