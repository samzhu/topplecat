package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.AcceptanceContract;
import io.github.samzhu.topplecat.core.CaseDefinition;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractIntegrityResult;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.PublicContractEntry;
import io.github.samzhu.topplecat.core.ReviewerContractApproval;
import io.github.samzhu.topplecat.core.SelectedSpecScope;
import io.github.samzhu.topplecat.core.ToppleCatException;
import io.github.samzhu.topplecat.core.VerificationPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Gradle-owned discovery and comparison for the reviewer-approved public contract. */
final class ContractApprovalFactory {
  private static final Set<String> EXCLUDED_TOP_LEVEL =
      Set.of("build", ".gradle", ".git", ".topplecat");

  private ContractApprovalFactory() {}

  static ReviewerContractApproval create(
      Path buildRoot,
      Collection<Path> publicSourceRoots,
      Path publicCaseRoot,
      ContractDefinition definition,
      VerificationPolicy policy) {
    return create(
        buildRoot,
        publicSourceRoots,
        List.of(),
        publicCaseRoot,
        definition,
        policy,
        SelectedSpecScope.empty());
  }

  static ReviewerContractApproval create(
      Path buildRoot,
      Collection<Path> publicSourceRoots,
      Collection<Path> compileClasspath,
      Path publicCaseRoot,
      ContractDefinition definition,
      VerificationPolicy policy,
      SelectedSpecScope selectedSpecScope) {
    Path root = normalizedRoot(buildRoot);
    Set<Path> files = new HashSet<>();
    ContractSourceClosure.resolve(root, publicSourceRoots, compileClasspath, definition)
        .forEach(source -> files.add(checkedFile(root, source)));
    addTree(root, publicCaseRoot, files);
    addGradleLogic(root, files);
    List<PublicContractEntry> entries =
        files.stream().map(path -> entry(root, path)).sorted().toList();
    return ReviewerContractApproval.create(
        entries, publicProjection(definition).digest(), policy, selectedSpecScope);
  }

  static ContractIntegrityResult compare(
      ReviewerContractApproval approved, ReviewerContractApproval current) {
    if (approved == null) {
      return new ContractIntegrityResult(
          ContractIntegrityResult.SCHEMA_VERSION,
          EvidenceVerdict.INCOMPLETE,
          null,
          current == null ? null : current.approvalDigest(),
          List.of(),
          List.of(),
          List.of(),
          false,
          List.of());
    }
    if (current == null) {
      return new ContractIntegrityResult(
          ContractIntegrityResult.SCHEMA_VERSION,
          EvidenceVerdict.INCOMPLETE,
          approved.approvalDigest(),
          null,
          List.of(),
          List.of(),
          List.of(),
          false,
          List.of());
    }
    Map<String, String> before = entries(approved.publicFiles());
    Map<String, String> after = entries(current.publicFiles());
    List<String> added =
        after.keySet().stream().filter(path -> !before.containsKey(path)).sorted().toList();
    List<String> removed =
        before.keySet().stream().filter(path -> !after.containsKey(path)).sorted().toList();
    List<String> changed =
        after.keySet().stream()
            .filter(before::containsKey)
            .filter(path -> !after.get(path).equals(before.get(path)))
            .sorted()
            .toList();
    boolean definitionMatches =
        approved.publicDefinitionDigest().equals(current.publicDefinitionDigest());
    boolean scopeMatches = approved.selectedSpecScope().equals(current.selectedSpecScope());
    List<String> changedPolicy =
        changedPolicyFields(approved.verificationPolicy(), current.verificationPolicy());
    EvidenceVerdict verdict =
        added.isEmpty()
                && removed.isEmpty()
                && changed.isEmpty()
                && definitionMatches
                && scopeMatches
                && changedPolicy.isEmpty()
            ? EvidenceVerdict.PASS
            : EvidenceVerdict.FAIL;
    return new ContractIntegrityResult(
        ContractIntegrityResult.SCHEMA_VERSION,
        verdict,
        approved.approvalDigest(),
        current.approvalDigest(),
        added,
        changed,
        removed,
        definitionMatches,
        changedPolicy);
  }

  private static Map<String, String> entries(List<PublicContractEntry> entries) {
    Map<String, String> result = new HashMap<>();
    for (PublicContractEntry entry : entries) {
      result.put(entry.path(), entry.sha256());
    }
    return result;
  }

