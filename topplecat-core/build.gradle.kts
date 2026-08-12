plugins {
    alias(libs.plugins.java.library)
}

dependencies {
    // JsonNode appears in the public case and evidence models.
    api(libs.jackson.databind)
    // YAML parsing is an internal reader implementation detail.
    implementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.junit.jupiter)
    testImplementation("org.pitest:pitest:1.25.5")
    testRuntimeOnly(libs.junit.platform.launcher)
}
