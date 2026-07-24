package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.ToppleCatException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Creates a minimal, non-destructive ToppleCat authoring skeleton. */
public abstract class ToppleCatInitTask extends DefaultTask {
    @Internal
    public abstract DirectoryProperty getProjectRoot();

    @TaskAction
    public void initialize() {
        Path root = getProjectRoot().get().getAsFile().toPath();
        for (Template template : templates()) {
            writeIfMissing(root, template);
        }
        getLogger().lifecycle("ToppleCat did not modify .gitignore. Suggested entries for a reviewer-owned workspace:");
        getLogger().lifecycle("  .topplecat/");
        getLogger().lifecycle("  src/hiddenTest/");
        getLogger().lifecycle("Next steps:");
        getLogger().lifecycle("  ./gradlew toppleCatCheck");
        getLogger().lifecycle("  ./gradlew toppleCatHide");
        getLogger().lifecycle("  ./gradlew toppleCatVerify");
    }

    private void writeIfMissing(Path root, Template template) {
        Path target = root.resolve(template.path());
        try {
            if (Files.exists(target)) {
                getLogger().lifecycle("ToppleCat init skipped: {} already exists.", template.path());
                return;
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, template.contents());
            getLogger().lifecycle("ToppleCat init created: {}", template.path());
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot create ToppleCat init file " + target + ": " + exception.getMessage(), exception);
        }
    }

    private static List<Template> templates() {
        return List.of(
                new Template("src/test/resources/topplecat/cases/order-public.json", """
                        [
                          {
                            "caseId": "order-public-example",
                            "acId": "AC-EXAMPLE-ORDER",
                            "inputs": {"result": 1},
                            "expected": {"result": 1}
                          }
                        ]
                        """),
                new Template("src/test/java/example/OrderAcceptanceTest.java", """
                        package example;

                        import io.github.samzhu.topplecat.junit.ToppleCase;
                        import io.github.samzhu.topplecat.junit.ToppleStage;
                        import io.github.samzhu.topplecat.junit.ToppleStageField;
                        import io.github.samzhu.topplecat.junit.ToppleTest;

                        class OrderAcceptanceTest {
                            @ToppleStageField
                            ResultThen then;

                            @ToppleTest("AC-EXAMPLE-ORDER")
                            void createsOrder(ToppleCase c) {
                                then.matches_the_contract(c);
                            }

                            static final class ResultThen extends ToppleStage<ResultThen> {
                                ResultThen matches_the_contract(ToppleCase c) {
                                    recorded();
                                    // Read typed inputs, call production code, and verify every expected key here.
                                    c.verify("result", c.input("result", Integer.class));
                                    return self();
                                }
                            }
                        }
                        """),
                new Template("src/hiddenTest/resources/topplecat/cases/order-reviewer.yaml", """
                        - caseId: order-reviewer-boundary
                          acId: AC-EXAMPLE-ORDER
                          inputs: {result: 2}
                          expected: {result: 2}
                        """),
                new Template("src/hiddenTest/README.md", """
                        # Reviewer-only ToppleCat source

                        Keep private boundary rows and reviewer JUnit tests under this directory.
                        Replace the example row with a value that distinguishes the intended business rule
                        from a public-case coincidence. Run `toppleCatHide` before implementation work;
                        it moves this complete source set into plaintext local custody storage.

                        Never commit reviewer source to Git history that an implementation agent can read.
                        Handoff requires a public export without `.git`, `.topplecat`, or `build/`, or an
                        isolated environment whose history never contained reviewer material.
                        """)
        );
    }

    private record Template(String path, String contents) {
    }
}
