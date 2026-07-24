pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "topplecat"

include(
    "topplecat-core",
    "topplecat-junit",
    "topplecat-report",
    "topplecat-gradle-plugin"
)
