# Authoring Contracts

## Java Acceptance Tests

Use one literal `@ToppleTest("AC-...")` method as the canonical parameterized
test for each acceptance condition with case data. Its body is a required,
static-readable `ToppleStage` orchestration—not a place for test plumbing. Use
`@ToppleAc("AC-...")` for additional ordinary JUnit coverage; only canonical
`@ToppleTest` methods have the Stage DSL restriction. `@DisplayName` supplies
the report title.

```java
@ToppleStageField OrderGiven given;
@ToppleStageField OrderWhen when;
@ToppleStageField ReceiptThen then;

@ToppleTest("AC-ORDER-CREATE")
@DisplayName("Create an accepted order")
void createsOrder(ToppleCase c) {
    given.an_order_request(c.input("request", OrderRequest.class));
    when.submits_it();
    then.matches_the_contract(c);
}
```

The canonical body may use `ToppleCase` typed access, constants, and normal
value expressions needed for arguments. It may not declare locals, construct a
service/repository, call the SUT, assert, call `c.verify(...)`, call a helper,
or contain control flow. Every direct call must target a field declared on the
same test class with `@ToppleStageField`, resolve to a `ToppleStage` method,
and produce a domain sentence. `toppleCatCheck` reports the AC, file, line, and
repair when this shape is broken.

Each Stage step calls `recorded(...)` as its first executable action, performs
the work, and ends with `return self();`. Put assertions in a Then step. `input`
and `expected` can be nested objects, arrays, maps, and API DTOs; Jackson
deserializes them directly to the requested Java type.

## Case Rows

Public rows are JSON or YAML under `src/test/resources/topplecat/cases/`.
Reviewer rows use the same schema under
`src/hiddenTest/resources/topplecat/cases/`. Each row contains exactly four
fields:

```yaml
- caseId: order-public-example
  acId: AC-ORDER-CREATE
  inputs:
    request: {items: [{sku: example-sku, quantity: 1}]}
  expected:
    response: {accepted: true}
```

Do not use CSV, string tables, undocumented fields, or dynamic AC identifiers.
A reviewer row must bind to an existing public acceptance condition.

## Expected Consumption

Each top-level key in `expected` begins as `UNTOUCHED`.

| Action | Result |
| --- | --- |
| `c.verify("response", actual)` | Deep-compares the value and marks it `ASSERTED`. |
| `c.expected("response", Response.class)` | Reads and deserializes the value, then marks it `READ`. |
| No access to the key | It remains `UNTOUCHED`. |

Only `ASSERTED` fulfils the declared expected-value obligation. When
enforcement is enabled, a successful test invocation fails after it returns if
any key is `READ` or `UNTOUCHED`. The evidence/report collector uses `UNKNOWN`
only when a completed invocation did not provide expected-consumption sidecar
data; `UNKNOWN` is not a substitute for an assertion.

## Numeric Contract Equality

`c.verify(...)` compares JSON numbers recursively by exact mathematical value,
not by JSON numeric node representation or decimal scale. Consequently `200`,
`200.0`, and `200.00` are equal, including inside objects and arrays. This is
not a floating-point tolerance: numerically different values still fail.

If a business contract needs to require displayed formatting or decimal scale,
express it as a string or a separate explicit output field. A JSON number and a
text value that looks numeric remain different values.

See the [0.0.2 release notes](../releases/0.0.2.md) for the version-specific
expected-consumption and numeric-equality corrections.

Run this authoring check before a handoff:

```bash
./gradlew toppleCatCheck
```

It validates JSON/YAML schema, literal bindings, the public-to-reviewer
acceptance-condition relationship, and the canonical Stage DSL without
executing tests.

Before handoff, an authorized reviewer runs the separate static review:

```bash
./gradlew toppleCatReview
```

It depends on the check and writes the bundle at
`build/topplecat/reports/review/`. The review contains all case data and must
never be handed to an implementation agent.

## Narrative Stages

`ToppleStage` is the required authoring surface for canonical `@ToppleTest`
methods. A compiler-backed descriptor is the shared definition consumed by
runtime and reviewer HTML; it is a projection of the Java method, not another
contract format. `@ToppleAc` remains ordinary supplementary JUnit coverage and
does not require stages.

Declare each `@ToppleStageField` as a non-static, non-final field whose stage
class has an accessible no-argument constructor. ToppleCat creates a fresh stage
set for every case invocation. A reporting method calls `recorded(...)` first,
performs its work, and returns `self()`.

Use `@ProvidedState` to publish a value to later stages and
`@ExpectedState(required = true)` when a later stage requires one. Method names
are rendered as readable text; `@As` can provide a custom sentence with
numbered placeholders for recorded values. Put result assertions in the Then
stage so a report can identify the exact sentence that failed.

Before handoff, `toppleCatReview` uses the compiler descriptor for the direct
stage calls in canonical source order. It never invents execution results or
falls back from malformed source: `toppleCatCheck` first rejects hidden helper
calls, control flow, unknown stage fields or methods, and steps that violate the
`recorded(...)`/`self()` contract.

## External Spec Documents

ToppleCat can add public SDD context from Markdown documents to the
reviewer-only contract review and final report projections. The Markdown is an
input for human reading, not a second contract: Java acceptance tests and typed
case rows stay authoritative.

There is no implicit scan. The recommended convention is a repository-root
`specs/` directory, configured explicitly so a project without external specs
keeps its existing silent behavior:

```kotlin
toppleCat {
    specDocs.from("specs")
    // More files or directories may be added with another from(...).
}
```

An `AC-...` literal in a Markdown heading or paragraph anchors that section to
the acceptance condition. A heading anchor runs until the next heading at the
same or higher level; a paragraph anchor uses its surrounding heading boundary.
Use one clear AC heading per requirement when possible:

```markdown
## AC-ORDER-CREATE Create an accepted order

The order service accepts a valid cart and returns a receipt with `accepted`
set to true.
```

ToppleCat renders only headings, paragraphs, lists, and inline code from those
sections. HTML and scripts are shown as escaped text and never execute. When
`specDocs` is configured, `toppleCatCheck` warns for a Markdown AC with no
canonical `@ToppleTest` and for a canonical test with no Markdown AC anchor.
These are warnings: they help maintain reading context without making Markdown
authoritative.

The alignment check uses canonical `@ToppleTest` descriptors compiled from
`src/test/java` only. Supplementary `@ToppleAc` methods and reviewer source do
not participate.

## Reviewer Attachments

Attachments are an advanced reviewer diagnostic, not part of the basic
contract. Inside an active Stage step, attach focused evidence with
`ToppleAttachment.json(...)`, `text(...)`, `png(...)`, or `jpeg(...)`.
Attachments inherit case visibility and are copied only into the reviewer
Verification bundle.

ToppleCat content-addresses and deduplicates attachments, allows only those four
media types, limits one file to 10 MiB and a report to 100 MiB, and applies
best-effort masking to common credential-shaped fields in JSON/text. Apply
domain-specific redaction first. Do not attach HTML, SVG, scripts, or raw
secrets.
