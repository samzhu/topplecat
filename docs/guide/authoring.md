# Authoring contracts

## Java acceptance tests

Use one literal `@ToppleTest("AC-...")` method as the canonical parameterized
test for each acceptance condition with case data. Its body contains only a
statically readable `ToppleStage` sequence; test plumbing belongs in the stage
methods. Use `@ToppleAc("AC-...")` for extra JUnit coverage. Only canonical
`@ToppleTest` methods follow the Stage DSL restriction. `@DisplayName` supplies
the report title.

Write `@DisplayName` as the business result a reviewer is checking, not as the
Java method name. Stage method names and `@As` sentences should likewise use
domain language. One Stage step should express one understandable business
action. Names such as `matches_the_contract`, `execute_test`, and
`verify_result` hide the behaviour the review needs to inspect; prefer a name
such as `receipt_shows_discount_and_discounted_subtotal` instead.

```java
@ToppleStageField OrderGiven given;
@ToppleStageField OrderWhen when;
@ToppleStageField ReceiptThen then;

@ToppleTest("AC-ORDER-CREATE")
@DisplayName("Create an accepted order")
void createsOrder(ToppleCase c) {
    given.an_order_request(c.input("request", OrderRequest.class));
    when.submits_it();
    then.confirms_accepted_order(c);
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
the work, and ends with `return self();`. Record values that help a reviewer
understand the business action, such as a customer ID, amount, or selected
option; do not record incidental implementation state. Put assertions in a Then step. `input`
and `expected` can be nested objects, arrays, maps, and API DTOs; Jackson
deserializes them directly to the requested Java type.

## Case rows

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

## Reviewer retests

Reviewer retests are independently chosen business cases. They are not secret
answer keys and cannot prove that every possible hard-coded shortcut fails.
Derive them from the approved rule rather than by changing literals in a public
row. Choose boundaries that expose likely shortcuts: mixed carts, threshold
transitions, idempotency, inventory conflicts, validation errors, or nested
response shape.

During review, ask what would happen if the implementation recognized only the
public SKU, coupon, threshold, or expected answer. An unseen rule combination
usually tells you more than another example from the same path. A hidden row
still targets an existing public AC; it cannot introduce a new requirement.

## Expected consumption

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

## Numeric contract equality

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

## Narrative stages

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

The HTML review resolves compiler-owned argument bindings against the selected
typed case row, then displays Given/When/Then beside that row's readable inputs
and expected output. It is a reading projection of the Java/JUnit contract, not
another specification or authoring format. JGiven is useful design reference for
that reading experience, but ToppleCat does not add a JGiven dependency.

## External spec documents

ToppleCat can add public SDD context from Markdown documents to the
reviewer-only contract review and final report projections. The Markdown is an
input for human reading, not a second contract: Java acceptance tests and typed
case rows stay authoritative.

The SDD tool or delivery workflow chooses which document is current and handles
its status, history, and organizational review. ToppleCat does not infer that
lifecycle. Its concern is narrower: an acceptance condition selected from the
Spec needs a literal, executable `@ToppleTest` and typed cases. This gives people
one traceable path from a written requirement to the Java/JUnit code that
actually accepts or rejects the implementation.

ToppleCat does not scan for Markdown or guess the active SDD change
automatically. The surrounding workflow supplies an exact delivery selection at
the command line; an exact current Spec is easier to review than an accumulated
directory:

```bash
./gradlew toppleCatCheck --spec specs/023-checkout/spec.md
./gradlew toppleCatReview --spec specs/023-checkout/spec.md
./gradlew toppleCatHide --spec specs/023-checkout/spec.md
# Repeat --spec for every document in a cross-cutting delivery.
```

Use the same repository-relative Markdown path or paths for Check, Review,
Hide, UpdateEscrow, and Verify. A selected document must contain at least one
`AC-...` identifier, and every selected AC must have a canonical public
`@ToppleTest` binding; Check fails otherwise. The selected paths, document
bytes, and normalized AC set become part of the mechanical approval. Verify
with a different selection fails contract integrity instead of silently running
another delivery.

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
sections. HTML and scripts are shown as escaped text and never execute. Selected
scope makes missing Markdown-to-canonical bindings a static error, but it does
not make Markdown executable. The older `toppleCat.specDocs` configuration is
still available for compatibility-only reading context: it warns for a Markdown
AC with no canonical `@ToppleTest` and for a canonical test with no Markdown AC
anchor. Those compatibility warnings do not select, filter, or seal a delivery.

The alignment check uses canonical `@ToppleTest` descriptors compiled from
`src/test/java` only. Supplementary `@ToppleAc` methods and reviewer source do
not participate.

## Reviewer attachments

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
