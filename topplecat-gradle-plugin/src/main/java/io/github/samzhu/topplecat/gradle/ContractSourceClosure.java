package io.github.samzhu.topplecat.gradle;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.PropertyDefinition;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Derives the sealed public test-source closure from compiler-owned acceptance descriptors.
 *
 * <p>The closure is intentionally a {@code javac} symbol graph, not a source-text heuristic. A
 * selected acceptance method or Stage method seals its declaring source file; every project test
 * source symbol referenced from that file is then resolved by {@code Trees} and added in turn. This
 * includes package-private and lower-case Java helpers, which cannot safely be inferred from import
 * or identifier spelling.
 */
final class ContractSourceClosure {
  private ContractSourceClosure() {}

  static Set<Path> resolve(
      Path projectRoot,
      Collection<Path> sourceRoots,
      Collection<Path> compileClasspath,
      ContractDefinition definition) {
    Set<String> requiredMethods = requiredMethods(definition);
    Set<String> requiredTypes = requiredStageTypes(definition);
    if (requiredMethods.isEmpty() && requiredTypes.isEmpty()) {
      return Set.of();
    }
    Analysis analysis = analyze(projectRoot, sourceRoots, compileClasspath);
    Set<Path> selected = new LinkedHashSet<>();
    ArrayDeque<Path> pending = new ArrayDeque<>();
    for (String identity : requiredMethods) {
      ExecutableElement method = analysis.methods().get(identity);
      if (method == null) {
        throw new ToppleCatException(
            "ToppleCat cannot seal an acceptance source whose javac symbol is not a project"
                + " test-side source: "
                + identity
                + ".");
      }
      Path source = sourceFor(method, analysis.trees(), analysis.sources());
      if (source == null) {
        throw new ToppleCatException(
            "ToppleCat cannot seal an acceptance source whose javac symbol has no project test"
                + " source: "
                + identity
                + ".");
      }
      add(source, selected, pending);
    }
    for (String binaryName : requiredTypes) {
      TypeElement type = analysis.types().get(binaryName);
      if (type == null) {
        throw new ToppleCatException(
            "ToppleCat cannot seal a Scenario Stage whose javac symbol is not a project test-side"
                + " source: "
                + binaryName
                + ".");
      }
      Path source = sourceFor(type, analysis.trees(), analysis.sources());
      if (source == null) {
        throw new ToppleCatException(
            "ToppleCat cannot seal a Scenario Stage whose javac symbol has no project test source: "
                + binaryName
                + ".");
      }
      add(source, selected, pending);
    }
    while (!pending.isEmpty()) {
      Path source = pending.removeFirst();
      CompilationUnitTree unit = analysis.units().get(source);
      if (unit == null) {
        throw new ToppleCatException(
            "ToppleCat cannot inspect the selected acceptance source " + source + ".");
      }
      new DependencyScanner(analysis.trees(), analysis.sources(), selected, pending)
          .scan(unit, null);
    }
    return Set.copyOf(selected);
  }

  private static Set<String> requiredMethods(ContractDefinition definition) {
    Set<String> result = new LinkedHashSet<>();
    for (AcceptanceContract contract : definition.acceptanceConditions()) {
      result.add(contract.scenario().acceptanceTestMethodIdentity());
      for (StepTemplate step : contract.scenario().steps()) {
        result.add(step.stepId());
      }
      for (PropertyDefinition property : contract.properties()) {
        result.add(property.methodIdentity());
      }
    }
    return Set.copyOf(result);
  }

  private static Set<String> requiredStageTypes(ContractDefinition definition) {
    Set<String> result = new LinkedHashSet<>();
    for (AcceptanceContract contract : definition.acceptanceConditions()) {
      contract.scenario().stageParameters().forEach(stage -> result.add(stage.stageBinaryName()));
    }
    return Set.copyOf(result);
  }

