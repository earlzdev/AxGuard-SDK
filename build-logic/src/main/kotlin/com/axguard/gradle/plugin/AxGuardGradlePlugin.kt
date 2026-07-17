package com.axguard.gradle.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.axguard.gradle.config.AxGuardBuild
import com.axguard.gradle.dsl.AxGuardExtension
import com.axguard.gradle.tasks.AxGuardCertFingerprintTask
import com.axguard.gradle.tasks.AxGuardConfigurationTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applied to a consumer's Android application module. Pulls in the AxGuard SDK,
 * resolves the signing-certificate fingerprint (see [AxGuardExtension] for the
 * resolution order) and injects it, obfuscated, as a `<meta-data>` element in the
 * merged manifest that the prebuilt SDK reads at runtime.
 */
class AxGuardGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("axguard", AxGuardExtension::class.java).apply {
            // gradle.properties / -P value is the default; explicit DSL overrides it.
            certFingerprint.convention(
                project.providers.gradleProperty("axguard.certFingerprint")
            )
            dexIntegrity.convention(
                project.providers.gradleProperty("axguard.dexIntegrity")
                    .map { it.toBoolean() }
                    .orElse(false)
            )
        }

        project.pluginManager.withPlugin("com.android.application") {
            project.dependencies.add(
                /* configurationName = */ "implementation",
                /* dependencyNotation = */ "${AxGuardBuild.GROUP}:axguard-sdk:${AxGuardBuild.VERSION}",
            )

            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

            project.tasks.register(
                "axguardCertFingerprint",
                AxGuardCertFingerprintTask::class.java,
            ) { task ->
                // -Paxguard.keystore=<path> -Paxguard.storepass=<pw>
                task.keystorePath.set(
                    project.providers.gradleProperty("axguard.keystore").map { project.file(it).absolutePath }
                )
                task.storePassword.set(project.providers.gradleProperty("axguard.storepass"))
            }

            androidComponents.onVariants { variant ->
                val capitalized = variant.name.replaceFirstChar { it.uppercase() }
                val task = project.tasks.register(
                    "inject${capitalized}AxguardFingerprint",
                    AxGuardConfigurationTask::class.java,
                ) { it.fingerprint.set(extension.certFingerprint) }

                variant.artifacts.use(task)
                    .wiredWithFiles(
                        taskInput = AxGuardConfigurationTask::mergedManifest,
                        taskOutput = AxGuardConfigurationTask::updatedManifest,
                    )
                    .toTransform(SingleArtifact.MERGED_MANIFEST)

                if (extension.dexIntegrity.getOrElse(false)) {
                    wireDexIntegrity(project, variant, capitalized)
                }
            }
        }
    }
}
