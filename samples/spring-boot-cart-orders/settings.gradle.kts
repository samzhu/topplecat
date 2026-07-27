pluginManagement {
    repositories {
        if (gradle.startParameter.projectProperties["topplecat.useMavenLocal"] == "true") {
            mavenLocal()
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        if (gradle.startParameter.projectProperties["topplecat.useMavenLocal"] == "true") {
            mavenLocal()
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "topplecat-spring-boot-cart-orders"
