package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.AttachmentRef;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic report-scale gate: 100 ACs, 1,000 cases, 5,000 steps, and 100 thumbnails. */
class ReportBundleScaleTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TempDir
    Path tempDir;

    @Test
    void writesTheSpecifiedLargeOfflineVerificationBundleWithinTwoSeconds() throws Exception {
        Map<String, String> titles = new LinkedHashMap<>();
        Map<String, List<StepTemplate>> templates = new LinkedHashMap<>();
        Map<String, ReportViews.CaseExecution> executions = new LinkedHashMap<>();
        List<ToppleCaseData> cases = new ArrayList<>();
        AttachmentRef thumbnail = new AttachmentRef("a".repeat(64), "Checkout screenshot", "image/png", 67,
                CaseVisibility.PUBLIC, "attachments/" + "a".repeat(64) + ".png");
        for (int ac = 0; ac < 100; ac++) {
            String acId = "AC-SCALE-%03d".formatted(ac);
            titles.put(acId, "長標題驗收條件 " + ac + " 這是一個可換行的 CJK 報表標題");
            templates.put(acId, steps(acId));
            for (int row = 0; row < 10; row++) {
                String caseId = acId + "-case-" + row + "-long-unbroken-identifier-for-layout-validation";
                cases.add(new ToppleCaseData(caseId, acId, CaseVisibility.PUBLIC,
                        JSON.readTree("{\"request\":{\"index\":" + row + ",\"nested\":{\"value\":\"x\"}}}"),
                        JSON.readTree("{\"response\":{\"accepted\":true,\"index\":" + row + "}}"), Path.of("scale.json")));
                List<NarrativeStep> stepRuns = new ArrayList<>();
                for (StepTemplate template : templates.get(acId)) {
                    List<AttachmentRef> attachments = row == 0 && template.stepId().endsWith("step2()V")
                            ? List.of(thumbnail) : List.of();
                    stepRuns.add(new NarrativeStep(template.stepId(), template.tokens().getLast().value(), NarrativeStepStatus.PASS,
                            1_000_000, List.of(JSON.readTree("{\"case\":\"" + caseId + "\"}")), attachments, ""));
                }
                executions.put(caseId, new ReportViews.CaseExecution(CaseResultStatus.PASS, null, stepRuns,
                        Map.of("response", "ASSERTED")));
            }
        }

        long started = System.nanoTime();
        VerificationView view = ReportViews.verificationFromTemplates(titles, cases, executions, Map.of(), templates,
                true, List.of(), Instant.parse("2026-07-24T00:00:00Z"));
        Path bundle = tempDir.resolve("verification");
        HtmlBundleWriter.verification(bundle, view);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertEquals(100, view.acceptanceConditions().size());
        assertEquals(1_000, view.acceptanceConditions().stream().mapToInt(ac -> ac.cases().size()).sum());
        assertEquals(5_000, view.acceptanceConditions().stream().flatMap(ac -> ac.cases().stream())
                .mapToInt(row -> row.steps().size()).sum());
        assertEquals(100, view.acceptanceConditions().stream().flatMap(ac -> ac.cases().stream())
                .flatMap(row -> row.steps().stream()).flatMap(step -> step.attachments().stream()).count());
        assertTrue(Files.size(bundle.resolve("data.json")) > 100_000);
        assertTrue(elapsed.compareTo(Duration.ofSeconds(2)) < 0,
                "large report generation took " + elapsed.toMillis() + "ms");
    }

    private static List<StepTemplate> steps(String acId) {
        List<StepTemplate> steps = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            StepPhase phase = index == 0 ? StepPhase.GIVEN : index == 1 ? StepPhase.WHEN : StepPhase.THEN;
            steps.add(new StepTemplate(acId + "#step" + index + "()V", phase,
                    List.of(new StepToken(StepTokenKind.PHASE, phase.name()),
                            new StepToken(StepTokenKind.LITERAL, "步驟 " + index)), List.of(),
                    new SourceRef("ScaleFixture.java", index + 1, 1)));
        }
        return steps;
    }
}
