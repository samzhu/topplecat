package io.github.samzhu.topplecat.junit.property;

import io.github.samzhu.topplecat.core.PropertyClassification;
import io.github.samzhu.topplecat.core.PropertyCounterexample;
import io.github.samzhu.topplecat.core.PropertyExecutionState;
import io.github.samzhu.topplecat.core.PropertyResult;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Original deterministic v1 engine behind the deliberately small public Property API. */
final class PropertyEngine {
  static final String VERSION = "topplecat.property.engine.v1";
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
          .build();

  private PropertyEngine() {}

  static Outcome execute(
      Config config,
      Generator<?> generator,
      List<ClassificationRule<?>> classifications,
      Map<String, Double> coverage,
      Consumer<Object> assertion) {
    Stats stats = new Stats();
    SplitMix64 random = new SplitMix64(config.seed());
    try {
      ReplayPayload replay = replay(config);
      if (replay != null) {
        return replay(config, generator, classifications, assertion, replay);
      }
      List<Candidate> edges = uniqueEdges(edges(generator));
      int edgeIndex = 0;
      while (stats.completed < config.tries()) {
        if (stats.discards > config.maxDiscards()) {
          return incomplete(
              config, stats, classifications, "Generator discarded more values than maxDiscards.");
        }
        boolean edgeTrial = edgeIndex < edges.size() && edgeIndex < 32;
        Candidate candidate = edgeTrial ? edges.get(edgeIndex++) : sample(generator, random, stats);
        if (candidate == null) {
          continue;
        }
        boolean failed = fails(assertion, candidate.value());
        if (failed) {
          if (!fails(assertion, candidate.value())) {
            return incomplete(
                config, stats, classifications, "Counterexample replay was unstable.");
          }
          ShrinkResult shrunk = shrink(assertion, candidate, config.maxShrinks());
          if (!fails(assertion, shrunk.candidate().value())) {
            return incomplete(
                config, stats, classifications, "Shrunk counterexample replay was unstable.");
          }
          return counterexample(
              config, stats, classifications, candidate, shrunk, stats.completed + 1);
        }
        stats.completed++;
        if (edgeTrial) stats.edgeTrials++;
        else stats.randomTrials++;
        for (ClassificationRule<?> rule : classifications) {
          if (matches(rule, candidate.value()))
            stats.classifications.merge(rule.label(), 1, Integer::sum);
        }
      }
      List<PropertyClassification> summary =
          classifications(config.tries(), stats.classifications, coverage);
      if (summary.stream()
          .anyMatch(
              item ->
                  item.minimumPercent() != null && item.percent() + 1e-9 < item.minimumPercent())) {
        return incomplete(
            config,
            stats,
            classifications,
            "A declared classification coverage requirement was not met.");
      }
      return new Outcome(
          new PropertyResult(
              config.acId(),
              config.methodIdentity(),
              PropertyExecutionState.COMPLETED_PASS,
              config.tries(),
              stats.completed,
              stats.edgeTrials,
              stats.randomTrials,
              stats.discards,
              summary,
              config.seed(),
              false,
              null,
              null,
              null,
              0,
              false,
              null),
          null);
    } catch (PropertyInfrastructureException exception) {
      return incomplete(config, stats, classifications, exception.getMessage());
    } catch (RuntimeException exception) {
      return incomplete(
          config,
          stats,
          classifications,
          "Property generator or classification infrastructure failed.");
    }
  }

  private static Outcome counterexample(
      Config config,
      Stats stats,
      List<ClassificationRule<?>> rules,
      Candidate original,
      ShrinkResult shrunk,
      int trial) {
    return new Outcome(
        new PropertyResult(
            config.acId(),
            config.methodIdentity(),
            PropertyExecutionState.COMPLETED_COUNTEREXAMPLE,
            config.tries(),
            stats.completed,
            stats.edgeTrials,
            stats.randomTrials,
            stats.discards,
            classifications(Math.max(1, stats.completed), stats.classifications, Map.of()),
            config.seed(),
            true,
            replayToken(config, trial, shrunk.path()),
            new PropertyCounterexample(original.choices(), List.of()),
            new PropertyCounterexample(shrunk.candidate().choices(), shrunk.path()),
            shrunk.attempts(),
            shrunk.complete(),
            null),
        new AssertionError("ToppleCat Property counterexample"));
  }

