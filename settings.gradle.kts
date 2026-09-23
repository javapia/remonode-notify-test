// No Gradle wrapper is committed here on purpose: the only binary this repo would carry is
// gradle-wrapper.jar, and CI provides a pinned Gradle instead (gradle/actions/setup-gradle).
// To build locally, install Gradle 8.7+ and run `gradle :app:assembleRelease`.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "remonode-notify-test"
include(":app")
