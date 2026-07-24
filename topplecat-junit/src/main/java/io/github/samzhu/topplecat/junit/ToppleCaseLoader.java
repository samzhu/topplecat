package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCaseReader;
import io.github.samzhu.topplecat.core.ToppleCaseSource;
import io.github.samzhu.topplecat.core.ToppleCatException;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

final class ToppleCaseLoader {
    private ToppleCaseLoader() {
    }

    static List<ToppleCase> load(String acId) {
        List<ToppleCaseSource> sources = new ArrayList<>();
        paths(System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, ToppleJunit.DEFAULT_PUBLIC_CASE_ROOT))
                .forEach(path -> sources.add(new ToppleCaseSource(path, CaseVisibility.PUBLIC)));
        if (Boolean.parseBoolean(System.getProperty(ToppleJunit.INCLUDE_HIDDEN_CASES_PROPERTY, "false"))) {
            paths(System.getProperty(ToppleJunit.HIDDEN_CASE_SOURCES_PROPERTY, ""))
                    .forEach(path -> sources.add(new ToppleCaseSource(path, CaseVisibility.HIDDEN)));
        }
        List<ToppleCase> result = ToppleCaseReader.readAll(sources).stream()
                .filter(testCase -> acId.equals(testCase.acId()))
                .map(ToppleCase::new)
                .toList();
        if (result.isEmpty()) {
            throw new ToppleCatException("@ToppleTest(" + acId + ") found no public JSON/YAML cases. "
                    + "Add a row under " + ToppleJunit.DEFAULT_PUBLIC_CASE_ROOT + ".");
        }
        return result;
    }

    private static List<Path> paths(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        return Arrays.stream(encoded.split(Pattern.quote(File.pathSeparator)))
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .toList();
    }
}
