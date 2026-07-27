# Frequently asked questions

[繁體中文](faq.zh-TW.md)

## Why doesn't ToppleCat use Cucumber or `.feature` files?

Because ToppleCat wants the readable specification and the code that JUnit
executes to stay together.

Cucumber deliberately separates them. A scenario lives in a Gherkin
`.feature` file, then a step definition connects each line to program code.
That is useful when people who do not work in the programming language need to
write scenarios themselves. It also creates another mapping to maintain.

ToppleCat takes a different trade-off. A canonical `@ToppleTest` is plain Java
that reads like a short business scenario:

```java
@ToppleTest("AC-CART-COUPON")
@DisplayName("Apply a coupon to an order")
void appliesCoupon(ToppleCase c) {
    given.a_cart(c.input("cart", Cart.class));
    when.creates_an_order();
    then.receipt_matches(c);
}
```

That method is checked by the ToppleCat compiler and executed by JUnit. The
same Stage calls become the sentences shown in the generated Spec and
verification reports. There is no separately editable `.feature` file and no
text-to-step binding between the displayed scenario and the test that ran.

This matters for delegated work. If an AI agent can change a prose scenario,
its step binding, and the implementation, a green result may hide a weakened
contract. ToppleCat keeps the authoring surface smaller and seals the approved
Java contract, case data, build logic, and verification policy before the
implementation is handed off.

## What does “single source of truth” mean here?

It does not mean that the whole contract must fit in one file.

ToppleCat's executable authority consists of ordinary Java acceptance tests and
typed JSON or YAML case rows. Java defines the behavior and assertions; the
rows provide inputs and expected results. Generated JSON and HTML are views of
that contract, not another specification to edit.

The important part is that a report cannot say one thing while JUnit silently
executes another interpretation of the same sentence.

## How does “DSLs Enable Reliable Use of LLMs” relate to ToppleCat?

