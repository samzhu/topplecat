#!/usr/bin/env bash
# Validates the tracked ToppleCat skill without depending on local agent tooling.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
skill="topplecat-verification"
skill_root="$root/.agents/skills/$skill"
skill_path="$skill_root/SKILL.md"
metadata_path="$skill_root/agents/openai.yaml"
former_skills=(
  topplecat-verification-shared
  topplecat-junit-verification-expert
)
references=(
  authoring.md
  reviewer-custody.md
  evidence.md
)

fail() {
  echo "ToppleCat skill validation failed: $1" >&2
  exit 1
}

[[ -f "$skill_path" ]] || fail "$skill_path was not found."
[[ -f "$metadata_path" ]] || fail "$metadata_path was not found."

for former_skill in "${former_skills[@]}"; do
  [[ ! -e "$root/.agents/skills/$former_skill" ]] \
    || fail "former skill directory .agents/skills/$former_skill must be removed."
done

[[ "$(sed -n '1p' "$skill_path")" == '---' ]] \
  || fail "$skill_path must start with YAML front matter."
[[ "$(sed -n '2p' "$skill_path")" == "name: $skill" ]] \
  || fail "$skill_path must declare name: $skill."
[[ "$(sed -n '3p' "$skill_path")" == description:\ * ]] \
  || fail "$skill_path must declare a non-empty description."
[[ "$(sed -n '4p' "$skill_path")" == '---' ]] \
  || fail "$skill_path front matter must end on line 4."
! grep -Fq 'disable-model-invocation:' "$skill_path" \
  || fail "$skill_path must remain model-invoked."

description="$(sed -n '3s/^description: //p' "$skill_path")"
[[ "$description" == "Gate Java delegation with ToppleCat."* ]] \
  || fail "description must front-load the Gate leading word."
[[ "$description" == *"Use when"* ]] \
  || fail "description must contain a positive 'Use when' trigger."
[[ "$description" == *"Don't use for"* ]] \
  || fail "description must contain a negative 'Don't use for' trigger."
[[ "$description" != *'<'* && "$description" != *'>'* ]] \
  || fail "description must not contain angle brackets."

for reference in "${references[@]}"; do
  reference_path="$skill_root/references/$reference"
  [[ -s "$reference_path" ]] || fail "$reference_path was not found or is empty."
  grep -Fq "references/$reference" "$skill_path" \
    || fail "$skill_path must load references/$reference just in time."
done

grep -Fxq '  display_name: "ToppleCat Verification"' "$metadata_path" \
  || fail "$metadata_path has a stale display_name."
grep -Fxq '  short_description: "Gate Java done claims with executable contracts"' "$metadata_path" \
  || fail "$metadata_path has a stale short_description."
grep -Fxq '  default_prompt: "Find the first unmet ToppleCat gate for this Java task, then author, review, hide, implement, restore, or verify it without crossing reviewer custody."' "$metadata_path" \
  || fail "$metadata_path has a stale default_prompt."

required_contract=(
  'Markdown-only AC is incomplete.'
  '@ToppleTest'
  '@ToppleStageField'
  './gradlew toppleCatCheck'
  './gradlew toppleCatReview'
  './gradlew toppleCatHide'
  './gradlew test'
  './gradlew toppleCatVerify'
  './gradlew toppleCatRestore'
  'build/topplecat/evidence.json'
  'build/topplecat/agent-feedback.json'
  '## Gate Invariants'
  '## Select The Gate'
  '### Author'
  '### Review And Hide'
  '### Implement'
  '### Verify'
  '### Restore'
  '### Assess Adoption'
)

for required in "${required_contract[@]}"; do
  grep -Fq -- "$required" "$skill_path" \
    || fail "$skill_path is missing required contract text: $required"
done

completion_count="$(grep -Fc '**Completion criterion:**' "$skill_path")"
[[ "$completion_count" -eq 7 ]] \
  || fail "$skill_path must contain exactly seven branch completion criteria; found $completion_count."

line_count="$(wc -l < "$skill_path" | tr -d ' ')"
word_count="$(wc -w < "$skill_path" | tr -d ' ')"
[[ "$line_count" -le 150 ]] \
  || fail "$skill_path has $line_count lines; keep the model-invoked body at or below 150."
[[ "$word_count" -le 1000 ]] \
  || fail "$skill_path has $word_count words; use progressive disclosure to stay at or below 1000."

for stale_heading in '## Examples' '## Error Handling'; do
  ! grep -Fxq "$stale_heading" "$skill_path" \
    || fail "$skill_path contains stale top-level reference: $stale_heading."
done

if grep -R -Fq '[TODO:' "$skill_root"; then
  fail "$skill_root still contains a template TODO."
fi

echo "ToppleCat skill validation PASS: $skill"
