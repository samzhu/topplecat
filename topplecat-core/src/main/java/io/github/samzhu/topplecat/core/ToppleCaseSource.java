package io.github.samzhu.topplecat.core;

import java.nio.file.Path;
import java.util.Objects;

/** A filesystem root and the visibility assigned to every case row below it. */
public record ToppleCaseSource(Path path, CaseVisibility visibility) {
    public ToppleCaseSource {
        path = Objects.requireNonNull(path, "path").normalize();
        visibility = Objects.requireNonNull(visibility, "visibility");
    }
}
