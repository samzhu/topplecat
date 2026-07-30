package io.github.samzhu.topplecat.core;

import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/** JSON codec for the current reviewer-local public-contract approval. */
public final class ReviewerContractApprovalJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ReviewerContractApprovalJson() {}

  public static String write(ReviewerContractApproval approval) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(approval) + "\n";
  }

  public static ReviewerContractApproval read(String source) {
    RawApproval approval = JSON.readValue(source, RawApproval.class);
    if (!ReviewerContractApproval.SCHEMA_VERSION.equals(approval.schemaVersion())) {
      throw new ToppleCatException(
          "Unsupported reviewer contract approval schema: "
              + approval.schemaVersion()
              + ". Recreate reviewer custody with toppleCatSeal.");
    }
    return new ReviewerContractApproval(
        approval.schemaVersion(),
        approval.publicFiles(),
        approval.publicDefinitionDigest(),
        approval.verificationPolicy(),
        approval.selectedSpecScope(),
        approval.approvalDigest());
  }

  private record RawApproval(
      String schemaVersion,
      List<PublicContractEntry> publicFiles,
      String publicDefinitionDigest,
      VerificationPolicy verificationPolicy,
      SelectedSpecScope selectedSpecScope,
      String approvalDigest) {}
}