  private static Analysis analyze(
      Path projectRoot, Collection<Path> sourceRoots, Collection<Path> compileClasspath) {
    Path root = projectRoot.toAbsolutePath().normalize();
    List<Path> sources = sourceFiles(root, sourceRoots);
    if (sources.isEmpty()) {
      throw new ToppleCatException(
          "ToppleCat cannot derive an acceptance source closure because no public Java sources were"
              + " found.");
    }
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new ToppleCatException(
          "ToppleCat requires the JDK Java compiler to derive the sealed acceptance source"
              + " closure.");
    }
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    // Gradle's daemon shares jar-backed classpath file systems with its compiler workers. Closing
    // a StandardJavaFileManager can close those shared archive handles on current JDKs, making a
    // later formal compilation fail with ClosedFileSystemException. The daemon owns this small
    // compiler cache; keep the manager open for its lifetime.
    StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null);
    try {
      List<String> options = new ArrayList<>(List.of("-proc:none", "-implicit:none"));
      String classpath = classpath(compileClasspath);
      if (!classpath.isBlank()) {
        options.add("-classpath");
        options.add(classpath);
      }
      JavacTask task =
          (JavacTask)
              compiler.getTask(
                  null,
                  files,
                  diagnostics,
                  options,
                  null,
                  files.getJavaFileObjectsFromPaths(sources));
      List<CompilationUnitTree> units = new ArrayList<>();
      task.parse().forEach(units::add);
      task.analyze();
      if (diagnostics.getDiagnostics().stream()
          .anyMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)) {
        throw new ToppleCatException(
            "ToppleCat cannot resolve the sealed acceptance source closure: "
                + diagnosticSummary(diagnostics));
      }
      Trees trees = Trees.instance(task);
      Elements elements = task.getElements();
      Map<Path, CompilationUnitTree> unitsByPath = new LinkedHashMap<>();
      for (CompilationUnitTree unit : units) {
        Path source = sourcePath(unit);
        if (!sources.contains(source)) {
          throw new ToppleCatException(
              "ToppleCat compiler analysis returned a public source outside the approved source"
                  + " roots.");
        }
        unitsByPath.put(source, unit);
      }
      Map<String, ExecutableElement> methods = methods(trees, elements, units);
      Map<String, TypeElement> types = types(trees, elements, units);
      return new Analysis(
          trees,
          Map.copyOf(unitsByPath),
          Set.copyOf(sources),
          Map.copyOf(methods),
          Map.copyOf(types));
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot derive the public acceptance source closure: " + exception.getMessage(),
          exception);
    } catch (RuntimeException exception) {
      if (exception instanceof ToppleCatException) {
        throw exception;
      }
      throw new ToppleCatException(
          "Cannot derive the public acceptance source closure from javac symbols: "
              + exception.getMessage(),
          exception);
    }
  }

  private static List<Path> sourceFiles(Path root, Collection<Path> sourceRoots) {
    Set<Path> sources = new LinkedHashSet<>();
    for (Path sourceRoot : sourceRoots) {
      if (sourceRoot == null || !Files.isDirectory(sourceRoot)) {
        continue;
      }
      Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
      requireInside(root, normalizedRoot);
      try (Stream<Path> files = Files.walk(normalizedRoot)) {
        for (Path source :
            files
                .filter(path -> path.toString().endsWith(".java"))
                .sorted(Comparator.naturalOrder())
                .toList()) {
          Path normalized = source.toAbsolutePath().normalize();
          requireInside(root, normalized);
          if (Files.isSymbolicLink(source)) {
            throw new ToppleCatException(
                "ToppleCat source closure rejects symbolic links: " + root.relativize(normalized));
          }
          sources.add(normalized);
        }
      } catch (IOException exception) {
        throw new ToppleCatException(
            "Cannot derive the public acceptance source closure: " + exception.getMessage(),
            exception);
      }
    }
    return sources.stream().sorted().toList();
  }

  private static Map<String, ExecutableElement> methods(
      Trees trees, Elements elements, List<CompilationUnitTree> units) {
    Map<String, ExecutableElement> result = new LinkedHashMap<>();
    for (CompilationUnitTree unit : units) {
      new TreePathScanner<Void, Void>() {
        @Override
        public Void visitMethod(MethodTree method, Void unused) {
          Element element = trees.getElement(getCurrentPath());
          if (element instanceof ExecutableElement executable) {
            String identity = methodIdentity(executable, elements);
            ExecutableElement previous = result.putIfAbsent(identity, executable);
            if (previous != null && previous != executable) {
              throw new ToppleCatException(
                  "ToppleCat cannot derive a unique javac symbol for " + identity + ".");
            }
          }
          return super.visitMethod(method, unused);
        }
      }.scan(unit, null);
    }
    return result;
  }

  private static Map<String, TypeElement> types(
      Trees trees, Elements elements, List<CompilationUnitTree> units) {
    Map<String, TypeElement> result = new LinkedHashMap<>();
    for (CompilationUnitTree unit : units) {
      new TreePathScanner<Void, Void>() {
        @Override
        public Void visitClass(com.sun.source.tree.ClassTree declaration, Void unused) {
          Element element = trees.getElement(getCurrentPath());
          if (element instanceof TypeElement type) {
            String binaryName = elements.getBinaryName(type).toString();
            TypeElement previous = result.putIfAbsent(binaryName, type);
            if (previous != null && previous != type) {
              throw new ToppleCatException(
                  "ToppleCat cannot derive a unique javac symbol for " + binaryName + ".");
            }
          }
          return super.visitClass(declaration, unused);
        }
      }.scan(unit, null);
    }
    return result;
  }

  private static String methodIdentity(ExecutableElement method, Elements elements) {
    Element enclosing = method.getEnclosingElement();
    if (!(enclosing instanceof TypeElement owner)) {
      throw new ToppleCatException("ToppleCat encountered a method without a declaring Java type.");
    }
    StringBuilder descriptor = new StringBuilder("(");
    method
        .getParameters()
        .forEach(parameter -> descriptor.append(typeDescriptor(parameter.asType(), elements)));
    descriptor.append(')').append(typeDescriptor(method.getReturnType(), elements));
    return elements.getBinaryName(owner) + "#" + method.getSimpleName() + descriptor;
  }

  private static String typeDescriptor(TypeMirror type, Elements elements) {
    return switch (type.getKind()) {
      case BOOLEAN -> "Z";
      case BYTE -> "B";
      case SHORT -> "S";
      case INT -> "I";
      case LONG -> "J";
      case CHAR -> "C";
      case FLOAT -> "F";
      case DOUBLE -> "D";
      case VOID -> "V";
      case ARRAY -> "[" + typeDescriptor(((ArrayType) type).getComponentType(), elements);
      case DECLARED ->
          "L"
              + elements
                  .getBinaryName((TypeElement) ((DeclaredType) type).asElement())
                  .toString()
                  .replace('.', '/')
              + ";";
      default ->
          throw new ToppleCatException(
              "ToppleCat cannot derive a JVM descriptor for "
                  + type
                  + " while sealing the contract.");
    };
  }

  private static Path sourceFor(Element element, Trees trees, Set<Path> sources) {
    Element current = element;
    while (current != null) {
      TreePath path = trees.getPath(current);
      if (path != null) {
        Path source = sourcePath(path.getCompilationUnit());
        return sources.contains(source) ? source : null;
      }
      current = current.getEnclosingElement();
    }
    return null;
  }

  private static Path sourcePath(CompilationUnitTree unit) {
    try {
      return Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize();
    } catch (RuntimeException exception) {
      throw new ToppleCatException(
          "ToppleCat cannot resolve the filesystem path of a compiler-owned source file.",
          exception);
    }
  }

  private static void add(Path source, Set<Path> selected, ArrayDeque<Path> pending) {
    if (selected.add(source)) {
      pending.addLast(source);
    }
  }

  private static String classpath(Collection<Path> entries) {
    return entries.stream()
        .filter(path -> path != null && Files.exists(path))
        .map(path -> path.toAbsolutePath().normalize().toString())
        .sorted()
        .collect(Collectors.joining(File.pathSeparator));
  }

  private static String diagnosticSummary(DiagnosticCollector<JavaFileObject> diagnostics) {
    return diagnostics.getDiagnostics().stream()
        .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
        .findFirst()
        .map(diagnostic -> diagnostic.getMessage(null))
        .orElse("javac reported an unresolved source symbol");
  }

  private static void requireInside(Path root, Path candidate) {
    if (!candidate.startsWith(root)) {
      throw new ToppleCatException(
          "ToppleCat source closure found a helper outside the project root.");
    }
  }

  private record Analysis(
      Trees trees,
      Map<Path, CompilationUnitTree> units,
      Set<Path> sources,
      Map<String, ExecutableElement> methods,
      Map<String, TypeElement> types) {}

  private static final class DependencyScanner extends TreePathScanner<Void, Void> {
    private final Trees trees;
    private final Set<Path> sources;
    private final Set<Path> selected;
    private final ArrayDeque<Path> pending;

    private DependencyScanner(
        Trees trees, Set<Path> sources, Set<Path> selected, ArrayDeque<Path> pending) {
      this.trees = trees;
      this.sources = sources;
      this.selected = selected;
      this.pending = pending;
    }

    @Override
    public Void scan(com.sun.source.tree.Tree tree, Void unused) {
      if (tree != null && getCurrentPath() != null) {
        Path source = sourceFor(trees.getElement(getCurrentPath()), trees, sources);
        if (source != null) {
          add(source, selected, pending);
        }
      }
      return super.scan(tree, unused);
    }
  }
}
