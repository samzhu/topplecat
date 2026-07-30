package io.github.samzhu.topplecat.junit.property;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/** Fixed first-release generator constructors. Execution and shrinking remain ToppleCat-owned. */
public final class Generators {
  private Generators() {}

  public static Generator<Boolean> booleans() {
    return new BuiltInGenerator<>("booleans", List.of());
  }

  public static Generator<Integer> integers(int minInclusive, int maxInclusive) {
    ordered(minInclusive, maxInclusive, "Integer bounds");
    return new BuiltInGenerator<>("integers", List.of(minInclusive, maxInclusive));
  }

  public static Generator<Long> longs(long minInclusive, long maxInclusive) {
    ordered(minInclusive, maxInclusive, "Long bounds");
    return new BuiltInGenerator<>("longs", List.of(minInclusive, maxInclusive));
  }

  public static Generator<BigDecimal> bigDecimals(
      BigDecimal minInclusive, BigDecimal maxInclusive, int scale) {
    Objects.requireNonNull(minInclusive, "minInclusive");
    Objects.requireNonNull(maxInclusive, "maxInclusive");
    if (scale < 0
        || minInclusive.scale() != scale
        || maxInclusive.scale() != scale
        || minInclusive.compareTo(maxInclusive) > 0) {
      throw new IllegalArgumentException(
          "BigDecimal bounds must be ordered at the exact declared scale.");
    }
    return new BuiltInGenerator<>("bigDecimals", List.of(minInclusive, maxInclusive, scale));
  }

  public static <T> Generator<T> elements(List<T> orderedValues) {
    List<T> values = immutableNonEmpty(orderedValues, "orderedValues");
    return new BuiltInGenerator<>("elements", values);
  }

  public static <E extends Enum<E>> Generator<E> enums(Class<E> enumType) {
    Objects.requireNonNull(enumType, "enumType");
    E[] values = enumType.getEnumConstants();
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("enumType must declare at least one constant.");
    }
    return new BuiltInGenerator<>("enums", List.of(enumType, List.of(values)));
  }

  public static Generator<String> strings(String alphabet, int minLength, int maxLength) {
    if (alphabet == null
        || alphabet.isEmpty()
        || alphabet.codePoints().distinct().count() != alphabet.codePoints().count()) {
      throw new IllegalArgumentException(
          "alphabet must contain distinct, non-empty Unicode code points.");
    }
    ordered(minLength, maxLength, "String length bounds");
    if (minLength < 0) {
      throw new IllegalArgumentException("String length bounds must be non-negative.");
    }
    return new BuiltInGenerator<>("strings", List.of(alphabet, minLength, maxLength));
  }

  public static <T> Generator<List<T>> lists(Generator<T> elements, int minSize, int maxSize) {
    Objects.requireNonNull(elements, "elements");
    ordered(minSize, maxSize, "List size bounds");
    if (minSize < 0) {
      throw new IllegalArgumentException("List size bounds must be non-negative.");
    }
    return new BuiltInGenerator<>("lists", List.of(elements, minSize, maxSize));
  }

  public static <T> Generator<Optional<T>> optional(Generator<T> values) {
    return new BuiltInGenerator<>("optional", List.of(Objects.requireNonNull(values, "values")));
  }

  public static <T> Generator<T> oneOf(List<Generator<? extends T>> generators) {
    List<Generator<? extends T>> values = immutableNonEmpty(generators, "generators");
    return new BuiltInGenerator<>("oneOf", values);
  }

  public static <A, B, R> Generator<R> combine(
      Generator<A> first,
      Generator<B> second,
      BiFunction<? super A, ? super B, ? extends R> mapper) {
    return new BuiltInGenerator<>(
        "combine2",
        List.of(
            Objects.requireNonNull(first, "first"),
            Objects.requireNonNull(second, "second"),
            Objects.requireNonNull(mapper, "mapper")));
  }

  public static <A, B, C, R> Generator<R> combine(
      Generator<A> first,
      Generator<B> second,
      Generator<C> third,
      Function3<? super A, ? super B, ? super C, ? extends R> mapper) {
    return new BuiltInGenerator<>(
        "combine3",
        List.of(
            Objects.requireNonNull(first, "first"),
            Objects.requireNonNull(second, "second"),
            Objects.requireNonNull(third, "third"),
            Objects.requireNonNull(mapper, "mapper")));
  }

  @FunctionalInterface
  public interface Function3<A, B, C, R> {
    R apply(A first, B second, C third);
  }

  static final class BuiltInGenerator<T> implements Generator<T> {
    private final String kind;
    private final List<?> arguments;

    private BuiltInGenerator(String kind, List<?> arguments) {
      this.kind = kind;
      this.arguments = List.copyOf(arguments);
    }

    @Override
    public <R> Generator<R> map(Function<? super T, ? extends R> mapper) {
      return new BuiltInGenerator<>("map", List.of(this, Objects.requireNonNull(mapper, "mapper")));
    }

    @Override
    public Generator<T> filter(Predicate<? super T> predicate) {
      return new BuiltInGenerator<>(
          "filter", List.of(this, Objects.requireNonNull(predicate, "predicate")));
    }

    String kind() {
      return kind;
    }

    List<?> arguments() {
      return arguments;
    }
  }

  private static void ordered(long minimum, long maximum, String label) {
    if (minimum > maximum) {
      throw new IllegalArgumentException(label + " must be ordered.");
    }
  }

  private static <T> List<T> immutableNonEmpty(List<T> values, String name) {
    if (values == null || values.isEmpty() || values.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(name + " must be non-empty and contain no nulls.");
    }
    return List.copyOf(values);
  }
}
