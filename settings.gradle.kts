pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // incubating warning bisa diabaikan
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "birthday_reminder"
include(":app")
