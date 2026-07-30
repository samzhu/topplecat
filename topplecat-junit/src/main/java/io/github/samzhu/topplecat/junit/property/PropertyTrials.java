package io.github.samzhu.topplecat.junit.property;

/** Entry point injected into one {@link ToppleProperty} method. */
public interface PropertyTrials {
  <T> PropertyCheck<T> forAll(Generator<T> generator);
}
