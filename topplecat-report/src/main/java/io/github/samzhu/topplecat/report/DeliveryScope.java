package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.SelectedSpecDocument;
import io.github.samzhu.topplecat.core.SelectedSpecScope;

import java.util.List;

/** Public selection metadata shown in reviewer projections; it never contains reviewer case values. */
public record DeliveryScope(
        List<SelectedSpecDocument> specDocuments,
        List<String> acceptanceConditionIds,
        String acceptanceConditionSetDigest,
        String hiddenMode,
        String mutationMode,
        int executedHiddenRows,
        int executedReviewerJavaTests,
        List<String> reviewerWarnings
) {
    public DeliveryScope {
        specDocuments = List.copyOf(specDocuments == null ? List.of() : specDocuments);
        acceptanceConditionIds = List.copyOf(acceptanceConditionIds == null ? List.of() : acceptanceConditionIds);
        reviewerWarnings = List.copyOf(reviewerWarnings == null ? List.of() : reviewerWarnings);
        if (acceptanceConditionSetDigest == null || !acceptanceConditionSetDigest.matches("[0-9a-f]{64}")
                || hiddenMode == null || hiddenMode.isBlank() || mutationMode == null || mutationMode.isBlank()
                || executedHiddenRows < 0 || executedReviewerJavaTests < 0) {
            throw new IllegalArgumentException("Delivery scope is invalid.");
        }
    }

    public static DeliveryScope from(SelectedSpecScope scope, String hiddenMode, String mutationMode,
                                     int executedHiddenRows, int executedReviewerJavaTests) {
        return new DeliveryScope(scope.specDocuments(), scope.acceptanceConditionIds(), scope.acceptanceConditionSetDigest(),
                hiddenMode, mutationMode, executedHiddenRows, executedReviewerJavaTests, List.of());
    }

    public DeliveryScope withReviewerWarnings(List<String> warnings) {
        return new DeliveryScope(specDocuments, acceptanceConditionIds, acceptanceConditionSetDigest, hiddenMode, mutationMode,
                executedHiddenRows, executedReviewerJavaTests, warnings);
    }
}
