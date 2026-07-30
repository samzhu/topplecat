package io.github.samzhu.topplecat.junit.property;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Fluent declaration of classifications, coverage requirements, and one Property assertion. */
public interface PropertyCheck<T> {
  PropertyCheck<T> classify(String label, Predicate<? super T> predicate);

  PropertyCheck<T> requireCoverage(String label, double minimumPercent);

  void check(Consumer<? super T> assertion);
}
