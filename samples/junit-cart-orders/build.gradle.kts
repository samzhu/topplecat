plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.5"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.0.5")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")
}

tasks.test {
    useJUnitPlatform()
}

toppleCat {
    adversarial {
        mutation {
            // Keep the tutorial fast.
            enabled.set(false)
        }
    }
}