  private static Outcome incomplete(
      Config config, Stats stats, List<ClassificationRule<?>> rules, String reason) {
    return new Outcome(
        new PropertyResult(
            config.acId(),
            config.methodIdentity(),
            PropertyExecutionState.COMPLETED_INCOMPLETE,
            config.tries(),
            stats.completed,
            stats.edgeTrials,
            stats.randomTrials,
            stats.discards,
            classifications(Math.max(1, stats.completed), stats.classifications, Map.of()),
            config.seed(),
            false,
            null,
            null,
            null,
            0,
            false,
            reason),
        new AssertionError(reason));
  }

  @SuppressWarnings("unchecked")
  private static boolean matches(ClassificationRule<?> rule, Object value) {
    try {
      return ((Predicate<Object>) rule.predicate()).test(value);
    } catch (RuntimeException exception) {
      throw new PropertyInfrastructureException("Property classification predicate failed.");
    }
  }

  private static boolean fails(Consumer<Object> assertion, Object value) {
    try {
      assertion.accept(value);
      return false;
    } catch (Throwable failure) {
      return true;
    }
  }

  private static ShrinkResult shrink(Consumer<Object> assertion, Candidate original, int limit) {
    Candidate best = original;
    List<Integer> path = new ArrayList<>();
    int attempts = 0;
    boolean progressed;
    do {
      progressed = false;
      List<Candidate> children = best.children();
      for (int index = 0; index < children.size() && attempts < limit; index++) {
        attempts++;
        Candidate child = children.get(index);
        if (fails(assertion, child.value())) {
          best = child;
          path.add(index);
          progressed = true;
          break;
        }
      }
    } while (progressed && attempts < limit);
    return new ShrinkResult(best, List.copyOf(path), attempts, !progressed);
  }

  private static List<PropertyClassification> classifications(
      int denominator, Map<String, Integer> counts, Map<String, Double> coverage) {
    LinkedHashSet<String> labels = new LinkedHashSet<>();
    labels.addAll(counts.keySet());
    labels.addAll(coverage.keySet());
    return labels.stream()
        .sorted()
        .map(
            label -> {
              int count = counts.getOrDefault(label, 0);
              return new PropertyClassification(
                  label, count, 100.0 * count / denominator, coverage.get(label));
            })
        .toList();
  }

