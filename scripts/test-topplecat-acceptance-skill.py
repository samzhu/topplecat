#!/usr/bin/env python3
"""Deterministic contract checks for the installed acceptance-skill package.

This test reads the instruction package and a synthetic canonical Markdown
fixture. It does not invoke a model, implement a Markdown parser, maintain a
selection registry, or claim to be a real skill invocation. The product Java
parser remains the only authority for AC structure and inventory.
"""

from __future__ import annotations

import copy
import json
import re
import shutil
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
PACKAGE = ROOT / ".agents/skills/topplecat-acceptance"
FIXTURE = ROOT / "scripts/fixtures/topplecat-acceptance-skill/project"
VERSION = "0.2.1"
MARKER = "<!-- topplecat:acceptance -->"
SELECTED_DOCUMENTS = ["specs/checkout.md", "specs/payment.md"]
MISSING_FILE_PATH = "specs/missing-from-fixture.md"


class SkillContractError(Exception):
    """Raised when the installed source contract is incomplete or contradictory."""


def read_json(path: Path) -> dict[str, object]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise SkillContractError(f"cannot read fixture JSON {path}: {error}") from error
    if not isinstance(value, dict):
        raise SkillContractError(f"fixture JSON must be an object: {path}")
    return value


def read_json_array(path: Path) -> list[dict[str, object]]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise SkillContractError(f"cannot read fixture JSON array {path}: {error}") from error
    if not isinstance(value, list) or not all(isinstance(item, dict) for item in value):
        raise SkillContractError(f"fixture JSON must be an array of objects: {path}")
    return value


def normalized(text: str) -> str:
    return " ".join(text.split())


def assert_contains(text: str, *phrases: str) -> None:
    for phrase in phrases:
        if phrase not in text and normalized(phrase) not in normalized(text):
            raise SkillContractError(f"instruction source is missing: {phrase}")


def inspect_package() -> str:
    if not PACKAGE.is_dir():
        raise SkillContractError(f"missing acceptance skill package: {PACKAGE}")
    skill = (PACKAGE / "SKILL.md").read_text(encoding="utf-8")
    interface = (PACKAGE / "agents/openai.yaml").read_text(encoding="utf-8")
    references = {
        path.name: path.read_text(encoding="utf-8")
        for path in (PACKAGE / "references").glob("*.md")
        if path.is_file()
    }
    expected_references = {"authoring.md", "safeguards.md", "reports.md"}
    if set(references) != expected_references:
        raise SkillContractError(
            "acceptance package must contain only the three retained references: "
            + ", ".join(sorted(references))
        )
    package_files = {
        path.relative_to(PACKAGE).as_posix()
        for path in PACKAGE.rglob("*")
        if path.is_file()
    }
    expected_files = {
        "SKILL.md",
        "agents/openai.yaml",
        "references/authoring.md",
        "references/reports.md",
        "references/safeguards.md",
    }
    if package_files != expected_files:
        raise SkillContractError(
            "acceptance package must contain only its current instruction and reference files"
        )
    package_text = "\n".join((skill, interface, *references.values()))
    assert_contains(
        skill,
        "Read `CONTEXT.md`",
        "exact repository-relative canonical",
        "same exact relative path set to Check, Review, and scoped Verify",
        "product CommonMark parser",
        "absolute, absent, ambiguous, missing, missing-file, structurally invalid, insufficient",
        "Never fall back to whole-contract material",
        "no `--spec` selection",
        "do not claim that a Spec Review is ready",
        "@ToppleAcceptanceTest",
        "@ToppleProperty",
        "public handoff",
        "reviewer handoff",
        "Never read or translate .feature files",
        "Preserve authored Gherkin-style Given/When/Then/And/But narratives verbatim",
    )
    assert_contains(
        interface,
        "$topplecat-acceptance",
        "repository-relative canonical Markdown path or paths",
        "same --spec paths through Check, Review, and scoped Verify",
    )
    reports = references["reports.md"]
    assert_contains(
        reports,
        "./gradlew toppleCatCheck --spec <canonical-path>",
        "./gradlew toppleCatReview --spec <canonical-path>",
        "./gradlew toppleCatVerify --spec <canonical-path>",
        "./gradlew toppleCatSeal",
        "does not claim Review readiness",
        "Absolute paths are machine-specific input",
        "supplied relative path whose canonical `.md` file is missing",
    )
    authoring = references["authoring.md"]
    assert_contains(authoring, "repository-relative `.md` paths", "Canonical Markdown", ".feature")
    if "Stage sentences" in package_text:
        raise SkillContractError("stale Stage sentences terminology remains")
    if "./gradlew toppleCat" in skill:
        raise SkillContractError("SKILL.md must leave task execution to the external workflow")
    if len(skill.splitlines()) > 120 or len(skill.split()) > 800:
        raise SkillContractError("SKILL.md exceeds progressive-disclosure limits")
    return package_text


