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

// Gradle 9.0 compatibility: Enable stable configuration cache
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "OCR App"
include(":app")
