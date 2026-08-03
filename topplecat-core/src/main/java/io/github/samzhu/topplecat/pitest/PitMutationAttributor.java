package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attributes PIT's full selector matrix to the exact public Acceptance Methods that executed it.
 */
public final class PitMutationAttributor {
  private PitMutationAttributor() {}

  /**
   * Keeps PIT's raw outcomes and test relationships while calculating coverage and detection for
   * each exact compiled Acceptance Method. It deliberately never infers a kill from a status or
   * boolean flag: only {@code killingTests} supplies contract-scoped detection evidence.
   */
  public static PitMutationAttribution attribute(
      PitMutationReport report, Map<String, ? extends Set<String>> canonicalMethodsByAc) {
    return attribute(report, canonicalMethodsByAc, ignored -> null);
  }

  /**
   * Attributes a report and optionally resolves the original source line for each PIT mutation. The
   * resolver is diagnostic-only; it cannot affect coverage, detection, or the Gate verdict.
   */
  public static PitMutationAttribution attribute(
      PitMutationReport report,
      Map<String, ? extends Set<String>> canonicalMethodsByAc,
      Function<PitMutation, String> originalSourceLineResolver) {
    if (report == null || !report.coverageMatrix()) {
      throw new ToppleCatException(
          "PIT fullMutationMatrix=true with coveringTests, killingTests, and succeedingTests is"
              + " required for automatic per-AC attribution.");
    }
    ToppleCatManagedMutationProfile.validate(report);

    List<MethodBinding> bindings = bindings(canonicalMethodsByAc);
    Function<PitMutation, String> sourceLineResolver =
        originalSourceLineResolver == null ? ignored -> null : originalSourceLineResolver;
    Map<String, AcCounts> countsByAc = new TreeMap<>();
    for (String acId : canonicalMethodsByAc.keySet().stream().sorted().toList()) {
      countsByAc.put(acId, new AcCounts(methodsFor(acId, bindings)));
    }

    List<PitMutationEvidence> evidence = new ArrayList<>();
    Map<Outcome, Integer> producerOutcomes = new TreeMap<>();
    Map<Outcome, Integer> unattributedOutcomes = new TreeMap<>();
    Map<String, Map<Outcome, Integer>> outcomesByMutator = new TreeMap<>();
    int attributed = 0;
    for (PitMutation mutation : report.mutations()) {
      Set<String> covered = matchedAcceptanceConditions(mutation.coveringTests(), bindings);
      Set<String> killed = matchedAcceptanceConditions(mutation.killingTests(), bindings);
      Set<String> detectedBy = new LinkedHashSet<>(killed);
      detectedBy.retainAll(covered);
      // Parse succeeding selectors even though they are not score input. An ambiguous selector is
      // unusable reviewer evidence and must not silently disappear from the matrix.
      matchedAcceptanceConditions(mutation.succeedingTests(), bindings);

      Outcome outcome = new Outcome(mutation.status(), mutation.detected());
      increment(producerOutcomes, outcome);
      increment(
          outcomesByMutator.computeIfAbsent(mutation.mutator(), ignored -> new TreeMap<>()),
          outcome);
      if (covered.isEmpty()) {
        increment(unattributedOutcomes, outcome);
      } else {
        attributed++;
      }
      for (String acId : covered) {
        AcCounts counts = countsByAc.get(acId);
        counts.covered++;
        increment(counts.outcomes, outcome);
        if (detectedBy.contains(acId)) {
          counts.killed++;
        }
      }
      evidence.add(
          new PitMutationEvidence(
              mutation.detected(),
              mutation.status(),
              mutation.mutatedClass(),
              mutation.sourceFile(),
              mutation.mutatedMethod(),
              mutation.methodDescription(),
              mutation.lineNumber(),
              mutation.block(),
              mutation.index(),
              mutation.mutator(),
              mutation.description(),
              mutation.coveringTests(),
              mutation.killingTests(),
              mutation.succeedingTests(),
              covered.stream().sorted().toList(),
              detectedBy.stream().sorted().toList(),
              sourceLineResolver.apply(mutation)));
    }

    List<PitMutationAssessment> assessments =
        countsByAc.entrySet().stream()
            .map(
                entry -> {
                  String acId = entry.getKey();
                  AcCounts counts = entry.getValue();
                  return new PitMutationAssessment(
                      acId,
                      counts.acceptanceMethods,
                      counts.covered,
                      counts.killed,
                      outcomeCounts(counts.outcomes),
                      counts.covered == 0);
                })
            .toList();
    return new PitMutationAttribution(
        ToppleCatManagedMutationProfile.PIT_VERSION,
        ToppleCatManagedMutationProfile.PROFILE_ID,
        ToppleCatManagedMutationProfile.operatorIds(),
        report.mutations().size(),
        attributed,
        report.mutations().size() - attributed,
        outcomeCounts(producerOutcomes),
        outcomeCounts(unattributedOutcomes),
        mutatorSummaries(outcomesByMutator),
        assessments,
        evidence);
  }

