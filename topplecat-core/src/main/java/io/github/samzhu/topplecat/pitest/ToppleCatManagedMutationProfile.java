package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The fixed PIT producer policy used by formal ToppleCat verification.
 *
 * <p>The raw identities are PIT 1.25.5's {@code MethodMutatorFactory#getGloballyUniqueId()} values,
 * verified against that pinned runtime. They are deliberately kept beside the configured operator
 * IDs so reviewer evidence can establish that a producer result came from this profile.
 */
public final class ToppleCatManagedMutationProfile {
  public static final String PIT_VERSION = "1.25.5";
  public static final String PROFILE_ID = "topplecat-managed-v1";

  private static final Map<String, String> RAW_IDENTITIES_BY_OPERATOR =
      Map.ofEntries(
          Map.entry(
              "TRUE_RETURNS",
              "org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanTrueReturnValsMutator"),
          Map.entry(
              "FALSE_RETURNS",
              "org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanFalseReturnValsMutator"),
          Map.entry(
              "PRIMITIVE_RETURNS",
              "org.pitest.mutationtest.engine.gregor.mutators.returns.PrimitiveReturnsMutator"),
          Map.entry(
              "EMPTY_RETURNS",
              "org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator"),
          Map.entry(
              "NULL_RETURNS",
              "org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator"),
          Map.entry(
              "REMOVE_CONDITIONALS_EQUAL_IF",
              "org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator_EQUAL_IF"),
          Map.entry(
              "REMOVE_CONDITIONALS_EQUAL_ELSE",
              "org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator_EQUAL_ELSE"),
          Map.entry(
              "REMOVE_CONDITIONALS_ORDER_IF",
              "org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator_ORDER_IF"),
          Map.entry(
              "REMOVE_CONDITIONALS_ORDER_ELSE",
              "org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator_ORDER_ELSE"),
          Map.entry(
              "CONDITIONALS_BOUNDARY",
              "org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator"),
          Map.entry(
              "VOID_METHOD_CALLS",
              "org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator"),
          Map.entry("MATH", "org.pitest.mutationtest.engine.gregor.mutators.MathMutator"));

  private static final List<String> OPERATOR_IDS =
      List.of(
          "TRUE_RETURNS",
          "FALSE_RETURNS",
          "PRIMITIVE_RETURNS",
          "EMPTY_RETURNS",
          "NULL_RETURNS",
          "REMOVE_CONDITIONALS_EQUAL_IF",
          "REMOVE_CONDITIONALS_EQUAL_ELSE",
          "REMOVE_CONDITIONALS_ORDER_IF",
          "REMOVE_CONDITIONALS_ORDER_ELSE",
          "CONDITIONALS_BOUNDARY",
          "VOID_METHOD_CALLS",
          "MATH");
  private static final Set<String> RAW_IDENTITIES = Set.copyOf(RAW_IDENTITIES_BY_OPERATOR.values());

  private ToppleCatManagedMutationProfile() {}

  /** The exact PIT operator IDs passed to the managed producer, in stable reviewer order. */
  public static List<String> operatorIds() {
    return OPERATOR_IDS;
  }

  /** PIT 1.25.5 raw mutator identity for each managed operator ID. */
  public static Map<String, String> rawIdentitiesByOperator() {
    return new LinkedHashMap<>(RAW_IDENTITIES_BY_OPERATOR);
  }

  /** Rejects a report that cannot be proven to be produced by this exact managed profile. */
  public static void validate(PitMutationReport report) {
    if (report == null) {
      throw new ToppleCatException("Managed PIT mutation evidence is required.");
    }
    for (PitMutation mutation : report.mutations()) {
      if (!RAW_IDENTITIES.contains(mutation.mutator())) {
        throw new ToppleCatException(
            "PIT mutation report contains a raw mutator outside " + PROFILE_ID + ".");
      }
    }
  }
}
