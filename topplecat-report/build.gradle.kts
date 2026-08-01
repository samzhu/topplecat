plugins {
    alias(libs.plugins.java.library)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Reviewer report projections accept ToppleCaseData values.
    api(project(":topplecat-core"))
    // Reviewer report case records expose JsonNode values.
    api(libs.jackson.databind)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.htmlunit)
    testRuntimeOnly(libs.junit.platform.launcher)
}
