import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish.vanniktech)
}

group = "io.github.earlzdev"

// AxGuardBuild.VERSION is the single source of truth for the version. The build
// script can't reference that not-yet-compiled constant, so read it from source
// here — this keeps the published plugin version in lockstep with the value the
// plugins stamp onto the SDK and its dependency coordinate.
version = Regex("\\bVERSION\\s*=\\s*\"([^\"]+)\"")
    .find(file("src/main/kotlin/com/axguard/gradle/config/AxGuardBuild.kt").readText())
    ?.groupValues?.get(1)
    ?: error("Could not read AxGuardBuild.VERSION from source")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    compileOnly(libs.android.gradle.api)
    implementation(libs.vanniktech.maven.publish)
}

gradlePlugin {
    plugins {
        create("axguard") {
            id = "io.github.earlzdev.axguard"
            implementationClass = "com.axguard.gradle.plugin.AxGuardGradlePlugin"
            displayName = "AxGuard SDK configuration"
            description = "Configures the AxGuard SDK for the consuming application."
        }
        create("axguardLibrary") {
            id = "com.axguard.library"
            implementationClass = "com.axguard.gradle.plugin.AxGuardLibraryPlugin"
            displayName = "AxGuard library conventions"
            description = "Shared Android-library build configuration for AxGuard modules."
        }
        create("axguardPublish") {
            id = "com.axguard.publish"
            implementationClass = "com.axguard.gradle.publishing.AxGuardPublishPlugin"
            displayName = "AxGuard library publishing"
            description = "Publishes an AxGuard Android library to Maven Central with sources and javadoc."
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    coordinates("io.github.earlzdev", "axguard-gradle-plugin", version.toString())
    configure(GradlePlugin(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    pom {
        name.set("AxGuard Gradle Plugin")
        description.set(
            "Gradle plugin that wires the AxGuard SDK into a consumer application and " +
                "injects the signing-certificate fingerprint into the merged manifest."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/earlzdev/AxGuardSDK")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("earlzdev")
                name.set("Ilya Saushin")
                email.set("esinilyadev@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/earlzdev/AxGuardSDK")
            connection.set("scm:git:git://github.com/earlzdev/AxGuardSDK.git")
            developerConnection.set("scm:git:ssh://git@github.com/earlzdev/AxGuardSDK.git")
        }
    }
}

val internalMarkerPublications = setOf(
    "axguardLibraryPluginMarkerMaven",
    "axguardPublishPluginMarkerMaven",
)
tasks.withType<AbstractPublishToMaven>().configureEach {
    onlyIf { task ->
        (task as AbstractPublishToMaven)
            .publication.name !in internalMarkerPublications
    }
}
