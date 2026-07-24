package io.github.samzhu.topplecat.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ToppleCatReportTaskTest {
    @TempDir
    Path tempDir;

    @Test
    void replacesAStableBundleWithoutKeepingFilesFromAnOlderRun() throws Exception {
        Path source = tempDir.resolve("current");
        Path target = tempDir.resolve("stable");
        Files.createDirectories(source);
        Files.createDirectories(target.resolve("attachments"));
        Files.writeString(source.resolve("index.html"), "current");
        Files.writeString(target.resolve("index.html"), "old");
        Files.writeString(target.resolve("attachments/old.png"), "old attachment");

        ToppleCatReportTask.replaceTree(source, target);

        assertEquals("current", Files.readString(target.resolve("index.html")));
        assertFalse(Files.exists(target.resolve("attachments/old.png")));
    }
}
