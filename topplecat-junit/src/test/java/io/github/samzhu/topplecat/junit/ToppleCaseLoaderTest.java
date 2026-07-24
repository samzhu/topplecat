package io.github.samzhu.topplecat.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToppleCaseLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void includesReviewerCasesOnlyWhenVerificationEnablesThem() throws Exception {
        Path publicCases = tempDir.resolve("public.json");
        Path hiddenCases = tempDir.resolve("hidden.yaml");
        Files.writeString(publicCases, """
                [{"caseId":"coupon-public","acId":"AC-CART-COUPON","inputs":{"subtotal":500},"expected":{"discount":100}}]
                """);
        Files.writeString(hiddenCases, """
                - caseId: coupon-hidden
                  acId: AC-CART-COUPON
                  inputs: {subtotal: 800}
                  expected: {discount: 100}
                """);

        try (Properties ignored = Properties.set(publicCases, hiddenCases, false)) {
            assertEquals(List.of("coupon-public"), ToppleCaseLoader.load("AC-CART-COUPON").stream()
                    .map(ToppleCase::caseId).toList());
        }
        try (Properties ignored = Properties.set(publicCases, hiddenCases, true)) {
            assertEquals(List.of("coupon-hidden", "coupon-public"), ToppleCaseLoader.load("AC-CART-COUPON").stream()
                    .map(ToppleCase::caseId).toList());
        }
    }

    private static final class Properties implements AutoCloseable {
        private final Map<String, String> prior;

        private Properties(Map<String, String> prior) {
            this.prior = prior;
        }

        static Properties set(Path publicCases, Path hiddenCases, boolean includeHidden) {
            List<String> keys = List.of(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY,
                    ToppleJunit.HIDDEN_CASE_SOURCES_PROPERTY, ToppleJunit.INCLUDE_HIDDEN_CASES_PROPERTY);
            Map<String, String> prior = new LinkedHashMap<>();
            keys.forEach(key -> prior.put(key, System.getProperty(key)));
            System.setProperty(ToppleJunit.PUBLIC_CASE_SOURCES_PROPERTY, publicCases.toString());
            System.setProperty(ToppleJunit.HIDDEN_CASE_SOURCES_PROPERTY, hiddenCases.toString());
            System.setProperty(ToppleJunit.INCLUDE_HIDDEN_CASES_PROPERTY, Boolean.toString(includeHidden));
            return new Properties(prior);
        }

        @Override
        public void close() {
            prior.forEach((key, value) -> {
                if (value == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, value);
                }
            });
        }
    }
}
