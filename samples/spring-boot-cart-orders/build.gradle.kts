plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.github.samzhu.topplecat") version "0.0.2"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:4.1.0")
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.0.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.1.0")
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
