package com.axguard.gradle.plugin

import com.android.build.api.dsl.LibraryExtension
import com.axguard.gradle.config.AxGuardBuild
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin holding the shared AxGuard Android-library configuration —
 * published coordinates, SDK/NDK levels, the BuildConfig/obfuscation-key wiring,
 * and Java compatibility — so each library module applies one plugin instead of
 * repeating it.
 *
 * It configures the already-applied `com.android.library` extension; the module
 * still declares the Android/Kotlin plugins itself and keeps its module-specific
 * bits (namespace, native build path, publishing, dependencies).
 */
class AxGuardLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.group = AxGuardBuild.GROUP
        project.version = AxGuardBuild.VERSION

        project.pluginManager.withPlugin("com.android.library") {
            project.extensions.configure(LibraryExtension::class.java) { ext ->
                ext.compileSdk = AxGuardBuild.COMPILE_SDK
                ext.ndkVersion = AxGuardBuild.NDK_VERSION

                ext.defaultConfig.minSdk = AxGuardBuild.MIN_SDK

                // BuildConfig gates AxLog; OBFS_KEY is the XOR key shared with native.
                ext.buildFeatures.buildConfig = true
                ext.defaultConfig.buildConfigField(
                    type = "int",
                    name = "OBFS_KEY",
                    value = AxGuardBuild.obfsKeyHex
                )
                ext.defaultConfig.externalNativeBuild.cmake.arguments.add(
                    "-DAXGUARD_OBFS_KEY=${AxGuardBuild.obfsKeyHex}"
                )

                val java = JavaVersion.toVersion(AxGuardBuild.JAVA_VERSION)
                ext.compileOptions.sourceCompatibility = java
                ext.compileOptions.targetCompatibility = java
            }
        }
    }
}
