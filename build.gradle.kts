plugins {
    base
}

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.plugins.signing.SigningExtension

allprojects {
    group = "io.github.samzhu.topplecat"
    version = "0.0.4"
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }
        tasks.withType<Javadoc>().configureEach {
            // Keep publishing the complete Javadoc artifact while suppressing only
            // missing-comment noise; malformed Javadoc and broken references still fail.
            (options as StandardJavadocDocletOptions)
                .addBooleanOption("Xdoclint:all,-missing", true)
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            systemProperty("topplecat.project.version", project.version.toString())
        }
    }
    plugins.withId("maven-publish") {
        val centralRelease = providers.gradleProperty("centralRelease")
                .map(String::toBoolean)
                .orElse(false)
        val publishingExtension = extensions.getByType<PublishingExtension>()
        publishingExtension.apply {
            publications.withType(MavenPublication::class.java).configureEach {
                pom {
                    name.set(project.name)
                    description.set("ToppleCat delegation verification support for Java/JUnit work.")
                    url.set("https://github.com/samzhu/topplecat")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/samzhu/topplecat.git")
                        developerConnection.set("scm:git:ssh://git@github.com/samzhu/topplecat.git")
                        url.set("https://github.com/samzhu/topplecat")
                    }
                    developers {
                        developer {
                            id.set("samzhu")
                            name.set("Sam Zhu")
                            url.set("https://github.com/samzhu")
                        }
                    }
                }
            }
            repositories {
                maven {
                    name = "centralStaging"
                    url = uri(
                        "https://ossrh-staging-api.central.sonatype.com/" +
                                "service/local/staging/deploy/maven2/"
                    )
                    credentials {
                        username = providers.gradleProperty("centralPortalUsername").orNull
                        password = providers.gradleProperty("centralPortalPassword").orNull
                    }
                }
            }
        }

        if (centralRelease.get()) {
            pluginManager.apply("signing")
            val signingExtension = extensions.getByType<SigningExtension>()
            signingExtension.useGpgCmd()
            publishingExtension.publications.configureEach {
                signingExtension.sign(this)
            }
        }

        tasks.matching { it.name.endsWith("ToCentralStagingRepository") }.configureEach {
            doFirst {
                check(centralRelease.get()) {
                    "Central publishing requires -PcentralRelease=true so artifacts are signed."
                }
            }
        }
    }
}

configure(listOf(
        project(":topplecat-core"),
        project(":topplecat-junit"),
        project(":topplecat-report")
)) {
    pluginManager.withPlugin("java-library") {
        pluginManager.apply("maven-publish")
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }
        }
    }
}

tasks.matching { it.name == "publishToMavenLocal" }.configureEach {
    dependsOn(":topplecat-gradle-plugin:publishToMavenLocal")
}
