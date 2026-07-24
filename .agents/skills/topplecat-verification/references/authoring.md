# Executable Contract Authoring

Read this reference when creating or changing a Spec, acceptance binding, typed
case row, DTO expectation, or narrative stage.

## Contents

- Spec and AC identity
- Source layout
- Canonical Java contract
- Typed rows
- Expected obligations
- Reviewer retests
- Attachment evidence

## Spec And AC Identity

Use the immutable issue or task-card identifier as the Spec namespace. Follow
the repository's established format; for example:

```text
SPEC-42
AC-SPEC-42-01
AC-SPEC-42-02
```

Keep the same AC ID in Markdown context, Java annotations, and typed rows.
Use direct string literals so `toppleCatCheck` can prove the relationship.

If Markdown requirements exist, configure them explicitly:

```kotlin
toppleCat {
    specDocs.from("specs")
}
```

Anchor each requirement with its literal AC ID:

```markdown
## AC-SPEC-42-01 Create an accepted order

The service accepts a valid cart and returns the created order.
```

Markdown supplies human context. Java tests and typed rows remain the executable
authority.

## Source Layout

```text
src/test/java/                                      public Java contract
src/test/resources/topplecat/cases/                 public typed rows
src/hiddenTest/java/                                reviewer-only Java tests
src/hiddenTest/resources/topplecat/cases/SPEC-42/   reviewer-only rows
```

Use JSON or YAML. Preserve nested objects and arrays so Jackson can deserialize
real DTOs. Keep CSV and string-table formats outside the ToppleCat contract.

## Canonical Java Contract

Use exactly one canonical `@ToppleTest` method for each data-driven AC. Use
`@ToppleAc` for additional non-parameterized ordinary JUnit coverage. Keep AC
IDs literal. A canonical method is required to be a static-readable Stage DSL:
only direct calls to fields declared on the same class with `@ToppleStageField`
may appear in its body.

```java
@ToppleStageField OrderGiven given;
@ToppleStageField OrderWhen when;
@ToppleStageField OrderThen then;

@ToppleTest("AC-SPEC-42-01")
@DisplayName("Create an accepted order")
void createsOrder(ToppleCase c) {
    given.an_order_request(c.input("request", OrderRequest.class));
    when.submits_it();
    then.matches_the_contract(c);
}
```

Search the existing domain Stage vocabulary before writing a canonical method.
Reuse its steps whenever possible; add a step only when the vocabulary cannot
express the approved behavior. The canonical method contains only direct calls
to its `@ToppleStageField` fields. Move locals, service construction, SUT calls,
assertions, `c.verify`, helpers, and control flow into Stage methods; keep
expected-value assertions in Then stages.

Declare each Stage field as non-static and non-final, backed by a class with an
accessible no-argument constructor. Every Stage step calls `recorded(...)` as
its first executable action, performs its work, and returns `self()`. Record
only report-safe public values in public stages; reviewer invocation values
remain in reviewer reports.

Prefer `@DisplayName` for the report title. Use
`@ToppleAc(title = "...")` only as a non-parameterized fallback.

## Typed Rows

Each row contains exactly `caseId`, `acId`, `inputs`, and `expected`:

```yaml
- caseId: order-public-example
  acId: AC-SPEC-42-01
  inputs:
    request:
      customerId: customer-public
      items:
        - sku: SKU-001
          quantity: 2
  expected:
    response:
      accepted: true
      itemCount: 2
```

Deserialize nested inputs directly:

```java
OrderRequest request = c.input("request", OrderRequest.class);
```

Keep notes, feature tags, names, and dynamic identifiers outside the four-field
row schema.

## Expected Obligations

Treat every top-level key below `expected` as an assertion obligation:

- `c.verify("response", actual)` deep-compares and marks the key `ASSERTED`.
- `c.expected("response", Response.class)` reads the value but does not assert
  it.
- An untouched key fails an otherwise successful invocation.

Prefer one meaningful aggregate output when it expresses the API contract. Use
multiple keys only when the production operation genuinely returns independent
outcomes.

## Reviewer Retests

Derive hidden values independently from the public examples. Choose boundaries
that expose likely shortcuts: mixed carts, threshold transitions, idempotency,
inventory conflicts, validation errors, or nested response shape.

Reuse the public canonical `@ToppleTest` by adding reviewer rows whenever the
same contract method can exercise the boundary. Add reviewer-only Java tests
only for behavior that cannot be expressed through the canonical method.

Bind every reviewer row to an existing public AC. Keep all reviewer material
under `src/hiddenTest`.

## Attachment Evidence

Attach focused diagnostics only when expected output alone cannot explain a
reviewer result. Call `attach(...)` inside the active Stage step:

```java
attach(ToppleAttachment.json("Order response", responseJson, this::redact));
attach(ToppleAttachment.png("Checkout result", screenshotBytes));
```

The allowlist is UTF-8 text, JSON, PNG, and JPEG. One attachment is limited to
10 MiB and the report bundle to 100 MiB. Apply domain redaction before attaching
logs, requests, responses, or screenshots; ToppleCat's common-field masking is
a second line of defense. Attachments inherit case visibility and appear only
in the reviewer Verification bundle.

Keep attachment titles, paths, and contents out of implementation-agent
instructions and `agent-feedback.json`.
