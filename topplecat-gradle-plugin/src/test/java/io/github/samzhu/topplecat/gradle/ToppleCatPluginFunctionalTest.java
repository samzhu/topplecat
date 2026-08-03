package io.github.samzhu.topplecat.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.samzhu.topplecat.core.ContractIntegrityResultJson;
import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.PropertyResults;
import io.github.samzhu.topplecat.core.PropertyResultsJson;
import io.github.samzhu.topplecat.core.ToppleEvidence;
import io.github.samzhu.topplecat.core.ToppleEvidenceJson;
import io.github.samzhu.topplecat.pitest.ToppleCatManagedMutationProfile;
import io.github.samzhu.topplecat.report.ReportJson;
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
    writeProjectWithConsumerPit(
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
  void reviewerLanguageIsInvocationScopedForReviewSealResealAndVerify() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeTraditionalChineseScenarioAcceptance();
    writePublicCase("coupon-public", "AC-COUPON", 100);

    var unsupported = runner("toppleCatVerify", "--language", "ja").buildAndFail();
    assertTrue(unsupported.getOutput().contains("Supported values: en, zh-TW"));
    assertFalse(Files.exists(project.resolve("build/topplecat/runs/current")));

    runner("toppleCatReview", "--language", "zh-TW").build();
    assertReportLanguage("review", "zh-TW");
    String reviewData =
        Files.readString(project.resolve("build/topplecat/reports/review/data.json"));
    assertFalse(reviewData.contains("zh-TW"));
    assertTrue(reviewData.contains("套用 SAVE100 折抵訂單小計"));
    assertTrue(reviewData.contains("準備可結帳的購物車"));

    runner("toppleCatReview").build();
    assertReportLanguage("review", "en");

    runner("toppleCatSeal", "--language", "zh-TW").build();
    assertReportLanguage("review", "zh-TW");
    runner("toppleCatRestore").build();
    runner("toppleCatReseal", "--language", "zh-TW").build();
    assertReportLanguage("review", "zh-TW");

    runner("toppleCatVerify", "--language", "zh-TW").build();
    assertReportLanguage("verification", "zh-TW");
    String verificationData =
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"));
    assertFalse(verificationData.contains("zh-TW"));
    assertTrue(verificationData.contains("套用 SAVE100 折抵訂單小計"));
    assertTrue(verificationData.contains("準備可結帳的購物車"));
    assertFalse(
        Files.readString(project.resolve("build/topplecat/evidence.json")).contains("zh-TW"));
    assertFalse(
        Files.readString(project.resolve("build/topplecat/agent-feedback.json")).contains("zh-TW"));
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
    Path stalePublic = project.resolve("build/topplecat/reports/public/stale.html");
    Files.createDirectories(stalePublic.getParent());
    Files.writeString(stalePublic, "retired public report");
    var verify = runner("toppleCatVerify").build();

    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatVerificationTest").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatHiddenTest").getOutcome());
    assertEquals(EvidenceVerdict.PASS, gate("JUNIT"));
    assertEquals(EvidenceVerdict.PASS, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.NOT_APPLICABLE, gate("PROPERTY"));
    assertTrue(Files.isRegularFile(project.resolve("build/topplecat/reports/review/index.html")));
    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/reports/verification/index.html")));
    assertFalse(Files.exists(project.resolve("build/topplecat/reports/public")));
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
  void wholeContractMarksOnlyTheAcWithoutAReviewerRowAsHiddenEvidenceIncomplete() throws Exception {
    writeProject(
        """
        toppleCat {
            propertyBasedTesting { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeTwoAcceptanceConditionsWithPropertyOnlyOnB();
    writePublicCasesForAAndB();
    writeHiddenCase("coupon-a-reviewer", "AC-A", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.PASS, gate("JUNIT"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.DISABLED, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.DISABLED, gate("MUTATION"));
    assertEquals(EvidenceVerdict.INCOMPLETE, evidence().verdict());

    var report =
        ReportJson.readVerification(
            Files.readString(project.resolve("build/topplecat/reports/verification/data.json")));
    var complete =
        report.acceptanceConditions().stream()
            .filter(ac -> ac.acId().equals("AC-A"))
            .findFirst()
            .orElseThrow();
    var missing =
        report.acceptanceConditions().stream()
            .filter(ac -> ac.acId().equals("AC-B"))
            .findFirst()
            .orElseThrow();
    assertEquals(
        EvidenceVerdict.PASS,
        complete.safeguards().stream()
            .filter(safeguard -> safeguard.name().equals("HIDDEN_TESTS"))
            .findFirst()
            .orElseThrow()
            .verdict());
    assertEquals(
        EvidenceVerdict.INCOMPLETE,
        missing.safeguards().stream()
            .filter(safeguard -> safeguard.name().equals("HIDDEN_TESTS"))
            .findFirst()
            .orElseThrow()
            .verdict());
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
  void propertySidecarsKeepIdentityWhenFivePropertiesUseTheSameDisplayName() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptanceWithDisplayNamedProperties(false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").build();

    assertEquals(EvidenceVerdict.PASS, gate("PROPERTY"));
    PropertyResults results =
        PropertyResultsJson.read(
            Files.readString(project.resolve("build/topplecat/property-results.json")));
    assertEquals(5, results.results().size());
    assertTrue(
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"))
            .contains("\"executedPublicProperties\" : 5"));
  }

  @Test
  void verificationReportProjectsTheAssessmentCompletedCountForIncompletePropertyEvidence()
      throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        tasks.named("toppleCatPropertyTest") {
            doLast {
                def events = layout.buildDirectory.file("topplecat/runs/current/public-property-events.jsonl").get().asFile
                events.text = events.readLines().get(0) + System.lineSeparator()
            }
        }
        """);
    writeAcceptance("100", true);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.INCOMPLETE, gate("PROPERTY"));
    assertTrue(
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"))
            .contains("\"executedPublicProperties\" : 0"));
  }

  @Test
  void propertySidecarsRetainFiveResultsWhenTwoDisplayedPropertiesFindCounterexamples()
      throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
        }
        """);
    writeAcceptanceWithDisplayNamedProperties(true);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.FAIL, gate("PROPERTY"));
    PropertyResults results =
        PropertyResultsJson.read(
            Files.readString(project.resolve("build/topplecat/property-results.json")));
    assertEquals(5, results.results().size());
    assertEquals(
        2,
        results.results().stream()
            .filter(
                result ->
                    result.state()
                        == io.github.samzhu.topplecat.core.PropertyExecutionState
                            .COMPLETED_COUNTEREXAMPLE)
            .count());
  }

  @Test
  void propertyFailureDoesNotChangeTheHiddenTypedRowGate() throws Exception {
    writeProject("toppleCat { mutationTesting { enabled.set(false) } }");
    writeAcceptanceWithFailingProperty();
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCase("coupon-hidden", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.PASS, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.FAIL, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.DISABLED, gate("MUTATION"));
  }

  @Test
  void hiddenTypedRowFailureDoesNotChangeThePropertyGate() throws Exception {
    writeProject("toppleCat { mutationTesting { enabled.set(false) } }");
    writeAcceptance("100", true);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    writeHiddenCaseWithSecretInput("coupon-hidden", "AC-COUPON", 99);

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.FAIL, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.PASS, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.DISABLED, gate("MUTATION"));
    assertCurrentRunCompleted("PROPERTY_PUBLIC");
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
  void formalVerifyUsesTheManagedPitProducerAndWritesItsFixedProfileEvidence() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        tasks.register("pitest") {
            doLast { throw new GradleException("consumer PIT task must not be used by ToppleCat") }
        }
        """);
    writeManagedMutationFixture();

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").build();

    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatManagedPit").getOutcome());
    assertNull(verify.task(":pitest"));
    assertEquals(EvidenceVerdict.PASS, gate("MUTATION"));
    String mutation = Files.readString(project.resolve("build/topplecat/mutation-results.json"));
    assertTrue(mutation.contains("topplecat-managed-v1"));
    assertTrue(mutation.contains("\"pitVersion\" : \"1.25.5\""));
    ToppleCatManagedMutationProfile.operatorIds()
        .forEach(operator -> assertTrue(mutation.contains("\"" + operator + "\"")));
    assertTrue(mutation.contains("\"mutator\""));
    assertTrue(mutation.contains("\"description\""));

    String firstRunId = evidence().runId();
    var repeated = runner("toppleCatVerify").build();
    assertEquals(TaskOutcome.SUCCESS, repeated.task(":toppleCatManagedPit").getOutcome());
    assertNotEquals(firstRunId, evidence().runId());
  }

  @Test
  void consumerPitTaskConventionsCannotRewriteTheManagedProducer() throws Exception {
    writeProjectWithConsumerPit(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        tasks.register("consumerPit", info.solidsoft.gradle.pitest.PitestTask)
        tasks.withType(info.solidsoft.gradle.pitest.PitestTask).configureEach {
            targetClasses.set(["consumer.DoesNotExist"])
            targetTests.set(["consumer.ConsumerTest"])
            mutators.set(["ALL"])
            outputFormats.set(["HTML"])
            reportDir.set(layout.buildDirectory.dir("consumer-pit"))
            fullMutationMatrix.set(false)
            timestampedReports.set(true)
            failWhenNoMutations.set(true)
            enableDefaultIncrementalAnalysis.set(true)
        }
        """);
    writeManagedMutationFixture();

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").build();

    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatManagedPit").getOutcome());
    assertNull(verify.task(":consumerPit"));
    assertFalse(Files.exists(project.resolve("build/consumer-pit")));
    assertEquals(EvidenceVerdict.PASS, gate("MUTATION"));
    String mutation = Files.readString(project.resolve("build/topplecat/mutation-results.json"));
    assertTrue(mutation.contains("\"pitVersion\" : \"1.25.5\""));
    ToppleCatManagedMutationProfile.operatorIds()
        .forEach(operator -> assertTrue(mutation.contains("\"" + operator + "\"")));
  }

  @Test
  void publicFailureStillRunsEveryEnabledSafeguardBeforeTheAggregateFailure() throws Exception {
    writeProject("toppleCat { mutationTesting { enabled.set(false) } }");
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
    assertEquals(EvidenceVerdict.DISABLED, gate("MUTATION"));
    assertEquals(EvidenceVerdict.FAIL, evidence().verdict());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatPropertyTest").getOutcome());
    assertCurrentRunCompleted("JUNIT", "REVIEWER_JUNIT", "PROPERTY_PUBLIC");
  }

  @Test
  void hiddenPropertyAndManagedMutationSafeguardsAllFinishAfterIndependentFailures()
      throws Exception {
    writeProject("");
    writeManagedMutationFixtureWithFailingProperty();
    writeHiddenCase("coupon-hidden-failure", "AC-COUPON", 99);

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.PASS, gate("CONTRACT_INTEGRITY"));
    assertEquals(EvidenceVerdict.PASS, gate("JUNIT"));
    assertEquals(EvidenceVerdict.FAIL, gate("REVIEWER_JUNIT"));
    assertEquals(EvidenceVerdict.FAIL, gate("PROPERTY"));
    assertEquals(EvidenceVerdict.FAIL, gate("MUTATION"));
    assertEquals(EvidenceVerdict.FAIL, evidence().verdict());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatHiddenTest").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatPropertyTest").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatManagedPit").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatMutationGate").getOutcome());
    assertCurrentRunCompleted("JUNIT", "REVIEWER_JUNIT", "PROPERTY_PUBLIC", "MUTATION");
    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/reports/verification/index.html")));
    assertTrue(Files.isRegularFile(project.resolve("build/topplecat/agent-feedback.json")));
    MutationGateResults mutation =
        MutationGateResults.read(
            Files.readString(project.resolve("build/topplecat/mutation-results.json")));
    assertTrue(
        mutation.mutations().stream()
            .anyMatch(
                item ->
                    item.status().equals("SURVIVED")
                        && item.mutator()
                            .equals(
                                "org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator")));
    String verificationReport =
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"));
    String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
    for (String rawMutationDetail : List.of("VoidMethodCallMutator", "SURVIVED", "CouponService")) {
      assertTrue(verificationReport.contains(rawMutationDetail));
      assertFalse(feedback.contains(rawMutationDetail));
    }
    assertFalse(Files.exists(project.resolve("src/hiddenTest")));
  }

  @Test
  void publicAcceptanceFailureLeavesMutationEvidenceIncompleteEvenWhenTheProducerRuns()
      throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        """);
    writeManagedMutationFixture();
    Path acceptance = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.writeString(
        acceptance,
        Files.readString(acceptance)
            .replace("CouponService.discountedTotal(110)", "CouponService.discountedTotal(109)"));

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertEquals(TaskOutcome.FAILED, verify.task(":toppleCatManagedPit").getOutcome());
    assertEquals(EvidenceVerdict.FAIL, gate("JUNIT"));
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("MUTATION"));
    assertEquals(EvidenceVerdict.FAIL, evidence().verdict());
    String report =
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"));
    assertTrue(
        report.contains(
            "Mutation Testing could not establish a reliable baseline because public acceptance"
                + " found a problem in this run."));
  }

  @Test
  void survivingManagedMutantsStillProduceReportFeedbackAndRehide() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        """);
    writeManagedMutationFixtureWithUnobservedVoidCall();

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatManagedPit").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, verify.task(":toppleCatMutationGate").getOutcome());
    assertEquals(EvidenceVerdict.FAIL, gate("MUTATION"));
    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/reports/verification/index.html")));
    assertTrue(Files.isRegularFile(project.resolve("build/topplecat/agent-feedback.json")));
    String verificationReport =
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"));
    String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
    MutationGateResults mutation =
        MutationGateResults.read(
            Files.readString(project.resolve("build/topplecat/mutation-results.json")));
    var rawMutation = mutation.mutations().getFirst();
    assertNotNull(rawMutation.sourceFile());
    assertNotNull(rawMutation.lineNumber());
    assertNotNull(rawMutation.mutatedMethod());
    assertNotNull(rawMutation.methodDescription());
    assertNotNull(rawMutation.originalSourceLine());
    assertTrue(verificationReport.contains("undetectedMutations"));
    String rawSelector =
        rawMutation.coveringTests().stream()
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("fixture must retain a raw PIT covering selector"));
    for (String reviewerOnly :
        List.of(
            mutation.managedProfileId(),
            mutation.pitVersion(),
            rawMutation.status(),
            rawMutation.mutator(),
            rawMutation.description(),
            rawMutation.mutatedClass(),
            rawMutation.sourceFile(),
            rawMutation.mutatedMethod(),
            rawMutation.methodDescription(),
            rawMutation.originalSourceLine(),
            rawSelector,
            "CouponAcceptanceTest",
            "appliesCoupon",
            "producerMutationCount",
            "uniquelyAttributedMutationCount",
            "unattributedMutationCount",
            "coveredMutantCount",
            "killedByAcceptanceMethodMutantCount")) {
      assertTrue(
          verificationReport.contains(reviewerOnly),
          "verification report omitted: " + reviewerOnly);
      assertFalse(feedback.contains(reviewerOnly), "agent feedback leaked: " + reviewerOnly);
    }
    assertFalse(verificationReport.contains("detectionRate"));
    assertFalse(feedback.contains("detectionRate"));
    assertFalse(Files.exists(project.resolve("src/hiddenTest")));
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
  void missingManagedMutationReportIsIncompleteAndCannotReuseAStaleArtifact() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        afterEvaluate {
            tasks.named("toppleCatManagedPit") {
                onlyIf { false }
            }
        }
        """);
    writeManagedMutationFixture();
    Path stale = project.resolve("build/topplecat/runs/current/pit/mutations.xml");
    Files.createDirectories(stale.getParent());
    Files.writeString(stale, "<mutations></mutations>");

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").buildAndFail();

    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/evidence.json")), verify.getOutput());
    assertEquals(EvidenceVerdict.INCOMPLETE, gate("MUTATION"));
    assertFalse(Files.exists(stale));
    assertCurrentRunCompleted("MUTATION");
  }

  @Test
  void mutationTestingDoesNotExposeConsumerConfiguredProducersOrReports() throws Exception {
    writeProject(
        """
        toppleCat {
            mutationTesting {
                producerTask.set("consumerPit")
            }
        }
        """);

    var failure = runner("tasks").buildAndFail();

    assertTrue(failure.getOutput().contains("unknown property 'producerTask'"));
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
  void usableManagedEvidenceWithoutExactPublicAttributionFailsWithoutLeakingSelectors()
      throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        """);
    writeManagedMutationFixtureWithoutProductionCoverage();

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.FAIL, gate("MUTATION"));
    String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
    assertFalse(feedback.contains("CouponAcceptanceTest"));
    assertFalse(feedback.contains("appliesCoupon"));
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
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"))
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
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"))
            .contains("onlyBHasAProperty"));
  }

  @Test
  void selectedSpecRejectsSameNamedAcceptanceMethodOverloadsBeforeManagedPitRuns()
      throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        """);
    writeOverloadedAcceptanceMethods();
    writePublicCasesForOverloadedAcceptanceMethods();
    writeProductionClass();
    Path spec = project.resolve("specs/a.md");
    Files.createDirectories(spec.getParent());
    Files.writeString(spec, "# A\n\nAC-A\n");

    runner("toppleCatSeal", "--spec", "specs/a.md").build();
    var failure = runner("toppleCatVerify", "--spec", "specs/a.md").buildAndFail();

    assertTrue(
        failure
            .getOutput()
            .contains(
                "cannot safely target selected Acceptance Methods when"
                    + " example.OverloadedAcceptanceTest"),
        failure.getOutput());
    assertTrue(
        failure.getOutput().contains("both selected and unselected Acceptance Methods"),
        failure.getOutput());
  }

  @Test
  void selectedSpecMutationRunExcludesUnselectedAcAndRetainsPerAcDetection() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        """);
    writeSelectedSpecMutationFixture();
    Path spec = project.resolve("specs/selected.md");
    Files.createDirectories(spec.getParent());
    Files.writeString(spec, "# Selected\n\nAC-A\n\nAC-C\n");

    runner("toppleCatSeal", "--spec", "specs/selected.md").build();
    var verify = runner("toppleCatVerify", "--spec", "specs/selected.md").buildAndFail();

    assertEquals(
        TaskOutcome.SUCCESS, verify.task(":toppleCatManagedPit").getOutcome(), verify.getOutput());
    assertEquals(
        TaskOutcome.SUCCESS,
        verify.task(":toppleCatMutationGate").getOutcome(),
        verify.getOutput());
    assertEquals(EvidenceVerdict.PASS, gate("JUNIT"));
    assertEquals(EvidenceVerdict.FAIL, gate("MUTATION"));
    assertEquals(EvidenceVerdict.FAIL, evidence().verdict());

    MutationGateResults mutation =
        MutationGateResults.read(
            Files.readString(project.resolve("build/topplecat/mutation-results.json")));
    var weakAcceptance =
        mutation.assessments().stream()
            .filter(assessment -> assessment.acId().equals("AC-A"))
            .findFirst()
            .orElseThrow();
    var detectingAcceptance =
        mutation.assessments().stream()
            .filter(assessment -> assessment.acId().equals("AC-C"))
            .findFirst()
            .orElseThrow();
    assertTrue(
        weakAcceptance.coveredMutantCount() > weakAcceptance.killedByAcceptanceMethodMutantCount());
    assertEquals(
        detectingAcceptance.coveredMutantCount(),
        detectingAcceptance.killedByAcceptanceMethodMutantCount());
    assertTrue(
        mutation.mutations().stream()
            .anyMatch(
                item ->
                    item.status().equals("KILLED")
                        && item.attributedAcceptanceConditionIds()
                            .containsAll(List.of("AC-A", "AC-C"))
                        && item.detectedAcceptanceConditionIds().equals(List.of("AC-C"))));
    assertTrue(
        mutation.mutations().stream()
            .flatMap(
                item ->
                    java.util.stream.Stream.of(
                        item.coveringTests(), item.killingTests(), item.succeedingTests()))
            .flatMap(List::stream)
            .noneMatch(selector -> selector.contains("unselectedAcceptanceFailsIfRun")));

    String report =
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"));
    String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
    assertTrue(
        Files.isRegularFile(project.resolve("build/topplecat/reports/verification/index.html")));
    assertTrue(report.contains("\"AC-A\""));
    assertTrue(report.contains("\"AC-C\""));
    assertFalse(report.contains("\"AC-B\""));
    assertFalse(feedback.contains("unselectedAcceptanceFailsIfRun"));
    assertFalse(feedback.contains("CouponService"));
  }

  @Test
  void contractQualityAdvisoriesAreReviewerOnlyAndSuppressedWhenCheckRunsForVerify()
      throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            mutationTesting { enabled.set(false) }
            expectedConsumption { enabled.set(false) }
        }
        """);
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);
    Path hidden = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon.json");
    Files.createDirectories(hidden.getParent());
    Files.writeString(
        hidden,
        "[{\"caseId\":\"reviewer-shape-secret\",\"acId\":\"AC-COUPON\",\"inputs\":{},\"expected\":{\"discount\":100,\"reviewerOnlyShape\":true}}]");

    var check = runner("toppleCatCheck").build();
    assertTrue(check.getOutput().contains("EXPECTED_SHAPE_VARIANT_MISSING"));
    assertFalse(check.getOutput().contains("reviewer-shape-secret"));
    assertFalse(check.getOutput().contains("reviewerOnlyShape"));

    runner("toppleCatReview").build();
    String review = Files.readString(project.resolve("build/topplecat/reports/review/data.json"));
    assertTrue(review.contains("topplecat.review-view.v7"));
    assertTrue(review.contains("contractQualityAdvisories"));
    assertTrue(review.contains("EXPECTED_SHAPE_VARIANT_MISSING"));

    runner("toppleCatSeal").build();
    var verify = runner("toppleCatVerify").build();
    assertFalse(verify.getOutput().contains("EXPECTED_SHAPE_VARIANT_MISSING"));
    assertFalse(
        Files.readString(project.resolve("build/topplecat/contract-definition.json"))
            .contains("EXPECTED_SHAPE_VARIANT_MISSING"));
    assertFalse(
        Files.readString(project.resolve("build/topplecat/evidence.json"))
            .contains("EXPECTED_SHAPE_VARIANT_MISSING"));
    assertFalse(
        Files.readString(project.resolve("build/topplecat/agent-feedback.json"))
            .contains("EXPECTED_SHAPE_VARIANT_MISSING"));
    assertFalse(
        Files.readString(project.resolve("build/topplecat/reports/verification/data.json"))
            .contains("EXPECTED_SHAPE_VARIANT_MISSING"));
  }

  @Test
  void zeroProducerMutantsAreIncompleteAndStillWriteTheV1ReviewerArtifact() throws Exception {
    writeProject(
        """
        toppleCat {
            hiddenTests { enabled.set(false) }
            propertyBasedTesting { enabled.set(false) }
        }
        """);
    writeProductionClass();
    writeAcceptance("100", false);
    writePublicCase("coupon-public", "AC-COUPON", 100);

    runner("toppleCatSeal").build();
    runner("toppleCatVerify").buildAndFail();

    assertEquals(EvidenceVerdict.INCOMPLETE, gate("MUTATION"));
    String results = Files.readString(project.resolve("build/topplecat/mutation-results.json"));
    assertTrue(results.contains("topplecat.mutation-results.v1"));
    assertTrue(results.contains("\"producerMutationCount\" : 0"));
  }

  private void assertCurrentRunCompleted(String... gates) throws Exception {
    Path archive = project.resolve("build/topplecat/runs").resolve(evidence().runId());
    for (String gate : gates) {
      assertTrue(Files.isRegularFile(archive.resolve("gates").resolve(gate + ".completed")));
    }
  }

  private void writeProject(String configuration) throws Exception {
    writeProject("", configuration);
  }

  private void writeProjectWithConsumerPit(String configuration) throws Exception {
    writeProject(
        """
        buildscript {
            repositories { mavenCentral() }
            dependencies {
                classpath 'info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.19.0'
            }
        }
        """,
        configuration);
  }

  private void writeProject(String prelude, String configuration) throws Exception {
    Files.writeString(
        project.resolve("settings.gradle"), "rootProject.name = 'verification-consumer'\n");
    Path junit = moduleJar("topplecat-junit");
    Path core = moduleJar("topplecat-core");
    Path byteBuddy = libraryJar(ByteBuddy.class);
    Files.writeString(
        project.resolve("build.gradle"),
        """
        %s
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
            .formatted(prelude, junit, core, byteBuddy, configuration));
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

  private void writeTraditionalChineseScenarioAcceptance() throws Exception {
    Path source = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.As;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        import org.junit.jupiter.api.DisplayName;
        class CouponAcceptanceTest {
            @ToppleAcceptanceTest("AC-COUPON")
            @DisplayName("套用 SAVE100 折抵訂單小計")
            void appliesCoupon(ToppleCase testCase, ToppleScenario scenario, CouponStage coupon) {
                scenario.given(coupon).reads_discount(testCase.expected("discount", Integer.class));
                scenario.then(coupon).matches(testCase);
            }
            static class CouponStage extends ToppleStage {
                private int actual;
                @As("準備可結帳的購物車 {discount}")
                void reads_discount(int discount) { actual = discount; }
                @As("收據符合預期")
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

  private void writeAcceptanceWithDisplayNamedProperties(boolean counterexamples) throws Exception {
    Path source = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    StringBuilder properties = new StringBuilder();
    for (int index = 0; index < 5; index++) {
      boolean fails = counterexamples && index < 2;
      properties.append(
          """
          @org.junit.jupiter.api.DisplayName("Repeated reviewer-facing Property title")
          @ToppleProperty("AC-COUPON")
          void property%d(PropertyTrials trials) {
              trials.forAll(Generators.integers(0, 5)).check(value -> {%s});
          }
          """
              .formatted(index, fails ? "throw new AssertionError(\"counterexample\");" : ""));
    }
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
                void matches(ToppleCase testCase) { testCase.verify("discount", 100); }
            }
        }
        """
            .formatted(properties));
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

  private void writeManagedMutationFixture() throws Exception {
    Path production = project.resolve("src/main/java/example/CouponService.java");
    Files.createDirectories(production.getParent());
    Files.writeString(
        production,
        """
        package example;
        public final class CouponService {
            public static int discountedTotal(int subtotal) { return subtotal - 10; }
        }
        """);
    Path acceptance = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(acceptance.getParent());
    Files.writeString(
        acceptance,
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
                    testCase.verify("discount", CouponService.discountedTotal(110));
                }
            }
        }
        """);
    writePublicCase("coupon-public", "AC-COUPON", 100);
  }

  private void writeManagedMutationFixtureWithUnobservedVoidCall() throws Exception {
    Path production = project.resolve("src/main/java/example/CouponService.java");
    Files.createDirectories(production.getParent());
    Files.writeString(
        production,
        """
        package example;
        public final class CouponService {
            public static int discountedTotal(int subtotal) {
                recordAudit();
                return subtotal - 10;
            }
            private static void recordAudit() {}
        }
        """);
    writeManagedMutationAcceptance("CouponService.discountedTotal(110)");
  }

  private void writeManagedMutationFixtureWithFailingProperty() throws Exception {
    Path production = project.resolve("src/main/java/example/CouponService.java");
    Files.createDirectories(production.getParent());
    Files.writeString(
        production,
        """
        package example;
        public final class CouponService {
            public static int discountedTotal(int subtotal) {
                recordAudit();
                return subtotal - 10;
            }
            private static void recordAudit() {}
        }
        """);
    writeManagedMutationAcceptance("CouponService.discountedTotal(110)");
    Path property = project.resolve("src/test/java/example/DeliberatelyFailingProperty.java");
    Files.createDirectories(property.getParent());
    Files.writeString(
        property,
        """
        package example;
        import io.github.samzhu.topplecat.junit.property.Generators;
        import io.github.samzhu.topplecat.junit.property.PropertyTrials;
        import io.github.samzhu.topplecat.junit.property.ToppleProperty;
        class DeliberatelyFailingProperty {
            @ToppleProperty("AC-COUPON")
            void deliberatelyFailsTheIndependentProperty(PropertyTrials trials) {
                trials.forAll(Generators.integers(0, 5)).check(value -> {
                    throw new AssertionError("independent property failure");
                });
            }
        }
        """);
    writePublicCase("coupon-public", "AC-COUPON", 100);
  }

  private void writeManagedMutationFixtureWithoutProductionCoverage() throws Exception {
    Path production = project.resolve("src/main/java/example/CouponService.java");
    Files.createDirectories(production.getParent());
    Files.writeString(
        production,
        """
        package example;
        public final class CouponService {
            public static int discountedTotal(int subtotal) { return subtotal - 10; }
        }
        """);
    writeManagedMutationAcceptance("100");
  }

  private void writeManagedMutationAcceptance(String actualDiscount) throws Exception {
    Path acceptance = project.resolve("src/test/java/example/CouponAcceptanceTest.java");
    Files.createDirectories(acceptance.getParent());
    Files.writeString(
        acceptance,
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
                    testCase.verify("discount", %s);
                }
            }
        }
        """
            .formatted(actualDiscount));
    writePublicCase("coupon-public", "AC-COUPON", 100);
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

  private void writeOverloadedAcceptanceMethods() throws Exception {
    Path source = project.resolve("src/test/java/example/OverloadedAcceptanceTest.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        class OverloadedAcceptanceTest {
            @ToppleAcceptanceTest("AC-A")
            void checksCoupon(ToppleCase testCase, ToppleScenario scenario, FirstStage first) {
                scenario.then(first).matches(testCase);
            }
            @ToppleAcceptanceTest("AC-B")
            void checksCoupon(ToppleCase testCase, ToppleScenario scenario, SecondStage second) {
                scenario.then(second).matches(testCase);
            }
            static class FirstStage extends ToppleStage {
                void matches(ToppleCase testCase) { testCase.verify("discount", 100); }
            }
            static class SecondStage extends ToppleStage {
                void matches(ToppleCase testCase) { testCase.verify("discount", 100); }
            }
        }
        """);
  }

  private void writePublicCasesForOverloadedAcceptanceMethods() throws Exception {
    Path cases = project.resolve("src/test/resources/topplecat/cases/overloaded.json");
    Files.createDirectories(cases.getParent());
    Files.writeString(
        cases,
        """
        [
          {"caseId":"overloaded-a","acId":"AC-A","inputs":{},"expected":{"discount":100}},
          {"caseId":"overloaded-b","acId":"AC-B","inputs":{},"expected":{"discount":100}}
        ]
        """);
  }

  private void writeSelectedSpecMutationFixture() throws Exception {
    Path production = project.resolve("src/main/java/example/CouponService.java");
    Files.createDirectories(production.getParent());
    Files.writeString(
        production,
        """
        package example;
        public final class CouponService {
            public static int discountedTotal(int subtotal) { return subtotal - 10; }
        }
        """);
    Path acceptance = project.resolve("src/test/java/example/ScopedMutationAcceptanceTest.java");
    Files.createDirectories(acceptance.getParent());
    Files.writeString(
        acceptance,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        class ScopedMutationAcceptanceTest {
            @ToppleAcceptanceTest("AC-A")
            void weakAcceptance(ToppleCase testCase, ToppleScenario scenario, WeakStage coupon) {
                scenario.then(coupon).callsDiscountWithoutObservingIt(testCase);
            }
            @ToppleAcceptanceTest("AC-C")
            void detectingAcceptance(ToppleCase testCase, ToppleScenario scenario, DetectingStage coupon) {
                scenario.then(coupon).verifiesDiscount(testCase);
            }
            static class WeakStage extends ToppleStage {
                void callsDiscountWithoutObservingIt(ToppleCase testCase) {
                    CouponService.discountedTotal(110);
                    testCase.verify("discount", 100);
                }
            }
            static class DetectingStage extends ToppleStage {
                void verifiesDiscount(ToppleCase testCase) {
                    testCase.verify("discount", CouponService.discountedTotal(110));
                }
            }
        }
        """);
    Path unselected =
        project.resolve("src/test/java/example/UnselectedMutationAcceptanceTest.java");
    Files.writeString(
        unselected,
        """
        package example;
        import io.github.samzhu.topplecat.junit.ToppleAcceptanceTest;
        import io.github.samzhu.topplecat.junit.ToppleCase;
        import io.github.samzhu.topplecat.junit.ToppleScenario;
        import io.github.samzhu.topplecat.junit.ToppleStage;
        class UnselectedMutationAcceptanceTest {
            @ToppleAcceptanceTest("AC-B")
            void unselectedAcceptanceFailsIfRun(ToppleCase testCase, ToppleScenario scenario, UnselectedStage coupon) {
                scenario.then(coupon).failsIfRun();
            }
            static class UnselectedStage extends ToppleStage {
                void failsIfRun() { throw new AssertionError("AC-B must stay outside the selected delivery"); }
            }
        }
        """);
    Path cases = project.resolve("src/test/resources/topplecat/cases/scoped-mutation.json");
    Files.createDirectories(cases.getParent());
    Files.writeString(
        cases,
        """
        [
          {"caseId":"selected-weak","acId":"AC-A","inputs":{},"expected":{"discount":100}},
          {"caseId":"unselected-failing","acId":"AC-B","inputs":{},"expected":{"discount":100}},
          {"caseId":"selected-detecting","acId":"AC-C","inputs":{},"expected":{"discount":100}}
        ]
        """);
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

  private void assertReportLanguage(String name, String language) throws Exception {
    String html =
        Files.readString(
            project.resolve("build/topplecat/reports").resolve(name).resolve("index.html"));
    assertTrue(html.contains("<html lang=\"" + language + "\">"));
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