  private static List<Candidate> uniqueEdges(List<Candidate> values) {
    Map<String, Candidate> unique = new LinkedHashMap<>();
    for (Candidate value : values) unique.putIfAbsent(value.choices(), value);
    return List.copyOf(unique.values());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Candidate sample(Generator<?> raw, SplitMix64 random, Stats stats) {
    Generators.BuiltInGenerator<?> generator = builtIn(raw);
    List<?> args = generator.arguments();
    return switch (generator.kind()) {
      case "booleans" -> scalar(random.nextBoolean(), List.of());
      case "integers" -> integer((Integer) args.get(0), (Integer) args.get(1), random);
      case "longs" -> longs((Long) args.get(0), (Long) args.get(1), random);
      case "bigDecimals" ->
          decimals(
              (BigDecimal) args.get(0), (BigDecimal) args.get(1), (Integer) args.get(2), random);
      case "elements" -> element((List<?>) args, random);
      case "enums" -> element((List<?>) args.get(1), random);
      case "strings" ->
          string((String) args.get(0), (Integer) args.get(1), (Integer) args.get(2), random);
      case "lists" ->
          list(
              (Generator<?>) args.get(0),
              (Integer) args.get(1),
              (Integer) args.get(2),
              random,
              stats);
      case "optional" -> optional((Generator<?>) args.getFirst(), random, stats);
      case "oneOf" ->
          sample(
              (Generator<?>) ((List<?>) args).get(random.nextInt(((List<?>) args).size())),
              random,
              stats);
      case "map" ->
          map(sample((Generator<?>) args.getFirst(), random, stats), (Function) args.get(1));
      case "filter" ->
          filtered((Generator<?>) args.getFirst(), (Predicate) args.get(1), random, stats);
      case "combine2" ->
          combine2(
              sample((Generator<?>) args.get(0), random, stats),
              sample((Generator<?>) args.get(1), random, stats),
              (BiFunction) args.get(2));
      case "combine3" ->
          combine3(
              sample((Generator<?>) args.get(0), random, stats),
              sample((Generator<?>) args.get(1), random, stats),
              sample((Generator<?>) args.get(2), random, stats),
              (Generators.Function3) args.get(3));
      default -> throw new PropertyInfrastructureException("Unknown built-in generator.");
    };
  }

  private static List<Candidate> edges(Generator<?> raw) {
    Generators.BuiltInGenerator<?> generator = builtIn(raw);
    List<?> args = generator.arguments();
    return switch (generator.kind()) {
      case "booleans" -> List.of(scalar(false, List.of()), scalar(true, List.of()));
      case "integers" -> integerEdges((Integer) args.get(0), (Integer) args.get(1));
      case "longs" -> longEdges((Long) args.get(0), (Long) args.get(1));
      case "bigDecimals" ->
          decimalEdges((BigDecimal) args.get(0), (BigDecimal) args.get(1), (Integer) args.get(2));
      case "elements" -> ((List<?>) args).stream().map(value -> scalar(value, List.of())).toList();
      case "enums" ->
          ((List<?>) args.get(1)).stream().map(value -> scalar(value, List.of())).toList();
      case "strings" ->
          stringEdges((String) args.get(0), (Integer) args.get(1), (Integer) args.get(2));
      case "lists" ->
          listEdges((Generator<?>) args.get(0), (Integer) args.get(1), (Integer) args.get(2));
      case "optional" -> optionalEdges((Generator<?>) args.getFirst());
      case "oneOf" ->
          ((List<?>) args).stream().flatMap(value -> edges((Generator<?>) value).stream()).toList();
      case "map" ->
          edges((Generator<?>) args.getFirst()).stream()
              .map(value -> map(value, (Function) args.get(1)))
              .toList();
      case "filter" ->
          edges((Generator<?>) args.getFirst()).stream()
              .filter(value -> accepts((Predicate<?>) args.get(1), value.value()))
              .toList();
      case "combine2" ->
          combineEdges2(
              edges((Generator<?>) args.get(0)),
              edges((Generator<?>) args.get(1)),
              (BiFunction) args.get(2));
      case "combine3" ->
          combineEdges3(
              edges((Generator<?>) args.get(0)),
              edges((Generator<?>) args.get(1)),
              edges((Generator<?>) args.get(2)),
              (Generators.Function3) args.get(3));
      default -> List.of();
    };
  }

  private static Generators.BuiltInGenerator<?> builtIn(Generator<?> generator) {
    if (!(generator instanceof Generators.BuiltInGenerator<?> builtIn)) {
      throw new PropertyInfrastructureException("Property generator is not ToppleCat-owned.");
    }
    return builtIn;
  }

  private static Candidate integer(int min, int max, SplitMix64 random) {
    int value = (int) random.between(min, max);
    return candidate(value, json(value), () -> integerShrinks(min, max, value));
  }

  private static Candidate longs(long min, long max, SplitMix64 random) {
    long value = random.between(min, max);
    return candidate(value, json(value), () -> longShrinks(min, max, value));
  }

  private static Candidate decimals(BigDecimal min, BigDecimal max, int scale, SplitMix64 random) {
    BigInteger lower = min.unscaledValue();
    BigInteger range = max.unscaledValue().subtract(lower).add(BigInteger.ONE);
    BigInteger value = lower.add(unsigned(random.nextLong()).mod(range));
    BigDecimal decimal = new BigDecimal(value, scale);
    return candidate(decimal, json(decimal), () -> decimalShrinks(min, max, decimal, scale));
  }

  private static Candidate element(List<?> values, SplitMix64 random) {
    int index = random.nextInt(values.size());
    Object value = values.get(index);
    return candidate(
        value,
        json(value),
        () -> values.subList(0, index).stream().map(item -> scalar(item, List.of())).toList());
  }

  private static Candidate string(String alphabet, int min, int max, SplitMix64 random) {
    int[] points = alphabet.codePoints().toArray();
    int length = (int) random.between(min, max);
    StringBuilder value = new StringBuilder();
    for (int index = 0; index < length; index++)
      value.appendCodePoint(points[random.nextInt(points.length)]);
    return stringCandidate(value.toString(), alphabet, min);
  }

  private static Candidate list(
      Generator<?> element, int min, int max, SplitMix64 random, Stats stats) {
    int size = (int) random.between(min, max);
    List<Candidate> candidates = new ArrayList<>();
    for (int index = 0; index < size; index++) candidates.add(sample(element, random, stats));
    return listCandidate(candidates, min);
  }

  private static Candidate optional(Generator<?> values, SplitMix64 random, Stats stats) {
    if (random.nextBoolean()) return candidate(Optional.empty(), "null", List::of);
    Candidate child = sample(values, random, stats);
    return candidate(
        Optional.of(child.value()),
        child.choices(),
        () -> List.of(candidate(Optional.empty(), "null", List::of)));
  }

  private static Candidate filtered(
      Generator<?> base, Predicate<Object> predicate, SplitMix64 random, Stats stats) {
    Candidate candidate = sample(base, random, stats);
    if (!accepts(predicate, candidate.value())) {
      stats.discards++;
      return null;
    }
    return candidate(
        candidate.value(),
        candidate.choices(),
        () ->
            candidate.children().stream()
                .filter(child -> accepts(predicate, child.value()))
                .toList());
  }

  @SuppressWarnings("unchecked")
  private static boolean accepts(Predicate<?> predicate, Object value) {
    try {
      return ((Predicate<Object>) predicate).test(value);
    } catch (RuntimeException exception) {
      throw new PropertyInfrastructureException("Generator filter predicate failed.");
    }
  }

  @SuppressWarnings("unchecked")
  private static Candidate map(Candidate base, Function<?, ?> mapper) {
    try {
      Object value = ((Function<Object, Object>) mapper).apply(base.value());
      if (value == null)
        throw new PropertyInfrastructureException("Property mapper returned null.");
      return candidate(
          value,
          base.choices(),
          () -> base.children().stream().map(child -> map(child, mapper)).toList());
    } catch (PropertyInfrastructureException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new PropertyInfrastructureException("Property mapper failed.");
    }
  }

  @SuppressWarnings("unchecked")
  private static Candidate combine2(Candidate first, Candidate second, BiFunction<?, ?, ?> mapper) {
    try {
      Object value =
          ((BiFunction<Object, Object, Object>) mapper).apply(first.value(), second.value());
      if (value == null)
        throw new PropertyInfrastructureException("Property mapper returned null.");
      return candidate(
          value,
          "[" + first.choices() + "," + second.choices() + "]",
          () -> {
            List<Candidate> children = new ArrayList<>();
            first.children().forEach(child -> children.add(combine2(child, second, mapper)));
            second.children().forEach(child -> children.add(combine2(first, child, mapper)));
            return children;
          });
    } catch (PropertyInfrastructureException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new PropertyInfrastructureException("Property mapper failed.");
    }
  }

  @SuppressWarnings("unchecked")
  private static Candidate combine3(
      Candidate first, Candidate second, Candidate third, Generators.Function3<?, ?, ?, ?> mapper) {
    try {
      Object value =
          ((Generators.Function3<Object, Object, Object, Object>) mapper)
              .apply(first.value(), second.value(), third.value());
      if (value == null)
        throw new PropertyInfrastructureException("Property mapper returned null.");
      return candidate(
          value,
          "[" + first.choices() + "," + second.choices() + "," + third.choices() + "]",
          () -> {
            List<Candidate> children = new ArrayList<>();
            first.children().forEach(child -> children.add(combine3(child, second, third, mapper)));
            second.children().forEach(child -> children.add(combine3(first, child, third, mapper)));
            third.children().forEach(child -> children.add(combine3(first, second, child, mapper)));
            return children;
          });
    } catch (PropertyInfrastructureException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new PropertyInfrastructureException("Property mapper failed.");
    }
  }

  private static Candidate listCandidate(List<Candidate> values, int minimumSize) {
    List<Object> actual = values.stream().map(Candidate::value).toList();
    String choices =
        "["
            + values.stream()
                .map(Candidate::choices)
                .reduce((left, right) -> left + "," + right)
                .orElse("")
            + "]";
    return candidate(
        actual,
        choices,
        () -> {
          List<Candidate> children = new ArrayList<>();
          int removable = values.size() - minimumSize;
          for (int width = removable; width >= 1; width--) {
            for (int start = 0; start + width <= values.size(); start++) {
              List<Candidate> remaining = new ArrayList<>(values);
              remaining.subList(start, start + width).clear();
              children.add(listCandidate(remaining, minimumSize));
            }
          }
          for (int index = 0; index < values.size(); index++) {
            int target = index;
            values
                .get(index)
                .children()
                .forEach(
                    child -> {
                      List<Candidate> replacement = new ArrayList<>(values);
                      replacement.set(target, child);
                      children.add(listCandidate(replacement, minimumSize));
                    });
          }
          return new ArrayList<>(children);
        });
  }

  private static List<Candidate> integerEdges(int min, int max) {
    LinkedHashSet<Integer> values = new LinkedHashSet<>();
    values.add(min);
    if (min != Integer.MAX_VALUE && min + 1 <= max) values.add(min + 1);
    if (min <= 0 && 0 <= max) values.add(0);
    if (max != Integer.MIN_VALUE && max - 1 >= min) values.add(max - 1);
    values.add(max);
    return values.stream().map(value -> scalar(value, List.of())).toList();
  }

  private static List<Candidate> longEdges(long min, long max) {
    LinkedHashSet<Long> values = new LinkedHashSet<>();
    values.add(min);
    if (min != Long.MAX_VALUE && min + 1 <= max) values.add(min + 1);
    if (min <= 0 && 0 <= max) values.add(0L);
    if (max != Long.MIN_VALUE && max - 1 >= min) values.add(max - 1);
    values.add(max);
    return values.stream().map(value -> scalar(value, List.of())).toList();
  }

  private static List<Candidate> decimalEdges(BigDecimal min, BigDecimal max, int scale) {
    LinkedHashSet<BigDecimal> values = new LinkedHashSet<>();
    values.add(min);
    if (min.compareTo(BigDecimal.ZERO.setScale(scale)) <= 0
        && max.compareTo(BigDecimal.ZERO.setScale(scale)) >= 0)
      values.add(BigDecimal.ZERO.setScale(scale));
    values.add(max);
    return values.stream().map(value -> scalar(value, List.of())).toList();
  }

  private static List<Candidate> stringEdges(String alphabet, int min, int max) {
    int first = alphabet.codePointAt(0);
    int last = alphabet.codePointBefore(alphabet.length());
    String letter = new String(Character.toChars(first));
    String finalLetter = new String(Character.toChars(last));
    LinkedHashSet<String> values = new LinkedHashSet<>();
    values.add(letter.repeat(min));
    values.add(finalLetter.repeat(min));
    values.add(letter.repeat(max));
    values.add(finalLetter.repeat(max));
    return values.stream().map(value -> scalar(value, List.of())).toList();
  }

  private static List<Candidate> listEdges(Generator<?> element, int min, int max) {
    List<Candidate> one = edges(element);
    Candidate first = one.isEmpty() ? null : one.getFirst();
    Candidate last = one.isEmpty() ? null : one.getLast();
    List<Candidate> values = new ArrayList<>();
    for (int size : new LinkedHashSet<>(List.of(min, max))) {
      List<Candidate> entries = new ArrayList<>();
      for (int index = 0; index < size && first != null; index++) entries.add(first);
      values.add(listCandidate(entries, min));
      if (last != null && last != first) {
        List<Candidate> lastEntries = new ArrayList<>();
        for (int index = 0; index < size; index++) lastEntries.add(last);
        values.add(listCandidate(lastEntries, min));
      }
    }
    return values;
  }

  private static List<Candidate> optionalEdges(Generator<?> generator) {
    List<Candidate> result = new ArrayList<>();
    result.add(candidate(Optional.empty(), "null", List::of));
    edges(generator).stream()
        .limit(1)
        .forEach(
            child -> result.add(candidate(Optional.of(child.value()), child.choices(), List::of)));
    return result;
  }

  @SuppressWarnings("unchecked")
  private static List<Candidate> combineEdges2(
      List<Candidate> first, List<Candidate> second, BiFunction<?, ?, ?> mapper) {
    List<Candidate> result = new ArrayList<>();
    if (!first.isEmpty() && !second.isEmpty()) {
      result.add(combine2(first.getFirst(), second.getFirst(), mapper));
      first.stream()
          .skip(1)
          .limit(15)
          .forEach(value -> result.add(combine2(value, second.getFirst(), mapper)));
      second.stream()
          .skip(1)
          .limit(15)
          .forEach(value -> result.add(combine2(first.getFirst(), value, mapper)));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static List<Candidate> combineEdges3(
      List<Candidate> first,
      List<Candidate> second,
      List<Candidate> third,
      Generators.Function3<?, ?, ?, ?> mapper) {
    List<Candidate> result = new ArrayList<>();
    if (first.isEmpty() || second.isEmpty() || third.isEmpty()) return result;
    Candidate baseFirst = first.getFirst();
    Candidate baseSecond = second.getFirst();
    Candidate baseThird = third.getFirst();
    result.add(combine3(baseFirst, baseSecond, baseThird, mapper));
    first.stream()
        .skip(1)
        .limit(10)
        .forEach(value -> result.add(combine3(value, baseSecond, baseThird, mapper)));
    second.stream()
        .skip(1)
        .limit(10)
        .forEach(value -> result.add(combine3(baseFirst, value, baseThird, mapper)));
    third.stream()
        .skip(1)
        .limit(10)
        .forEach(value -> result.add(combine3(baseFirst, baseSecond, value, mapper)));
    return result;
  }

  private static List<Candidate> integerShrinks(int min, int max, int value) {
    return shrinkingIntegers(min, max, value).stream()
        .map(candidate -> scalar(candidate, List.of()))
        .toList();
  }

  private static List<Candidate> longShrinks(long min, long max, long value) {
    return shrinkingLongs(min, max, value).stream()
        .map(candidate -> scalar(candidate, List.of()))
        .toList();
  }

  private static List<Candidate> decimalShrinks(
      BigDecimal min, BigDecimal max, BigDecimal value, int scale) {
    BigInteger lower = min.unscaledValue();
    BigInteger upper = max.unscaledValue();
    BigInteger target = lower.signum() > 0 ? lower : upper.signum() < 0 ? upper : BigInteger.ZERO;
    List<Candidate> children = new ArrayList<>();
    BigInteger current = value.unscaledValue();
    while (!current.equals(target)) {
      current = current.add(target).divide(BigInteger.TWO);
      if (!current.equals(value)) children.add(scalar(new BigDecimal(current, scale), List.of()));
      if (current.equals(target)) break;
    }
    return children;
  }

  private static List<Integer> shrinkingIntegers(int min, int max, int value) {
    int target = min > 0 ? min : max < 0 ? max : 0;
    List<Integer> children = new ArrayList<>();
    long current = value;
    while (current != target) {
      long previous = current;
      current = previous + (target - previous) / 2;
      if (current == previous) current += Long.signum(target - previous);
      children.add((int) current);
    }
    return children;
  }

  private static List<Long> shrinkingLongs(long min, long max, long value) {
    long target = min > 0 ? min : max < 0 ? max : 0;
    List<Long> children = new ArrayList<>();
    BigInteger current = BigInteger.valueOf(value);
    BigInteger targetValue = BigInteger.valueOf(target);
    while (!current.equals(targetValue)) {
      BigInteger previous = current;
      current = previous.add(targetValue).divide(BigInteger.TWO);
      if (current.equals(previous))
        current = current.add(BigInteger.valueOf(targetValue.subtract(previous).signum()));
      children.add(current.longValueExact());
    }
    return children;
  }

  private static Candidate stringCandidate(String value, String alphabet, int minimumLength) {
    return candidate(
        value,
        json(value),
        () -> {
          List<Candidate> children = new ArrayList<>();
          int[] points = value.codePoints().toArray();
          int removable = points.length - minimumLength;
          for (int width = removable; width >= 1; width--) {
            for (int start = 0; start + width <= points.length; start++) {
              StringBuilder remaining = new StringBuilder();
              for (int index = 0; index < points.length; index++) {
                if (index < start || index >= start + width)
                  remaining.appendCodePoint(points[index]);
              }
              children.add(stringCandidate(remaining.toString(), alphabet, minimumLength));
            }
          }
          int first = alphabet.codePointAt(0);
          for (int index = 0; index < points.length; index++) {
            if (points[index] == first) continue;
            int target = index;
            int[] replacement = points.clone();
            replacement[target] = first;
            StringBuilder changed = new StringBuilder();
            for (int point : replacement) changed.appendCodePoint(point);
            children.add(stringCandidate(changed.toString(), alphabet, minimumLength));
          }
          return children;
        });
  }

  private static BigInteger unsigned(long value) {
    return new BigInteger(1, java.nio.ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }

  private static String json(Object value) {
    try {
      return JSON.writeValueAsString(value);
    } catch (RuntimeException exception) {
      throw new PropertyInfrastructureException(
          "Generator value has no stable canonical JSON presentation.");
    }
  }

  private static Candidate scalar(Object value, List<Candidate> children) {
    return candidate(value, json(value), () -> children);
  }

  private static Candidate candidate(
      Object value, String choices, java.util.function.Supplier<List<Candidate>> children) {
    return new Candidate(value, choices, children);
  }

  record Config(
      String acId,
      String methodIdentity,
      int tries,
      int maxDiscards,
      int maxShrinks,
      long seed,
      String executionContext,
      String replayOverride) {
    Config(
        String acId, String methodIdentity, int tries, int maxDiscards, int maxShrinks, long seed) {
      this(acId, methodIdentity, tries, maxDiscards, maxShrinks, seed, "unmanaged", null);
    }
  }

  record ClassificationRule<T>(String label, Predicate<? super T> predicate) {}

  record Outcome(PropertyResult result, Throwable failure) {}

  private record Candidate(
      Object value, String choices, java.util.function.Supplier<List<Candidate>> childSupplier) {
    List<Candidate> children() {
      return List.copyOf(childSupplier.get());
    }
  }

  private record ShrinkResult(
      Candidate candidate, List<Integer> path, int attempts, boolean complete) {}

  private static final class Stats {
    int completed;
    int edgeTrials;
    int randomTrials;
    int discards;
    final Map<String, Integer> classifications = new LinkedHashMap<>();
  }

  private static final class PropertyInfrastructureException extends RuntimeException {
    PropertyInfrastructureException(String message) {
      super(message);
    }
  }

  private static final class SplitMix64 {
    private long state;

    SplitMix64(long seed) {
      state = seed;
    }

    long nextLong() {
      long value = (state += 0x9E3779B97F4A7C15L);
      value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
      value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
      return value ^ value >>> 31;
    }

    boolean nextBoolean() {
      return (nextLong() & 1) == 0;
    }

    int nextInt(int bound) {
      if (bound <= 0) throw new PropertyInfrastructureException("Generator bound is invalid.");
      return (int) Long.remainderUnsigned(nextLong(), bound);
    }

    long between(long min, long max) {
      if (min == max) return min;
      BigInteger span =
          BigInteger.valueOf(max).subtract(BigInteger.valueOf(min)).add(BigInteger.ONE);
      return BigInteger.valueOf(min).add(unsigned(nextLong()).mod(span)).longValueExact();
    }
  }

  private static ReplayPayload replay(Config config) {
    if (config.replayOverride() == null || config.replayOverride().isBlank()) return null;
    try {
      ReplayPayload replay =
          JSON.readValue(
              new String(
                  java.util.Base64.getUrlDecoder().decode(config.replayOverride()),
                  StandardCharsets.UTF_8),
              ReplayPayload.class);
      if (!config.methodIdentity().equals(replay.methodIdentity())) return null;
      if (!"topplecat.property-replay.v1".equals(replay.schema())
          || !VERSION.equals(replay.engine())
          || !config.executionContext().equals(replay.executionContext())
          || config.seed() != replay.seed()
          || replay.trial() < 1
          || replay.shrinkPath() == null
          || replay.shrinkPath().stream().anyMatch(index -> index == null || index < 0)) {
        throw new PropertyInfrastructureException(
            "Property replay token does not match this execution context.");
      }
      return replay;
    } catch (PropertyInfrastructureException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new PropertyInfrastructureException("Property replay token is invalid.");
    }
  }

  private static Outcome replay(
      Config config,
      Generator<?> generator,
      List<ClassificationRule<?>> classifications,
      Consumer<Object> assertion,
      ReplayPayload replay) {
    Stats stats = new Stats();
    SplitMix64 random = new SplitMix64(config.seed());
    List<Candidate> edges = uniqueEdges(edges(generator));
    int trial = 0;
    while (trial < replay.trial()) {
      Candidate candidate =
          trial < edges.size() && trial < 32 ? edges.get(trial) : sample(generator, random, stats);
      if (candidate == null) {
        if (stats.discards > config.maxDiscards())
          return incomplete(
              config, stats, classifications, "Property replay exceeded maxDiscards.");
        continue;
      }
      trial++;
      if (trial != replay.trial()) continue;
      if (!fails(assertion, candidate.value())) {
        return incomplete(
            config,
            stats,
            classifications,
            "Replay candidate did not reproduce the counterexample.");
      }
      Candidate shrunk = candidate;
      for (int index : replay.shrinkPath()) {
        List<Candidate> children = shrunk.children();
        if (index >= children.size() || !fails(assertion, children.get(index).value())) {
          return incomplete(
              config,
              stats,
              classifications,
              "Replay shrink path did not reproduce the counterexample.");
        }
        shrunk = children.get(index);
      }
      ShrinkResult result =
          new ShrinkResult(shrunk, replay.shrinkPath(), replay.shrinkPath().size(), false);
      return counterexample(config, stats, classifications, candidate, result, replay.trial());
    }
    return incomplete(
        config, stats, classifications, "Replay token did not identify a generated trial.");
  }

  private static String replayToken(Config config, int trial, List<Integer> path) {
    String payload =
        "{\"schema\":\"topplecat.property-replay.v1\",\"engine\":\""
            + VERSION
            + "\",\"methodIdentity\":\""
            + escape(config.methodIdentity())
            + "\",\"executionContext\":\""
            + escape(config.executionContext())
            + "\",\"seed\":"
            + config.seed()
            + ",\"trial\":"
            + trial
            + ",\"shrinkPath\":"
            + path
            + "}";
    return java.util.Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private record ReplayPayload(
      String schema,
      String engine,
      String methodIdentity,
      String executionContext,
      long seed,
      int trial,
      List<Integer> shrinkPath) {}
}