In
[DSLs Enable Reliable Use of LLMs](https://martinfowler.com/articles/llm-and-dsls.html),
Unmesh Joshi argues that a small, constrained DSL gives an LLM fewer ways to
invent its own interpretation. A parser, type checker, or compiler can then
reject invalid output and give the agent concrete feedback to repair.

ToppleCat follows that idea with a small internal Java DSL. `@ToppleTest`,
Stage methods, and typed case data give the agent a limited vocabulary for
describing an acceptance contract. The ToppleCat compiler checks its shape,
JUnit executes it, and the same Stage calls become the human-readable
specification. The lasting source of truth is the executable contract, not the
prompt that originally asked an agent to create it.

The DSL does not prove that the implementation is correct. Valid Java can still
omit a rule, return a hard-coded public answer, or make assertions that never
consume the expected result. That is why ToppleCat also uses reviewer-only
retests, expected-result consumption, mutation testing, and contract-integrity
checks.

## Does ToppleCat use PIT for mutation testing?

Yes. [PIT](https://pitest.org/) is ToppleCat's default mutation-testing engine.
A consumer may not see PIT explicitly in its own `build.gradle` because
ToppleCat applies and configures the PIT Gradle plugin when the default
`pitest` producer is used.

The responsibilities are separate:

- PIT changes production bytecode, runs the selected public tests, and writes
  the mutation report.
- ToppleCat selects the canonical public `@ToppleTest` classes for its managed
  PIT run, reads that report, attributes mutants to canonical ACs, applies the
  required threshold, and records the current verdict.

ToppleCat 0.0.5 uses the PIT Gradle plugin 1.19.0, PIT 1.25.5, and the PIT
JUnit 5 plugin 1.2.3. These are implementation versions, not new ToppleCat
authoring APIs. A project can configure a custom mutation producer instead;
in that case, the custom producer is responsible for running mutation testing
and producing the report ToppleCat evaluates.

## Why can't I find an `org.pitest` dependency in the ToppleCat modules?

Because PIT is an execution tool, not part of ToppleCat's Java API. The
`topplecat-gradle-plugin` module depends on the
`info.solidsoft.pitest` Gradle plugin adapter. When a consumer uses the default
mutation producer, that adapter resolves the `org.pitest` engine, command-line
runner, and JUnit 5 plugin for the `pitest` task.

ToppleCat does not import PIT classes into its public modules. It reads PIT's
XML output through its own parser, so PIT does not appear on an application's
main or test classpath. After a mutation run, the downloaded `org.pitest`
artifacts can be found in Gradle's dependency cache rather than in this source
repository.

## Does the PIT license allow ToppleCat to use it this way?

Yes. PIT, its JUnit 5 plugin, and the Gradle PIT plugin are licensed under the
Apache License 2.0. That license permits use, including commercial use, as well
as modification and redistribution under its conditions.

ToppleCat currently uses the unmodified published artifacts as build-time
tools. Its own plugin JAR does not contain PIT or Gradle PIT plugin classes;
the published dependency metadata lets Gradle download them separately.
ToppleCat is also licensed under Apache 2.0. It does not use the separate
commercial ArcMutate products.

If ToppleCat later embeds or modifies PIT code, the distribution will also
need to carry the applicable license, copyright, change, and NOTICE
information required by Apache 2.0.

## What came from the BDD discussion with Dan North and Dave Farley?

Dan North originated Behaviour-Driven Development (BDD) and created JBehave.
He developed BDD while teaching Test-Driven Development, after seeing teams
struggle with where to start, what to test, and how to describe a failure.
His answer was to describe software in terms of business behaviour and make
acceptance criteria executable.

The episode
[The Origins of Behaviour Driven Development](https://open.spotify.com/episode/5sWDCL6J21dFbjDyyeVT9B)
spends a substantial section discussing Cucumber, internal DSLs, and executable
examples. The relevant discussion runs roughly from 16:13 to 38:00.

The discussion makes four points that shaped this choice:

- BDD is primarily about shared understanding. It is not defined by plain-text
  files.
- A scenario can be written in the project's programming language and still
  read like a business example.
- Starting with Cucumber can add several layers between the scenario and the
  code: Gherkin parsing, step matching, step definitions, and the system-facing
  driver.
- Plain-text feature files have a valuable, narrower use case. They work well
  when domain experts need to co-author scenarios line by line and the shared
  output is part of repairing communication between business and engineering.

The speakers also describe a large SpecFlow suite that took many hours to run
and was later reduced to small, direct tests with an internal DSL. The lesson is
not that Cucumber is always wrong. It is that `.feature` files should solve a
real collaboration problem, not be the default definition of BDD.

## What did JGiven influence?

[JGiven](https://github.com/TNG/JGiven) describes itself as BDD in plain Java.
Its scenarios use a fluent, domain-specific Java API, run through JUnit or
TestNG, and generate reports that domain experts can read.

ToppleCat follows that basic shape:

- scenarios are Java;
- Stage methods provide the domain vocabulary;
- the test runner executes the real scenario;
- reports are generated from the execution.

ToppleCat is not a JGiven fork or replacement. It adds a different concern:
checking an AI agent's completion claim with reviewer-only cases, expected
result consumption, mutation testing, contract integrity, and current-run
evidence.

ToppleCat does not depend on or bundle JGiven, and it does not import
`com.tngtech.jgiven` classes. JGiven is prior art that informed the design,
not a runtime component. Linking to it gives credit and helps readers compare
the approaches.

JGiven itself is published under Apache License 2.0. That license is compatible
with ToppleCat's Apache 2.0 license, but it does not impose distribution
requirements on ToppleCat today because no JGiven code or binary is included.

## Can a product owner still read the specification?

Yes. They review business titles, Stage sentences, public examples, and
expected outcomes in the generated HTML report. They do not need to read Stage
implementations or production code.

The author still has work to do: names such as
`a_cart_with_two_eligible_items()` must use the language of the domain. Moving
the scenario into Java does not automatically make it readable.

## When is Cucumber the better choice?

Choose Cucumber when the `.feature` file itself is a shared working document:

- domain experts really will write or edit scenario steps;
- those experts understand the line-level detail;
- the team is prepared to maintain step definitions as a separate interface;
- the rendered feature status is important to the wider organization.

That is a legitimate design. It is not the problem ToppleCat is trying to
solve.

## Can a project use Cucumber alongside ToppleCat?

Yes, but Cucumber scenarios remain a separate test suite. ToppleCat does not
read `.feature` files or treat them as canonical AC bindings. A ToppleCat AC
still needs its Java annotation and, for data-driven behavior, a canonical
`@ToppleTest` with typed case rows.

## References

- [Unmesh Joshi: DSLs Enable Reliable Use of LLMs](https://martinfowler.com/articles/llm-and-dsls.html)
- [PIT Mutation Testing](https://pitest.org/)
- [PIT license](https://github.com/hcoles/pitest/blob/master/LICENSE.txt)
- [Gradle PIT plugin license](https://github.com/szpak/gradle-pitest-plugin/blob/master/LICENSE-2.0.txt)
- [Dan North: Introducing BDD](https://dannorth.net/blog/introducing-bdd/)
- [Dan North and Dave Farley: The Origins of Behaviour Driven Development](https://open.spotify.com/episode/5sWDCL6J21dFbjDyyeVT9B)
- [JGiven: Behavior-Driven Development in plain Java](https://github.com/TNG/JGiven)
- [JGiven license](https://github.com/TNG/JGiven/blob/master/LICENSE)
- [Cucumber introduction](https://cucumber.io/docs/)
- [Cucumber step definitions](https://cucumber.io/docs/cucumber/step-definitions/)
