package io.github.samzhu.topplecat.junit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToppleAcExtensionTest {
    private static final String FIXTURE_RUN = "topplecat.fixtureRun";

    @TempDir
    Path tempDir;

    @Test
    void failsAOtherwiseSuccessfulTestWhenExpectedValueWasNotVerified() throws Exception {
        Path cases = tempDir.resolve("cases.json");
        Files.writeString(cases, """
                [{"caseId":"coupon-public-500","acId":"AC-CART-COUPON",
                  "inputs":{"subtotal":500},"expected":{"discount":100}}]
                """);
        String previous = System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
        try {
            System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, cases.toString());
            SummaryGeneratingListener summary = new SummaryGeneratingListener();
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(HollowFixture.class))
                    .configurationParameter(FIXTURE_RUN, "true")
                    .build();
            Launcher launcher = LauncherFactory.create();
            launcher.execute(request, summary);

            assertEquals(1, summary.getSummary().getTestsFailedCount());
            assertEquals(true, summary.getSummary().getFailures().getFirst().getException().getMessage()
                    .contains("expected.discount was declared by AC-CART-COUPON but never verified"));
        } finally {
            if (previous == null) {
                System.clearProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
            } else {
                System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, previous);
            }
        }
    }

    @Test
    void failsAReadOnlyExpectedValueWhenEnforcementIsEnabled() throws Exception {
        Path cases = tempDir.resolve("cases.json");
        Files.writeString(cases, """
                [{"caseId":"coupon-public-500","acId":"AC-CART-COUPON",
                  "inputs":{"subtotal":500},"expected":{"discount":100}}]
                """);
        String previous = System.getProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
        try {
            System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, cases.toString());
            SummaryGeneratingListener summary = new SummaryGeneratingListener();
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(ReadOnlyFixture.class))
                    .configurationParameter(FIXTURE_RUN, "true")
                    .build();
            Launcher launcher = LauncherFactory.create();
            launcher.execute(request, summary);

            assertEquals(1, summary.getSummary().getTestsFailedCount());
            String message = summary.getSummary().getFailures().getFirst().getException().getMessage();
            assertEquals(true, message.contains("expected.discount"));
            assertEquals(true, message.contains("Call c.verify(\"discount\", actual)."));
        } finally {
            if (previous == null) {
                System.clearProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY);
            } else {
                System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, previous);
            }
        }
    }

    @Test
    void resolvesHumanTitlesInDeclaredOrder() throws Exception {
        Method display = TitleFixture.class.getDeclaredMethod("displayTitle");
        Method ac = TitleFixture.class.getDeclaredMethod("acTitle");
        Method derived = TitleFixture.class.getDeclaredMethod("derivedFromMethodName");

        assertEquals("Readable title", ToppleTitleResolver.title(display));
        assertEquals("Fallback title", ToppleTitleResolver.title(ac));
        assertEquals("derived From Method Name", ToppleTitleResolver.title(derived));
    }

    static final class FixtureOnlyCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return context.getConfigurationParameter(FIXTURE_RUN).filter("true"::equals)
                    .map(value -> ConditionEvaluationResult.enabled("launcher fixture"))
                    .orElseGet(() -> ConditionEvaluationResult.disabled("nested fixture"));
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class HollowFixture {
        @ToppleTest("AC-CART-COUPON")
        void acceptsTheCaseButNeverVerifiesIt(ToppleCase ignored) {
        }
    }

    @ExtendWith(FixtureOnlyCondition.class)
    static final class ReadOnlyFixture {
        @ToppleTest("AC-CART-COUPON")
        void readsExpectedButNeverVerifiesIt(ToppleCase c) {
            c.expected("discount", Integer.class);
        }
    }

    static final class TitleFixture {
        @Test
        @DisplayName("Readable title")
        @ToppleAc(value = "AC-ONE", title = "Ignored fallback")
        void displayTitle() {
        }

        @Test
        @ToppleAc(value = "AC-TWO", title = "Fallback title")
        void acTitle() {
        }

        @Test
        @ToppleAc("AC-THREE")
        void derivedFromMethodName() {
        }
    }
}
