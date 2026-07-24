plugins {
    alias(libs.plugins.java.library)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // JsonNode appears in the public case and evidence models.
    api(libs.jackson.databind)
    // YAML parsing is an internal reader implementation detail.
    implementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
