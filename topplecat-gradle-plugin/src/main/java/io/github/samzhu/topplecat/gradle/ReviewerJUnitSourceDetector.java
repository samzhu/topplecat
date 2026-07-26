package io.github.samzhu.topplecat.gradle;

import org.gradle.api.GradleException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Identifies reviewer Java sources that declare an executable JUnit method. */
final class ReviewerJUnitSourceDetector {
    private static final Pattern EXECUTABLE_JUNIT_ANNOTATION = Pattern.compile(
            "(?m)^\\s*@(?:[A-Za-z_$][\\w$]*\\.)*(?:Test|RepeatedTest|TestFactory|TestTemplate|ParameterizedTest)\\b"
    );

    private ReviewerJUnitSourceDetector() {
    }

    static boolean hasJavaSources(Path sourceRoot) {
        if (!Files.isDirectory(sourceRoot)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            return files.anyMatch(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"));
        } catch (IOException exception) {
            throw new GradleException("Cannot inspect the reviewer source root " + sourceRoot, exception);
        }
    }

    static boolean hasExecutableJUnitTests(Path sourceRoot) {
        if (!Files.isDirectory(sourceRoot)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            return files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .anyMatch(ReviewerJUnitSourceDetector::hasExecutableJUnitTest);
        } catch (IOException exception) {
            throw new GradleException("Cannot inspect the reviewer source root " + sourceRoot, exception);
        }
    }

    private static boolean hasExecutableJUnitTest(Path sourceFile) {
        try {
            return EXECUTABLE_JUNIT_ANNOTATION.matcher(Files.readString(sourceFile)).find();
        } catch (IOException exception) {
            throw new GradleException("Cannot inspect reviewer Java source " + sourceFile, exception);
        }
    }
}
