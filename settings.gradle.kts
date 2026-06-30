pluginManagement {
    // The consumer-facing plugin is applied from source via this composite build.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Resolves the axguard-sdk AAR published via publishToMavenLocal.
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "AxGuardSDK"
include(":demo-app")
include(":axguard-sdk")
