package io.github.samzhu.topplecat.core;

import tools.jackson.databind.json.JsonMapper;

/** JSON codec for the reviewer-local public-contract approval embedded in escrow v2. */
public final class ReviewerContractApprovalJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ReviewerContractApprovalJson() {
    }

    public static String write(ReviewerContractApproval approval) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(approval) + "\n";
    }

    public static ReviewerContractApproval read(String source) {
        return JSON.readValue(source, ReviewerContractApproval.class);
    }
}
