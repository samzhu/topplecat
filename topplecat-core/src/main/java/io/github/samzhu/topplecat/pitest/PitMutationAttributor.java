package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Assigns PIT mutants to ACs from the exact acceptance methods that cover each mutant. */
public final class PitMutationAttributor {
  private PitMutationAttributor() {}

  /**
   * Evaluates every AC using PIT's full mutation matrix. Each mutant belongs to every covering
   * JUnit invocation whose class and method signature match the compiled acceptance contract. Class
   * membership alone is deliberately insufficient: one Java acceptance-test class may contain
   * several AC methods.
   */
  public static List<PitMutationAssessment> assess(
      PitMutationReport report,
      Map<String, ? extends Set<String>> canonicalMethodsByAc,
      int threshold) {
    if (report == null || !report.coverageMatrix()) {
      throw new ToppleCatException(
          "PIT fullMutationMatrix=true is required for automatic per-AC attribution.");
    }
    if (threshold < 0 || threshold > 100) {
      throw new ToppleCatException("PIT mutation threshold must be between 0 and 100.");
    }
    List<PitMutationAssessment> assessments = new ArrayList<>();
    for (String acId : canonicalMethodsByAc.keySet().stream().sorted().toList()) {
      List<CanonicalTestMethod> methods =
          canonicalMethodsByAc.get(acId).stream()
              .map(CanonicalTestMethod::parse)
              .sorted(Comparator.comparing(CanonicalTestMethod::identity))
              .toList();
      List<PitMutation> selected =
          report.mutations().stream()
              .filter(
                  mutation ->
                      mutation.coveringTests().stream()
                          .anyMatch(
                              test -> methods.stream().anyMatch(method -> method.matches(test))))
              .toList();
      int detected = (int) selected.stream().filter(PitMutation::killed).count();
      int score = selected.isEmpty() ? 0 : (detected * 100) / selected.size();
      EvidenceVerdict verdict =
          !selected.isEmpty() && score >= threshold ? EvidenceVerdict.PASS : EvidenceVerdict.FAIL;
      List<String> testClasses =
          methods.stream().map(CanonicalTestMethod::className).distinct().toList();
      assessments.add(
          new PitMutationAssessment(
              acId, testClasses, threshold, selected.size(), detected, score, verdict));
    }
    return assessments.stream().sorted(Comparator.comparing(PitMutationAssessment::acId)).toList();
  }

  private record CanonicalTestMethod(String identity, String className, String junitSignature) {
    private static CanonicalTestMethod parse(String identity) {
      if (identity == null) {
        throw invalidIdentity(null);
      }
      int methodSeparator = identity.indexOf('#');
      int parametersStart = identity.indexOf('(', methodSeparator + 1);
      int parametersEnd = identity.indexOf(')', parametersStart + 1);
      if (methodSeparator < 1
          || parametersStart <= methodSeparator + 1
          || parametersEnd < parametersStart) {
        throw invalidIdentity(identity);
      }
      String className = identity.substring(0, methodSeparator);
      String methodName = identity.substring(methodSeparator + 1, parametersStart);
      String parameters =
          descriptorParameters(identity.substring(parametersStart + 1, parametersEnd), identity);
      return new CanonicalTestMethod(identity, className, methodName + "(" + parameters + ")");
    }

    private boolean matches(String pitName) {
      if (pitName == null) {
        return false;
      }
      String normalized = pitName.trim();
      if (!(normalized.equals(className)
          || normalized.startsWith(className + ".")
          || normalized.startsWith(className + "["))) {
        return false;
      }
      return normalized.contains("[test-template:" + junitSignature + "]")
          || normalized.contains("[method:" + junitSignature + "]");
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
