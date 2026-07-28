package io.github.samzhu.topplecat.core;

/** Reviewer-only current-run proof that an AC-bound Java test entered its test body. */
public record ReviewerJavaExecution(String acId) {
    public ReviewerJavaExecution {
        if (acId == null || !acId.matches("AC-[A-Za-z0-9][A-Za-z0-9-]*")) {
            throw new ToppleCatException("Reviewer Java execution AC id is invalid.");
        }
    }
}