def assert_public_fixture_consistency(
    handoff: dict[str, object],
    java_source: str,
    public_cases: list[dict[str, object]],
    yaml_source: str,
    label: str,
) -> None:
    ids = list(handoff["acceptanceConditionIds"])
    methods = list(handoff["acceptanceMethods"])
    rows = list(handoff["publicRows"])
    if len(ids) != len(methods) or len(ids) != len(public_cases) or len(ids) != len(rows):
        raise SkillContractError(f"{label} handoff and authored fixture counts differ")
    for ac_id, method in zip(ids, methods):
        pattern = rf'@ToppleAcceptanceTest\("{re.escape(ac_id)}"\).*?void\s+{re.escape(method)}\s*\('
        if not re.search(pattern, java_source, flags=re.DOTALL):
            raise SkillContractError(f"{label} handoff method is missing from authored Java: {method}")
    case_ids = [str(case["caseId"]) for case in public_cases]
    case_ac_ids = [str(case["acId"]) for case in public_cases]
    if case_ids != rows or case_ac_ids != ids:
        raise SkillContractError(f"{label} handoff rows and authored JSON cases differ")
    for case_id, ac_id in zip(case_ids, case_ac_ids):
        pattern = rf"caseId:\s*{re.escape(case_id)}\s+acId:\s*{re.escape(ac_id)}"
        if not re.search(pattern, yaml_source):
            raise SkillContractError(f"{label} handoff row is missing from authored YAML: {case_id}")


def assert_reviewer_fixture_consistency(
    handoff: dict[str, object], hidden_java: str, reviewer_cases: list[dict[str, object]], label: str
) -> None:
    rows = list(handoff["reviewerRows"])
    case_ids = [str(case["caseId"]) for case in reviewer_cases]
    if case_ids != rows:
        raise SkillContractError(f"{label} reviewer handoff rows and authored cases differ")
    for case in reviewer_cases:
        ac_id = str(case["acId"])
        if not re.search(rf'@ToppleAcceptanceTest\("{re.escape(ac_id)}"\)', hidden_java):
            raise SkillContractError(f"{label} reviewer AC is missing from authored Java: {ac_id}")


