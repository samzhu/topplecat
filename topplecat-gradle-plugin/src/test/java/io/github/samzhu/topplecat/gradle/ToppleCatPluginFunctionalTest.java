package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.core.EvidenceVerdict;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.EscrowService;
import io.github.samzhu.topplecat.core.ToppleEvidence;
import io.github.samzhu.topplecat.core.ToppleEvidenceJson;
import io.github.samzhu.topplecat.core.VerificationRunJson;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

class ToppleCatPluginFunctionalTest {
    @TempDir
    Path project;

    @Test
    void runsStaticCheckAgainstAnOrdinaryJavaProject() throws Exception {
        basicProject();
        writeTestSource("""
                package example;
                class OrderTest {
                    @ToppleStageField ResultThen then;
                    @ToppleTest("AC-CART-ORDER") void createsOrder() { then.matches_contract(); }
                    static final class ResultThen extends ToppleStage<ResultThen> {
                        ResultThen matches_contract() { recorded(); return self(); }
                    }
                }
                """);
        Path cases = project.resolve("src/test/resources/topplecat/cases/orders.json");
        Files.createDirectories(cases.getParent());
        Files.writeString(cases, """
                [{"caseId":"order-public","acId":"AC-CART-ORDER",
                  "inputs":{"cart":{"subtotal":500}},"expected":{"receipt":{"total":500}}}]
                """);

        var result = GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath()
                .withArguments("toppleCatCheck", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatCheck").getOutcome());
        assertTrue(result.getOutput().contains("ToppleCat check passed: 1 ACs, 1 case rows,"));
    }

    @Test
    void cleansCompilerDescriptorsWhenTheCanonicalSourceIsDeleted() throws Exception {
        basicProject();
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField ResultThen then;
                    @ToppleTest("AC-STALE") void createsOrder() { then.matches_contract(); }
                    static final class ResultThen extends ToppleStage<ResultThen> {
                        ResultThen matches_contract() { recorded(); return self(); }
                    }
                }
                """);
        writePublicCase("stale.json", """
                [{"caseId":"stale-public","acId":"AC-STALE","inputs":{},"expected":{"result":true}}]
                """);

        runner("toppleCatCheck").build();
        Path descriptorIndex = project.resolve("build/topplecat/compiler/META-INF/topplecat/contracts/index");
        assertTrue(Files.isRegularFile(descriptorIndex));
        Files.delete(project.resolve("src/test/java/example/OrderTest.java"));

        var failed = runner("toppleCatCheck", "--rerun-tasks").buildAndFail();

        assertFalse(Files.exists(descriptorIndex));
        assertTrue(failed.getOutput().contains("javac emitted no canonical @ToppleTest descriptor"), failed.getOutput());
    }

    @Test
    void preservesJavacTypeDiagnosticsWithoutWritingADescriptor() throws Exception {
        basicProject();
        writeTestSource("""
                class TypeErrorTest {
                    @ToppleStageField Given given;
                    @ToppleTest("AC-TYPE-ERROR") void rejects() { given.a_value(42); }
                    static final class Given extends ToppleStage<Given> {
                        Given a_value(String value) { recorded(value); return self(); }
                    }
                }
                """);
        writePublicCase("type-error.json", """
                [{"caseId":"type-error-public","acId":"AC-TYPE-ERROR","inputs":{},"expected":{"result":true}}]
                """);

        var failed = runner("toppleCatCheck").buildAndFail();

        assertTrue(failed.getOutput().contains("incompatible types"), failed.getOutput());
        assertFalse(Files.exists(project.resolve("build/topplecat/compiler/META-INF/topplecat/contracts/index")));
    }

    @Test
    void enforcesTheCanonicalStageDslThroughCheckAndKeepsToppleAcAsOrdinaryJUnitCoverage() throws Exception {
        basicProject();
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField ResultThen then;
                    @ToppleTest("AC-CART-ORDER")
                    void createsOrder(ToppleCase c) { then.matches_contract(c); }
                    @ToppleAc("AC-EXTRA")
                    void ordinaryExtraCoverage(ToppleCase c) { c.verify("receipt", c.input("receipt", Object.class)); }
                    static final class ResultThen extends ToppleStage<ResultThen> {
                        @As("驗證訂單結果")
                        ResultThen matches_contract(ToppleCase c) {
                            recorded();
                            c.verify("receipt", c.input("receipt", Object.class));
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("orders.json", """
                [{"caseId":"order-public","acId":"AC-CART-ORDER","inputs":{},"expected":{"receipt":{}}}]
                """);

        assertEquals(TaskOutcome.SUCCESS, runner("toppleCatCheck").build().task(":toppleCatCheck").getOutcome());
        var reviewed = runner("toppleCatReview").build();
        String html = Files.readString(project.resolve("build/topplecat/reports/review/index.html"));
        assertEquals(TaskOutcome.SUCCESS, reviewed.task(":toppleCatReview").getOutcome());
        assertTrue(html.contains("驗證訂單結果"), html);
        assertTrue(html.contains("order-public"), html);
        assertFalse(html.contains(">PASS<"), html);
        assertFalse(html.contains(">FAIL<"), html);

        writeTestSource("""
                class OrderTest {
                    @ToppleTest("AC-CART-ORDER")
                    void createsOrder(ToppleCase c) { c.verify("receipt", c.input("receipt", Object.class)); }
                }
                """);
        var rejected = runner("toppleCatCheck").buildAndFail();
        assertTrue(rejected.getOutput().contains("AC-CART-ORDER"), rejected.getOutput());
        assertTrue(rejected.getOutput().contains("receiver must be a same-class @ToppleStageField"), rejected.getOutput());
    }

    @Test
    void hidesAndRestoresReviewerSource() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();
        Path reviewerSource = project.resolve("src/hiddenTest/java/example/ReviewerTest.java");
        String originalReviewerSource = Files.readString(reviewerSource);

        var hidden = runner("toppleCatHide", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, hidden.task(":toppleCatHide").getOutcome());
        assertTrue(hidden.getOutput().contains("Local hidden storage is plaintext"), hidden.getOutput());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
        assertFalse(Files.exists(project.resolve(".topplecat/escrow")));
        assertTrue(Files.isRegularFile(reviewerEscrowRoot().resolve("manifest.json")));
        var publicOnlyCheck = runner("toppleCatCheck", "--stacktrace").build();
        assertEquals(TaskOutcome.SUCCESS, publicOnlyCheck.task(":toppleCatCheck").getOutcome());
        assertFalse(Files.readString(project.resolve("build/topplecat/contract-definition.json"))
                .contains("coupon-reviewer"));

        var restored = runner("toppleCatRestore", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, restored.task(":toppleCatRestore").getOutcome());
        assertEquals(originalReviewerSource, Files.readString(reviewerSource));
        assertTrue(restored.getOutput().contains("ToppleCat restore complete")
                || restored.getOutput().contains("reviewer files are available to the reviewer"), restored.getOutput());
        var reviewerCheck = runner("toppleCatCheck", "--stacktrace").build();
        assertEquals(TaskOutcome.SUCCESS, reviewerCheck.task(":toppleCatCheck").getOutcome());
        assertTrue(Files.readString(project.resolve("build/topplecat/contract-definition.json"))
                .contains("coupon-reviewer"));
    }

    @Test
    void migratesLegacyEscrowToReviewerLocalState() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        deleteRecursively(project.resolve(".topplecat-state"));
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();

        Path legacy = project.resolve(".toppleCatEscrowLocalLegacy");
        Path legacyStateRoot = project.resolve(legacy);
        new EscrowService(legacyStateRoot).hide(project, project.resolve("src/hiddenTest"));
        Path legacyEscrow = project.resolve(".topplecat/escrow");
        copyDirectory(legacyStateRoot.resolve("projects")
                .resolve(EscrowService.projectKey(project))
                .resolve("escrow"), legacyEscrow);
        assertTrue(Files.exists(legacyEscrow.resolve("manifest.json")));

        var migrated = runner("toppleCatMigrateEscrow", "--stacktrace").build();
        assertEquals(TaskOutcome.SUCCESS, migrated.task(":toppleCatMigrateEscrow").getOutcome());
        assertTrue(Files.exists(reviewerEscrowRoot().resolve("manifest.json")));
        assertFalse(Files.exists(legacyEscrow.resolve("manifest.json")));

        var verify = runner("toppleCatVerify", "--stacktrace").buildAndFail();
        assertEquals(TaskOutcome.FAILED, verify.task(":toppleCatReport").getOutcome());
        ToppleEvidence verifyEvidence = ToppleEvidenceJson.read(Files.readString(
                project.resolve("build/topplecat/evidence.json")));
        assertEquals(EvidenceVerdict.INCOMPLETE, verifyEvidence.gates().stream()
                .filter(gate -> "CONTRACT_INTEGRITY".equals(gate.name()))
                .findFirst()
                .orElseThrow()
                .verdict());
        var restored = runner("toppleCatRestore", "--stacktrace").build();
        assertEquals(TaskOutcome.SUCCESS, restored.task(":toppleCatRestore").getOutcome());
        assertFalse(Files.exists(legacyEscrow));
    }

    @Test
    void refusesVerificationWhenProjectMovesWithoutReviewerStateRootMatch() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();
        runner("toppleCatHide", "--stacktrace").build();

        Path moved = Files.createTempDirectory("topplecat-move-project");
        try {
            Files.createDirectories(moved.resolve("src"));
            copyDirectory(project.resolve("src"), moved.resolve("src"));
            Files.copy(project.resolve("settings.gradle"), moved.resolve("settings.gradle"));
            Files.copy(project.resolve("build.gradle"), moved.resolve("build.gradle"));

            var movedVerify = runner(moved, moved.resolve(".topplecat-state"), "toppleCatVerify", "--stacktrace").buildAndFail();
            System.out.println(movedVerify.getOutput());

            assertEquals(TaskOutcome.FAILED, movedVerify.task(":toppleCatHide").getOutcome());
            assertTrue(movedVerify.getOutput().contains("Cannot create reviewer approval without reviewer source"),
                    movedVerify.getOutput());
            assertFalse(Files.exists(EscrowService.reviewerStatePath(moved, moved.resolve(".topplecat-state"))
                    .resolve("manifest.json")));
        } finally {
            deleteRecursively(moved);
        }
    }

    @Test
    void refusesManualRestoreWithoutAnExistingHide() throws Exception {
        basicProject();

        var result = runner("toppleCatRestore", "--stacktrace").buildAndFail();

        assertTrue(result.getOutput().contains("Run toppleCatHide before restoring reviewer source"), result.getOutput());
    }

    @Test
    void toppleCatUpdateEscrowFailsSafelyWithoutExistingCustody() throws Exception {
        verificationProject("");
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);

        var result = runner("toppleCatUpdateEscrow", "--stacktrace").buildAndFail();

        assertTrue(result.getOutput().contains("No ToppleCat escrow manifest exists"), result.getOutput());
        assertFalse(Files.exists(reviewerEscrowRoot().resolve("manifest.json")));
    }

    @Test
    void toppleCatUpdateEscrowRejectsHiddenManifest() throws Exception {
        verificationProject("");
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();
        runner("toppleCatHide", "--stacktrace").build();

        var result = runner("toppleCatUpdateEscrow", "--stacktrace").buildAndFail();

        assertTrue(result.getOutput().contains("toppleCatRestore"), result.getOutput());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void toppleCatUpdateEscrowUpdatesReviewedSuiteAndRehidesAfterVerification() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();
        runner("toppleCatHide", "--stacktrace").build();
        runner("toppleCatRestore", "--stacktrace").build();
        Path reviewerTest = project.resolve("src/hiddenTest/java/example/ReviewerTest.java");
        Path removedCase = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon-reviewer.yaml");
        Path addedCase = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon-reviewer-updated.yaml");
        Files.writeString(reviewerTest, """
                package example;
                import org.junit.jupiter.api.Test;
                class ReviewerTest { @Test void updatedReviewerGuard() {} }
                """);
        Files.delete(removedCase);
        Files.writeString(addedCase, """
                - caseId: coupon-reviewer-updated
                  acId: AC-CART-COUPON
                  inputs: {}
                  expected: {discount: 100}
                """);

        var updated = runner("toppleCatUpdateEscrow", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, updated.task(":toppleCatUpdateEscrow").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, updated.task(":toppleCatReview").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
        runner("toppleCatRestore", "--stacktrace").build();
        assertTrue(Files.readString(reviewerTest).contains("updatedReviewerGuard"));
        assertFalse(Files.exists(removedCase));
        assertTrue(Files.readString(addedCase).contains("coupon-reviewer-updated"));

        var verified = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, verified.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
        Path reviewerJunitXml = Files.list(currentRun(project).resolve("junit/JUNIT"))
                .filter(path -> path.toString().endsWith(".xml")).findFirst().orElseThrow();
        String reviewerResults = Files.readString(reviewerJunitXml);
        assertTrue(reviewerResults.contains("coupon-reviewer-updated"), reviewerResults);
        assertFalse(reviewerResults.contains("coupon-reviewer\""), reviewerResults);
        String publicSpec = Files.readString(project.resolve("build/topplecat/reports/spec/data.json"));
        String evidence = Files.readString(project.resolve("build/topplecat/evidence.json"));
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        for (String publicArtifact : List.of(publicSpec, evidence, feedback)) {
            assertFalse(publicArtifact.contains("topplecat.escrow-update"), publicArtifact);
            assertFalse(publicArtifact.contains("previousManifestSha256"), publicArtifact);
            assertFalse(publicArtifact.contains("coupon-reviewer-updated"), publicArtifact);
        }
    }

    @Test
    void verifyAutomaticallyHidesRestoresAndRehidesReviewerSource() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatHide").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRestore").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void failsClosedWhenRuntimeExecutionReferencesAnUnknownDefinitionCase() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { enabled.set(false) }
                }
                tasks.named('toppleCatVerificationTest') {
                    doLast {
                        def definition = file("$buildDir/topplecat/contract-definition.json").text
                        def digest = (definition =~ /"digest" : "([^"]+)"/)
                        if (!digest.find()) {
                            throw new GradleException('Test fixture could not read the contract definition digest.')
                        }
                        file("$buildDir/topplecat/runs/current/narrative-executions.jsonl").append(
                                '{"definitionDigest":"' + digest.group(1)
                                        + '","caseId":"unknown-execution-case","steps":[]}' + System.lineSeparator())
                    }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);

        var failed = runner("toppleCatVerify", "--stacktrace").buildAndFail();
        String digest = ContractDefinitionJson.read(Files.readString(
                project.resolve("build/topplecat/contract-definition.json"))).digest();

        assertEquals(TaskOutcome.FAILED, failed.task(":toppleCatReport").getOutcome());
        assertTrue(failed.getOutput().contains("unknown-execution-case"), failed.getOutput());
        assertTrue(failed.getOutput().contains(digest), failed.getOutput());
    }

    @Test
    void refusesVerificationBeforeChangingReviewerSourceWhenAnotherCustodyOperationHoldsTheProjectLock() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();
        Path lockPath = reviewerEscrowRoot().resolve(".lock");
        Files.createDirectories(lockPath.getParent());

        try (FileChannel channel = FileChannel.open(lockPath, CREATE, WRITE); FileLock ignored = channel.lock()) {
            var failed = runner("toppleCatVerify", "--stacktrace").buildAndFail();
            assertEquals(TaskOutcome.FAILED, failed.task(":toppleCatAcquireCustody").getOutcome());
            assertTrue(failed.getOutput().contains("Another ToppleCat custody operation is already running"),
                    failed.getOutput());
            assertTrue(Files.exists(project.resolve("src/hiddenTest")));
        }

        var completed = runner("toppleCatVerify", "--stacktrace").build();
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void doesNotSchedulePitWhenTheConsumerHasNoProductionPackages() throws Exception {
        basicProject();

        var result = runner("toppleCatVerify", "--dry-run").build();

        assertTrue(result.getOutput().contains(":toppleCatVerificationTest SKIPPED"), result.getOutput());
        assertTrue(result.getOutput().contains(":hiddenTest SKIPPED"), result.getOutput());
        assertFalse(result.getOutput().contains(":pitest SKIPPED"), result.getOutput());
        assertFalse(result.getOutput().contains(":toppleCatMutationGate SKIPPED"), result.getOutput());
    }

    @Test
    void defaultMutationConfigurationRunsPitAgainstDiscoveredProductionPackages() throws Exception {
        verificationProject("");
        Path production = project.resolve("src/main/java/example/CouponService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package example;
                public final class CouponService {
                    public int discountFor(int subtotal) { return subtotal >= 500 ? 100 : 0; }
                }
                """);
        writeTestSource("""
                package example;
                import io.github.samzhu.topplecat.junit.ToppleCase;
                import io.github.samzhu.topplecat.junit.ToppleStage;
                import io.github.samzhu.topplecat.junit.ToppleStageField;
                import io.github.samzhu.topplecat.junit.ToppleTest;
                class CouponTest {
                    @ToppleStageField CouponThen then;
                    @ToppleTest("AC-CART-COUPON")
                    void appliesCoupon(ToppleCase c) {
                        then.matches_contract(c);
                    }
                    static final class CouponThen extends ToppleStage<CouponThen> {
                        private final CouponService service = new CouponService();
                        CouponThen matches_contract(ToppleCase c) {
                            recorded();
                            c.verify("discount", service.discountFor(c.input("subtotal", Integer.class)));
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{"subtotal":500},"expected":{"discount":100}}]
                """);
        writeReviewerTestOnly();

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pitest").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatMutationGate").getOutcome());
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "MUTATION"));
    }

    @Test
    void defaultPitTargetsCanonicalAcceptanceTestsAcrossDifferentPackages() throws Exception {
        verificationProject("");
        Path production = project.resolve("src/main/java/com/example/checkout/CheckoutService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package com.example.checkout;
                public final class CheckoutService {
                    public int total(int subtotal) { return subtotal >= 500 ? subtotal - 100 : subtotal; }
                }
                """);
        writeTestSource("""
                package example;
                class CheckoutAcceptanceTest {
                    @ToppleStageField CheckoutThen then;
                    @ToppleTest("AC-CHECKOUT-TOTAL")
                    void appliesDiscount(ToppleCase c) { then.matches_contract(c); }
                    static final class CheckoutThen extends ToppleStage<CheckoutThen> {
                        private final com.example.checkout.CheckoutService service =
                                new com.example.checkout.CheckoutService();
                        CheckoutThen matches_contract(ToppleCase c) {
                            recorded();
                            c.verify("total", service.total(c.input("subtotal", Integer.class)));
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("checkout.json", """
                [{"caseId":"checkout-public","acId":"AC-CHECKOUT-TOTAL",
                  "inputs":{"subtotal":500},"expected":{"total":400}}]
                """);
        writeHiddenReviewAsset();
        Files.writeString(project.resolve("src/hiddenTest/resources/topplecat/cases/coupon-reviewer.yaml"), """
                - caseId: checkout-reviewer
                  acId: AC-CHECKOUT-TOTAL
                  inputs: {subtotal: 500}
                  expected: {total: 400}
                """);

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pitest").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatMutationGate").getOutcome());
        assertEquals(EvidenceVerdict.PASS, evidenceVerdict(project),
                Files.readString(project.resolve("build/topplecat/evidence.json")));
        for (String gate : List.of("CONTRACT_INTEGRITY", "JUNIT", "REVIEWER_JUNIT",
                "EXPECTED_CONSUMPTION", "MUTATION")) {
            assertEquals(EvidenceVerdict.PASS, gateVerdict(project, gate), gate);
        }
        String pitReport = Files.readString(project.resolve("build/reports/pitest/mutations.xml"));
        assertTrue(pitReport.contains("example.CheckoutAcceptanceTest"), pitReport);
        assertFalse(pitReport.contains("ReviewerTest"), pitReport);
        MutationGateResults mutation = MutationGateResults.read(
                Files.readString(project.resolve("build/topplecat/mutation-results.json")));
        var canonicalAssessment = mutation.assessments().stream()
                .filter(assessment -> assessment.acId().equals("AC-CHECKOUT-TOTAL"))
                .findFirst().orElseThrow();
        assertTrue(canonicalAssessment.totalMutations() > 0, mutation.toString());
        assertTrue(canonicalAssessment.detectedMutations() > 0, mutation.toString());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void defaultPitTargetsCanonicalAcceptanceTestsForAConsumerFaithfulKotlinDslProject() throws Exception {
        verificationProjectKotlin("");
        Path production = project.resolve("src/main/java/cart/service/CartService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package cart.service;
                public final class CartService {
                    public int total(int subtotal) { return subtotal >= 500 ? subtotal - 100 : subtotal; }
                }
                """);
        writeTestSource("""
                package cart.topplecat;
                class CartAcceptanceTest {
                    @ToppleStageField CartThen then;
                    @ToppleTest("AC-CART-TOTAL")
                    void appliesDiscount(ToppleCase c) { then.matches_contract(c); }
                    static final class CartThen extends ToppleStage<CartThen> {
                        private final cart.service.CartService service = new cart.service.CartService();
                        CartThen matches_contract(ToppleCase c) {
                            recorded();
                            c.verify("total", service.total(c.input("subtotal", Integer.class)));
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("cart.json", """
                [{"caseId":"cart-public-500","acId":"AC-CART-TOTAL",
                  "inputs":{"subtotal":500},"expected":{"total":400}}]
                """);
        writeHiddenRowsOnly("""
                - caseId: cart-reviewer-800
                  acId: AC-CART-TOTAL
                  inputs: {subtotal: 800}
                  expected: {total: 700}
                """);

        var result = runner("toppleCatVerify", "--info", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pitest").getOutcome(), result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatMutationGate").getOutcome(), result.getOutput());
        assertEquals(EvidenceVerdict.PASS, evidenceVerdict(project));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "MUTATION"));
        MutationGateResults mutation = MutationGateResults.read(
                Files.readString(project.resolve("build/topplecat/mutation-results.json")));
        var assessment = mutation.assessments().stream()
                .filter(candidate -> candidate.acId().equals("AC-CART-TOTAL"))
                .findFirst().orElseThrow();
        assertEquals(List.of("cart.topplecat.CartAcceptanceTest"), assessment.testClasses(), mutation.toString());
        assertTrue(assessment.totalMutations() > 0, mutation.toString());
        assertTrue(assessment.detectedMutations() > 0, mutation.toString());
        String pitReport = Files.readString(project.resolve("build/reports/pitest/mutations.xml"));
        assertTrue(pitReport.contains("cart.topplecat.CartAcceptanceTest"), pitReport);
        assertFalse(pitReport.contains("NO_COVERAGE"), pitReport);
        assertTrue(result.getOutput().contains("--targetTests=cart.topplecat.CartAcceptanceTest"),
                result.getOutput());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void preservesAnExplicitPitTargetThatExcludesTheCanonicalAcceptanceTest() throws Exception {
        assertExplicitPitTargetRemainsAuthoritative("""
                apply plugin: 'info.solidsoft.pitest'
                pitest {
                    pitestVersion = '1.25.5'
                    junit5PluginVersion = '1.2.3'
                    targetClasses = ['com.example.checkout.*']
                    targetTests = ['other.UnrelatedTest']
                    outputFormats = ['XML']
                }
                """);
    }

    @Test
    void preservesAnExplicitTaskLevelPitTargetThatExcludesTheCanonicalAcceptanceTest() throws Exception {
        assertExplicitPitTargetRemainsAuthoritative("""
                apply plugin: 'info.solidsoft.pitest'
                pitest {
                    pitestVersion = '1.25.5'
                    junit5PluginVersion = '1.2.3'
                    targetClasses = ['com.example.checkout.*']
                    outputFormats = ['XML']
                }
                tasks.withType(info.solidsoft.gradle.pitest.PitestTask).configureEach {
                    targetTests.set(['other.UnrelatedTest'] as Set)
                }
                """);
    }

    @Test
    void failsClosedWhenConsumerOwnedPitLeavesTargetTestsToItsConvention() throws Exception {
        assertExplicitPitTargetRemainsAuthoritative("""
                apply plugin: 'info.solidsoft.pitest'
                pitest {
                    pitestVersion = '1.25.5'
                    junit5PluginVersion = '1.2.3'
                    targetClasses = ['com.example.checkout.*']
                    outputFormats = ['XML']
                }
                """);
    }

    @Test
    void rowsOnlyReviewerRetestPassesThroughThePublicCanonicalAcceptanceTest() throws Exception {
        verificationProject("");
        writeMutationCouponFixture();
        writeHiddenRowsOnly("""
                - caseId: coupon-reviewer-800
                  acId: AC-CART-COUPON
                  inputs: {subtotal: 800}
                  expected: {discount: 100}
                """);

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.PASS, evidenceVerdict(project));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "EXPECTED_CONSUMPTION"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "MUTATION"));
        String junit = Files.readString(Files.list(currentRun(project).resolve("junit/JUNIT"))
                .filter(path -> path.toString().endsWith(".xml")).findFirst().orElseThrow());
        assertTrue(junit.contains("coupon-reviewer-800"), junit);
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void rowsOnlyReviewerRetestIgnoresAHiddenJavaHelperWithoutJUnitTests() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenRowsOnly("""
                - caseId: coupon-reviewer-helper
                  acId: AC-CART-COUPON
                  inputs: {}
                  expected: {discount: 100}
                """);
        writeHiddenJavaTest("ReviewerSupport", """
                package example;
                final class ReviewerSupport {
                    static int approvedDiscount() { return 100; }
                }
                """);

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "EXPECTED_CONSUMPTION"));
        assertEquals(EvidenceVerdict.PASS, evidenceVerdict(project));
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void rowsOnlyReviewerFailureFailsTheReviewerGateWithoutAHiddenJavaTask() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenRowsOnly("""
                - caseId: coupon-reviewer-fail
                  acId: AC-CART-COUPON
                  inputs: {}
                  expected: {discount: 99}
                """);

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.FAIL, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.FAIL, evidenceVerdict(project));
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        assertFalse(feedback.contains("coupon-reviewer-fail"), feedback);
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void hiddenRowsAndAnExecutableHiddenJUnitTestMustBothPass() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":hiddenTest").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "EXPECTED_CONSUMPTION"));
        assertEquals(EvidenceVerdict.PASS, evidenceVerdict(project));
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void failsClosedWhenReviewerRetestHasNeitherHiddenRowsNorJavaTests() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        Path reviewerReadme = project.resolve("src/hiddenTest/README.md");
        Files.createDirectories(reviewerReadme.getParent());
        Files.writeString(reviewerReadme, "reviewer-only notes\n");

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.INCOMPLETE, evidenceVerdict(project));
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void hiddenJavaFailureCannotBeMaskedByPassingReviewerRows() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenRowsOnly("""
                - caseId: coupon-reviewer-pass
                  acId: AC-CART-COUPON
                  inputs: {}
                  expected: {discount: 100}
                """);
        writeHiddenJavaTest("ReviewerFailureTest", """
                package example;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.fail;
                class ReviewerFailureTest {
                    @Test void meaningfulReviewerGuard() { fail("reviewer guard failed"); }
                }
                """);

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":hiddenTest").getOutcome());
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.FAIL, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.FAIL, evidenceVerdict(project));
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        assertFalse(feedback.contains("ReviewerFailureTest"), feedback);
        assertFalse(feedback.contains("reviewer guard failed"), feedback);
        assertFalse(feedback.contains("coupon-reviewer-pass"), feedback);
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void hiddenExpectedConsumptionCannotPassWhenTheCanonicalStageOnlyReadsIt() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { mutation { enabled.set(false) } }
                }
                """);
        writeTestSource("""
                package example;
                class CouponTest {
                    @ToppleStageField CouponThen then;
                    @ToppleTest("AC-CART-COUPON")
                    void appliesCoupon(ToppleCase testCase) { then.matches_contract(testCase); }
                    static final class CouponThen extends ToppleStage<CouponThen> {
                        CouponThen matches_contract(ToppleCase testCase) {
                            recorded();
                            if (testCase.hidden()) {
                                testCase.expected("discount", Integer.class);
                            } else {
                                testCase.verify("discount", 100);
                            }
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenRowsOnly("""
                - caseId: coupon-reviewer-unasserted
                  acId: AC-CART-COUPON
                  inputs: {}
                  expected: {discount: 100}
                """);

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        assertNotEquals(EvidenceVerdict.PASS, gateVerdict(project, "JUNIT"));
        assertNotEquals(EvidenceVerdict.PASS, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.FAIL, gateVerdict(project, "EXPECTED_CONSUMPTION"));
        assertEquals(EvidenceVerdict.FAIL, evidenceVerdict(project));
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        assertFalse(feedback.contains("coupon-reviewer-unasserted"), feedback);
        assertFalse(feedback.contains("100"), feedback);
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    private void assertExplicitPitTargetRemainsAuthoritative(String pitConfiguration) throws Exception {
        verificationProject(pitConfiguration);
        Path production = project.resolve("src/main/java/com/example/checkout/CheckoutService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package com.example.checkout;
                public final class CheckoutService {
                    public int total(int subtotal) { return subtotal >= 500 ? subtotal - 100 : subtotal; }
                }
                """);
        writeTestSource("""
                package example;
                class CheckoutAcceptanceTest {
                    @ToppleStageField CheckoutThen then;
                    @ToppleTest("AC-CHECKOUT-TOTAL")
                    void appliesDiscount(ToppleCase c) { then.matches_contract(c); }
                    static final class CheckoutThen extends ToppleStage<CheckoutThen> {
                        private final com.example.checkout.CheckoutService service =
                                new com.example.checkout.CheckoutService();
                        CheckoutThen matches_contract(ToppleCase c) {
                            recorded();
                            c.verify("total", service.total(c.input("subtotal", Integer.class)));
                            return self();
                        }
                    }
                }
                """);
        Path unrelated = project.resolve("src/test/java/other/UnrelatedTest.java");
        Files.createDirectories(unrelated.getParent());
        Files.writeString(unrelated, """
                package other;
                import org.junit.jupiter.api.Test;
                class UnrelatedTest { @Test void doesNotExerciseTheContract() {} }
                """);
        writePublicCase("checkout.json", """
                [{"caseId":"checkout-public","acId":"AC-CHECKOUT-TOTAL",
                  "inputs":{"subtotal":500},"expected":{"total":400}}]
                """);
        writeHiddenReviewAsset();
        Files.writeString(project.resolve("src/hiddenTest/resources/topplecat/cases/coupon-reviewer.yaml"), """
                - caseId: checkout-reviewer
                  acId: AC-CHECKOUT-TOTAL
                  inputs: {subtotal: 500}
                  expected: {total: 400}
                """);

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();
        assertTrue(result.task(":pitest") != null, result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":pitest").getOutcome(), result.getOutput());
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatMutationGate").getOutcome());
        assertNotEquals(EvidenceVerdict.PASS, gateVerdict(project, "MUTATION"));
        assertNotEquals(EvidenceVerdict.PASS, evidenceVerdict(project));
        String pitReport = Files.readString(project.resolve("build/reports/pitest/mutations.xml"));
        assertFalse(pitReport.contains("example.CheckoutAcceptanceTest"), pitReport);
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        assertTrue(feedback.contains("Mutation verification did not exercise the required public acceptance contract")
                || feedback.contains("MUTATION"), feedback);
        assertFalse(feedback.contains("CheckoutAcceptanceTest"), feedback);
        assertFalse(feedback.contains("src/hiddenTest"), feedback);
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void recordsMutationIncompleteWhenTheProducerDoesNotWriteAUsableReport() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial {
                        hiddenRetest { enabled.set(false) }
                        mutation {
                            enabled.set(true)
                            producerTask.set('missingPitReport')
                            reportFile.set(layout.buildDirectory.file('pit/mutations.xml'))
                        }
                    }
                }
                tasks.register('missingPitReport') {
                    doLast { layout.buildDirectory.dir('pit').get().asFile.mkdirs() }
                }
                """);
        writeProductionSource();
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();
        Path staleEvidence = project.resolve("build/topplecat/evidence.json");
        Files.createDirectories(staleEvidence.getParent());
        Files.writeString(staleEvidence, "STALE-EVIDENCE");

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.SUCCESS, result.task(":missingPitReport").getOutcome());
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "MUTATION"));
        assertEquals(EvidenceVerdict.INCOMPLETE, evidenceVerdict(project));
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        assertTrue(feedback.contains("MUTATION") || feedback.contains("mutation"), feedback);
        assertFalse(feedback.contains("STALE-EVIDENCE"), feedback);
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void leavesAnUnrelatedPitExtensionUntouchedForACustomMutationProducer() throws Exception {
        verificationProject("""
                apply plugin: 'info.solidsoft.pitest'
                pitest { fullMutationMatrix = false }
                toppleCat {
                    adversarial {
                        hiddenRetest { enabled.set(false) }
                        mutation {
                            producerTask.set('writeCustomPit')
                            reportFile.set(layout.buildDirectory.file('pit/mutations.xml'))
                        }
                    }
                }
                tasks.register('writeCustomPit') {
                    doLast {
                        if (pitest.fullMutationMatrix.get()) {
                            throw new GradleException('ToppleCat changed an unrelated PIT extension')
                        }
                        def output = layout.buildDirectory.file('pit/mutations.xml').get().asFile
                        output.parentFile.mkdirs()
                        output.text = '''<mutations>
                          <mutation detected="true" status="KILLED"><mutatedClass>example.CouponService</mutatedClass>
                            <coveringTests>example.CouponTest.[engine:junit-jupiter]/[class:example.CouponTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase)]/[test-template-invocation:#1]</coveringTests></mutation>
                        </mutations>'''
                    }
                }
                """);
        writeProductionSource();
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":writeCustomPit").getOutcome());
        assertNull(result.task(":pitest"), result.getOutput());
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "MUTATION"));
        assertEquals(EvidenceVerdict.PASS, evidenceVerdict(project));
    }

    @Test
    void defaultMutationProducerDoesNotUseReviewerCasesOrTests() throws Exception {
        verificationProject("");
        Path production = project.resolve("src/main/java/example/CouponService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package example;
                public final class CouponService {
                    public int discountFor(int subtotal) { return subtotal >= 500 ? 100 : 0; }
                }
                """);
        writeTestSource("""
                package example;
                class CouponTest {
                    @ToppleStageField CouponThen then;
                    @ToppleTest("AC-CART-COUPON")
                    void appliesCoupon(ToppleCase testCase) {
                        then.matches_contract(testCase);
                    }
                    static final class CouponThen extends ToppleStage<CouponThen> {
                        private final CouponService service = new CouponService();
                        CouponThen matches_contract(ToppleCase testCase) {
                            recorded();
                            testCase.verify("discount", service.discountFor(testCase.input("subtotal", Integer.class)));
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public-non-boundary","acId":"AC-CART-COUPON",
                  "inputs":{"subtotal":600},"expected":{"discount":100}}]
                """);
        Path reviewerCase = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon-reviewer-boundary.yaml");
        Files.createDirectories(reviewerCase.getParent());
        Files.writeString(reviewerCase, """
                - caseId: coupon-reviewer-boundary
                  acId: AC-CART-COUPON
                  inputs: {subtotal: 500}
                  expected: {discount: 100}
                """);
        Path reviewerTest = project.resolve("src/hiddenTest/java/example/ReviewerBoundaryTest.java");
        Files.createDirectories(reviewerTest.getParent());
        Files.writeString(reviewerTest, """
                package example;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                import org.junit.jupiter.api.Test;
                class ReviewerBoundaryTest {
                    @Test void detectsTheReviewerOnlyBoundary() {
                        assertEquals(100, new CouponService().discountFor(500));
                    }
                }
                """);

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pitest").getOutcome());
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatMutationGate").getOutcome());
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.FAIL, gateVerdict(project, "MUTATION"));
        String pitReport = Files.readString(project.resolve("build/reports/pitest/mutations.xml"));
        assertFalse(pitReport.contains("ReviewerBoundaryTest"), pitReport);
        assertFalse(pitReport.contains("coupon-reviewer-boundary"), pitReport);
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void rejectsTheProductionPlusPublicAnswerAttackAfterReviewerHidesTheContract() throws Exception {
        verificationProject("");
        Path production = project.resolve("src/main/java/example/AmountService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package example;
                public final class AmountService {
                    public int amountFor(String customer) { return 250; }
                }
                """);
        Path contract = project.resolve("src/test/java/example/AmountContractTest.java");
        Files.createDirectories(contract.getParent());
        Files.writeString(contract, """
                package example;
                import io.github.samzhu.topplecat.junit.ToppleCase;
                import io.github.samzhu.topplecat.junit.ToppleStage;
                import io.github.samzhu.topplecat.junit.ToppleStageField;
                import io.github.samzhu.topplecat.junit.ToppleTest;
                class AmountContractTest {
                    @ToppleStageField AmountThen then;
                    @ToppleTest("AC-CONTRACT-INTEGRITY")
                    void returns_the_approved_amount(ToppleCase testCase) {
                        then.matches_contract(testCase);
                    }
                    static final class AmountThen extends ToppleStage<AmountThen> {
                        AmountThen matches_contract(ToppleCase testCase) {
                            recorded();
                            testCase.verify("amount", new AmountService().amountFor(testCase.input("customer", String.class)));
                            return self();
                        }
                    }
                }
                """);
        Path publicCases = project.resolve("src/test/resources/topplecat/cases/amount.json");
        Files.createDirectories(publicCases.getParent());
        Files.writeString(publicCases, """
                [
                  {"caseId":"approved-visible-customer","acId":"AC-CONTRACT-INTEGRITY",
                   "inputs":{"customer":"visible-customer"},"expected":{"amount":250}},
                  {"caseId":"visible-control-customer","acId":"AC-CONTRACT-INTEGRITY",
                   "inputs":{"customer":"visible-control"},"expected":{"amount":250}}
                ]
                """);
        Path reviewerTest = project.resolve("src/hiddenTest/java/example/ReviewerAmountTest.java");
        Files.createDirectories(reviewerTest.getParent());
        Files.writeString(reviewerTest, """
                package example;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                import org.junit.jupiter.api.Test;
                class ReviewerAmountTest {
                    @Test void keeps_the_reviewer_boundary() {
                        assertEquals(250, new AmountService().amountFor("reviewer-boundary"));
                    }
                }
                """);
        Path reviewerCases = project.resolve("src/hiddenTest/resources/topplecat/cases/amount-reviewer.yaml");
        Files.createDirectories(reviewerCases.getParent());
        Files.writeString(reviewerCases, """
                - caseId: reviewer-boundary-customer
                  acId: AC-CONTRACT-INTEGRITY
                  inputs: {customer: reviewer-boundary}
                  expected: {amount: 250}
                """);

        assertEquals(TaskOutcome.SUCCESS, runner("toppleCatReview", "--stacktrace").build()
                .task(":toppleCatReview").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, runner("toppleCatHide", "--stacktrace").build()
                .task(":toppleCatHide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));

        Files.writeString(publicCases, """
                [
                  {"caseId":"approved-visible-customer","acId":"AC-CONTRACT-INTEGRITY",
                   "inputs":{"customer":"visible-customer"},"expected":{"amount":251}},
                  {"caseId":"visible-control-customer","acId":"AC-CONTRACT-INTEGRITY",
                   "inputs":{"customer":"visible-control"},"expected":{"amount":250}}
                ]
                """);
        Files.writeString(production, """
                package example;
                public final class AmountService {
                    public int amountFor(String customer) {
                        return "visible-customer".equals(customer) ? 251 : 250;
                    }
                }
                """);

        var rejected = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.FAILED, rejected.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.FAIL, gateVerdict(project, "CONTRACT_INTEGRITY"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "EXPECTED_CONSUMPTION"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "MUTATION"));
        assertEquals(EvidenceVerdict.FAIL, evidenceVerdict(project));
        assertEquals(TaskOutcome.SUCCESS, rejected.task(":toppleCatRehide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
        assertFalse(Files.exists(project.resolve("build/topplecat/reports/spec")));
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        for (String forbidden : List.of("250", "251", "visible-customer", "reviewer-boundary", "amount.json",
                "AC-CONTRACT-INTEGRITY")) {
            assertFalse(feedback.contains(forbidden), feedback);
        }
    }

    @Test
    void livePitKeepsAcceptanceConditionsSeparateWhenTheyShareATestClass() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial {
                        hiddenRetest { enabled.set(false) }
                    }
                }
                """);
        Path production = project.resolve("src/main/java/example/ResultService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package example;
                public final class ResultService {
                    public int result() { return 1; }
                }
                """);
        writeTestSource("""
                package example;
                class OrderAcceptanceTest {
                    @ToppleStageField ResultThen then;

                    @ToppleTest("AC-FROM-SERVICE")
                    void readsServiceResult(ToppleCase c) {
                        then.matches_service_result(c);
                    }

                    @ToppleTest("AC-FROM-INPUT")
                    void readsInputResult(ToppleCase c) {
                        then.matches_input_result(c);
                    }

                    static final class ResultThen extends ToppleStage<ResultThen> {
                        private final ResultService service = new ResultService();

                        ResultThen matches_service_result(ToppleCase c) {
                            recorded();
                            c.verify("result", service.result());
                            return self();
                        }

                        ResultThen matches_input_result(ToppleCase c) {
                            recorded();
                            c.verify("result", c.input("result", Integer.class));
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("results.json", """
                [
                  {"caseId":"service-public","acId":"AC-FROM-SERVICE",
                   "inputs":{},"expected":{"result":1}},
                  {"caseId":"input-public","acId":"AC-FROM-INPUT",
                   "inputs":{"result":2},"expected":{"result":2}}
                ]
                """);

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pitest").getOutcome());
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatMutationGate").getOutcome());
        String pitReport = Files.readString(project.resolve("build/reports/pitest/mutations.xml"));
        MutationGateResults mutation = MutationGateResults.read(
                Files.readString(project.resolve("build/topplecat/mutation-results.json")));
        var service = mutation.assessments().stream()
                .filter(assessment -> assessment.acId().equals("AC-FROM-SERVICE")).findFirst().orElseThrow();
        var input = mutation.assessments().stream()
                .filter(assessment -> assessment.acId().equals("AC-FROM-INPUT")).findFirst().orElseThrow();
        assertTrue(service.totalMutations() > 0, pitReport + "\n" + mutation);
        assertEquals(EvidenceVerdict.PASS, service.verdict(), mutation.toString());
        assertEquals(0, input.totalMutations(), mutation.toString());
        assertEquals(EvidenceVerdict.FAIL, input.verdict(), mutation.toString());
    }

    @Test
    void failsToppleCatVerifyWhenAggregateVerdictIsIncompleteEvenWithoutAnyFailingGate() throws Exception {
        verificationProject("");
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertEquals(EvidenceVerdict.INCOMPLETE, evidenceVerdict(project));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "MUTATION"));
        assertTrue(Files.isRegularFile(project.resolve("build/topplecat/evidence.json")));
        assertTrue(Files.isRegularFile(project.resolve("build/topplecat/agent-feedback.json")));
        assertTrue(Files.isRegularFile(project.resolve("build/topplecat/reports/spec/index.html")));
        assertTrue(Files.isRegularFile(project.resolve("build/topplecat/reports/verification/index.html")));
        assertTrue(Files.readString(project.resolve("build/topplecat/agent-feedback.json")).contains("INCOMPLETE"));
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void recordsMutationIncompleteWithoutSchedulingPitWhenProductionPackagesCannotBeFound() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial {
                        hiddenRetest { enabled.set(false) }
                    }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);

        var noSources = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertMutationIncompleteWithoutPit(noSources,
                "no production sources found under src/main/java; the mutation gate cannot run.");

        Path unpackagedProduction = project.resolve("src/main/java/Unpackaged.java");
        Files.createDirectories(unpackagedProduction.getParent());
        Files.writeString(unpackagedProduction, "public final class Unpackaged {}\n");

        var noPackages = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertMutationIncompleteWithoutPit(noPackages,
                "no production packages found under src/main/java; the mutation gate cannot run.");
    }

    @Test
    void marksAllAdversarialSafeguardsDisabledWithoutBlockingAPassingPublicVerification() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial { enabled.set(false) }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.PASS, evidenceVerdict(project),
                Files.readString(project.resolve("build/topplecat/evidence.json")));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "JUNIT"));
        assertDisabled(project, "REVIEWER_JUNIT", "disabled by toppleCat.adversarial.enabled=false");
        assertDisabled(project, "EXPECTED_CONSUMPTION", "disabled by toppleCat.adversarial.enabled=false");
        assertDisabled(project, "MUTATION", "disabled by toppleCat.adversarial.enabled=false");
        String verificationHtml = Files.readString(project.resolve("build/topplecat/reports/verification/index.html"));
        assertTrue(verificationHtml.contains("disabled by toppleCat.adversarial.enabled=false"), verificationHtml);
        assertTrue(result.task(":toppleCatRestore") == null, result.getOutput());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void disablesHiddenRetestWithoutRestoringReviewerSource() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial {
                        hiddenRetest { enabled.set(false) }
                        mutation { enabled.set(false) }
                    }
                }
                """);
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        writeHiddenReviewAsset();

        var result = runner("toppleCatVerify", "--stacktrace").build();

        assertDisabled(project, "REVIEWER_JUNIT", "disabled by toppleCat.adversarial.hiddenRetest.enabled=false");
        assertDisabled(project, "MUTATION", "disabled by toppleCat.adversarial.mutation.enabled=false");
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "EXPECTED_CONSUMPTION"));
        assertTrue(result.task(":toppleCatRestore") == null, result.getOutput());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void failsExpectedConsumptionWhenExpectedValueWasOnlyRead() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial {
                        hiddenRetest { enabled.set(false) }
                        mutation { enabled.set(false) }
                    }
                }
                """);
        writeTestSource("""
                package example;
                class CouponTest {
                    @ToppleStageField CouponThen then;
                    @ToppleTest("AC-CART-COUPON")
                    void readsExpectedButNeverVerifiesIt(ToppleCase testCase) {
                        then.reads_expected_only(testCase);
                    }
                    static final class CouponThen extends ToppleStage<CouponThen> {
                        CouponThen reads_expected_only(ToppleCase testCase) {
                            recorded();
                            testCase.expected("discount", Integer.class);
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);

        runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(EvidenceVerdict.FAIL, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.FAIL, gateVerdict(project, "EXPECTED_CONSUMPTION"));
        String reviewerData = Files.readString(project.resolve("build/topplecat/reports/verification/data.json"));
        assertTrue(reviewerData.contains("\"READ\""), reviewerData);
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        assertFalse(feedback.contains("coupon-public"), feedback);
        assertFalse(feedback.contains("discount"), feedback);
        assertFalse(feedback.contains("100"), feedback);
    }

    @Test
    void recordsDisabledExpectedConsumptionWhileKeepingUntouchedValuesInReviewerEvidence() throws Exception {
        verificationProject("""
                toppleCat {
                    adversarial {
                        hiddenRetest { enabled.set(false) }
                        mutation { enabled.set(false) }
                        expectedConsumption { enabled.set(false) }
                    }
                }
                """);
        writeTestSource("""
                package example;
                import io.github.samzhu.topplecat.junit.ToppleCase;
                import io.github.samzhu.topplecat.junit.ToppleStage;
                import io.github.samzhu.topplecat.junit.ToppleStageField;
                import io.github.samzhu.topplecat.junit.ToppleTest;
                class CouponTest {
                    @ToppleStageField CouponThen then;
                    @ToppleTest("AC-CART-COUPON")
                    void readsOneExpectedValueAndLeavesAnotherUntouched(ToppleCase testCase) {
                        then.reads_one_expected_value(testCase);
                    }
                    static final class CouponThen extends ToppleStage<CouponThen> {
                        CouponThen reads_one_expected_value(ToppleCase testCase) {
                            recorded();
                            testCase.expected("discount", Integer.class);
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100,"total":400}}]
                """);

        runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(EvidenceVerdict.PASS, evidenceVerdict(project));
        assertDisabled(project, "EXPECTED_CONSUMPTION",
                "disabled by toppleCat.adversarial.expectedConsumption.enabled=false");
        String view = Files.readString(project.resolve("build/topplecat/reports/verification/data.json"));
        String html = Files.readString(project.resolve("build/topplecat/reports/verification/index.html"));
        assertTrue(view.contains("\"READ\""), view);
        assertTrue(view.contains("\"UNTOUCHED\""), view);
        assertTrue(view.contains("\"expectedConsumptionEnforced\" : false"), view);
        assertTrue(html.contains("disabled by toppleCat.adversarial.expectedConsumption.enabled=false"), html);
    }

    @Test
    void initializesAnEmptyProjectWithoutOverwritingAndLeavesCheckGreen() throws Exception {
        verificationProject("");

        var first = GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath()
                .withArguments("toppleCatInit", "--stacktrace").build();

        Path publicCase = project.resolve("src/test/resources/topplecat/cases/order-public.json");
        Path test = project.resolve("src/test/java/example/OrderAcceptanceTest.java");
        Path hiddenCase = project.resolve("src/hiddenTest/resources/topplecat/cases/order-reviewer.yaml");
        Path reviewerReadme = project.resolve("src/hiddenTest/README.md");
        assertTrue(Files.isRegularFile(publicCase));
        assertTrue(Files.isRegularFile(test));
        assertTrue(Files.isRegularFile(hiddenCase));
        assertTrue(Files.isRegularFile(reviewerReadme));
        assertTrue(Files.readString(test).contains("c.verify(\"result\""));
        assertTrue(first.getOutput().contains("ToppleCat init created: src/test/resources/topplecat/cases/order-public.json"));
        assertTrue(first.getOutput().contains("ToppleCat did not modify .gitignore"));
        assertTrue(first.getOutput().contains("./gradlew toppleCatCheck"));
        String firstContents = Files.readString(publicCase);

        var check = GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath()
                .withArguments("toppleCatCheck", "--stacktrace").build();
        assertEquals(TaskOutcome.SUCCESS, check.task(":toppleCatCheck").getOutcome());

        var second = GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath()
                .withArguments("toppleCatInit", "--stacktrace").build();
        assertEquals(firstContents, Files.readString(publicCase));
        assertTrue(second.getOutput().contains("ToppleCat init skipped: src/test/resources/topplecat/cases/order-public.json already exists."));
    }

    @Test
    void explainsUnknownCaseAcWithSourceAndRepair() throws Exception {
        basicProject();
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField ResultThen then;
                    @ToppleTest("AC-CART-ORDER") void createsOrder() { then.records_order(); }
                    static final class ResultThen extends ToppleStage<ResultThen> {
                        ResultThen records_order() { recorded(); return self(); }
                    }
                }
                """);
        writePublicCase("orders.json", """
                [{"caseId":"order-public","acId":"AC-UNKNOWN","inputs":{"total":1},"expected":{"total":1}}]
                """);

        var result = runner("toppleCatCheck").buildAndFail();

        assertTrue(result.getOutput().contains("Case order-public"), result.getOutput());
        assertTrue(result.getOutput().contains("orders.json"), result.getOutput());
        assertTrue(result.getOutput().contains("a compilable @ToppleTest(\"AC-UNKNOWN\")"), result.getOutput());
    }

    @Test
    void rejectsReviewerCasesWhenNoPublicCaseDataExists() throws Exception {
        basicProject();
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField ResultThen then;
                    @ToppleTest("AC-CART-ORDER") void createsOrder() { then.records_order(); }
                    static final class ResultThen extends ToppleStage<ResultThen> {
                        ResultThen records_order() { recorded(); return self(); }
                    }
                }
                """);
        Path hidden = project.resolve("src/hiddenTest/resources/topplecat/cases/order-reviewer.yaml");
        Files.createDirectories(hidden.getParent());
        Files.writeString(hidden, """
                - caseId: order-reviewer
                  acId: AC-CART-ORDER
                  inputs: {}
                  expected: {accepted: true}
                """);

        var result = runner("toppleCatCheck").buildAndFail();

        assertTrue(result.getOutput().contains("No public ToppleCat JSON/YAML cases found under"), result.getOutput());
    }

    @Test
    void explainsMalformedAndUnsupportedCaseSources() throws Exception {
        basicProject();
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField ResultThen then;
                    @ToppleTest("AC-CART-ORDER") void createsOrder() { then.records_order(); }
                    static final class ResultThen extends ToppleStage<ResultThen> {
                        ResultThen records_order() { recorded(); return self(); }
                    }
                }
                """);
        writePublicCase("orders.json", """
                [{"caseId":"order-public","acId":"AC-CART-ORDER","inputs":{},"expected":{"total":1},"typo":true}]
                """);

        var malformed = runner("toppleCatCheck").buildAndFail();
        assertTrue(malformed.getOutput().contains("orders.json row 1"), malformed.getOutput());
        assertTrue(malformed.getOutput().contains("Fix the named JSON/YAML file and row"), malformed.getOutput());

        Files.delete(project.resolve("src/test/resources/topplecat/cases/orders.json"));
        writePublicCase("notes.txt", "not a ToppleCat case\n");
        var unsupported = runner("toppleCatCheck").buildAndFail();
        assertTrue(unsupported.getOutput().contains("Topple case source must be JSON or YAML"), unsupported.getOutput());
        assertTrue(unsupported.getOutput().contains("notes.txt"), unsupported.getOutput());
    }

    @Test
    void warnsButDoesNotFailForUnusedBindingsOrNoLiteralVerify() throws Exception {
        basicProject();
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField OrderStage stage;
                    @ToppleTest("AC-CART-ORDER") void createsOrder() { stage.records_order(); }
                    @ToppleTest("AC-CART-UNUSED") void unusedOrder() { stage.records_unused_order(); }
                    static final class OrderStage extends ToppleStage<OrderStage> {
                        OrderStage records_order() { recorded(); return self(); }
                        OrderStage records_unused_order() { recorded(); return self(); }
                    }
                }
                """);
        writePublicCase("orders.json", """
                [{"caseId":"order-public","acId":"AC-CART-ORDER","inputs":{"total":1},"expected":{"total":1}}]
                """);

        var result = runner("toppleCatCheck").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatCheck").getOutcome());
        assertTrue(result.getOutput().contains("ToppleCat check passed: 2 ACs, 1 case rows,"), result.getOutput());
    }

    @Test
    void writesReviewerOnlyStaticReviewAndWarnsForBidirectionalSpecDrift() throws Exception {
        verificationProject("""
                toppleCat {
                    specDocs.from('specs')
                }
                """);
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField CartGiven given;
                    @ToppleStageField OrderWhen when;

                    @ToppleTest("AC-CART-ORDER")
                    void createsOrder(ToppleCase c) {
                        given.a_cart(c.input("cart", Cart.class));
                        when.creates_order();
                    }
                    @ToppleTest("AC-TEST-ONLY") void testOnly() { when.creates_order(); }

                    static final class CartGiven extends ToppleStage<CartGiven> {
                        @As("準備 {0} 元的購物車")
                        CartGiven a_cart(Cart cart) { recorded(cart.total()); return self(); }
                    }
                    static final class OrderWhen extends ToppleStage<OrderWhen> {
                        @As("建立訂單")
                        OrderWhen creates_order() { recorded(); return self(); }
                    }
                    record Cart(int total) {}
                }
                """);
        writePublicCase("orders.json", """
                [{"caseId":"order-public","acId":"AC-CART-ORDER","inputs":{"cart":{"total":500}},"expected":{"total":500}}]
                """);
        Path hidden = project.resolve("src/hiddenTest/resources/topplecat/cases/order-reviewer.yaml");
        Files.createDirectories(hidden.getParent());
        Files.writeString(hidden, """
                - caseId: order-reviewer-secret
                  acId: AC-CART-ORDER
                  inputs: {cart: {total: 800}}
                  expected: {total: 700}
                """);
        Path spec = project.resolve("specs/cart-orders.md");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, """
                # Cart orders

                ## AC-CART-ORDER Create the order
                Public contract text with `<script>window.previewInjected = true</script>`,
                `&lt;script&gt;window.entityInjected = true&lt;/script&gt;`, and `inline code`.

                ## AC-SPEC-ONLY Deliberately unmatched
                This AC has no Java binding.
                """);

        var result = runner("toppleCatCheck").build();

        assertTrue(result.getOutput().contains("specs/cart-orders.md mentions AC-SPEC-ONLY"), result.getOutput());
        assertTrue(result.getOutput().contains("Java binding AC-TEST-ONLY"), result.getOutput());
        Path review = project.resolve("build/topplecat/reports/review");
        assertFalse(Files.exists(review));

        var reviewed = runner("toppleCatReview").build();

        assertEquals(TaskOutcome.SUCCESS, reviewed.task(":toppleCatReview").getOutcome());
        String html = Files.readString(review.resolve("index.html"));
        assertTrue(html.contains("topplecat.review-view"));
        assertTrue(html.contains("Public contract text"));
        assertTrue(html.contains("window.previewInjected = true"));
        assertTrue(html.contains("window.entityInjected = true"));
        assertFalse(html.contains("<script>window.previewInjected = true</script>"));
        assertFalse(html.contains("<script>window.entityInjected = true</script>"));
        assertTrue(html.contains("order-reviewer-secret"));
        assertTrue(html.contains("\\u003ctotal\\u003e"), html);
        assertTrue(html.contains("建立訂單"));
        assertTrue(html.contains("\"sourceCode\""));
        assertFalse(html.contains(">PASS<"));
        assertFalse(html.contains(">FAIL<"));

        Files.writeString(project.resolve("src/test/resources/topplecat/cases/orders.json"), """
                [{"caseId":"order-public","acId":"AC-UNKNOWN","inputs":{},"expected":{}}]
                """);
        runner("toppleCatReview").buildAndFail();
        assertFalse(Files.exists(review));
    }

    @Test
    void doesNotEmitSpecAlignmentWarningsWithoutConfiguredSpecDocs() throws Exception {
        basicProject();
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField ResultThen then;
                    @ToppleTest("AC-CART-ORDER") void createsOrder() { then.matches_contract(); }
                    static final class ResultThen extends ToppleStage<ResultThen> {
                        ResultThen matches_contract() { recorded(); return self(); }
                    }
                }
                """);
        writePublicCase("orders.json", """
                [{"caseId":"order-public","acId":"AC-CART-ORDER","inputs":{},"expected":{"total":1}}]
                """);

        var result = runner("toppleCatCheck").build();

        assertFalse(result.getOutput().contains("configured specDocs"), result.getOutput());
        assertFalse(result.getOutput().contains("external spec"), result.getOutput());
        assertFalse(Files.exists(project.resolve("build/topplecat/review")));
    }

    @Test
    void alignsExternalSpecsOnlyAgainstPublicBindingsAfterReviewerHandoff() throws Exception {
        verificationProject("""
                toppleCat {
                    specDocs.from('specs')
                }
                """);
        writeTestSource("""
                class OrderTest {
                    @ToppleStageField ResultThen then;
                    @ToppleTest("AC-CART-ORDER") void createsOrder() { then.matches_contract(); }
                    static final class ResultThen extends ToppleStage<ResultThen> {
                        ResultThen matches_contract() { recorded(); return self(); }
                    }
                }
                """);
        writePublicCase("orders.json", """
                [{"caseId":"order-public","acId":"AC-CART-ORDER","inputs":{},"expected":{"total":1}}]
                """);
        Path reviewer = project.resolve("src/hiddenTest/java/example/ReviewerBoundaryTest.java");
        Files.createDirectories(reviewer.getParent());
        Files.writeString(reviewer, """
                class ReviewerBoundaryTest {
                    @ToppleAc("AC-REVIEWER-ONLY") void rejectsBoundary() {}
                }
                """);
        Path spec = project.resolve("specs/orders.md");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, "## AC-CART-ORDER Create the order\n");

        var result = runner("toppleCatCheck").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatCheck").getOutcome());
        assertFalse(result.getOutput().contains("AC-REVIEWER-ONLY"), result.getOutput());
    }

    @Test
    void verifiesHiddenCasesAcrossSeparateGradleInvocationsAndRehidesReviewerSource() throws Exception {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Path junit = moduleJar("topplecat-junit");
        Path core = moduleJar("topplecat-core");
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.samzhu.topplecat'
                }
                repositories { mavenCentral() }
                dependencies {
                    testImplementation files('%s', '%s')
                    testImplementation 'org.junit.jupiter:junit-jupiter:6.1.1'
                    testImplementation 'tools.jackson.core:jackson-databind:3.2.0'
                    testImplementation 'tools.jackson.dataformat:jackson-dataformat-yaml:3.2.0'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.1.1'
                }
                toppleCat {
                    specDocs.from('specs')
                }
                """.formatted(junit, core));
        Path test = project.resolve("src/test/java/example/CouponTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, """
                package example;
                import io.github.samzhu.topplecat.junit.As;
                import io.github.samzhu.topplecat.junit.ExpectedState;
                import io.github.samzhu.topplecat.junit.ProvidedState;
                import io.github.samzhu.topplecat.junit.ToppleCase;
                import io.github.samzhu.topplecat.junit.ToppleStage;
                import io.github.samzhu.topplecat.junit.ToppleStageField;
                import io.github.samzhu.topplecat.junit.ToppleTest;
                import org.junit.jupiter.api.DisplayName;
                class CouponTest {
                    @ToppleStageField CartGiven given;
                    @ToppleStageField CouponWhen when;
                    @ToppleStageField CouponThen then;

                    @ToppleTest("AC-CART-COUPON")
                    @DisplayName("Apply a fixed coupon")
                    void appliesCoupon(ToppleCase c) {
                        given.a_cart(c.input("subtotal", Integer.class));
                        when.applies_coupon();
                        then.matches_contract(c);
                    }

                    static final class CartGiven extends ToppleStage<CartGiven> {
                        @ProvidedState Integer subtotal;
                        @As("準備金額為 {0} 元的購物車")
                        CartGiven a_cart(Integer subtotal) {
                            recorded(subtotal);
                            this.subtotal = subtotal;
                            return self();
                        }
                    }

                    static final class CouponWhen extends ToppleStage<CouponWhen> {
                        @ExpectedState(required = true) Integer subtotal;
                        @ProvidedState Integer discount;
                        @As("套用優惠券")
                        CouponWhen applies_coupon() {
                            recorded();
                            discount = subtotal / 5;
                            return self();
                        }
                    }

                    static final class CouponThen extends ToppleStage<CouponThen> {
                        @ExpectedState(required = true) Integer discount;
                        @As("驗證折抵結果")
                        CouponThen matches_contract(ToppleCase c) {
                            recorded();
                            c.verify("discount", discount);
                            return self();
                        }
                    }
                }
                """);
        Path publicCases = project.resolve("src/test/resources/topplecat/cases/coupon.json");
        Files.createDirectories(publicCases.getParent());
        Files.writeString(publicCases, """
                [{"caseId":"coupon-public-500","acId":"AC-CART-COUPON",
                  "inputs":{"subtotal":500},"expected":{"discount":100}}]
                """);
        Path production = project.resolve("src/main/java/example/Cart.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package example;
                public record Cart(int subtotal) {}
                """);
        Path hiddenTest = project.resolve("src/hiddenTest/java/example/ReviewerTest.java");
        Files.createDirectories(hiddenTest.getParent());
        Files.writeString(hiddenTest, """
                package example;
                import org.junit.jupiter.api.Test;
                class ReviewerTest {
                    @Test void reviewerGuard() { new Cart(800); }
                }
                """);
        Path hiddenCases = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon-hidden.yaml");
        Files.createDirectories(hiddenCases.getParent());
        Files.writeString(hiddenCases, """
                - caseId: coupon-hidden-pass-800
                  acId: AC-CART-COUPON
                  inputs: {subtotal: 800}
                  expected: {discount: 160}
                - caseId: coupon-hidden-fail-900
                  acId: AC-CART-COUPON
                  inputs: {subtotal: 900}
                  expected: {discount: 999}
                """);
        Path specDocument = project.resolve("specs/coupon.md");
        Files.createDirectories(specDocument.getParent());
        Files.writeString(specDocument, """
                ## AC-CART-COUPON Apply a fixed coupon
                External spec context describes the public coupon contract.
                """);

        runner("clean").build();
        var hide = runner("toppleCatHide", "--stacktrace").build();
        assertEquals(TaskOutcome.SUCCESS, hide.task(":toppleCatHide").getOutcome());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));

        var result = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertTrue(result.task(":toppleCatReport") != null, result.getOutput());
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":toppleCatRehide").getOutcome());
        assertTrue(result.task(":toppleCatVerificationTest") != null, result.getOutput());
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
        Path junitXml = Files.list(currentRun(project).resolve("junit/JUNIT"))
                .filter(path -> path.toString().endsWith(".xml")).findFirst().orElseThrow();
        String junitResults = Files.readString(junitXml);
        assertTrue(junitResults.contains("coupon-hidden-pass-800"), junitResults);
        assertTrue(junitResults.contains("coupon-hidden-fail-900"), junitResults);
        String spec = Files.readString(project.resolve("build/topplecat/reports/spec/data.json"));
        String specHtml = Files.readString(project.resolve("build/topplecat/reports/spec/index.html"));
        String verification = Files.readString(project.resolve("build/topplecat/reports/verification/data.json"));
        String verificationHtml = Files.readString(project.resolve("build/topplecat/reports/verification/index.html"));
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        Path archivedRun = archivedRuns(project).getFirst();
        String verificationRun = Files.readString(archivedRun.resolve("verification-run.json"));
        String reviewerDefinitionDigest = ContractDefinitionJson.read(Files.readString(
                archivedRun.resolve("reviewer-definition.json"))).digest();
        assertFalse(spec.contains("coupon-hidden-pass-800"));
        assertFalse(spec.contains("coupon-hidden-fail-900"));
        assertFalse(containsStandaloneToken(spec, "800"));
        assertFalse(containsStandaloneToken(spec, "900"));
        assertFalse(containsStandaloneToken(spec, "999"));
        assertFalse(specHtml.contains("coupon-hidden-pass-800"));
        assertFalse(specHtml.contains("coupon-hidden-fail-900"));
        assertTrue(spec.contains("Given 準備金額為 <subtotal> 元的購物車"), spec);
        assertFalse(spec.contains("\"steps\""), spec);
        assertFalse(spec.contains("\"status\""), spec);
        assertFalse(spec.contains("\"failure\""), spec);
        assertTrue(spec.contains("Apply a fixed coupon"), spec);
        assertTrue(spec.contains("External spec context"), spec);
        assertTrue(verification.contains("coupon-hidden-pass-800"), verification);
        assertTrue(verification.contains("coupon-hidden-fail-900"), verification);
        assertTrue(verificationHtml.contains("coupon-hidden-pass-800"), verificationHtml);
        assertTrue(verificationHtml.contains("coupon-hidden-fail-900"), verificationHtml);
        assertTrue(verification.contains("\"HIDDEN\""), verification);
        assertTrue(verification.contains("\"PASS\""), verification);
        assertTrue(verification.contains("\"FAIL\""), verification);
        assertTrue(verification.contains("準備金額為 800 元的購物車"), verification);
        assertTrue(verification.contains("驗證折抵結果"), verification);
        assertTrue(verification.contains("\"steps\""), verification);
        assertTrue(verification.contains("\"failure\""), verification);
        assertTrue(verification.contains("External spec context"), verification);
        assertTrue(verificationRun.contains("coupon-hidden-pass-800"), verificationRun);
        assertTrue(verificationRun.contains("coupon-hidden-fail-900"), verificationRun);
        assertTrue(verificationRun.contains("\"FAIL\""), verificationRun);
        assertEquals(reviewerDefinitionDigest, VerificationRunJson.read(verificationRun).definitionDigest());
        assertTrue(Files.readString(archivedRun.resolve("narrative-executions.jsonl")).contains(reviewerDefinitionDigest));
        assertFalse(feedback.contains("coupon-hidden-pass-800"));
        assertFalse(feedback.contains("coupon-hidden-fail-900"));
        assertFalse(feedback.contains("800"));
        assertFalse(feedback.contains("900"));
        assertFalse(feedback.contains("999"));
        assertFalse(feedback.contains("AssertionFailedError"));
    }

    @Test
    void neverReusesPreviousPassArtifactsWhenAPublicVerificationFails() throws Exception {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'lifecycle-consumer'\n");
        Path junit = moduleJar("topplecat-junit");
        Path core = moduleJar("topplecat-core");
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.samzhu.topplecat'
                }
                repositories { mavenCentral() }
                dependencies {
                    testImplementation files('%s', '%s')
                    testImplementation 'org.junit.jupiter:junit-jupiter:6.1.1'
                    testImplementation 'tools.jackson.core:jackson-databind:3.2.0'
                    testImplementation 'tools.jackson.dataformat:jackson-dataformat-yaml:3.2.0'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.1.1'
                }
                toppleCat {
                    adversarial {
                        mutation {
                            enabled.set(true)
                            threshold.set(100)
                            producerTask.set('writePitFixture')
                            reportFile.set(layout.buildDirectory.file('pit/mutations.xml'))
                        }
                    }
                }
                tasks.register('writePitFixture') {
                    doLast {
                        def output = layout.buildDirectory.file('pit/mutations.xml').get().asFile
                        output.parentFile.mkdirs()
                        output.text = '''<mutations>
                          <mutation detected="true" status="KILLED"><mutatedClass>example.CouponService</mutatedClass>
                            <coveringTests>example.CouponTest.[engine:junit-jupiter]/[class:example.CouponTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase)]/[test-template-invocation:#1]</coveringTests></mutation>
                        </mutations>'''
                    }
                }
                """.formatted(junit, core));
        writeProductionSource();
        Path test = project.resolve("src/test/java/example/CouponTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, couponSource("100"));
        Path publicCases = project.resolve("src/test/resources/topplecat/cases/coupon.json");
        Files.createDirectories(publicCases.getParent());
        Files.writeString(publicCases, """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        Path reviewer = project.resolve("src/hiddenTest/java/example/ReviewerTest.java");
        Files.createDirectories(reviewer.getParent());
        Files.writeString(reviewer, """
                package example;
                import org.junit.jupiter.api.Test;
                class ReviewerTest { @Test void reviewerGuard() {} }
                """);
        Path hiddenCases = project.resolve("src/hiddenTest/resources/topplecat/cases/reviewer.yaml");
        Files.createDirectories(hiddenCases.getParent());
        Files.writeString(hiddenCases, """
                - caseId: coupon-reviewer
                  acId: AC-CART-COUPON
                  inputs: {}
                  expected: {discount: 100}
                """);

        runner("toppleCatVerify", "--stacktrace").build();

        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "MUTATION"));

        Files.writeString(test, couponSource("99"));
        var failed = runner("toppleCatVerify", "--stacktrace").buildAndFail();

        assertEquals(TaskOutcome.FAILED, failed.task(":toppleCatReport").getOutcome());
        assertEquals(EvidenceVerdict.FAIL, gateVerdict(project, "CONTRACT_INTEGRITY"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "MUTATION"));
        String failedEvidence = Files.readString(project.resolve("build/topplecat/evidence.json"));
        assertTrue(failedEvidence.contains("The contract-integrity gate did not permit downstream verification in this run."),
                failedEvidence);
        assertFalse(failedEvidence.contains("hiddenTest"), failedEvidence);
        assertFalse(failedEvidence.contains("toppleCatVerificationTest"), failedEvidence);
        assertFalse(failedEvidence.contains("toppleCatMutationGate"), failedEvidence);

        Files.writeString(test, couponSource("100"));
        runner("toppleCatVerify", "--stacktrace").build();
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.PASS, gateVerdict(project, "MUTATION"));

        Files.writeString(test, couponSource("99"));
        runner("toppleCatVerify", "--stacktrace").buildAndFail();
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "REVIEWER_JUNIT"));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "MUTATION"));
        assertFalse(Files.exists(project.resolve("src/hiddenTest")));
    }

    @Test
    void failsVerificationWhenAutomaticMutationAttributionFindsASurvivingMutant() throws Exception {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'mutation-consumer'\n");
        Path junit = moduleJar("topplecat-junit");
        Path core = moduleJar("topplecat-core");
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.samzhu.topplecat'
                }
                repositories { mavenCentral() }
                dependencies {
                    testImplementation files('%s', '%s')
                    testImplementation 'org.junit.jupiter:junit-jupiter:6.1.1'
                    testImplementation 'tools.jackson.core:jackson-databind:3.2.0'
                    testImplementation 'tools.jackson.dataformat:jackson-dataformat-yaml:3.2.0'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.1.1'
                }
                toppleCat {
                    adversarial {
                        hiddenRetest { enabled.set(false) }
                        mutation {
                            enabled.set(true)
                            threshold.set(100)
                            producerTask.set('writePitFixture')
                            reportFile.set(layout.buildDirectory.file('pit/mutations.xml'))
                        }
                    }
                }
                tasks.register('writePitFixture') {
                    doLast {
                        def output = layout.buildDirectory.file('pit/mutations.xml').get().asFile
                        output.parentFile.mkdirs()
                        output.text = '''<mutations>
                          <mutation detected="false" status="SURVIVED"><mutatedClass>example.CouponService</mutatedClass>
                            <coveringTests>example.CouponTest.[engine:junit-jupiter]/[class:example.CouponTest]/[test-template:appliesCoupon(io.github.samzhu.topplecat.junit.ToppleCase)]/[test-template-invocation:#1]</coveringTests></mutation>
                        </mutations>'''
                    }
                }
                """.formatted(junit, core));
        writeProductionSource();
        Path test = project.resolve("src/test/java/example/CouponTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, """
                package example;
                import io.github.samzhu.topplecat.junit.ToppleCase;
                import io.github.samzhu.topplecat.junit.ToppleStage;
                import io.github.samzhu.topplecat.junit.ToppleStageField;
                import io.github.samzhu.topplecat.junit.ToppleTest;
                class CouponTest {
                    @ToppleStageField CouponThen then;
                    @ToppleTest("AC-COUPON")
                    void appliesCoupon(ToppleCase c) { then.matches_contract(c); }
                    static final class CouponThen extends ToppleStage<CouponThen> {
                        CouponThen matches_contract(ToppleCase c) {
                            recorded();
                            c.verify("discount", c.input("discount", Integer.class));
                            return self();
                        }
                    }
                }
                """);
        Path cases = project.resolve("src/test/resources/topplecat/cases/coupon.json");
        Files.createDirectories(cases.getParent());
        Files.writeString(cases, """
                [{"caseId":"coupon-public-500","acId":"AC-COUPON",
                  "inputs":{"discount":100},"expected":{"discount":100}}]
                """);

        var result = GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath()
                .withArguments("toppleCatVerify", "--stacktrace").buildAndFail();

        assertTrue(result.task(":toppleCatMutationGate") != null, result.getOutput());
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatMutationGate").getOutcome());
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        String mutation = Files.readString(project.resolve("build/topplecat/mutation-results.json"));
        String evidence = Files.readString(project.resolve("build/topplecat/evidence.json"));
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        assertTrue(mutation.contains("AC-COUPON"), mutation);
        assertTrue(mutation.contains("\"FAIL\""), mutation);
        assertTrue(evidence.contains("\"MUTATION\""), evidence);
        assertFalse(feedback.contains("AC-COUPON"), feedback);
    }

    @Test
    void createsFreshArchivedEvidenceForEachConfigurationCacheVerification() throws Exception {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'configuration-cache-consumer'\n");
        Path junit = moduleJar("topplecat-junit");
        Path core = moduleJar("topplecat-core");
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.samzhu.topplecat'
                }
                repositories { mavenCentral() }
                dependencies {
                    testImplementation files('%s', '%s')
                    testImplementation 'org.junit.jupiter:junit-jupiter:6.1.1'
                    testImplementation 'tools.jackson.core:jackson-databind:3.2.0'
                    testImplementation 'tools.jackson.dataformat:jackson-dataformat-yaml:3.2.0'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.1.1'
                }
                toppleCat {
                    adversarial {
                        mutation { enabled.set(false) }
                    }
                }
                """.formatted(junit, core));
        writeTestSource(couponSource("100"));
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON",
                  "inputs":{},"expected":{"discount":100}}]
                """);
        Path reviewer = project.resolve("src/hiddenTest/java/example/ReviewerTest.java");
        Files.createDirectories(reviewer.getParent());
        Files.writeString(reviewer, """
                package example;
                import org.junit.jupiter.api.Test;
                class ReviewerTest { @Test void reviewerGuard() {} }
                """);

        assertEquals(TaskOutcome.SUCCESS, runner("toppleCatVerify", "--configuration-cache").build()
                .task(":toppleCatReport").getOutcome());
        List<Path> firstRuns = archivedRuns(project);
        assertEquals(1, firstRuns.size());
        String firstRunId = evidenceRunId(firstRuns.getFirst());

        assertEquals(TaskOutcome.SUCCESS, runner("toppleCatVerify", "--configuration-cache").build()
                .task(":toppleCatReport").getOutcome());
        List<Path> secondRuns = archivedRuns(project);
        assertEquals(2, secondRuns.size());
        List<String> runIds = secondRuns.stream().map(ToppleCatPluginFunctionalTest::evidenceRunId).toList();
        assertTrue(runIds.contains(firstRunId));
        assertEquals(2, runIds.stream().distinct().count());
        String stableRunId = ToppleEvidenceJson.read(Files.readString(project.resolve("build/topplecat/evidence.json"))).runId();
        assertTrue(runIds.contains(stableRunId));
        assertNotEquals(firstRunId, stableRunId);

        runner("toppleCatVerify", "--configuration-cache").build();
        runner("toppleCatVerify", "--configuration-cache").build();
        assertEquals(3, archivedRuns(project).size());
        assertFalse(Files.exists(project.resolve("build/topplecat/runs/current")));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate repository root for functional test jars.");
        }
        return current;
    }

    private static Path moduleJar(String module) {
        String projectVersion = System.getProperty("topplecat.project.version");
        if (projectVersion == null || projectVersion.isBlank()) {
            throw new IllegalStateException("Missing topplecat.project.version test system property.");
        }
        return repositoryRoot().resolve(module).resolve("build/libs")
                .resolve(module + "-" + projectVersion + ".jar");
    }

    private static Path currentRun(Path project) throws Exception {
        try (var runs = Files.list(project.resolve("build/topplecat/runs"))) {
            return runs.filter(Files::isDirectory).max(Comparator.comparing(Path::toString)).orElseThrow();
        }
    }

    private static List<Path> archivedRuns(Path project) throws Exception {
        try (var runs = Files.list(project.resolve("build/topplecat/runs"))) {
            return runs.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().equals("current"))
                    .sorted()
                    .toList();
        }
    }

    private static String evidenceRunId(Path run) {
        try {
            return ToppleEvidenceJson.read(Files.readString(run.resolve("evidence.json"))).runId();
        } catch (Exception exception) {
            throw new AssertionError("Cannot read archived ToppleCat evidence from " + run, exception);
        }
    }

    private static EvidenceVerdict gateVerdict(Path project, String name) throws Exception {
        ToppleEvidence evidence = ToppleEvidenceJson.read(Files.readString(project.resolve("build/topplecat/evidence.json")));
        return evidence.gates().stream().filter(gate -> gate.name().equals(name)).findFirst().orElseThrow().verdict();
    }

    private static EvidenceVerdict evidenceVerdict(Path project) throws Exception {
        return ToppleEvidenceJson.read(Files.readString(project.resolve("build/topplecat/evidence.json"))).verdict();
    }

    private void assertMutationIncompleteWithoutPit(org.gradle.testkit.runner.BuildResult result, String reason) throws Exception {
        assertEquals(TaskOutcome.FAILED, result.task(":toppleCatReport").getOutcome());
        assertTrue(result.task(":pitest") == null, result.getOutput());
        assertTrue(result.task(":toppleCatMutationGate") == null, result.getOutput());
        assertEquals(EvidenceVerdict.INCOMPLETE, evidenceVerdict(project));
        assertEquals(EvidenceVerdict.INCOMPLETE, gateVerdict(project, "MUTATION"));
        assertEquals(reason, gateReason(project, "MUTATION"));
        String feedback = Files.readString(project.resolve("build/topplecat/agent-feedback.json"));
        assertTrue(feedback.contains(reason), feedback);
    }

    private static String gateReason(Path project, String name) throws Exception {
        ToppleEvidence evidence = ToppleEvidenceJson.read(Files.readString(project.resolve("build/topplecat/evidence.json")));
        return evidence.gates().stream().filter(gate -> gate.name().equals(name)).findFirst().orElseThrow().reason();
    }

    private static void assertDisabled(Path project, String name, String reason) throws Exception {
        ToppleEvidence evidence = ToppleEvidenceJson.read(Files.readString(project.resolve("build/topplecat/evidence.json")));
        var gate = evidence.gates().stream().filter(candidate -> candidate.name().equals(name)).findFirst().orElseThrow();
        assertEquals(EvidenceVerdict.DISABLED, gate.verdict());
        assertEquals(reason, gate.reason());
    }

    private static String couponSource(String actualDiscount) {
        return """
                package example;
                import io.github.samzhu.topplecat.junit.ToppleCase;
                import io.github.samzhu.topplecat.junit.ToppleStage;
                import io.github.samzhu.topplecat.junit.ToppleStageField;
                import io.github.samzhu.topplecat.junit.ToppleTest;
                class CouponTest {
                    @ToppleStageField CouponThen then;
                    @ToppleTest("AC-CART-COUPON")
                    void appliesCoupon(ToppleCase testCase) {
                        then.matches_contract(testCase);
                    }
                    static final class CouponThen extends ToppleStage<CouponThen> {
                        CouponThen matches_contract(ToppleCase testCase) {
                            recorded();
                            testCase.verify("discount", %s);
                            return self();
                        }
                    }
                }
                """.formatted(actualDiscount);
    }

    private void writeMutationCouponFixture() throws Exception {
        Path production = project.resolve("src/main/java/example/CouponService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package example;
                public final class CouponService {
                    public int discountFor(int subtotal) { return subtotal >= 500 ? 100 : 0; }
                }
                """);
        writeTestSource("""
                package example;
                class CouponTest {
                    @ToppleStageField CouponThen then;
                    @ToppleTest("AC-CART-COUPON")
                    void appliesCoupon(ToppleCase testCase) { then.matches_contract(testCase); }
                    static final class CouponThen extends ToppleStage<CouponThen> {
                        private final CouponService service = new CouponService();
                        CouponThen matches_contract(ToppleCase testCase) {
                            recorded();
                            testCase.verify("discount",
                                    service.discountFor(testCase.input("subtotal", Integer.class)));
                            return self();
                        }
                    }
                }
                """);
        writePublicCase("coupon.json", """
                [{"caseId":"coupon-public-500","acId":"AC-CART-COUPON",
                  "inputs":{"subtotal":500},"expected":{"discount":100}}]
                """);
    }

    private void basicProject() throws Exception {
        verificationProject("");
    }

    private void verificationProject(String configuration) throws Exception {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'verification-consumer'\n");
        Path junit = moduleJar("topplecat-junit");
        Path core = moduleJar("topplecat-core");
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.samzhu.topplecat'
                }
                repositories { mavenCentral() }
                dependencies {
                    testImplementation files('%s', '%s')
                    testImplementation 'org.junit.jupiter:junit-jupiter:6.1.1'
                    testImplementation 'tools.jackson.core:jackson-databind:3.2.0'
                    testImplementation 'tools.jackson.dataformat:jackson-dataformat-yaml:3.2.0'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.1.1'
                }
                %s
                """.formatted(junit, core, configuration));
    }

    private void verificationProjectKotlin(String configuration) throws Exception {
        Files.writeString(project.resolve("settings.gradle.kts"), "rootProject.name = \"verification-consumer-kotlin\"\n");
        Path junit = moduleJar("topplecat-junit");
        Path core = moduleJar("topplecat-core");
        Files.writeString(project.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("io.github.samzhu.topplecat")
                }
                repositories { mavenCentral() }
                dependencies {
                    testImplementation(files("%s", "%s"))
                    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
                    testImplementation("tools.jackson.core:jackson-databind:3.2.0")
                    testImplementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.2.0")
                    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
                }
                %s
                """.formatted(junit, core, configuration));
    }

    private void writeHiddenReviewAsset() throws Exception {
        Path test = project.resolve("src/hiddenTest/java/example/ReviewerTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, """
                package example;
                import org.junit.jupiter.api.Test;
                class ReviewerTest { @Test void reviewerGuard() {} }
                """);
        Path cases = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon-reviewer.yaml");
        Files.createDirectories(cases.getParent());
        Files.writeString(cases, """
                - caseId: coupon-reviewer
                  acId: AC-CART-COUPON
                  inputs: {}
                  expected: {discount: 100}
                """);
    }

    private void writeHiddenRowsOnly(String source) throws Exception {
        Path cases = project.resolve("src/hiddenTest/resources/topplecat/cases/coupon-reviewer.yaml");
        Files.createDirectories(cases.getParent());
        Files.writeString(cases, source);
    }

    private void writeHiddenJavaTest(String className, String source) throws Exception {
        Path test = project.resolve("src/hiddenTest/java/example").resolve(className + ".java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, source);
    }

    private void writeProductionSource() throws Exception {
        Path production = project.resolve("src/main/java/example/CouponService.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package example;
                public final class CouponService {}
                """);
    }

    private void writeReviewerTestOnly() throws Exception {
        Path test = project.resolve("src/hiddenTest/java/example/ReviewerTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, """
                package example;
                import org.junit.jupiter.api.Test;
                class ReviewerTest { @Test void reviewerGuard() {} }
                """);
    }

    private void writeTestSource(String source) throws Exception {
        Path test = project.resolve("src/test/java/example/OrderTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, withToppleImports(source));
    }

    private static String withToppleImports(String source) {
        if (source.contains("io.github.samzhu.topplecat.junit.")) {
            return source;
        }
        String imports = """
                import io.github.samzhu.topplecat.junit.As;
                import io.github.samzhu.topplecat.junit.ExpectedState;
                import io.github.samzhu.topplecat.junit.ProvidedState;
                import io.github.samzhu.topplecat.junit.ToppleAc;
                import io.github.samzhu.topplecat.junit.ToppleCase;
                import io.github.samzhu.topplecat.junit.ToppleStage;
                import io.github.samzhu.topplecat.junit.ToppleStageField;
                import io.github.samzhu.topplecat.junit.ToppleTest;

                """;
        if (source.stripLeading().startsWith("package ")) {
            int packageEnd = source.indexOf(';');
            return source.substring(0, packageEnd + 1) + "\n\n" + imports + source.substring(packageEnd + 1);
        }
        return imports + source;
    }

    private static void copyDirectory(Path source, Path destination) throws Exception {
        try (var paths = Files.walk(source)) {
            paths.sorted().forEach(path -> {
                Path target = destination.resolve(source.relativize(path));
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(path, target);
                    }
                } catch (Exception exception) {
                    throw new RuntimeException("Cannot copy directory " + source + " to " + destination + ": "
                            + exception.getMessage(), exception);
                }
            });
        }
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(target -> {
                try {
                    Files.deleteIfExists(target);
                } catch (Exception exception) {
                    throw new RuntimeException("Cannot delete " + target, exception);
                }
            });
        }
    }

    private void writePublicCase(String name, String source) throws Exception {
        Path cases = project.resolve("src/test/resources/topplecat/cases/").resolve(name);
        Files.createDirectories(cases.getParent());
        Files.writeString(cases, source);
    }

    private Path reviewerStateRoot() {
        try {
            return project.toRealPath().resolve(".topplecat-state");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot resolve canonical reviewer state root for " + project, exception);
        }
    }

    private Path reviewerEscrowRoot() {
        return EscrowService.reviewerStatePath(project, reviewerStateRoot());
    }

    private GradleRunner runner(String... arguments) {
        return runner(project, reviewerStateRoot(), arguments);
    }

    private GradleRunner runner(Path projectRoot, Path stateRoot, String... arguments) {
        String[] runnerArguments = Stream.concat(Stream.of(arguments),
                Stream.of("-Dtopplecat.stateRoot=" + stateRoot.toAbsolutePath())).toArray(String[]::new);
        return GradleRunner.create().withProjectDir(projectRoot.toFile()).withPluginClasspath().withArguments(runnerArguments);
    }

    private static boolean containsStandaloneToken(String source, String token) {
        return java.util.regex.Pattern.compile(
                "(?<![A-Za-z0-9])" + java.util.regex.Pattern.quote(token) + "(?![A-Za-z0-9])")
                .matcher(source).find();
    }
}
