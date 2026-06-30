package com.axguard.gradle.publishing

import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin that publishes an AxGuard Android library to Maven Central via the
 * Vanniktech maven-publish plugin: release AAR + sources + javadoc jars, signed, with the
 * shared POM metadata. Coordinates are inherited from the project (group/version set by
 * [com.axguard.gradle.plugin.AxGuardLibraryPlugin]; the module name is the artifactId).
 */
class AxGuardPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("com.vanniktech.maven.publish")

        project.pluginManager.withPlugin("com.android.library") {
            val publishing = project.extensions.getByType(MavenPublishBaseExtension::class.java)

            publishing.publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
            if (project.providers.gradleProperty("signingInMemoryKey").isPresent) {
                publishing.signAllPublications()
            }
            publishing.configure(
                AndroidSingleVariantLibrary(
                    variant = "release",
                    sourcesJar = true,
                    publishJavadocJar = true,
                )
            )
            publishing.pom { pom ->
                pom.name.set("AxGuard SDK")
                pom.description.set(
                    "Android security SDK: on-device integrity, root, hook, debugger, " +
                        "SELinux, emulator and related runtime checks."
                )
                pom.inceptionYear.set("2026")
                pom.url.set("https://github.com/earlzdev/AxGuardSDK")
                pom.licenses { spec ->
                    spec.license { license ->
                        license.name.set("The Apache License, Version 2.0")
                        license.url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                pom.developers { spec ->
                    spec.developer { dev ->
                        dev.id.set("earlzdev")
                        dev.name.set("Ilya Saushin")
                        dev.email.set("esinilyadev@gmail.com")
                    }
                }
                pom.scm { scm ->
                    scm.url.set("https://github.com/earlzdev/AxGuardSDK")
                    scm.connection.set("scm:git:git://github.com/earlzdev/AxGuardSDK.git")
                    scm.developerConnection.set("scm:git:ssh://git@github.com/earlzdev/AxGuardSDK.git")
                }
            }
        }
    }
}