def assert_authored_fixture_consistency(
    fixture_root: Path,
    selected_public: dict[str, object],
    selected_reviewer: dict[str, object],
    whole_public: dict[str, object],
    whole_reviewer: dict[str, object],
    whole_contract: dict[str, object],
) -> None:
    public_java = (fixture_root / "src/test/java/example/CheckoutAcceptance.java").read_text(
        encoding="utf-8"
    )
    hidden_java = (fixture_root / "src/hiddenTest/java/example/CheckoutHiddenTest.java").read_text(
        encoding="utf-8"
    )
    public_cases = read_json_array(
        fixture_root / "src/test/resources/topplecat/cases/checkout-public.json"
    )
    reviewer_cases = read_json_array(
        fixture_root / "src/hiddenTest/resources/topplecat/cases/checkout-reviewer.json"
    )
    yaml_source = (
        fixture_root / "src/test/resources/topplecat/cases/checkout.yml"
    ).read_text(encoding="utf-8")
    assert_public_fixture_consistency(
        selected_public, public_java, public_cases[:3], yaml_source, "selected"
    )
    assert_public_fixture_consistency(
        whole_public, public_java, public_cases, yaml_source, "whole-contract"
    )
    assert_reviewer_fixture_consistency(
        selected_reviewer, hidden_java, reviewer_cases[:2], "selected"
    )
    assert_reviewer_fixture_consistency(
        whole_reviewer, hidden_java, reviewer_cases, "whole-contract"
    )
    selected_ids = list(selected_public["acceptanceConditionIds"])
    whole_ids = list(whole_public["acceptanceConditionIds"])
    bound_ids = list(whole_contract["boundAcceptanceConditionIds"])
    if selected_ids != ["AC-CHECKOUT-001", "AC-CHECKOUT-002", "AC-CHECKOUT-003"]:
        raise SkillContractError("selected authored fixture must contain only the three selected ACs")
    if "AC-OLD" in selected_ids or "AC-OLD" in selected_reviewer.get("reviewerRows", []):
        raise SkillContractError("selected handoff included the unselected AC")
    if whole_ids != bound_ids or "AC-OLD" not in whole_ids:
        raise SkillContractError("whole-contract bound IDs do not match authored public ACs")
    serialized_selected = json.dumps(
        {"public": selected_public, "reviewer": selected_reviewer}, ensure_ascii=False
    ).lower()
    if "old-spec" in serialized_selected or ".feature" in serialized_selected:
        raise SkillContractError("selected authored handoff crossed the Spec boundary")


def inspect_fixture(
    fixture_root: Path = FIXTURE,
) -> tuple[
    dict[str, object],
    dict[str, object],
    dict[str, object],
    dict[str, object],
    dict[str, object],
    list[dict[str, object]],
]:
    if not fixture_root.is_dir():
        raise SkillContractError(f"missing synthetic acceptance fixture: {fixture_root}")
    all_files = [path for path in fixture_root.rglob("*") if path.is_file()]
    canonical = (fixture_root / "specs/checkout.md").read_text(encoding="utf-8")
    second = (fixture_root / "specs/payment.md").read_text(encoding="utf-8")
    if (fixture_root / MISSING_FILE_PATH).exists():
        raise SkillContractError("missing-file fixture path must not exist")
    if canonical.count(MARKER) != 2:
        raise SkillContractError("synthetic canonical Markdown must contain two exact markers")
    assert_contains(
        canonical,
        "## AC-CHECKOUT-001: Apply the checkout discount",
        "### AC-CHECKOUT-002： Keep the receipt complete",
        "And the cart has an active discount",
        "But the cart has no eligible promotion",
    )
    assert_contains(
        second,
        "## AC-CHECKOUT-003: Keep payment confirmation consistent",
        MARKER,
    )
    if (fixture_root / "specs/old-spec.md").read_text(encoding="utf-8").find(MARKER) < 0:
        raise SkillContractError("fixture must retain an unselected old Spec")
    if ".feature" not in " ".join(path.name for path in all_files):
        raise SkillContractError("fixture must include an upstream .feature artifact")
    public = read_json(fixture_root / "contract/public-handoff.json")
    reviewer = read_json(fixture_root / "contract/reviewer-handoff.json")
    whole = read_json(fixture_root / "contract/whole-contract.json")
    whole_public = read_json(fixture_root / "contract/whole-public-handoff.json")
    whole_reviewer = read_json(fixture_root / "contract/whole-reviewer-handoff.json")
    narratives = [
        {
            "acId": "AC-CHECKOUT-001",
            "groups": [
                {
                    "steps": [
                        "Given a cart qualifies for checkout",
                        "And the cart has an active discount",
                        "When the customer submits the cart",
                        "Then the checkout total includes the discount",
                    ]
                },
                {
                    "steps": [
                        "Given a cart does not qualify",
                        "But the cart has no eligible promotion",
                        "When the customer submits the cart",
                        "Then the checkout total remains unchanged",
                    ]
                },
            ],
        },
        {
            "acId": "AC-CHECKOUT-002",
            "groups": [
                {
                    "steps": [
                        "Given a completed checkout",
                        "When the receipt is generated",
                        "Then the receipt contains the order identity",
                    ]
                }
            ],
        },
        {
            "acId": "AC-CHECKOUT-003",
            "groups": [
                {
                    "steps": [
                        "Given payment has been authorized",
                        "When confirmation is issued",
                        "Then the confirmation retains the payment identity",
                    ]
                }
            ],
        },
    ]
    assert_authored_fixture_consistency(
        fixture_root, public, reviewer, whole_public, whole_reviewer, whole
    )
    return public, reviewer, whole, whole_public, whole_reviewer, narratives


