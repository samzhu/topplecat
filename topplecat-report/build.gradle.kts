plugins {
    alias(libs.plugins.java.library)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Public report projections accept ToppleCaseData values.
    api(project(":topplecat-core"))
    // Public report case records expose JsonNode values.
    api(libs.jackson.databind)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
