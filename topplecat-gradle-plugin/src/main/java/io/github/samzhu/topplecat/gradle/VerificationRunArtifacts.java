package io.github.samzhu.topplecat.gradle;

import org.gradle.api.GradleException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Per-invocation task-completion markers used to prevent stale verification evidence. */
final class VerificationRunArtifacts {
    static final String CONTRACT_INTEGRITY = "CONTRACT_INTEGRITY";
    static final String JUNIT = "JUNIT";
    static final String REVIEWER_JUNIT = "REVIEWER_JUNIT";
    static final String EXPECTED_CONSUMPTION = "EXPECTED_CONSUMPTION";
    static final String MUTATION = "MUTATION";

    private VerificationRunArtifacts() {
    }

    static void markCompleted(Path runDirectory, String gate) {
        Path marker = marker(runDirectory, gate);
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "completed\n");
        } catch (IOException exception) {
            throw new GradleException("Cannot mark ToppleCat " + gate + " gate as completed: " + exception.getMessage(), exception);
        }
    }

    static boolean completed(Path runDirectory, String gate) {
        return Files.isRegularFile(marker(runDirectory, gate));
    }

    private static Path marker(Path runDirectory, String gate) {
        return runDirectory.resolve("gates").resolve(gate + ".completed");
    }
}
