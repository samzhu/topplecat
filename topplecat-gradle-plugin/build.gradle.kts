plugins {
    alias(libs.plugins.java.gradle.plugin)
    `maven-publish`
}

dependencies {
    // Plugin implementation uses these modules internally; its Gradle DSL exposes no module types.
    implementation(project(":topplecat-core"))
    implementation(project(":topplecat-junit"))
    implementation(project(":topplecat-report"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.byte.buddy)
    testImplementation(gradleTestKit())
    testRuntimeOnly(libs.junit.platform.launcher)
}

gradlePlugin {
    plugins {
        create("topplecat") {
            id = "io.github.samzhu.topplecat"
            implementationClass = "io.github.samzhu.topplecat.gradle.ToppleCatPlugin"
            displayName = "ToppleCat"
            description = "Delegation verification gate for Java/JUnit work."
            tags = listOf("verification", "junit", "testing", "ai", "java")
        }
    }
}
