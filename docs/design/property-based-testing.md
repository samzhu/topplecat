# Property-Based Testing safeguard

**Status:** Implemented

**Date:** 2026-07-29

**Target:** ToppleCat 0.0.7

## User example

Public checkout rows prove a few approved carts return approved receipts. A
separate invariant says that reordering the same cart lines never changes the
total. A bounded Property-Based Testing declaration can challenge that rule with
deterministic boundaries and generated carts without turning generated values
into approved rows.

```java
@ToppleProperty("AC-CHECKOUT-TOTAL")
void lineOrderDoesNotChangeTotal(PropertyTrials trials) {
    trials.forAll(CartGenerators.validCarts())
        .classify("free-shipping-boundary", CartRules::crossesFreeShippingBoundary)
        .requireCoverage("free-shipping-boundary", 5.0)
        .check(cart -> assertEquals(checkout.total(cart), checkout.total(cart.reversed())));
}
```

This is bounded testing, not a proof of correctness. People still approve the
property, choose its domain, and decide which cases belong in the public
acceptance contract.

## Decision

Property-Based Testing is a third independent verification capability beside
Hidden Tests and Mutation Testing. It remains inside the existing four modules;
ToppleCat adds no property-testing dependency, new artifact, new source set,
new CLI, or second authoring language.

| Capability | Input | Execution | Gate |
| --- | --- | --- | --- |
| Hidden Tests | Reviewer-owned typed rows | `toppleCatHiddenTest` | `REVIEWER_JUNIT` |
| Mutation Testing | Public acceptance methods and producer report | mutation producer | `MUTATION` |
| Property-Based Testing | `@ToppleProperty`, generators, runtime events | dedicated Property task | `PROPERTY` |

The capabilities share scope, integrity, reports, and one aggregate verdict.
Reviewer custody belongs only to Hidden Tests. They never share coverage or
accepted evidence. In particular:

- a Property never passes or makes `REVIEWER_JUNIT` not applicable;
- hidden rows never improve `PROPERTY` or `MUTATION`;
- Property runs never enter PIT targets, attribution, or scores; and
- Mutation Testing never supplies a missing Property run.

When enabled, Hidden Tests require executed hidden typed rows for the selected
ACs. Missing rows produce `REVIEWER_JUNIT=INCOMPLETE`, even when Properties
pass. A Property-only team explicitly disables `hiddenTests` and reseals the
policy, so the gate visibly becomes `DISABLED`.

## Java authoring surface

An AC has one public `@ToppleAcceptanceTest("AC-...")` method for typed
examples. `@ToppleProperty` is optional, may appear more than once per AC, and
is ordinary JUnit rather than Stage orchestration. Property declarations belong
under `src/test`.

```text
ToppleProperty
PropertyTrials
PropertyCheck<T>
Generator<T>
Generators
```

`@ToppleProperty` has a literal existing AC, returns `void`, receives exactly
one `PropertyTrials`, uses a positive `tries` value and non-negative discard /
shrink limits, and executes one `forAll(...).check(...)`. It exposes these
annotation values:

```java
String value();
int tries() default 200;
int maxDiscards() default 1_000;
int maxShrinks() default 500;
```

Built-ins support booleans, bounded integers/longs/decimals, ordered values and
enums, explicit-alphabet strings, bounded lists, optionals, `oneOf`, `map`,
`filter`, and two/three-way `combine`. There is no recursive generation,
`flatMap`, custom engine, custom shrinker, state machine, concurrency, or
coverage-guided fuzzing in 0.0.7.

## Determinism, results, and safety

Every generator retains deterministic JSON choices independent of the returned
Java object's `toString()`. `map` and `filter` preserve their pre-mapping choice
presentation. A failed Property is reproduced and then shrunk within its
configured bound. The report calls the result a shrunk counterexample, not a
globally smallest input.

A replay token is diagnostic only. It contains versioned engine choices for the
same checked Property source and cannot become current evidence in a later
Verify run. Exhausted filters, unmet classification coverage, malformed events,
or unstable replay are `PROPERTY=INCOMPLETE`; a reproducible assertion failure
is `PROPERTY=FAIL`.

The compiler emits a `CompilerPropertyDescriptor` separate from typed case
descriptors. It contains AC ID, stable method identity, source reference, trial
limits, and source digest. Property events and results are current-run models;
generated trials never enter `CaseDefinition`, `CaseRun`, `VerificationRun`, or
expected consumption.

## Scope, reports, and policy

Properties follow the selected ACs and run in their own formal task.
`--all-hidden-tests` affects only hidden typed rows and never changes Property
selection. Property execution cannot read, depend on, or generate hidden rows.

Spec Review may show Property declarations beside typed cases and mutation
policy. Reviewer-only Verification Report shows result state, classifications,
generator choices,
counterexamples, and replay token. `agent-feedback.json` supplies only a
generic Property remediation reason and never generated values, AC IDs, labels,
paths, seeds, tokens, or raw failures.

`propertyBasedTesting { enabled.set(false) }` is sealed policy. It yields
`PROPERTY=DISABLED`; it is not equivalent to `NOT_APPLICABLE`. The latter means
the safeguard is enabled but no declarations apply to the effective scope.

## Acceptance criteria

1. Property declarations compile into distinct descriptors and execute only
   for the selected ACs in the dedicated Property task.
2. A Property-only run with Hidden Tests enabled records
   `REVIEWER_JUNIT=INCOMPLETE` and the actual Property result.
3. Disabling Hidden Tests records `REVIEWER_JUNIT=DISABLED` while evaluating
   Property normally.
4. Property output cannot affect PIT targeting, mutation attribution, hidden
   typed-row coverage, or expected-consumption state.
5. Counterexamples use retained JSON choices, shrink reproducibly, and stay out
   of safe public feedback.
6. Reports show the three capabilities side by side without blended coverage or
   score.
7. Property execution cannot read, depend on, or generate hidden rows.
