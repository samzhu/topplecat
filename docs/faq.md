# FAQ

## Is JGiven a ToppleCat dependency?

No. JGiven is a high-level readability reference for the distinction between a
Scenario, a Stage, and a Step. ToppleCat does not depend on its runtime or
reporting system, and it is not a design authority: ToppleCat makes different
choices when delegation verification requires a different boundary.

## Which mutation producer does ToppleCat use by default?

PIT is the default Mutation Testing producer. It changes production behavior
and reports whether public acceptance work detected each change.

## Does ToppleCat use Cucumber, `.feature` files, or JGiven runtime/reporting?

No. Java/JUnit acceptance methods and typed JSON or YAML rows are the
executable source of truth; ToppleCat does not add those authoring or runtime
surfaces.