def assert_broken_authored_fixture_rejected() -> None:
    mutations = ("method", "row")
    for mutation in mutations:
        with tempfile.TemporaryDirectory(prefix="topplecat-skill-fixture-") as temporary:
            broken_root = Path(temporary) / "project"
            shutil.copytree(FIXTURE, broken_root)
            if mutation == "method":
                source_path = broken_root / "src/test/java/example/CheckoutAcceptance.java"
                source = source_path.read_text(encoding="utf-8")
                source_path.write_text(
                    source.replace(
                        '@ToppleAcceptanceTest("AC-CHECKOUT-003")',
                        '@ToppleAcceptanceTest("AC-CHECKOUT-RENAMED")',
                        1,
                    ),
                    encoding="utf-8",
                )
            else:
                source_path = broken_root / "src/test/resources/topplecat/cases/checkout-public.json"
                cases = read_json_array(source_path)
                source_path.write_text(
                    json.dumps(
                        [case for case in cases if case.get("caseId") != "payment-public"],
                        indent=2,
                    )
                    + "\n",
                    encoding="utf-8",
                )
            try:
                inspect_fixture(broken_root)
            except SkillContractError:
                continue
            raise SkillContractError(
                f"broken authored fixture unexpectedly passed consistency check: {mutation}"
            )


def selected_expected(
    public: dict[str, object], reviewer: dict[str, object], narratives: list[dict[str, object]]
) -> dict[str, object]:
    documents = list(SELECTED_DOCUMENTS)
    ids = list(public["acceptanceConditionIds"])
    commands = [
        "./gradlew toppleCatCheck --spec specs/checkout.md --spec specs/payment.md",
        "./gradlew toppleCatReview --spec specs/checkout.md --spec specs/payment.md",
        "./gradlew toppleCatVerify --spec specs/checkout.md --spec specs/payment.md",
    ]
    return {
        "branch": "selected-delivery",
        "scope": {
            "documents": documents,
            "acceptanceConditionIds": ids,
            "boundAcceptanceConditionIds": ids,
        },
        "commands": commands,
        "reviewReady": True,
        "publicHandoff": public,
        "reviewerHandoff": reviewer,
        "authoredNarratives": narratives,
        "failureRouting": {"owner": "", "action": ""},
    }


def assert_selected(result: dict[str, object], expected: dict[str, object]) -> None:
    if result != expected:
        raise SkillContractError("selected output did not preserve the exact path, scope, narratives, or custody")
    serialized = json.dumps(result, ensure_ascii=False).lower()
    if any(token in serialized for token in ("ac-old", "old-spec", ".feature", "details.md")):
        raise SkillContractError("selected output crossed the selected canonical boundary")
    if any(Path(value).is_absolute() for value in result["scope"]["documents"]):
        raise SkillContractError("selected output exposed an absolute path")


