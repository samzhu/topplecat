plugins {
    java
    id("io.github.samzhu.topplecat") version "0.0.8"
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
    testImplementation("io.github.samzhu.topplecat:topplecat-junit:0.0.8")
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
        producerTask.set("writePitFixture")
        reportFile.set(layout.buildDirectory.file("reports/pitest/mutations.xml"))
    }
}

tasks.register("writePitFixture") {
    doLast {
        val report = layout.buildDirectory.file("reports/pitest/mutations.xml").get().asFile
        report.parentFile.mkdirs()
        report.writeText(
            """
            <mutations>
              <mutation detected="false" status="SURVIVED">
                <mutatedClass>integration.mutation.CouponService</mutatedClass>
                <coveringTests>integration.mutation.CouponAcceptanceTest.[engine:junit-jupiter]/[class:integration.mutation.CouponAcceptanceTest]/[test-template:acceptsThePublicCase(io.github.samzhu.topplecat.junit.ToppleCase)]/[test-template-invocation:#1]</coveringTests>
              </mutation>
            </mutations>
            """.trimIndent()
        )
    }
}
