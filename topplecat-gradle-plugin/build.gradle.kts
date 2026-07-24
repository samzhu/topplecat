plugins {
    alias(libs.plugins.java.gradle.plugin)
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Plugin implementation uses these modules internally; its Gradle DSL exposes no module types.
    implementation(project(":topplecat-core"))
    implementation(project(":topplecat-junit"))
    implementation(project(":topplecat-report"))
    implementation("info.solidsoft.pitest:info.solidsoft.pitest.gradle.plugin:1.19.0")
    testImplementation(libs.junit.jupiter)
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
