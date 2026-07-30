package io.github.samzhu.topplecat.junit.property;

import java.util.function.Function;
import java.util.function.Predicate;

/** A sealed, ToppleCat-owned source of deterministic generated choices. */
public sealed interface Generator<T> permits Generators.BuiltInGenerator {
  <R> Generator<R> map(Function<? super T, ? extends R> mapper);

  Generator<T> filter(Predicate<? super T> predicate);
}