def assert_failure(kind: str, result: dict[str, object]) -> None:
    if result.get("branch") != "selected-failure":
        raise SkillContractError(f"{kind} failure chose a non-selected branch")
    if result.get("commands") != [] or result.get("reviewReady") is not False:
        raise SkillContractError(f"{kind} failure emitted commands or readiness")
    if result.get("scope") != {"documents": [], "acceptanceConditionIds": [], "boundAcceptanceConditionIds": []}:
        raise SkillContractError(f"{kind} failure formed selected scope")
    if result.get("publicHandoff") != {"acceptanceMethods": [], "publicRows": [], "acceptanceConditionIds": []}:
        raise SkillContractError(f"{kind} failure emitted public material")
    if result.get("reviewerHandoff") != {"reviewerRows": [], "reviewerOnlyValue": "", "source": ""}:
        raise SkillContractError(f"{kind} failure emitted reviewer material")
    if result.get("authoredNarratives") != []:
        raise SkillContractError(f"{kind} failure emitted authored narratives")
    routing = result.get("failureRouting")
    if not isinstance(routing, dict) or not routing.get("owner") or not routing.get("action"):
        raise SkillContractError(f"{kind} failure omitted owner/repair action")
    owner = str(routing["owner"]).lower()
    action = str(routing["action"]).lower()
    if kind in {"absolute-path", "ambiguous-path", "missing-path", "missing-file", "invalid-structure", "insufficient-detail", "thin-wrapper"}:
        if not any(token in owner for token in ("human", "canonical", "spec", "sdd", "workflow")):
            raise SkillContractError(f"{kind} failure routed to the wrong owner")
    required_action = {
        "absolute-path": ("relative", "path"),
        "ambiguous-path": ("exact", "path"),
        "missing-path": ("provide", "path"),
        "invalid-structure": ("heading", "marker"),
        "insufficient-detail": ("business", "detail"),
        "thin-wrapper": ("complete", "canonical"),
    }
    if kind == "missing-file":
        repairs_missing_file = (
            "restore" in action and "canonical" in action and "markdown" in action,
            "provide" in action
            and "correct" in action
            and "existing" in action
            and "relative" in action,
        )
        if not any(repairs_missing_file):
            raise SkillContractError(f"{kind} failure did not name the smallest repair")
    elif not all(token in action for token in required_action[kind]):
        raise SkillContractError(f"{kind} failure did not name the smallest repair")
    serialized = json.dumps(result, ensure_ascii=False).lower()
    if re.search(r"/(?:private|var|tmp|users)/|details\.md|\.feature", serialized):
        raise SkillContractError(f"{kind} failure exposed a machine or wrapper path")


def assert_forged_outputs(expected: dict[str, object]) -> None:
    forged = copy.deepcopy(expected)
    forged["scope"]["acceptanceConditionIds"] = ["AC-OLD"]
    try:
        assert_selected(forged, expected)
    except SkillContractError:
        pass
    else:
        raise SkillContractError("forged selected scope unexpectedly passed")

    for mutation in (
        lambda value: value["authoredNarratives"][0]["groups"][0]["steps"].pop(1),
        lambda value: value["authoredNarratives"][0]["groups"][0]["steps"].__setitem__(1, "And an invented rule"),
        lambda value: value["reviewerHandoff"].__setitem__("source", "specs/checkout.feature"),
    ):
        forged = copy.deepcopy(expected)
        mutation(forged)
        try:
            assert_selected(forged, expected)
        except SkillContractError:
            pass
        else:
            raise SkillContractError("forged narrative or custody output unexpectedly passed")

    for kind in ("absolute-path", "ambiguous-path", "missing-path", "missing-file", "invalid-structure", "insufficient-detail", "thin-wrapper"):
        forged = {
            "branch": "selected-failure",
            "scope": {"documents": ["specs/checkout.md"], "acceptanceConditionIds": ["AC-CHECKOUT-001"], "boundAcceptanceConditionIds": []},
            "commands": [],
            "reviewReady": False,
            "publicHandoff": expected["publicHandoff"],
            "reviewerHandoff": expected["reviewerHandoff"],
            "authoredNarratives": expected["authoredNarratives"],
            "failureRouting": {"owner": "canonical Spec owner", "action": "provide an exact path"},
        }
        try:
            assert_failure(kind, forged)
        except SkillContractError:
            pass
        else:
            raise SkillContractError(f"forged {kind} failure unexpectedly passed")


