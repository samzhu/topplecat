package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.ContractIntegrityResultJson;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ToppleEvidence;
import io.github.samzhu.topplecat.core.ToppleEvidenceJson;
import io.github.samzhu.topplecat.pitest.PitMutationAssessment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToppleCatPluginFunctionalTest {
  @TempDir Path project;

  @Test
  void ordinaryTestRunsPublicAcceptanceButCreatesNoFormalEvidenceOrPropertyRun() throws Exception {
    writeProject("");
    writeAcceptance("100", true);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    var result = runner("test").build();

    assertEquals(TaskOutcome.SUCCESS, result.task(":test").getOutcome());
    assertFalse(Files.exists(project.resolve("build/topplecat/evidence.json")));
    assertFalse(Files.exists(project.resolve("build/topplecat/runs")));
  }

  @Test
  void ordinaryTestLoadsCompilerOwnedDescriptorsForNewScenarioAuthoring() throws Exception {
    writeProject("");
    writeScenarioAcceptance();
    writePublicCase("coupon-public", "AC-COUPON", 100);

    var result = runner("test").build();

    assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatCompileContracts").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, result.task(":test").getOutcome());
    assertTrue(
        Files.isRegularFile(
            project.resolve("build/topplecat/compiler/META-INF/topplecat/contracts/index")));
    assertFalse(Files.exists(project.resolve("build/topplecat/evidence.json")));
    assertFalse(Files.exists(project.resolve("build/topplecat/runs")));
  }

  @Test
  void formalVerificationSealsAndExecutesNewScenarioAuthoring() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeScenarioAcceptance();
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").build();

    assertEquals(EvidenceVerdict.PASS, gate("JUNIT"));
    assertEquals(EvidenceVerdict.PASS, gate("EXPECTED_CONSUMPTION"));
    assertEquals(EvidenceVerdict.PASS, evidence().verdict());
  }

  @Test
  void verificationUsesOnlyFormalAcceptanceTasksAndPublishesTheThreeReportAudiences()
      throws Exception {
    writeProject(
        """
        toppleCat {
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCase("coupon-hidden", "AC-COUPON", 100);

    assertEquals(
        TaskOutcome.SUCCESS, runner("toppleCatSeal").build().task(":toppleCatSeal").getOutcome());
    var verify = runner("toppleCatVerify").build();

    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatVerificationTest").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatHiddenTest").getOutcome());
    assertEquals(EvidenceVerdict.PASS, gate("JUNIT"));
    assertEquals(EvidenceVerdict.PASS, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.NOT_APPLICABLE, gate("PROPERTY"));
    assertTrue(Files.isRegularFile(project.resolve("build/topplecat/reports/review/index.html")));
    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/reports/verification/index.html")));
    assertTrue(Files.isRegularFile(project.resolve("build/topplecat/reports/public/index.html")));
    assertFalse(Files.exists(project.resolve("build/topplecat/reports/spec")));
    assertFalse(Files.exists(project.resolve("src/hiddenTest")));
  }

  @Test
  void rejectsTopplePropertyUnderHiddenReviewerSource() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenProperty("AC-COUPON");

    var failure = runner("toppleCatCheck").buildAndFail();

    assertTrue(failure.getOutput().contains("@ToppleProperty is supported only under src/test"));
  }

  @Test
  void propertyDoesNotSupplyHiddenTypedRowCoverage() throws Exception {
    writeProject("toppleCat { mutationTesting { enabled.set(false) } }");
    writeAcceptance("100", true);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCustodyMarker();

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.INCOMPLETE, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.PASS, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.INCOMPLETE, evidence().verdict());
  }

  @Test
  void publicPropertyRunsWithoutAHiddenCaseSource() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptanceWithPropertyThatRejectsHiddenCaseSource();
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCase("coupon-hidden", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").build();

    assertEquals(EvidenceVerdict.PASS, gate("PROPERTY"));
  }

  @Test
  void propertyFailureDoesNotChangeTheHiddenTypedRowGate() throws Exception {
    writeProject(mutationFixtureConfiguration(true));
    writeProductionClass();
    writeAcceptanceWithFailingProperty();
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCase("coupon-hidden", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.PASS, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.FAIL, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.PASS, gate("MUTATION"));
  }

  @Test
  void hiddenTypedRowFailureDoesNotChangeThePropertyGate() throws Exception {
    writeProject(mutationFixtureConfiguration(true));
    writeProductionClass();
    writeAcceptance("100", true);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCaseWithSecretInput("coupon-hidden", "AC-COUPON", 99);

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.FAIL, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.PASS, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.PASS, gate("MUTATION"));
    assertCurrentRunCompleted("PROPERTY_PUBLIC", "MUTATION");
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatPropertyTest").getOutcome());
    assertFalse(
        Files.readString(project.resolve("build/topplecat/agent-feedback.json"))
            .contains("coupon-hidden"));
    assertFalse(
        Files.readString(project.resolve("build/topplecat/agent-feedback.json"))
            .contains("reviewer-only-secret"));
  }

  @Test
  void expectedValueReadsWithoutAnAssertionFailTheirOwnGate() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptanceThatOnlyReadsExpectedValues();
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.FAIL, gate("JUNIT"));
    assertEquals(EvidenceVerdict.FAIL, gate("EXPECTED_CONSUMPTION"));
  }

  @Test
  void directPublicVerificationTaskStillFailsAtTheDiagnosticTask() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptance("99", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    var failure = runner("toppleCatVerificationTest").buildAndFail();

    assertTrue(failure.getOutput().contains("There were failing tests"));
    assertEquals(EvidenceVerdict.FAIL, gate("JUNIT"));
  }

  @Test
  void verifyWithoutAnExistingSealIsIncompleteAndDoesNotCreateApproval() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    var failure = runner("toppleCatVerify").buildAndFail();

    assertTrue(failure.getOutput().contains("Run toppleCatSeal before Verify"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("CONTRACT_INTEGRITY"));
    assertEquals(EvidenceVerdict.INCOMPLETE, evidence().verdict());
    assertFalse(Files.exists(project.resolve("src/hiddenTest")));
  }

  @Test
  void directReportDoesNotReuseACompletedVerificationRunsEvidence() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").build();
    runner("toppleCatReport", "--continue").buildAndFail();

    assertEquals(EvidenceVerdict.INCOMPLETE, gate("JUNIT"));
    assertEquals(EvidenceVerdict.INCOMPLETE, evidence().verdict());
  }

  @Test
  void mutationGateAttributesThePublicCanonicalMethodToItsAcceptanceCondition() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting {
                enabled.set(true)
                threshold.set(100)
                producerTask.set("writePitFixture")
                reportFile.set(layout.buildDirectory.file("reports/pitest/mutations.xml"))
            }
        }
        tasks.register("writePitFixture") {
            doLast {
                def report = layout.buildDirectory.file("reports/pitest/mutations.xml").get().asFile
                report.parentFile.mkdirs()
                report.text = '''
                <mutations>
                  <mutation detected="true" status="KILLED">
                    <mutatedClass>example.CouponService</mutatedClass>
                    <coveringTests>example.CouponAcceptanceTest.[engine:junit-jupiter]/[class:example.CouponAcceptanceTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,io.github.samzhu.topplecat.junit.ToppleScenario,example.CouponAcceptanceTest$CouponStage)]/[test-template-invocation:#1]</coveringTests>
                  </mutation>
                </mutations>
                '''
            }
        }
        """);
    writeProductionClass();
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").build();

    assertEquals(EvidenceVerdict.PASS, gate("MUTATION"));
    String mutation = Files.readString(project.resolve("build/topplecat/mutation-results.json"));
    assertTrue(mutation.contains("AC-COUPON"));
    assertTrue(mutation.contains("example.CouponAcceptanceTest"));
  }

  @Test
  void publicFailureStillRunsEveryEnabledSafeguardBeforeTheAggregateFailure() throws Exception {
    writeProject(mutationFixtureConfiguration(true));
    writeProductionClass();
    writeAcceptance("99", true);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCase("coupon-hidden", "AC-COUPON", 99);

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/evidence.json")), verify.getOutput());
    assertEquals(EvidenceVerdict.FAIL, gate("JUNIT"));
    assertEquals(EvidenceVerdict.PASS, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.PASS, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.PASS, gate("MUTATION"));
    assertEquals(EvidenceVerdict.FAIL, evidence().verdict());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatPropertyTest").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatMutationGate").getOutcome());
    assertCurrentRunCompleted("JUNIT", "REVIEWER_JUNIT", "PROPERTY_PUBLIC", "MUTATION");
  }

  @Test
  void survivingMutantsStillProduceReportFeedbackAndRehide() throws Exception {
    writeProject(mutationFixtureConfiguration(false));
    writeProductionClass();
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCustodyMarker();

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatMutationGate").getOutcome());
    assertEquals(EvidenceVerdict.FAIL, gate("MUTATION"));
    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/reports/verification/index.html")));
    assertTrue(Files.isRegularFile(project.resolve("build/topplecat/agent-feedback.json")));
    assertFalse(Files.exists(project.resolve("src/hiddenTest")));
  }

  @Test
  void missingMutationReportIsIncompleteAndCannotReuseAStaleProducerArtifact() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting {
                producerTask.set("writeNothing")
                reportFile.set(layout.buildDirectory.file("reports/pitest/mutations.xml"))
            }
        }
        tasks.register("writeNothing")
        """);
    writeProductionClass();
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    Path stale = project.resolve("build/reports/pitest/mutations.xml");
    Files.createDirectories(stale.getParent());
    Files.writeString(
        stale,
        """
        <mutations>
          <mutation detected="true" status="KILLED">
            <mutatedClass>example.CouponService</mutatedClass>
            <coveringTests>example.CouponAcceptanceTest</coveringTests>
          </mutation>
        </mutations>
        """);

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/evidence.json")), verify.getOutput());
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("MUTATION"));
    assertFalse(Files.exists(stale));
    assertCurrentRunCompleted("MUTATION");
  }

  @Test
  void interruptedMutationProducerRunsAfterEarlierSafeguardsAndStillReportsAndRehides()
      throws Exception {
    writeProject(
        """
        toppleCat {
            mutationTesting {
                producerTask.set("interruptPitFixture")
                reportFile.set(layout.buildDirectory.file("reports/pitest/mutations.xml"))
            }
        }
        tasks.register("interruptPitFixture") {
            doLast {
                def gates = layout.buildDirectory.file("topplecat/runs/current/gates").get().asFile
                if (!new File(gates, "JUNIT.completed").isFile() ||
                    !new File(gates, "REVIEWER_JUNIT.completed").isFile() ||
                    !new File(gates, "PROPERTY_PUBLIC.completed").isFile()) {
                    throw new GradleException("Mutation producer ran before its prerequisite safeguards")
                }
                throw new GradleException("simulated mutation producer interruption")
            }
        }
        tasks.register("assertMutationProducerOrdering") {
            doLast {
                def producer = tasks.named("interruptPitFixture").get()
                def prerequisiteNames = producer.mustRunAfter.getDependencies(producer)*.name as Set
                def required = [
                    "toppleCatContractIntegrity",
                    "toppleCatVerificationTest",
                    "toppleCatHiddenTest",
                    "toppleCatPropertyTest"
                ] as Set
                if (!prerequisiteNames.containsAll(required)) {
                    throw new GradleException("Mutation producer does not declare every prerequisite safeguard")
                }
            }
        }
        """);
    writeProductionClass();
    writeAcceptance("100", true);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCase("coupon-hidden", "AC-COUPON", 100);
    writeHiddenCustodyMarker();

    runner("assertMutationProducerOrdering").build();
    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertEquals(TaskOutcome.FAILED, verify.task(":interruptPitFixture").getOutcome());
    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/evidence.json")), verify.getOutput());
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("MUTATION"));
    assertTrue(Files.isRegularFile(project.resolve("build/topplecat/agent-feedback.json")));
    assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    assertCurrentRunCompleted("JUNIT", "REVIEWER_JUNIT", "PROPERTY_PUBLIC");
  }

  @Test
  void formalVerifyRerunsADeclaredOutputMutationProducerForEachRun() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting {
                producerTask.set("writeDeclaredPitFixture")
                reportFile.set(layout.buildDirectory.file("reports/pitest/mutations.xml"))
            }
        }
        tasks.register("writeDeclaredPitFixture") {
            def report = layout.buildDirectory.file("reports/pitest/mutations.xml")
            outputs.file(report)
            doLast {
                def reportFile = report.get().asFile
                reportFile.parentFile.mkdirs()
                reportFile.text = '''
                <mutations>
                  <mutation detected="true" status="KILLED">
                    <mutatedClass>example.CouponService</mutatedClass>
                    <coveringTests>example.CouponAcceptanceTest.[engine:junit-jupiter]/[class:example.CouponAcceptanceTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,io.github.samzhu.topplecat.junit.ToppleScenario,example.CouponAcceptanceTest$CouponStage)]/[test-template-invocation:#1]</coveringTests>
                  </mutation>
                </mutations>
                '''
                def invocations = layout.buildDirectory.file("producer-invocations.txt").get().asFile
                invocations << "run\\n"
            }
        }
        """);
    writeProductionClass();
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    var first = runner("toppleCatVerify").build();
    var second = runner("toppleCatVerify").build();

    assertEquals(TaskOutcome.SUCCESS, first.task(":writeDeclaredPitFixture").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, second.task(":writeDeclaredPitFixture").getOutcome());
    assertEquals(
        2,
        Files.readAllLines(project.resolve("build/producer-invocations.txt")).stream()
            .filter(line -> line.equals("run"))
            .count());
    assertEquals(EvidenceVerdict.PASS, gate("MUTATION"));
    assertCurrentRunCompleted("MUTATION");
  }

  @Test
  void integrityRechecksTheCurrentDefinitionBeforeComparingTheSeal() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    writeIndependentPublicProperty();

    var integrity = runner("toppleCatContractIntegrity").build();

    assertEquals(TaskOutcome.SUCCESS, integrity.task(":toppleCatCheck").getOutcome());
    assertEquals(
        EvidenceVerdict.FAIL,
        ContractIntegrityResultJson.read(
                Files.readString(
                    project.resolve("build/topplecat/runs/current/contract-integrity.json")))
            .verdict());
  }

  @Test
  void skippedMutationProducerCannotReuseItsReportFromBeforeTheRun() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting {
                producerTask.set("skipPitFixture")
                reportFile.set(layout.buildDirectory.file("reports/pitest/mutations.xml"))
            }
        }
        tasks.register("skipPitFixture") {
            onlyIf { false }
        }
        """);
    writeProductionClass();
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    Path stale = project.resolve("build/reports/pitest/mutations.xml");
    Files.createDirectories(stale.getParent());
    Files.writeString(stale, killedMutationFixture());

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/evidence.json")), verify.getOutput());
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("MUTATION"));
    assertFalse(Files.exists(stale));
    assertCurrentRunCompleted("MUTATION");
  }

  @Test
  void missingNarrativeSidecarMakesTheJUnitEvidenceIncomplete() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        tasks.named("toppleCatReport") {
            doFirst {
                layout.buildDirectory.file("topplecat/runs/current/narrative-executions.jsonl").get().asFile.delete()
            }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/evidence.json")), verify.getOutput());
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("JUNIT"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("EXPECTED_CONSUMPTION"));
    assertEquals(EvidenceVerdict.INCOMPLETE, evidence().verdict());
  }

  @Test
  void abandonedActiveWorkspaceCannotSupplyMutationEvidenceToTheNextVerify() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting {
                producerTask.set("writeNothing")
                reportFile.set(layout.buildDirectory.file("reports/pitest/mutations.xml"))
            }
        }
        tasks.register("writeNothing")
        """);
    writeProductionClass();
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    Path abandoned = project.resolve("build/topplecat/runs/current");
    Files.createDirectories(abandoned.resolve("gates"));
    Files.writeString(abandoned.resolve(".active"), "active\n");
    Files.writeString(abandoned.resolve("run-id"), "abandoned-run\n");
    Files.writeString(abandoned.resolve("gates/MUTATION.completed"), "completed\n");
    Files.writeString(
        abandoned.resolve("mutation-results.json"),
        MutationGateResults.write(
            new MutationGateResults(
                MutationGateResults.SCHEMA_VERSION,
                List.of(
                    new PitMutationAssessment(
                        "AC-COUPON",
                        List.of("example.CouponAcceptanceTest"),
                        100,
                        1,
                        1,
                        100,
                        EvidenceVerdict.PASS)))));

    var verify = runner("toppleCatVerify").buildAndFail();

    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/evidence.json")), verify.getOutput());
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("MUTATION"));
    assertCurrentRunCompleted("MUTATION");
  }

  @Test
  void specSelectionScopesFormalAcceptanceAndTheHiddenExpansionOptionHasNoLegacyAlias()
      throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    Path spec = project.resolve("specs/coupon.md");
    Files.createDirectories(spec.getParent());
    Files.writeString(spec, "# Coupon\n\nAC-COUPON\n");

    runner("toppleCatSeal", "--spec", "specs/coupon.md").build();
    var verify = runner("toppleCatVerify", "--spec", "specs/coupon.md").build();

    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatVerificationTest").getOutcome());
    assertTrue(
        Files.readString(project.resolve("build/topplecat/reports/public/data.json"))
            .contains("AC-COUPON"));
    String tasks = runner("tasks", "--all").build().getOutput();
    assertTrue(tasks.contains("toppleCatSeal"));
    assertTrue(tasks.contains("toppleCatReseal"));
    assertFalse(tasks.contains("toppleCatHide"));
    assertFalse(tasks.contains("toppleCatUpdateEscrow"));
    assertFalse(tasks.contains("hiddenPropertyTest"));
    assertTrue(
        runner("toppleCatVerify", "--all-hidden")
            .buildAndFail()
            .getOutput()
            .contains("Unknown command-line option"));
  }

  @Test
  void sealedSourceClosureIgnoresOrdinaryTestsButRejectsAcceptanceChanges() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    Path ordinary = project.resolve("src/test/java/example/OrdinaryTest.java");
    Files.createDirectories(ordinary.getParent());
    Files.writeString(
        ordinary, "package example; class OrdinaryTest { int value() { return 1; } }\n");

    runner("toppleCatSeal").build();
    Files.writeString(
        ordinary, "package example; class OrdinaryTest { int value() { return 2; } }\n");
    runner("toppleCatVerify").build();
    assertEquals(EvidenceVerdict.PASS, gate("CONTRACT_INTEGRITY"));

    writeAcceptance("99", false);
    runner("toppleCatVerify").buildAndFail();
    assertEquals(EvidenceVerdict.FAIL, gate("CONTRACT_INTEGRITY"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("JUNIT"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("EXPECTED_CONSUMPTION"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("MUTATION"));
    assertFalse(Files.exists(project.resolve("src/hiddenTest")));
  }

  @Test
  void sealedSourceClosureRejectsTamperingOfASamePackageLowerCaseHelper() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptanceUsingLowerCaseHelper();
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    Files.writeString(
        project.resolve("src/test/java/example/couponmath.java"),
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        final class couponmath {
            static int actual(ToppleCase testCase) {
                return testCase.expected("discount", Integer.class);
            }
        }
        """);

    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.FAIL, gate("CONTRACT_INTEGRITY"));
  }

  @Test
  void selectedSpecDoesNotRequireAnUnselectedPublicProperty() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeTwoAcceptanceConditionsWithPropertyOnlyOnB();
    writePublicCasesForAAndB();
    Path spec = project.resolve("specs/a.md");
    Files.createDirectories(spec.getParent());
    Files.writeString(spec, "# A\n\nAC-A\n");

    runner("toppleCatSeal", "--spec", "specs/a.md").build();
    runner("toppleCatVerify", "--spec", "specs/a.md").build();

    assertEquals(EvidenceVerdict.PASS, gate("JUNIT"));
    assertEquals(EvidenceVerdict.DISABLED, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.NOT_APPLICABLE, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.PASS, evidence().verdict());
    assertFalse(
        Files.readString(project.resolve("build/topplecat/reports/public/data.json"))
            .contains("onlyBHasAProperty"));
  }

  private static String mutationFixtureConfiguration(boolean detected) {
    return """
    toppleCat {
        mutationTesting {
            enabled.set(true)
            threshold.set(100)
            producerTask.set("writePitFixture")
            reportFile.set(layout.buildDirectory.file("reports/pitest/mutations.xml"))
        }
    }
    tasks.register("writePitFixture") {
        doLast {
            def report = layout.buildDirectory.file("reports/pitest/mutations.xml").get().asFile
            report.parentFile.mkdirs()
            report.text = '''
            <mutations>
              <mutation detected="%s" status="%s">
                <mutatedClass>example.CouponService</mutatedClass>
                <coveringTests>example.CouponAcceptanceTest.[engine:junit-jupiter]/[class:example.CouponAcceptanceTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,io.github.samzhu.topplecat.junit.ToppleScenario,example.CouponAcceptanceTest$CouponStage)]/[test-template-invocation:#1]</coveringTests>
              </mutation>
            </mutations>
            '''
        }
    }
    """
        .formatted(detected, detected ? "KILLED" : "SURVIVED");
  }

  private static String killedMutationFixture() {
    return """
    <mutations>
      <mutation detected="true" status="KILLED">
        <mutatedClass>example.CouponService</mutatedClass>
        <coveringTests>example.CouponAcceptanceTest.[engine:junit-jupiter]/[class:example.CouponAcceptanceTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase,io.github.samzhu.topplecat.junit.ToppleScenario,example.CouponAcceptanceTest$CouponStage)]/[test-template-invocation:#1]</coveringTests>
      </mutation>
    </mutations>
    """;
  }

  private void assertCurrentRunCompleted(String... gates) throws Exception {
    Path archive = project.resolve("build/topplecat/runs").resolve(evidence().runId());
    for (String gate : gates) {
      assertTrue(Files.isRegularFile(archive.resolve("gates").resolve(gate + ".completed")));
    }
  }

  private void writeProject(String configuration) throws Exception {
    Files.writeString(
        project.resolve("settings.gradle"), "rootProject.name = 'verification-consumer'\n");
    Path junit = moduleJar("topplecat-junit");
    Path core = moduleJar("topplecat-core");
    Path byteBuddy = libraryJar(ByteBuddy.class);
    Files.writeString(
        project.resolve("build.gradle"),
        """
        plugins {
            id 'java'
            id 'io.github.samzhu.topplecat'
        }
        repositories { mavenCentral() }
        dependencies {
            testImplementation files('%s', '%s', '%s')
            testImplementation 'org.junit.jupiter:junit-jupiter:6.1.1'
            testImplementation 'tools.jackson.core:jackson-databind:3.2.0'
            testImplementation 'tools.jackson.dataformat:jackson-dataformat-yaml:3.2.0'
            testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.1.1'
        }
        %s
        """
            .formatted(junit, core, byteBuddy, configuration));
  }

  private void writeScenarioAcceptance() throws Exception {
    Path source = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        class CouponAcceptanceTest {
            @ToppleAcceptanceTest("AC-COUPON")
            void appliesCoupon(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.given(coupon).reads_discount(testCase.expected("discount", Integer.class));
                scenario.then(coupon).matches(testCase);
            }
            static class CouponStage extends ToppleStage {
                private int actual;
                void reads_discount(int value) { actual = value; }
                void matches(ToppleCase testCase) { testCase.verify("discount", actual); }
            }
        }
        """);
  }

  private void writeAcceptance(String actualDiscount, boolean property) throws Exception {
    Path source = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    String propertyMethod =
        property
            ? """
            @ToppleProperty("AC-COUPON")
            void discountIsBounded(PropertyTrials trials) {
                trials.forAll(Generators.integers(0, 5)).check(value -> {});
            }
            """
            : "";
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        import io.github.samzhu.topplecat.junit.property.Generators;
        import io.github.samzhu.topplecat.junit.property.PropertyTrials;
        import io.github.samzhu.topplecat.junit.property.ToppleProperty;
        class CouponAcceptanceTest {
            @ToppleAcceptanceTest("AC-COUPON")
            void appliesCoupon(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.then(coupon).matches(testCase);
            }
            %s
            static class CouponStage extends ToppleStage {
                void matches(ToppleCase testCase) {
                    testCase.verify("discount", %s);
                }
            }
        }
        """
            .formatted(propertyMethod, actualDiscount));
  }

  private void writeIndependentPublicProperty() throws Exception {
    Path source = project.resolve("src/test/java/example/AdditionalCouponProperty.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.property.Generators;
        import io.github.samzhu.topplecat.junit.property.PropertyTrials;
        import io.github.samzhu.topplecat.junit.property.ToppleProperty;
        class AdditionalCouponProperty {
            @ToppleProperty("AC-COUPON")
            void separatelyAddedRule(PropertyTrials trials) {
                trials.forAll(Generators.integers(0, 5)).check(value -> {});
            }
        }
        """);
  }

  private void writeAcceptanceUsingLowerCaseHelper() throws Exception {
    Path source = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        class CouponAcceptanceTest {
            @ToppleAcceptanceTest("AC-COUPON")
            void appliesCoupon(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.then(coupon).matches(testCase);
            }
            static class CouponStage extends ToppleStage {
                void matches(ToppleCase testCase) {
                    testCase.verify("discount", couponmath.actual(testCase));
                }
            }
        }
        """);
    Files.writeString(
        project.resolve("src/test/java/example/couponmath.java"),
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        final class couponmath {
            static int actual(ToppleCase testCase) { return 100; }
        }
        """);
  }

  private void writeAcceptanceWithPropertyThatRejectsHiddenCaseSource() throws Exception {
    Path source = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        import io.github.samzhu.topplecat.junit.property.Generators;
        import io.github.samzhu.topplecat.junit.property.PropertyTrials;
        import io.github.samzhu.topplecat.junit.property.ToppleProperty;
        class CouponAcceptanceTest {
            @ToppleAcceptanceTest("AC-COUPON")
            void appliesCoupon(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.then(coupon).matches(testCase);
            }
            @ToppleProperty("AC-COUPON")
            void doesNotReceiveReviewerCaseSources(PropertyTrials trials) {
                trials.forAll(Generators.integers(0, 5)).check(value -> {
                    if (!System.getProperty("topplecat.hiddenCaseSources", "").isBlank()) {
                        throw new AssertionError("Property received reviewer case sources");
                    }
                });
            }
            static class CouponStage extends ToppleStage {
                void matches(ToppleCase testCase) {
                    testCase.verify("discount", 100);
                }
            }
        }
        """);
  }

  private void writeAcceptanceWithFailingProperty() throws Exception {
    Path source = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        import io.github.samzhu.topplecat.junit.property.Generators;
        import io.github.samzhu.topplecat.junit.property.PropertyTrials;
        import io.github.samzhu.topplecat.junit.property.ToppleProperty;
        class CouponAcceptanceTest {
            @ToppleAcceptanceTest("AC-COUPON")
            void appliesCoupon(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.then(coupon).matches(testCase);
            }
            @ToppleProperty("AC-COUPON")
            void deliberatelyBrokenPublicRule(PropertyTrials trials) {
                trials.forAll(Generators.integers(0, 5)).check(value -> {
                    throw new AssertionError("public Property failure");
                });
            }
            static class CouponStage extends ToppleStage {
                void matches(ToppleCase testCase) {
                    testCase.verify("discount", 100);
                }
            }
        }
        """);
  }

  private void writeAcceptanceThatOnlyReadsExpectedValues() throws Exception {
    Path source = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        class CouponAcceptanceTest {
            @ToppleAcceptanceTest("AC-COUPON")
            void appliesCoupon(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.then(coupon).readsExpectedValue(testCase);
            }
            static class CouponStage extends ToppleStage {
                void readsExpectedValue(ToppleCase testCase) {
                    testCase.expected("discount", Integer.class);
                }
            }
        }
        """);
  }

  private void writeProductionClass() throws Exception {
    Path source = project.resolve("src/main/java/example/CouponService.java");
    Files.createDirectories(source.getParent());
    Files.writeString(source, "package example; public final class CouponService {}\n");
  }

  private void writeTwoAcceptanceConditionsWithPropertyOnlyOnB() throws Exception {
    Path source = project.resolve("src/test/java/example/ScopedAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        import io.github.samzhu.topplecat.junit.property.Generators;
        import io.github.samzhu.topplecat.junit.property.PropertyTrials;
        import io.github.samzhu.topplecat.junit.property.ToppleProperty;
        class ScopedAcceptanceTest {
            @ToppleAcceptanceTest("AC-A")
            void appliesA(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.then(coupon).matches(testCase);
            }
            @ToppleAcceptanceTest("AC-B")
            void appliesB(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.then(coupon).matches(testCase);
            }
            @ToppleProperty("AC-B")
            void onlyBHasAProperty(PropertyTrials trials) {
                trials.forAll(Generators.integers(0, 5)).check(value -> {
                    throw new AssertionError("AC-B Property must not run for AC-A scope");
                });
            }
            static class CouponStage extends ToppleStage {
                void matches(ToppleCase testCase) {
                    testCase.verify("discount", 100);
                }
            }
        }
        """);
  }

  private void writePublicCase(String id, String acId, int discount) throws Exception {
    Path cases = project.resolve("src/test/resources/topplecat/cases/coupon.json");
    Files.createDirectories(cases.getParent());
    Files.writeString(
        cases,
        "[{\"caseId\":\"%s\",\"acId\":\"%s\",\"inputs\":{},\"expected\":{\"discount\":%d}}]"
            .formatted(id, acId, discount));
  }

  private void writePublicCasesForAAndB() throws Exception {
    Path cases = project.resolve("src/test/resources/topplecat/cases/coupon.json");
    Files.createDirectories(cases.getParent());
    Files.writeString(
        cases,
        """
        [
          {"caseId":"coupon-a","acId":"AC-A","inputs":{},"expected":{"discount":100}},
          {"caseId":"coupon-b","acId":"AC-B","inputs":{},"expected":{"discount":100}}
        ]
        """);
  }

  private void writeHiddenCase(String id, String acId, int discount) throws Exception {
    Path cases = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon.json");
    Files.createDirectories(cases.getParent());
    Files.writeString(
        cases,
        "[{\"caseId\":\"%s\",\"acId\":\"%s\",\"inputs\":{},\"expected\":{\"discount\":%d}}]"
            .formatted(id, acId, discount));
  }

  private void writeHiddenCustodyMarker() throws Exception {
    Path marker = project.resolve("src/hiddenTest/java/example/ReviewerSupport.java");
    Files.createDirectories(marker.getParent());
    Files.writeString(marker, "package example;\nfinal class ReviewerSupport {}\n");
  }

  private void writeHiddenCaseWithSecretInput(String id, String acId, int discount)
      throws Exception {
    Path cases = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon.json");
    Files.createDirectories(cases.getParent());
    Files.writeString(
        cases,
        """
        [{"caseId":"%s","acId":"%s","inputs":{"reviewerSecret":"reviewer-only-secret"},"expected":{"discount":%d}}]
        """
            .formatted(id, acId, discount));
  }

  private void writeHiddenProperty(String acId) throws Exception {
    Path source = project.resolve("src/hiddenTest/java/example/HiddenCouponProperty.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.property.Generators;
        import io.github.samzhu.topplecat.junit.property.PropertyTrials;
        class HiddenCouponProperty {
            @io.github.samzhu.topplecat.junit.property.ToppleProperty("%s")
            void remainsBounded(PropertyTrials trials) {
                trials.forAll(Generators.integers(0, 5)).check(value -> {});
            }
        }
        """
            .formatted(acId));
  }

  private GradleRunner runner(String... arguments) {
    return GradleRunner.create()
        .withProjectDir(project.toFile())
        .withPluginClasspath()
        .withArguments(List.of(arguments));
  }

  private EvidenceVerdict gate(String name) throws Exception {
    return evidence().gates().stream()
        .filter(gate -> gate.name().equals(name))
        .findFirst()
        .orElseThrow()
        .verdict();
  }

  private ToppleEvidence evidence() throws Exception {
    return ToppleEvidenceJson.read(
        Files.readString(project.resolve("build/topplecat/evidence.json")));
  }

  private static Path moduleJar(String module) {
    String version = System.getProperty("topplecat.project.version");
    if (version == null || version.isBlank()) {
      throw new IllegalStateException("Missing topplecat.project.version test system property.");
    }
    Path root = Path.of("").toAbsolutePath();
    while (root != null && !Files.isRegularFile(root.resolve("settings.gradle.kts")))
      root = root.getParent();
    if (root == null)
      throw new IllegalStateException("Cannot locate the ToppleCat repository root.");
    return root.resolve(module).resolve("build/libs").resolve(module + "-" + version + ".jar");
  }

  private static Path libraryJar(Class<?> type) {
    try {
      return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot locate test library " + type.getName(), exception);
    }
  }
}
