plugins {
    base
}

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.JavaVersion
import org.gradle.plugins.signing.SigningExtension

allprojects {
    group = "io.github.samzhu.topplecat"
    version = "0.2.1"
}

subprojects {
    plugins.withId("java") {
        val buildJdk = providers.gradleProperty("topplecat.buildJdk")
                .map(String::toInt)
                .orElse(25)
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(buildJdk.map(JavaLanguageVersion::of))
            }
            // The outgoing JVM attribute is part of the publication contract. The
            // compiler's --release flag below remains the bytecode/API authority.
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
            withSourcesJar()
            withJavadocJar()
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(21)
        }
        tasks.withType<Javadoc>().configureEach {
            // Keep the complete Javadoc artifact while suppressing only missing-comment
            // noise. Malformed Javadoc and broken references still fail; the doclet
            // options must remain valid on both JDK 21 and JDK 25.
            val javadocOptions = options as StandardJavadocDocletOptions
            javadocOptions.addBooleanOption("Xdoclint:all,-missing", true)
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