def main() -> int:
    try:
        inspect_package()
        public, reviewer, whole, whole_public, whole_reviewer, narratives = inspect_fixture()
        assert_broken_authored_fixture_rejected()
        expected = selected_expected(public, reviewer, narratives)
        if expected["scope"]["documents"] != SELECTED_DOCUMENTS:
            raise SkillContractError("selected contract did not carry the complete relative document set")
        if expected["commands"] != [
            "./gradlew toppleCatCheck --spec specs/checkout.md --spec specs/payment.md",
            "./gradlew toppleCatReview --spec specs/checkout.md --spec specs/payment.md",
            "./gradlew toppleCatVerify --spec specs/checkout.md --spec specs/payment.md",
        ]:
            raise SkillContractError("selected contract did not repeat every --spec path on every command")
        if expected["scope"]["acceptanceConditionIds"] != public["acceptanceConditionIds"]:
            raise SkillContractError("selected contract did not derive the complete AC scope from the fixture contract")
        assert_selected(copy.deepcopy(expected), expected)
        whole_result = {
            "branch": "whole-contract",
            "scope": {
                "documents": [],
                "acceptanceConditionIds": [],
                "boundAcceptanceConditionIds": whole["boundAcceptanceConditionIds"],
            },
            "commands": ["./gradlew toppleCatCheck", "./gradlew toppleCatSeal", "./gradlew toppleCatVerify"],
            "reviewReady": False,
            "publicHandoff": whole_public,
            "reviewerHandoff": whole_reviewer,
            "authoredNarratives": [],
            "failureRouting": {"owner": "", "action": ""},
        }
        if whole_result["scope"]["documents"] != [] or whole_result["scope"]["acceptanceConditionIds"] != []:
            raise SkillContractError("whole-contract scope formed a selected document or AC scope")
        if whole_result["scope"]["boundAcceptanceConditionIds"] != whole_public["acceptanceConditionIds"]:
            raise SkillContractError("whole-contract scope did not carry every authored bound AC")
        if whole_result["publicHandoff"] != whole_public or whole_result["reviewerHandoff"] != whole_reviewer:
            raise SkillContractError("whole-contract handoff did not use its complete authored contract")
        if whole_result["reviewReady"] or any("Review" in command for command in whole_result["commands"]):
            raise SkillContractError("whole-contract guidance created Review readiness")
        for kind, action in {
            "absolute-path": "provide an exact repository-relative path",
            "ambiguous-path": "choose one exact path or explicitly list the selected paths",
            "missing-path": "provide an exact repository-relative path",
            "missing-file": "restore the canonical Markdown file or provide a correct existing relative path",
            "invalid-structure": "repair the canonical heading and marker structure",
            "insufficient-detail": "add complete business detail, rules, and examples",
            "thin-wrapper": "provide the complete canonical Markdown document",
        }.items():
            assert_failure(
                kind,
                {
                    "branch": "selected-failure",
                    "scope": {"documents": [], "acceptanceConditionIds": [], "boundAcceptanceConditionIds": []},
                    "commands": [],
                    "reviewReady": False,
                    "publicHandoff": {"acceptanceMethods": [], "publicRows": [], "acceptanceConditionIds": []},
                    "reviewerHandoff": {"reviewerRows": [], "reviewerOnlyValue": "", "source": ""},
                    "authoredNarratives": [],
                    "failureRouting": {"owner": "canonical Spec owner", "action": action},
                },
            )
        assert_forged_outputs(expected)
    except (OSError, SkillContractError, KeyError, TypeError) as error:
        print(f"acceptance skill deterministic validation failed: {error}")
        return 1
    print("acceptance skill deterministic package and assertion tests: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