  /**
   * @deprecated use {@link #attribute(PitMutationReport, Map)}.
   */
  @Deprecated
  public static PitMutationAttribution attribute(
      PitMutationReport report,
      Map<String, ? extends Set<String>> canonicalMethodsByAc,
      int ignoredSealedThreshold) {
    return attribute(report, canonicalMethodsByAc);
  }

  private static List<MethodBinding> bindings(
      Map<String, ? extends Set<String>> canonicalMethodsByAc) {
    if (canonicalMethodsByAc == null) {
      throw new ToppleCatException("Canonical Acceptance Method identities are required.");
    }
    List<MethodBinding> bindings = new ArrayList<>();
    for (String acId : canonicalMethodsByAc.keySet().stream().sorted().toList()) {
      if (acId == null || acId.isBlank()) {
        throw new ToppleCatException("Canonical Acceptance Condition id is required.");
      }
      Set<String> identities = canonicalMethodsByAc.get(acId);
      if (identities == null || identities.isEmpty()) {
        throw new ToppleCatException(
            "Canonical Acceptance Method identity is required for " + acId + ".");
      }
      for (String identity : identities.stream().sorted().toList()) {
        bindings.add(new MethodBinding(acId, CanonicalTestMethod.parse(identity)));
      }
    }
    return List.copyOf(bindings);
  }

  private static List<String> methodsFor(String acId, List<MethodBinding> bindings) {
    return bindings.stream()
        .filter(binding -> binding.acId.equals(acId))
        .map(binding -> binding.method.identity)
        .sorted()
        .toList();
  }

  private static Set<String> matchedAcceptanceConditions(
      List<String> selectors, List<MethodBinding> bindings) {
    Set<String> result = new LinkedHashSet<>();
    for (String selector : selectors) {
      for (MethodBinding binding : bindings) {
        SelectorMatch match = binding.method.matches(selector);
        if (match == SelectorMatch.MALFORMED) {
          throw new ToppleCatException(
              "PIT full mutation matrix contains an unparseable Acceptance Method selector: "
                  + selector);
        }
        if (match == SelectorMatch.MATCH) {
          result.add(binding.acId);
        }
      }
    }
    return result;
  }

  private static void increment(Map<Outcome, Integer> counts, Outcome outcome) {
    counts.merge(outcome, 1, Integer::sum);
  }

  private static List<PitOutcomeCount> outcomeCounts(Map<Outcome, Integer> counts) {
    return counts.entrySet().stream()
        .map(
            entry ->
                new PitOutcomeCount(
                    entry.getKey().status, entry.getKey().detected, entry.getValue()))
        .toList();
  }

  private static List<PitMutatorSummary> mutatorSummaries(
      Map<String, Map<Outcome, Integer>> outcomesByMutator) {
    return outcomesByMutator.entrySet().stream()
        .map(
            entry ->
                new PitMutatorSummary(
                    entry.getKey(),
                    entry.getValue().values().stream().mapToInt(Integer::intValue).sum(),
                    outcomeCounts(entry.getValue())))
        .toList();
  }

  private record MethodBinding(String acId, CanonicalTestMethod method) {}

  private static final class AcCounts {
    private final List<String> acceptanceMethods;
    private final Map<Outcome, Integer> outcomes = new TreeMap<>();
    private int covered;
    private int killed;

    private AcCounts(List<String> acceptanceMethods) {
      this.acceptanceMethods = List.copyOf(acceptanceMethods);
    }
  }

  private record Outcome(String status, boolean detected) implements Comparable<Outcome> {
    @Override
    public int compareTo(Outcome other) {
      int statusOrder = status.compareTo(other.status);
      return statusOrder != 0 ? statusOrder : Boolean.compare(detected, other.detected);
    }
  }

  private enum SelectorMatch {
    MATCH,
    NO_MATCH,
    MALFORMED
  }

