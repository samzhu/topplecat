plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.13"
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
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.0.13")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test {
    useJUnitPlatform()
}

toppleCat {
    mutationTesting {
        enabled.set(true)
        threshold.set(100)
    }
}
