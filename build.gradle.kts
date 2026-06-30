// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publishes the SDK AAR and the Gradle plugin to Maven Central."
    dependsOn(":axguard-sdk:publishToMavenCentral")
    dependsOn(gradle.includedBuild("build-logic").task(":publishToMavenCentral"))
}
