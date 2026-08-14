import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
    id("io.github.samzhu.topplecat") version "0.2.2"
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.2.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test {
    useJUnitPlatform()
}

toppleCat {
    mutationTesting {
        enabled.set(true)
    }
}
