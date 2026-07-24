plugins {
    alias(libs.plugins.java.library)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // ToppleCase exposes ToppleCaseData in its public constructor.
    api(project(":topplecat-core"))
    // ToppleCase exposes JsonNode and TypeReference in its public API.
    api(libs.jackson.databind)
    // Consumers compile composed JUnit annotations and extension types from this module.
    api(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    // Test fixtures directly compile against the launcher; it is never a main dependency.
    testCompileOnly(libs.junit.platform.launcher)
    // The launcher is needed only to execute this module's own tests.
    testRuntimeOnly(libs.junit.platform.launcher)
}
