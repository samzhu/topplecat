import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

val consumerJdk = providers.gradleProperty("topplecat.consumerJdk").map(String::toInt).orElse(25)
val consumerRelease = providers.gradleProperty("topplecat.consumerRelease").map(String::toInt).orElse(17)

plugins {
    java
    id("io.github.samzhu.topplecat") version "0.2.2"
}

java {
    toolchain {
        languageVersion.set(consumerJdk.map(JavaLanguageVersion::of))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(consumerRelease)
}

configurations.configureEach {
    if (isCanBeResolved) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, consumerJdk.get())
        }
    }
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
        // Keep the tutorial fast.
        enabled.set(false)
    }
}