  private static List<String> changedPolicyFields(
      VerificationPolicy approved, VerificationPolicy current) {
    List<String> result = new ArrayList<>();
    if (!approved.toppleCatVersion().equals(current.toppleCatVersion()))
      result.add("toppleCatVersion");
    if (approved.hiddenTestsEnabled() != current.hiddenTestsEnabled())
      result.add("hiddenTestsEnabled");
    if (approved.expectedConsumptionEnabled() != current.expectedConsumptionEnabled())
      result.add("expectedConsumptionEnabled");
    if (approved.propertyBasedTestingEnabled() != current.propertyBasedTestingEnabled())
      result.add("propertyBasedTestingEnabled");
    if (approved.mutationEnabled() != current.mutationEnabled()) result.add("mutationEnabled");
    if (approved.mutationThreshold() != current.mutationThreshold())
      result.add("mutationThreshold");
    if (approved.mutationProducerKind() != current.mutationProducerKind())
      result.add("mutationProducerKind");
    if (!java.util.Objects.equals(
        approved.mutationProducerTaskPath(), current.mutationProducerTaskPath())) {
      result.add("mutationProducerTaskPath");
    }
    result.sort(Comparator.naturalOrder());
    return result;
  }

  /**
   * Removes reviewer-only rows without changing the existing public Check task's authoring
   * behaviour.
   */
  private static ContractDefinition publicProjection(ContractDefinition definition) {
    List<AcceptanceContract> contracts =
        definition.acceptanceConditions().stream()
            .map(
                contract ->
                    new AcceptanceContract(
                        contract.acId(),
                        contract.title(),
                        contract.scenario(),
                        contract.cases().stream()
                            .filter(testCase -> testCase.visibility() == CaseVisibility.PUBLIC)
                            .map(ContractApprovalFactory::copyCase)
                            .toList(),
                        contract.properties().stream().toList()))
            .toList();
    return ContractDefinition.withComputedDigest(contracts);
  }

  private static CaseDefinition copyCase(CaseDefinition testCase) {
    return new CaseDefinition(
        testCase.caseId(),
        testCase.acId(),
        testCase.visibility(),
        testCase.inputs(),
        testCase.expected());
  }

  private static Path normalizedRoot(Path root) {
    if (root == null || !Files.isDirectory(root) || Files.isSymbolicLink(root)) {
      throw new ToppleCatException(
          "ToppleCat contract approval requires a non-symbolic project root.");
    }
    return root.toAbsolutePath().normalize();
  }

  private static void addGradleLogic(Path root, Set<Path> files) {
    try (Stream<Path> paths = Files.walk(root)) {
      for (Path path : paths.toList()) {
        if (excluded(root, path)) {
          continue;
        }
        if (Files.isSymbolicLink(path)) {
          throw new ToppleCatException(
              "ToppleCat contract approval rejects symbolic links: " + relative(root, path));
        }
        if (Files.isRegularFile(path) && isGradleLogic(root.relativize(path))) {
          files.add(checkedFile(root, path));
        }
      }
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot inventory project-local Gradle logic: " + exception.getMessage(), exception);
    }
  }

  private static void addTree(Path root, Path candidate, Set<Path> files) {
    if (candidate == null || !Files.exists(candidate)) {
      return;
    }
    Path start = candidate.toAbsolutePath().normalize();
    requireInside(root, start);
    if (Files.isSymbolicLink(start)) {
      throw new ToppleCatException(
          "ToppleCat contract approval rejects symbolic links: " + relative(root, start));
    }
    try (Stream<Path> paths = Files.walk(start)) {
      for (Path path : paths.toList()) {
        if (Files.isSymbolicLink(path)) {
          throw new ToppleCatException(
              "ToppleCat contract approval rejects symbolic links: " + relative(root, path));
        }
        if (Files.isRegularFile(path)) {
          files.add(checkedFile(root, path));
        }
      }
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot inventory public contract files: " + exception.getMessage(), exception);
    }
  }

  private static boolean excluded(Path root, Path path) {
    Path relative = root.relativize(path.toAbsolutePath().normalize());
    if (relative.getNameCount() == 0) {
      return false;
    }
    String first = relative.getName(0).toString();
    return EXCLUDED_TOP_LEVEL.contains(first) || relative.startsWith(Path.of("src", "hiddenTest"));
  }

  private static boolean isGradleLogic(Path relative) {
    String normalized = relative.toString().replace('\\', '/');
    String filename = relative.getFileName().toString();
    return filename.equals("settings.gradle")
        || filename.equals("settings.gradle.kts")
        || filename.equals("build.gradle")
        || filename.equals("build.gradle.kts")
        || filename.equals("gradle.properties")
        || normalized.startsWith("gradle/")
        || normalized.startsWith("buildSrc/");
  }

  private static PublicContractEntry entry(Path root, Path file) {
    try {
      return new PublicContractEntry(
          relative(root, file), Hashing.sha256(Files.readAllBytes(file)));
    } catch (IOException exception) {
      throw new ToppleCatException(
          "Cannot hash public contract file "
              + relative(root, file)
              + ": "
              + exception.getMessage(),
          exception);
    }
  }

  private static Path checkedFile(Path root, Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    requireInside(root, normalized);
    return normalized;
  }

  private static void requireInside(Path root, Path path) {
    if (!path.startsWith(root)) {
      throw new ToppleCatException(
          "ToppleCat contract approval found a file outside the project root.");
    }
  }

  private static String relative(Path root, Path path) {
    return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
  }
}
