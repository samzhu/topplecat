# Acceptance safeguards

Use safeguards independently. A passing result from one never supplies evidence
for another.

## Hidden Tests

Reviewer-owned typed rows run the same public acceptance method with examples
the implementation agent did not receive. Choose unseen boundaries and rule
combinations, such as:

- exactly at and just below a threshold;
- eligible and ineligible products with similar values;
- conflicting discounts or status transitions;
- repeated requests, nested results, and rejection paths; and
- values that distinguish two plausible readings of the same rule.

Hidden Tests help expose visible-answer hard-coding. They cannot guarantee that
every shortcut will be found, and they must not add a private business rule.
When Hidden Tests are enabled, every selected AC needs an executed reviewer
row; a missing row makes `REVIEWER_JUNIT=INCOMPLETE`. Explicitly disabling the
safeguard and resealing its policy records `REVIEWER_JUNIT=DISABLED`.

## Mutation Testing

Mutation Testing deliberately changes production behavior and asks whether the
public acceptance contract notices. Write public cases whose assertions would
fail if an important comparison, return value, condition, or calculation were
changed.

Mutation strength comes from public acceptance methods and public rows. Hidden
rows and Properties do not improve its result.

## Property-Based Testing

Use a Property when one approved invariant should survive many bounded inputs:
totals stay non-negative, ordering does not change a result, or applying the
same idempotency key twice has the approved effect.

Properties are public declarations. They do not read reviewer rows, satisfy
Hidden Tests, consume expected case values, or contribute to mutation scores.

## Contract integrity and expected consumption

Contract integrity lets an external workflow detect changes to selected Spec
context, compiler-derived acceptance source closure, public rows, Properties,
Gradle logic, compiler semantics, or verification policy after sealing.

Expected consumption catches every declared expected value that was not
asserted. Use `ToppleCase.verify(...)` for observable case results.

Humans remain responsible for complete rules. ToppleCat checks that the approved
executable contract is the contract that runs; it does not infer omitted
requirements.
