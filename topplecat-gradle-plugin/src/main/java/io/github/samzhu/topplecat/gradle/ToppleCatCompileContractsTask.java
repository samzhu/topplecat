package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.junit.ToppleAcceptanceProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Gradle-safe compiler fallback for scenario descriptors.
 *
 * <p>Gradle wraps annotation-processing environments, which prevents the JDK's Trees API from
 * obtaining a compiler context. This task invokes the public JavaCompiler API directly with {@code
 * -proc:only}; the same ToppleAcceptanceProcessor therefore still receives a real javac AST and
 * resolved symbols, without relying on a regex parser.
 */
public abstract class ToppleCatCompileContractsTask extends DefaultTask {
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getSourceFiles();

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getCompileClasspath();

  @OutputDirectory
  public abstract DirectoryProperty getDescriptorClassesDirectory();

  @TaskAction
  public void compileContracts() {
    int runtime = Runtime.version().feature();
    if (runtime < 21) {
      throw new GradleException(
          "ToppleCat contract compilation requires JDK 21 or newer; detected JDK "
              + runtime
              + ". A Java 17 consumer source target does not imply Java 17 runtime support.");
    }
    Path output = getDescriptorClassesDirectory().get().getAsFile().toPath();
    clean(output);
    List<Path> sources =
        getSourceFiles().getFiles().stream()
            .map(file -> file.toPath())
            .filter(path -> path.toString().endsWith(".java"))
            .sorted()
            .toList();
    if (sources.isEmpty()) {
      return;
    }
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new GradleException(
          "ToppleCat requires a full JDK 21 or newer with a Java compiler to validate "
              + "@ToppleAcceptanceTest scenarios; a JRE is not sufficient.");
    }
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    // Keep Gradle's daemon-shared jar classpath handles open; closing this manager can invalidate
    // the archive file systems needed by the Seal-time javac source-closure pass.
    StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null);
    List<String> options =
        List.of(
            "-proc:only",
            "-classpath",
            classpath(
                getCompileClasspath().getFiles().stream().map(file -> file.toPath()).toList()),
            "-processorpath",
            classpath(
                List.of(
                    codeSource(ToppleAcceptanceProcessor.class),
                    codeSource(CompilerScenarioDescriptor.class))),
            "-processor",
            ToppleAcceptanceProcessor.class.getName(),
            "-d",
            output.toString());
    boolean success =
        Boolean.TRUE.equals(
            compiler
                .getTask(
                    null,
                    files,
                    diagnostics,
                    options,
                    null,
                    files.getJavaFileObjectsFromPaths(sources))
                .call());
    if (!success) {
      throw new GradleException(
          "ToppleCat compiler validation failed:\n" + diagnostics(diagnostics));
    }
  }

  private static String classpath(List<Path> files) {
    return files.stream()
        .map(Path::toString)
        .collect(Collectors.joining(java.io.File.pathSeparator));
  }

  private static Path codeSource(Class<?> type) {
    try {
      return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (Exception exception) {
      throw new GradleException(
          "ToppleCat cannot locate compiler dependency " + type.getName(), exception);
    }
  }

  private static String diagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
    return diagnostics.getDiagnostics().stream()
        .map(Diagnostic::toString)
        .collect(Collectors.joining("\n"));
  }

  private static void clean(Path output) {
    try {
      if (Files.exists(output)) {
        try (Stream<Path> paths = Files.walk(output)) {
          for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(path);
          }
        }
      }
      Files.createDirectories(output);
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot prepare ToppleCat compiler descriptor output "
              + output
              + ": "
              + exception.getMessage(),
          exception);
    }
  }
}
