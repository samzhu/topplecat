package io.github.samzhu.topplecat.core;

/** Reviewer-only, non-blocking observation about the expected shape of typed case rows. */
public record ContractQualityAdvisory(
    String ruleCode, String acId, String expectedPath, int publicCount, int hiddenCount)
    implements Comparable<ContractQualityAdvisory> {
  public static final String EXPECTED_SHAPE_VARIANT_MISSING = "EXPECTED_SHAPE_VARIANT_MISSING";
  public static final String EXPECTED_OPAQUE_IDENTIFIER_LITERALS =
      "EXPECTED_OPAQUE_IDENTIFIER_LITERALS";

  public ContractQualityAdvisory {
    if ((!EXPECTED_SHAPE_VARIANT_MISSING.equals(ruleCode)
            && !EXPECTED_OPAQUE_IDENTIFIER_LITERALS.equals(ruleCode))
        || acId == null
        || acId.isBlank()
        || expectedPath == null
        || expectedPath.isBlank()
        || publicCount < 0
        || hiddenCount < 0) {
      throw new ToppleCatException("Contract quality advisory is invalid.");
    }
  }

  @Override
  public int compareTo(ContractQualityAdvisory other) {
    int rule = ruleCode.compareTo(other.ruleCode);
    if (rule != 0) {
      return rule;
    }
    int ac = acId.compareTo(other.acId);
    return ac != 0 ? ac : expectedPath.compareTo(other.expectedPath);
  }
}
