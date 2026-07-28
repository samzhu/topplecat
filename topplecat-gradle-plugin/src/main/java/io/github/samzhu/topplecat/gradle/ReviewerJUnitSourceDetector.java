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

    static boolean hasOrdinaryExecutableJUnitTests(Path sourceRoot) {
        if (!Files.isDirectory(sourceRoot)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            return files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .anyMatch(ReviewerJUnitSourceDetector::hasOrdinaryExecutableJUnitTest);
        } catch (IOException exception) {
            throw new GradleException("Cannot inspect reviewer Java source root " + sourceRoot, exception);
        }
    }

    private static boolean hasOrdinaryExecutableJUnitTest(Path sourceFile) {
        try {
            String source = Files.readString(sourceFile);
            var matcher = EXECUTABLE_JUNIT_ANNOTATION.matcher(source);
            while (matcher.find()) {
                if (!hasToppleContractAnnotationFor(source, matcher.start())) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            throw new GradleException("Cannot inspect reviewer Java source " + sourceFile, exception);
        }
    }

    /**
     * Identifies the annotation block immediately attached to a JUnit method.
     * This deliberately stays lightweight: source validation belongs to javac,
     * while this detector only decides whether Review should flag an ignored
     * plain JUnit method.
     */
    private static boolean hasToppleContractAnnotationFor(String source, int junitAnnotationStart) {
        int lineStart = source.lastIndexOf('\n', junitAnnotationStart - 1) + 1;
        if (lineContainsToppleContractAnnotation(source.substring(lineStart, junitAnnotationStart))) {
            return true;
        }
        int cursor = lineStart;
        while (cursor > 0) {
            int previousLineEnd = cursor - 1;
            int previousLineStart = source.lastIndexOf('\n', previousLineEnd - 1) + 1;
            String previousLine = source.substring(previousLineStart, previousLineEnd).strip();
            if (previousLine.isEmpty()) {
                cursor = previousLineStart;
                continue;
            }
            if (!previousLine.startsWith("@") || previousLine.contains("{") || previousLine.contains(";")) {
                return false;
            }
            if (lineContainsToppleContractAnnotation(previousLine)) {
                return true;
            }
            cursor = previousLineStart;
        }
        return false;
    }

    private static boolean lineContainsToppleContractAnnotation(String line) {
        return line.matches(".*@(?:[A-Za-z_$][\\w$]*\\.)*Topple(?:Ac|Test)\\s*\\(.*");
    }
}