  private record CanonicalTestMethod(String identity, String className, String junitSignature) {
    private static final Pattern CLASS_SELECTOR = Pattern.compile("\\[class:([^\\]]+)]");
    private static final Pattern METHOD_SIGNATURE =
        Pattern.compile("^([A-Za-z_$][A-Za-z0-9_$]*)\\((.*)\\)$");

    private static CanonicalTestMethod parse(String identity) {
      if (identity == null) {
        throw invalidIdentity(null);
      }
      int methodSeparator = identity.indexOf('#');
      int parametersStart = identity.indexOf('(', methodSeparator + 1);
      int parametersEnd = identity.indexOf(')', parametersStart + 1);
      if (methodSeparator < 1
          || parametersStart <= methodSeparator + 1
          || parametersEnd < parametersStart
          || parametersEnd + 1 >= identity.length()) {
        throw invalidIdentity(identity);
      }
      String className = identity.substring(0, methodSeparator);
      String methodName = identity.substring(methodSeparator + 1, parametersStart);
      if (!methodName.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
        throw invalidIdentity(identity);
      }
      String parameters =
          descriptorParameters(identity.substring(parametersStart + 1, parametersEnd), identity);
      return new CanonicalTestMethod(identity, className, methodName + "(" + parameters + ")");
    }

    private SelectorMatch matches(String pitName) {
      if (pitName == null) {
        return SelectorMatch.NO_MATCH;
      }
      String normalizedName = pitName.trim();
      Matcher classSelector = CLASS_SELECTOR.matcher(normalizedName);
      if (!classSelector.find()) {
        return selectorStart(normalizedName) >= 0
            ? SelectorMatch.MALFORMED
            : SelectorMatch.NO_MATCH;
      }
      if (!className.equals(classSelector.group(1))) {
        return SelectorMatch.NO_MATCH;
      }
      int template = normalizedName.indexOf("[test-template:");
      int method = normalizedName.indexOf("[method:");
      int start = earliest(template, method);
      if (start < 0) {
        // A class-only selector is never enough to attribute a mutant.
        return SelectorMatch.NO_MATCH;
      }
      int prefixLength = normalizedName.startsWith("[test-template:", start) ? 15 : 8;
      int closing = normalizedName.indexOf(")]", start + prefixLength);
      if (closing < 0) {
        return SelectorMatch.MALFORMED;
      }
      String signature = normalizedName.substring(start + prefixLength, closing + 1);
      Matcher parsed = METHOD_SIGNATURE.matcher(signature);
      if (!parsed.matches()) {
        return SelectorMatch.MALFORMED;
      }
      String parameters = parsed.group(2);
      String normalized = parameters.replaceAll("\\s*,\\s*", ",");
      return junitSignature.equals(parsed.group(1) + "(" + normalized + ")")
          ? SelectorMatch.MATCH
          : SelectorMatch.NO_MATCH;
    }

    private static int selectorStart(String selector) {
      return earliest(selector.indexOf("[test-template:"), selector.indexOf("[method:"));
    }

    private static int earliest(int first, int second) {
      if (first < 0) {
        return second;
      }
      if (second < 0) {
        return first;
      }
      return Math.min(first, second);
    }

    private static String descriptorParameters(String descriptor, String identity) {
      List<String> parameters = new ArrayList<>();
      int index = 0;
      while (index < descriptor.length()) {
        int dimensions = 0;
        while (index < descriptor.length() && descriptor.charAt(index) == '[') {
          dimensions++;
          index++;
        }
        if (index >= descriptor.length()) {
          throw invalidIdentity(identity);
        }
        char type = descriptor.charAt(index++);
        String name =
            switch (type) {
              case 'Z' -> "boolean";
              case 'B' -> "byte";
              case 'S' -> "short";
              case 'I' -> "int";
              case 'J' -> "long";
              case 'C' -> "char";
              case 'F' -> "float";
              case 'D' -> "double";
              case 'L' -> {
                int end = descriptor.indexOf(';', index);
                if (end < index) {
                  throw invalidIdentity(identity);
                }
                String declared = descriptor.substring(index, end).replace('/', '.');
                index = end + 1;
                yield declared;
              }
              default -> throw invalidIdentity(identity);
            };
        parameters.add(name + "[]".repeat(dimensions));
      }
      return String.join(",", parameters);
    }

    private static ToppleCatException invalidIdentity(String identity) {
      return new ToppleCatException(
          "Canonical test method identity is invalid for PIT attribution: " + identity);
    }
  }
}
